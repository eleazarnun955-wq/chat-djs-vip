package com.djsebas.chatdjsvip

import android.content.Context
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

    fun reply(context: Context, question: String, onResult: (String) -> Unit) {
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
                        "\n\nPara activar respuestas de IA más amplias, configura tu clave gratuita en Ajustes. Sin clave sigo usando la guía local."
                }
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 20000
                    readTimeout = 30000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("x-goog-api-key", apiKey)
                }
                val prompt = """Eres CHAT DJ'S VIP, un asistente general en español y un instructor experto de DJ. Responde la pregunta real del usuario; no te limites a una lista fija de temas. Si la pregunta no es sobre DJ, responde normalmente con información útil.

Cuando sea sobre DJ, enseña desde principiante hasta avanzado y cubre técnicas, teoría musical, BPM, beatmatching manual, fraseo, mezcla armónica, EQ, ganancia, monitoreo, loops, hot cues, scratch, preparación de biblioteca, grabación, streaming, conexiones y solución de fallas. Conoce y explica paso a paso software como rekordbox, Serato DJ, VirtualDJ, Traktor, djay, Engine DJ y Mixxx; y equipos como Pioneer DJ/AlphaTheta, Denon DJ, Numark, Rane, Hercules, Technics y Reloop.

Adapta los pasos a la marca, modelo, versión y sistema operativo que indique el usuario. Si falta un dato que cambia los pasos, haz una sola pregunta concreta. No inventes nombres de menús, botones, compatibilidad, precios ni especificaciones; aclara cuando algo dependa de la versión y recomienda confirmar en el manual oficial. Organiza las instrucciones en pasos numerados, explica qué debe observar y cómo comprobar que funcionó. Advierte antes de acciones que puedan dañar equipo, oírse a volumen peligroso o implicar electricidad. Usa español claro y cercano, sin relleno.

Pregunta del usuario: $question"""
                val body = JSONObject().put(
                    "contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    )
                ).put("generationConfig", JSONObject().put("temperature", 0.6).put("maxOutputTokens", 1800))
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val code = connection.responseCode
                val stream = if (code in 200..299) connection.inputStream else connection.errorStream
                val responseText = stream.bufferedReader().use { it.readText() }
                connection.disconnect()
                if (code !in 200..299) {
                    if (code == 429) throw IllegalStateException("Se alcanzó el límite gratuito temporal. Espera y vuelve a intentarlo.")
                    if (code == 400 || code == 403) throw IllegalStateException("La clave no es válida o el modelo no está disponible en tu cuenta. Revisa tu clave de AI Studio.")
                    throw IllegalStateException("El servicio respondió con error HTTP $code. Intenta más tarde.")
                }
                val json = JSONObject(responseText)
                json.getJSONArray("candidates").getJSONObject(0)
                    .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }.getOrElse { error ->
                "No pude conectar con la IA gratuita: ${error.message ?: "error de conexión"}.\n\nRespuesta local:\n" + djKnowledgeReply(question)
            }
            Handler(Looper.getMainLooper()).post { onResult(result) }
        }.start()
    }
}
