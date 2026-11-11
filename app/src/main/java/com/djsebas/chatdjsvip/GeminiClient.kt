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
                    return@runCatching djKnowledgeReply(question) +
                        (if (imageUri != null) "\n\nPara analizar imágenes necesitas configurar la IA de Gemini en Ajustes." else "") +
                        "\n\nPara activar respuestas de IA más amplias, configura tu clave gratuita en Ajustes. Sin clave sigo usando la guía local."
                }

                val contents = JSONArray()
                // Send only complete recent exchanges, so follow-up questions retain context.
                val recent = previousMessages.takeLast(24)
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

Adapta los pasos a la marca, modelo, versión y sistema operativo que indique el usuario. Si falta un dato que cambia los pasos, haz una sola pregunta concreta. No inventes nombres de menús, botones, compatibilidad, precios ni especificaciones; aclara cuando algo dependa de la versión y recomienda confirmar en el manual oficial. Organiza las instrucciones en pasos numerados, explica qué debe observar y cómo comprobar que funcionó. Advierte antes de acciones que puedan dañar equipo, oírse a volumen peligroso o implicar electricidad. Usa español claro y cercano, sin relleno."""
                        )
                    )
                )
                val body = JSONObject()
                    .put("systemInstruction", systemInstruction)
                    .put("contents", contents)
                    .put(
                        "generationConfig",
                        JSONObject().put("temperature", 0.6).put("maxOutputTokens", 1800)
                    )

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
                var code = 0
                var responseText = ""
                var lastNetworkError: java.io.IOException? = null
                // Reintenta fallas transitorias de red/servidor; no insiste si la cuota gratuita se agotó.
                for (attempt in 0..2) {
                    var connection: HttpURLConnection? = null
                    try {
                        connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            connectTimeout = 20000
                            readTimeout = 90000
                            doOutput = true
                            setRequestProperty("Content-Type", "application/json")
                            setRequestProperty("x-goog-api-key", apiKey)
                        }
                        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                        code = connection.responseCode
                        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                        responseText = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                        lastNetworkError = null
                    } catch (error: java.io.IOException) {
                        lastNetworkError = error
                        if (attempt == 2) throw error
                        Thread.sleep(1000L * (attempt + 1))
                        continue
                    } finally {
                        connection?.disconnect()
                    }
                    if (code !in listOf(502, 503, 504) || attempt == 2) break
                    Thread.sleep(1000L * (attempt + 1))
                }
                if (lastNetworkError != null) throw lastNetworkError

                if (code !in 200..299) {
                    val providerMessage = runCatching {
                        JSONObject(responseText).optJSONObject("error")?.optString("message").orEmpty()
                    }.getOrDefault("").take(300)
                    val detail = if (providerMessage.isBlank()) "" else " Detalle: $providerMessage"
                    when (code) {
                        429 -> throw IllegalStateException("Se alcanzó el límite gratuito temporal. Espera y vuelve a intentarlo.$detail")
                        400, 403 -> throw IllegalStateException("La clave no es válida o el modelo no está disponible en tu cuenta. Revisa tu clave de AI Studio.$detail")
                        404 -> throw IllegalStateException("El modelo no está disponible para esta clave o proyecto. Revisa el acceso en Google AI Studio.$detail")
                        503 -> throw IllegalStateException("Gemini sigue temporalmente ocupado después de varios intentos. Intenta más tarde.$detail")
                        else -> throw IllegalStateException("El servicio respondió con error HTTP $code.$detail")
                    }
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
}
