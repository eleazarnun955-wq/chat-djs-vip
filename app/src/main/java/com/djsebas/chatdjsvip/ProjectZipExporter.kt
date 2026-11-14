package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ProjectZipResult(val success: Boolean, val fileCount: Int = 0, val error: String = "")

object ProjectZipExporter {
    private val fence = Regex("(?s)\\x60\\x60\\x60([^\\n\\x60]*)\\n(.*?)\\x60\\x60\\x60")
    private val pathLine = Regex("(?im)^\\s*(?:Archivo|Ruta|File|Path)\\s*:\\s*([^\\n]+)")
    private const val MAX_FILES = 100
    private const val MAX_TOTAL_BYTES = 5 * 1024 * 1024

    fun export(
        context: Context,
        uri: Uri,
        markdown: String,
        projectFiles: List<ImportedProjectFile> = emptyList()
    ): ProjectZipResult {
        if (markdown.isBlank() && projectFiles.isEmpty()) {
            return ProjectZipResult(false, error = "No hay archivos ni código para exportar.")
        }
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { raw ->
                ZipOutputStream(raw).use { zip ->
                    var count = 0
                    var totalBytes = 0L
                    val used = mutableSetOf<String>()

                    fun addFile(path: String, content: ByteArray) {
                        require(count < MAX_FILES) { "El proyecto supera el límite de $MAX_FILES archivos." }
                        totalBytes += content.size
                        require(totalBytes <= MAX_TOTAL_BYTES) { "El proyecto supera el límite de 5 MB." }
                        var finalPath = path
                        var suffix = 2
                        while (!used.add(finalPath.lowercase(Locale.ROOT))) {
                            val dot = path.lastIndexOf('.')
                            finalPath = if (dot > path.lastIndexOf('/')) {
                                path.substring(0, dot) + "-$suffix" + path.substring(dot)
                            } else "$path-$suffix"
                            suffix++
                        }
                        zip.putNextEntry(ZipEntry(finalPath))
                        zip.write(content)
                        zip.closeEntry()
                        count++
                    }

                    projectFiles.forEach { file ->
                        val safePath = sanitize(file.path, count, "")
                        val binaryWrapper = ProjectZipBinaryFileCodec.decodeGradleWrapper(file.path, file.content)
                        val fileBytes = binaryWrapper ?: file.content.toByteArray(Charsets.UTF_8)
                        if (binaryWrapper == null) {
                            require(fileBytes.size <= 512 * 1024) {
                                "El archivo supera el límite de 512 KB: ${file.path}"
                            }
                        }
                        addFile(safePath, fileBytes)
                    }

                    // Include a ready-to-run CI workflow for complete Android Gradle projects.
                    // Existing user workflows are preserved and never overwritten.
                    ProjectBuildWorkflowGenerator.generate(projectFiles)?.let { workflow ->
                        addFile(workflow.path, workflow.content.toByteArray(Charsets.UTF_8))
                    }

                    if (markdown.isNotBlank()) {
                        addFile("README-GENERADO.md", markdown.toByteArray(Charsets.UTF_8))
                        val matches = fence.findAll(markdown).toList()
                        matches.forEachIndexed { index, match ->
                            val code = match.groupValues[2].trimEnd()
                            if (code.isNotBlank()) {
                                val previousEnd = if (index == 0) 0 else matches[index - 1].range.last + 1
                                val betweenBlocks = markdown.substring(previousEnd, match.range.first)
                                val declared = pathLine.findAll(betweenBlocks).lastOrNull()?.groupValues?.get(1)?.trim()
                                val info = match.groupValues[1].trim().split(Regex("\\s+")).firstOrNull().orEmpty()
                                val candidate = declared?.takeIf { it.isNotBlank() } ?: info.takeIf { it.contains("/") }
                                val safePath = sanitize(candidate.orEmpty(), index, info)
                                addFile(safePath, (code + "\n").toByteArray(Charsets.UTF_8))
                            }
                        }
                        if (matches.isEmpty()) {
                            addFile("AVISO.txt", "No se detectaron bloques de código Markdown. Consulta README-GENERADO.md.".toByteArray(Charsets.UTF_8))
                        }
                    }
                    count
                }
            } ?: error("No se pudo abrir el destino de guardado.")
        }.fold(
            onSuccess = { ProjectZipResult(true, fileCount = it) },
            onFailure = { ProjectZipResult(false, error = it.message ?: "Error al exportar.") }
        )
    }

    private fun sanitize(candidate: String, index: Int, language: String): String {
        val raw = candidate.replace("\\", "/").trim().trimStart('/')
        val safe = raw.split('/').filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/") { segment -> segment.replace(Regex("[^A-Za-z0-9._-]"), "_") }
        if (safe.isNotBlank()) return safe
        val extension = when (language.lowercase(Locale.ROOT)) {
            "kotlin", "kt" -> "kt"
            "java" -> "java"
            "javascript", "js" -> "js"
            "typescript", "ts" -> "ts"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "xml" -> "xml"
            "yaml", "yml" -> "yml"
            "python", "py" -> "py"
            "bash", "sh" -> "sh"
            else -> "txt"
        }
        return "generated/archivo-%02d.%s".format(Locale.ROOT, index + 1, extension)
    }
}
