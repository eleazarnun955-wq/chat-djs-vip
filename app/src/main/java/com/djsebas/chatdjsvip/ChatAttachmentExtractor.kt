package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream
import java.util.concurrent.Executors
import javax.xml.parsers.DocumentBuilderFactory

/** Extrae texto localmente de documentos seleccionados explícitamente por la persona. */
object ChatAttachmentExtractor {
    private const val MAX_TEXT_CHARS = 120_000
    private const val MAX_PDF_PAGES = 25
    private const val MAX_ARCHIVE_BYTES = 20 * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 5 * 1024 * 1024
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun extract(context: Context, uri: Uri, callback: (Result<String>) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val result = runCatching {
                val mime = appContext.contentResolver.getType(uri).orEmpty().lowercase()
                val name = appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else "" }.orEmpty()
                val extension = name.substringAfterLast('.', "").lowercase()
                val raw = when {
                    mime == "application/pdf" || extension == "pdf" -> extractPdf(appContext, uri)
                    mime.startsWith("text/") || mime in setOf("application/json", "application/xml", "text/xml") ||
                        extension in setOf("txt", "md", "kt", "java", "py", "js", "ts", "html", "css", "json", "xml", "csv", "log", "gradle", "kts") ->
                        appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                            ?: error("No se pudo leer el archivo.")
                    mime.contains("wordprocessingml") || extension == "docx" ->
                        extractOpenXml(appContext, uri, "docx")
                    mime.contains("spreadsheetml") || extension == "xlsx" ->
                        extractOpenXml(appContext, uri, "xlsx")
                    mime.contains("presentationml") || extension == "pptx" ->
                        extractOpenXml(appContext, uri, "pptx")
                    else -> error("Formato no compatible. Adjunta TXT, CSV, código, JSON, XML, PDF, DOCX, XLSX o PPTX.")
                }
                val cleaned = raw.trim()
                require(cleaned.isNotEmpty()) { "El documento no contiene texto extraíble." }
                limitAttachmentText(cleaned)
            }
            mainHandler.post { callback(result) }
        }
    }

    private fun extractPdf(context: Context, uri: Uri): String {
        PDFBoxResourceLoader.init(context)
        val input = context.contentResolver.openInputStream(uri) ?: error("No se pudo abrir el PDF.")
        return input.use { stream ->
            PDDocument.load(stream).use { document ->
                require(!document.isEncrypted) { "El PDF está protegido o cifrado y no se puede leer." }
                require(document.numberOfPages > 0) { "El PDF no contiene páginas." }
                val stripper = PDFTextStripper().apply {
                    startPage = 1
                    endPage = minOf(document.numberOfPages, MAX_PDF_PAGES)
                    sortByPosition = true
                }
                stripper.getText(document) + if (document.numberOfPages > MAX_PDF_PAGES)
                    "\n\n[Se analizaron las primeras $MAX_PDF_PAGES de ${document.numberOfPages} páginas.]" else ""
            }
        }
    }

    private fun extractOpenXml(context: Context, uri: Uri, extension: String): String {
        val entries = linkedMapOf<String, ByteArray>()
        var total = 0
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory && (entry.name == "word/document.xml" ||
                        entry.name == "xl/sharedStrings.xml" || entry.name.startsWith("xl/worksheets/sheet") && entry.name.endsWith(".xml") ||
                        entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml"))) {
                        val output = java.io.ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var entrySize = 0
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            entrySize += count
                            total += count
                            require(entrySize <= MAX_ENTRY_BYTES && total <= MAX_ARCHIVE_BYTES) {
                                "El documento contiene secciones demasiado grandes para procesarlas con seguridad."
                            }
                            output.write(buffer, 0, count)
                        }
                        entries[entry.name] = output.toByteArray()
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("No se pudo abrir el documento.")
        return extractOfficeOpenXmlText(entries, extension)
    }
}

internal fun extractOfficeOpenXmlText(entries: Map<String, ByteArray>, extension: String): String {
    fun xml(path: String): org.w3c.dom.Document? {
        val bytes = entries[path] ?: return null
        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }
        return factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    }
    fun textNodes(document: org.w3c.dom.Document): List<String> {
        // Algunos parsers JVM no resuelven de forma uniforme el comodín de namespace
        // usado por OOXML. Recorrer elementos por localName funciona para w:t, a:t y x:t.
        val nodes = document.getElementsByTagName("*")
        return (0 until nodes.length).mapNotNull { index ->
            val node = nodes.item(index)
            val element = node as? org.w3c.dom.Element ?: return@mapNotNull null
            val local = element.localName ?: element.nodeName.substringAfter(':')
            if (local == "t") element.textContent?.takeIf(String::isNotBlank) else null
        }
    }
    return when (extension.lowercase()) {
        "docx" -> textNodes(xml("word/document.xml") ?: error("El DOCX no contiene el documento principal.")).joinToString(" ")
        "pptx" -> entries.keys.filter { it.matches(Regex("ppt/slides/slide[0-9]+\\.xml")) }
            .sortedBy { it.substringAfter("slide").substringBefore(".").toIntOrNull() ?: Int.MAX_VALUE }
            .mapNotNull { xml(it) }.mapIndexed { index, doc -> "Diapositiva ${index + 1}: " + textNodes(doc).joinToString(" ") }
            .joinToString("\n")
        "xlsx" -> {
            val shared = xml("xl/sharedStrings.xml")?.let(::textNodes).orEmpty()
            val sheets = entries.keys.filter { it.matches(Regex("xl/worksheets/sheet[0-9]+\\.xml")) }
                .sortedBy { it.substringAfter("sheet").substringBefore(".").toIntOrNull() ?: Int.MAX_VALUE }
            sheets.mapIndexed { sheetIndex, path ->
                val doc = xml(path) ?: return@mapIndexed ""
                val cells = doc.getElementsByTagNameNS("*", "c")
                val rows = mutableListOf<String>()
                for (i in 0 until cells.length) {
                    val cell = cells.item(i) as? org.w3c.dom.Element ?: continue
                    val valueNode = cell.getElementsByTagNameNS("*", "v").item(0)
                    val inline = cell.getElementsByTagNameNS("*", "is").item(0) as? org.w3c.dom.Element
                    val inlineText = inline?.getElementsByTagNameNS("*", "t")
                    val inlineValue = if (inlineText == null) "" else (0 until inlineText.length)
                        .mapNotNull { inlineText.item(it)?.textContent }.joinToString("")
                    val value = when (cell.getAttribute("t")) {
                        "s" -> shared.getOrNull(valueNode?.textContent?.toIntOrNull() ?: -1).orEmpty()
                        "inlineStr" -> inlineValue
                        else -> valueNode?.textContent.orEmpty()
                    }
                    if (value.isNotBlank()) rows += "${cell.getAttribute("r")}: $value"
                }
                "Hoja ${sheetIndex + 1}:\n" + rows.joinToString("\n")
            }.filter { it.isNotBlank() }.joinToString("\n\n")
        }
        else -> error("Formato Office no compatible.")
    }
}

internal fun limitAttachmentText(text: String): String =
    if (text.length <= 120_000) text else text.take(120_000) + "\n\n[Contenido recortado por el límite de 120 KB de texto.]"
