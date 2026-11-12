package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import java.util.zip.ZipInputStream

data class ImportedProjectFile(val path: String, val content: String)
data class ProjectZipImportResult(val files: List<ImportedProjectFile>, val error: String = "") {
    val success: Boolean get() = files.isNotEmpty() && error.isBlank()
}

object ProjectZipImporter {
    private const val MAX_FILES = 100
    private const val MAX_FILE_BYTES = 512 * 1024
    private const val MAX_TOTAL_BYTES = 5 * 1024 * 1024

    fun import(context: Context, uri: Uri): ProjectZipImportResult = runCatching {
        val imported = mutableListOf<ImportedProjectFile>()
        var totalBytes = 0
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        val path = safePath(entry.name)
                        if (path != null && imported.size < MAX_FILES && isTextFile(path)) {
                            val output = java.io.ByteArrayOutputStream()
                            val buffer = ByteArray(8192)
                            var fileBytes = 0
                            while (true) {
                                val read = zip.read(buffer)
                                if (read <= 0) break
                                fileBytes += read
                                totalBytes += read
                                require(fileBytes <= MAX_FILE_BYTES) { "Un archivo supera el límite de 512 KB: $path" }
                                require(totalBytes <= MAX_TOTAL_BYTES) { "El proyecto supera el límite de 5 MB." }
                                output.write(buffer, 0, read)
                            }
                            val bytes = output.toByteArray()
                            val text = bytes.toString(Charsets.UTF_8)
                            if (!text.contains('\u0000')) imported.add(ImportedProjectFile(path, text))
                        }
                    }
                    zip.closeEntry()
                    require(imported.size <= MAX_FILES) { "El ZIP contiene demasiados archivos." }
                }
            }
        } ?: error("No se pudo abrir el archivo ZIP.")
        require(imported.isNotEmpty()) { "No se encontraron archivos de texto compatibles en el ZIP." }
        imported
    }.fold(
        onSuccess = { ProjectZipImportResult(it) },
        onFailure = { ProjectZipImportResult(emptyList(), it.message ?: "No se pudo importar el ZIP.") }
    )

    private fun safePath(path: String): String? {
        val normalized = path.replace('\\', '/')
        if (normalized.startsWith('/') || normalized.contains(':')) return null
        val parts = normalized.split('/')
        if (parts.any { it == ".." || it.isBlank() }) return null
        return parts.joinToString("/") { it.replace(Regex("[^A-Za-z0-9._-]"), "_") }
    }

    private fun isTextFile(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in setOf(
            "kt", "java", "xml", "gradle", "kts", "properties", "json", "md", "txt",
            "html", "css", "js", "jsx", "ts", "tsx", "py", "sh", "yml", "yaml", "toml",
            "sql", "dart", "swift", "c", "h", "cpp", "hpp", "cs", "go", "rs", "php", "vue"
        ) || !path.contains('.')
    }
}
