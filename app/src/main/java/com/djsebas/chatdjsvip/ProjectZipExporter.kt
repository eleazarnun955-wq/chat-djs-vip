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

    fun export(context: Context, uri: Uri, markdown: String): ProjectZipResult {
        if (markdown.isBlank()) return ProjectZipResult(false, error = "No hay código para exportar.")
        return runCatching {
            val matches = fence.findAll(markdown).toList()
            context.contentResolver.openOutputStream(uri)?.use { raw ->
                ZipOutputStream(raw).use { zip ->
                    var count = 0
                    val used = mutableSetOf<String>()
                    zip.putNextEntry(ZipEntry("README-GENERADO.md"))
                    zip.write(markdown.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                    count++

                    matches.forEachIndexed { index, match ->
                        val code = match.groupValues[2].trimEnd()
                        if (code.isNotBlank()) {
                            // Only inspect text since the previous code block. This prevents a
                            // missing path declaration from accidentally reusing an older file path.
                            val previousEnd = if (index == 0) 0 else matches[index - 1].range.last + 1
                            val betweenBlocks = markdown.substring(previousEnd, match.range.first)
                            val declared = pathLine.findAll(betweenBlocks).lastOrNull()?.groupValues?.get(1)?.trim()
                            val info = match.groupValues[1].trim().split(Regex("\\s+")).firstOrNull().orEmpty()
                            val candidate = declared?.takeIf { it.isNotBlank() } ?: info.takeIf { it.contains("/") }
                            val safePath = sanitize(candidate, index, info)
                            var finalPath = safePath
                            var suffix = 2
                            while (!used.add(finalPath.lowercase(Locale.ROOT))) {
                                val dot = safePath.lastIndexOf('.')
                                finalPath = if (dot > safePath.lastIndexOf('/')) safePath.substring(0, dot) + "-$suffix" + safePath.substring(dot)
                                    else "$safePath-$suffix"
                                suffix++
                            }
                            zip.putNextEntry(ZipEntry(finalPath))
                            zip.write((code + "\n").toByteArray(Charsets.UTF_8))
                            zip.closeEntry()
                            count++
                        }
                    }
                    if (matches.isEmpty()) {
                        zip.putNextEntry(ZipEntry("AVISO.txt"))
                        zip.write("No se detectaron bloques de código Markdown. Consulta README-GENERADO.md.".toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                        count++
                    }
                    count
                }
            } ?: error("No se pudo abrir el destino de guardado.")
        }.fold(
            onSuccess = { ProjectZipResult(true, fileCount = it) },
            onFailure = { ProjectZipResult(false, error = it.message ?: "Error al exportar.") }
        )
    }

    private fun sanitize(candidate: String?, index: Int, language: String): String {
        val raw = candidate.orEmpty().replace('\\', '/').trim().trimStart('/')
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
