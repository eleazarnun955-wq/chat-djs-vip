package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.concurrent.Executors

/** Extrae texto de documentos seleccionados explícitamente por la persona. */
object ChatAttachmentExtractor {
    private const val MAX_TEXT_CHARS = 120_000
    private const val MAX_PDF_PAGES = 25
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun extract(context: Context, uri: Uri, callback: (Result<String>) -> Unit) {
        val appContext = context.applicationContext
        executor.execute {
            val result = runCatching {
                val mime = appContext.contentResolver.getType(uri).orEmpty().lowercase()
                val raw = when {
                    mime == "application/pdf" -> extractPdf(appContext, uri)
                    mime.startsWith("text/") || mime in setOf(
                        "application/json", "application/xml", "text/xml"
                    ) -> appContext.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    } ?: error("No se pudo leer el archivo.")
                    else -> error("Formato no compatible. Adjunta texto, código, JSON, XML o PDF.")
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
        val input = context.contentResolver.openInputStream(uri)
            ?: error("No se pudo abrir el PDF.")
        input.use { stream ->
            PDDocument.load(stream).use { document ->
                require(!document.isEncrypted) { "El PDF está protegido o cifrado y no se puede leer." }
                require(document.numberOfPages > 0) { "El PDF no contiene páginas." }
                val stripper = PDFTextStripper().apply {
                    startPage = 1
                    endPage = minOf(document.numberOfPages, MAX_PDF_PAGES)
                    sortByPosition = true
                }
                val extracted = stripper.getText(document)
                val pageNote = if (document.numberOfPages > MAX_PDF_PAGES) {
                    "\n\n[Se analizaron las primeras $MAX_PDF_PAGES de ${document.numberOfPages} páginas.]"
                } else ""
                extracted + pageNote
            }
        }
    }
}

internal fun limitAttachmentText(text: String): String =
    if (text.length <= 120_000) text else text.take(120_000) + "\n\n[Contenido recortado por el límite de 120 KB de texto.]"
