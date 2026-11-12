package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import android.util.Base64
import android.os.Handler
import android.os.Looper
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKey.KeyScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object GeminiClient {
    private const val MODEL = "gemini-3.8-flash"
    private const val FALLBACK_MODEL = "gemini-3.5-flash-lite"
    private const val MAX_RETRIES = 2

    private class GeminiHttpException(val code: Int, val providerMessage: String) : Exception(providerMessage)

    fun reply(
        context: Context,
        question: String,
        previousMessages: List<String>,
        imageUri: Uri? = null,
        onResult: (String) -> Unit
    ) {
        Thread {
            val result = runCatching {
                val masterKey = MasterKey.Builder(context).setKeyScheme(KeyScheme.AES256_GCM).build()
                val prefs = EncryptedSharedPreferences.create(
                    context, "chat_djs_vip_secure", masterKey,
                    PrefKeyEncryptionScheme.AES256_SIV,
                    PrefValueEncryptionScheme.AES256_GCM
                )
                val apiKey = prefs.getString("gemini_api_key", "").orEmpty()
                if (apiKey.isBlank()) {
                    if (question.startsWith("MODO_CREADOR_APPS:")) {
                        return@runCatching "Para generar código de aplicaciones necesitas configurar tu clave de Gemini en Ajustes. Puedes obtenerla desde Google AI Studio; revisa sus límites y condiciones antes de usarla."
                    }
                    return@runCatching djKnowledgeReply(question) +
                        (if (imageUri != null) "\n\nPara analizar imágenes necesitas configurar la IA de Gemini en Ajustes." else "") +
                        "\n\nPara activar respuestas de IA más amplias, configura tu clave gratuita en Ajustes. Sin clave sigo usando la guía local."
                }

                val contents = JSONArray()
                // Send only complete recent exchanges, so follow-up questions retain context.
                val recent = previousMessages.takeLast(12)
                var index = 0
                while (index + 1 < recent.size) {
                    val userMessage = recent[index]
                    val modelMessage = recent[index + 1]
                    if (userMessage.startsWith("Tú:") && modelMessage.startsWith("VIP:") &&
                        !modelMessage.contains("Consultando...")) {
                        contents.put(
                            JSONObject().put("role", "user").put(
                                "parts", JSONArray().put(
                                    JSONObject().put("text", userMessage.removePrefix("Tú:").trim())
                                )
                            )
                        )
                        contents.put(
                            JSONObject().put("role", "model").put(
                                "parts", JSONArray().put(
                                    JSONObject().put("text", modelMessage.removePrefix("VIP:").trim())
                                )
                            )
                        )
                        index += 2
                    } else {
                        index += 1
                    }
                }
                val latestParts = JSONArray()
                if (imageUri != null) {
                    val imageBytes = context.contentResolver.openInputStream(imageUri)?.use { it.readBytes() }
                        ?: throw IllegalStateException("No pude leer la imagen seleccionada.")
                    if (imageBytes.size > 12 * 1024 * 1024) {
                        throw IllegalStateException("La imagen es demasiado grande. Elige una imagen de menos de 12 MB.")
                    }
                    val mimeType = context.contentResolver.getType(imageUri) ?: "image/jpeg"
                    latestParts.put(
                        JSONObject().put(
                            "inline_data",
                            JSONObject()
                                .put("mime_type", mimeType)
                                .put("data", Base64.encodeToString(imageBytes, Base64.NO_WRAP))
                        )
                    )
                }
                latestParts.put(JSONObject().put("text", question))
                contents.put(JSONObject().put("role", "user").put("parts", latestParts))

                val systemInstruction = JSONObject().put(
                    "parts",
                    JSONArray().put(
                        JSONObject().put(
                            "text",
                            """Eres CHAT DJ'S VIP, un asistente general en español y un instructor experto de DJ. Responde la pregunta real del usuario; no te limites a una lista fija de temas. Si la pregunta no es sobre DJ, responde normalmente con información útil.

Cuando sea sobre DJ, enseña desde principiante hasta avanzado y cubre técnicas, teoría musical, BPM, beatmatching manual, fraseo, mezcla armónica, EQ, ganancia, monitoreo, loops, hot cues, scratch, preparación de biblioteca, grabación, streaming, conexiones y solución de fallas. Conoce y explica paso a paso software como rekordbox, Serato DJ, VirtualDJ, Traktor, djay, Engine DJ y Mixxx; y equipos como Pioneer DJ/AlphaTheta, Denon DJ, Numark, Rane, Hercules, Technics y Reloop.

Adapta los pasos a la marca, modelo, versión y sistema operativo que indique el usuario. Si falta un dato que cambia los pasos, haz una sola pregunta concreta. No inventes nombres de menús, botones, compatibilidad, precios ni especificaciones; aclara cuando algo dependa de la versión y recomienda confirmar en el manual oficial. Organiza las instrucciones en pasos numerados, explica qué debe observar y cómo comprobar que funcionó. Advierte antes de acciones que puedan dañar equipo, oírse a volumen peligroso o implicar electricidad. Usa español claro y cercano, sin relleno.

También eres un asistente de desarrollo de software. Puedes ayudar a planear y programar aplicaciones Android, aplicaciones web y herramientas de escritorio: aclarar requisitos, proponer arquitectura, diseñar interfaces, escribir código, organizar archivos, explicar dependencias, revisar errores, crear pruebas y preparar instrucciones de compilación y publicación. Cuando el usuario active MODO_CREADOR_APPS, prioriza una entrega técnica por archivos con rutas exactas y contenido completo; no entregues pseudocódigo como si fuera una aplicación terminada. Indica versiones y comandos compatibles, valida supuestos y divide proyectos grandes en fases. Nunca afirmes que ejecutaste, compilaste, probaste, publicaste o subiste una aplicación si no ocurrió realmente. Advierte sobre claves, permisos, datos personales y seguridad; nunca incrustes secretos en el código."""
                        )
                    )
                )
                val isAppCreation = question.startsWith("MODO_CREADOR_APPS:") ||
                    question.contains("código", ignoreCase = true) ||
                    question.contains("programa", ignoreCase = true) ||
                    question.contains("aplicación", ignoreCase = true)
                val maxOutputTokens = if (isAppCreation) 6000 else 2048
                val body = JSONObject()
                    .put("systemInstruction", systemInstruction)
                    .put("contents", contents)
                    .put(
                        "generationConfig",
                        JSONObject().put("maxOutputTokens", maxOutputTokens)
                    )

                val responseText = try {
                    requestWithFailover(body, apiKey)
                } catch (error: GeminiHttpException) {
                    val detail = error.providerMessage.take(350)
                    val explanation = when (error.code) {
                        429 -> if (isQuotaError(detail)) {
                            "Google agotó la cuota gratuita de esta clave o proyecto. Revisa el uso y los límites en AI Studio. Detalle: $detail"
                        } else {
                            "Gemini está limitando temporalmente las solicitudes. Espera un poco y vuelve a intentarlo. Detalle: $detail"
                        }
                        503 -> "Los servidores de Gemini están saturados; también falló el modelo ligero de respaldo. Intenta de nuevo en unos minutos. Detalle: $detail"
                        400, 403 -> "La clave fue rechazada o no tiene permiso para este modelo. Comprueba la clave en Google AI Studio. Detalle: $detail"
                        404 -> "El modelo no está disponible para esta clave o proyecto. Detalle: $detail"
                        else -> "Gemini respondió con HTTP " + error.code + ". Detalle: $detail"
                    }
                    throw IllegalStateException(explanation)
                }

                val json = JSONObject(responseText)
                val candidates = json.optJSONArray("candidates")
                    ?: throw IllegalStateException("La IA no devolvió una respuesta. Intenta reformular la pregunta.")
                val parts = candidates.optJSONObject(0)?.optJSONObject("content")?.optJSONArray("parts")
                    ?: throw IllegalStateException("La IA devolvió una respuesta vacía.")
                buildString {
                    for (i in 0 until parts.length()) {
                        val partText = parts.optJSONObject(i)?.optString("text").orEmpty()
                        if (partText.isNotBlank()) {
                            if (isNotEmpty()) append("\n")
                            append(partText)
                        }
                    }
                }.ifBlank { throw IllegalStateException("La IA devolvió una respuesta vacía.") }
            }.getOrElse { error ->
                "No pude conectar con la IA gratuita: ${error.message ?: "error de conexión"}.\n\nRespuesta local:\n" +
                    djKnowledgeReply(question)
            }
            Handler(Looper.getMainLooper()).post { onResult(result) }
        }.start()
    }
    private fun requestWithFailover(body: JSONObject, apiKey: String): String {
        return try {
            requestModel(MODEL, body, apiKey)
        } catch (primaryError: GeminiHttpException) {
            val retryWithFallback = primaryError.code == 503 ||
                primaryError.code == 502 || primaryError.code == 504 ||
                (primaryError.code == 429 && !isQuotaError(primaryError.providerMessage))
            if (!retryWithFallback) throw primaryError
            try {
                requestModel(FALLBACK_MODEL, body, apiKey)
            } catch (fallbackError: GeminiHttpException) {
                throw GeminiHttpException(
                    fallbackError.code,
                    "El modelo principal respondió HTTP " + primaryError.code +
                        " y el modelo alternativo también falló: " + fallbackError.providerMessage
                )
            }
        }
    }

    private fun requestModel(model: String, body: JSONObject, apiKey: String): String {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent")
        for (attempt in 0..MAX_RETRIES) {
            var connection: HttpURLConnection? = null
            try {
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 60000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", apiKey)
                }
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code in 200..299) return responseText

                val providerMessage = runCatching {
                    val error = JSONObject(responseText).optJSONObject("error")
                    error?.optString("message").orEmpty().ifBlank {
                        error?.optString("status").orEmpty()
                    }
                }.getOrDefault("Sin detalle del proveedor")
                val retryable = code == 429 || code == 500 || code == 502 || code == 503 || code == 504
                if (!retryable || attempt == MAX_RETRIES) {
                    throw GeminiHttpException(code, providerMessage)
                }
                val retryAfter = connection.getHeaderField("Retry-After")?.toLongOrNull()?.coerceIn(1, 8)
                Thread.sleep(retryAfter?.times(1000L) ?: (1000L shl attempt))
            } catch (error: GeminiHttpException) {
                throw error
            } catch (error: java.net.SocketTimeoutException) {
                if (attempt == MAX_RETRIES) {
                    throw IllegalStateException("La conexión con " + model + " agotó el tiempo de espera después de varios intentos.")
                }
                Thread.sleep(1000L shl attempt)
            } catch (error: IOException) {
                if (attempt == MAX_RETRIES) {
                    throw IllegalStateException("No se pudo conectar con Google: " + (error.message ?: "error de red"))
                }
                Thread.sleep(1000L shl attempt)
            } finally {
                connection?.disconnect()
            }
        }
        throw IllegalStateException("No se pudo completar la solicitud.")
    }

    private fun isQuotaError(message: String): Boolean {
        val normalized = message.lowercase()
        return normalized.contains("quota") || normalized.contains("per day") ||
            normalized.contains("daily limit") || normalized.contains("billing")
    }

}
