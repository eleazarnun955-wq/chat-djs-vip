package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

data class ImportedProjectFile(val path: String, val content: String)
data class ProjectZipImportResult(val files: List<ImportedProjectFile>, val error: String = "") {
    val success: Boolean get() = files.isNotEmpty() && error.isBlank()
}

object ProjectZipPathPolicy {
    fun safePath(path: String): String? {
        if (path.isBlank() || path.contains('\u0000')) return null
        val normalized = path.replace('\\\\', '/')
        if (normalized.startsWith('/') || normalized.contains(':')) return null
        val parts = normalized.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null
        val safe = parts.map { part ->
            part.replace(Regex("[^A-Za-z0-9._-]"), "_")
        }
        if (safe.any { it.isBlank() || it == "." || it == ".." }) return null
        return safe.joinToString("/")
    }

    fun isTextFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in setOf(
            "kt", "java", "xml", "gradle", "kts", "properties", "json", "md", "txt",
            "html", "css", "js", "jsx", "ts", "tsx", "py", "sh", "yml", "yaml", "toml",
            "sql", "dart", "swift", "c", "h", "cpp", "hpp", "cs", "go", "rs", "php", "vue"
        ) || !path.contains('.')
    }
}

object ProjectZipImporter {
    private const val MAX_ENTRIES = 200
    private const val MAX_IMPORTED_FILES = 100
    private const val MAX_ENTRY_BYTES = 1024 * 1024
    private const val MAX_TEXT_FILE_BYTES = 512 * 1024
    private const val MAX_TOTAL_BYTES = 5 * 1024 * 1024

    fun import(context: Context, uri: Uri): ProjectZipImportResult = runCatching {
        val imported = mutableListOf<ImportedProjectFile>()
        val seenPaths = mutableSetOf<String>()
        var totalBytes = 0
        var entryCount = 0
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        entryCount++
                        require(entryCount <= MAX_ENTRIES) { "El ZIP supera el límite de 200 archivos." }
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var fileBytes = 0
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            if (read == 0) continue
                            fileBytes += read
                            totalBytes += read
                            require(fileBytes <= MAX_ENTRY_BYTES) { "Un archivo del ZIP supera el límite de 1 MB." }
                            require(totalBytes <= MAX_TOTAL_BYTES) { "El ZIP supera el límite de 5 MB descomprimidos." }
                            output.write(buffer, 0, read)
                        }
                        val safePath = ProjectZipPathPolicy.safePath(entry.name)
                        if (safePath != null && ProjectZipPathPolicy.isTextFile(safePath)) {
                            require(fileBytes <= MAX_TEXT_FILE_BYTES) {
                                "Un archivo de texto supera el límite de 512 KB: $safePath"
                            }
                            require(seenPaths.add(safePath.lowercase())) {
                                "El ZIP contiene rutas duplicadas: $safePath"
                            }
                            val text = output.toByteArray().toString(Charsets.UTF_8)
                            if (!text.contains('\u0000')) {
                                imported.add(ImportedProjectFile(safePath, text))
                                require(imported.size <= MAX_IMPORTED_FILES) {
                                    "El proyecto supera el límite de 100 archivos de texto."
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }
        } ?: error("No se pudo abrir el archivo ZIP.")
        require(imported.isNotEmpty()) { "No se encontraron archivos de texto compatibles en el ZIP." }
        imported
    }.fold(
        onSuccess = { ProjectZipImportResult(it) },
        onFailure = { ProjectZipImportResult(emptyList(), it.message ?: "No se pudo importar el ZIP.") }
    )
}
