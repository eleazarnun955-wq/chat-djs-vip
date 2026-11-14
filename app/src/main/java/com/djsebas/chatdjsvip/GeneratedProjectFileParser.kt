package com.djsebas.chatdjsvip

/** Convierte bloques "Archivo: ruta" del asistente en archivos editables del proyecto. */
object GeneratedProjectFileParser {
    private val fileBlock = Regex(
        pattern = """(?ms)^Archivo:\s*([^\r\n]+)\s*\r?\n\s*```[\w+-]*\s*\r?\n(.*?)\r?\n```\s*(?=\r?\n|$)"""
    )

    fun parse(response: String): List<ImportedProjectFile> {
        val files = linkedMapOf<String, ImportedProjectFile>()
        var totalChars = 0
        for (match in fileBlock.findAll(response)) {
            val rawPath = match.groupValues[1].trim()
            val safePath = ProjectZipPathPolicy.safePath(rawPath) ?: continue
            if (safePath != rawPath || !ProjectZipPathPolicy.isTextFile(safePath)) continue
            val content = match.groupValues[2].trimEnd()
            if (content.isBlank() || content.length > 512 * 1024) continue
            val key = safePath.lowercase()
            if (key in files) continue
            if (files.size >= 100 || totalChars + content.length > 5 * 1024 * 1024) break
            files[key] = ImportedProjectFile(safePath, content)
            totalChars += content.length
        }
        return files.values.toList()
    }
}
