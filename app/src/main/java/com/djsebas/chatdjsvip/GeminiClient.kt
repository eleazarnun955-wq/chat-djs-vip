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
                        "\\n\\nPara activar respuestas de IA más amplias, configura tu clave gratuita en Ajustes. Sin clave sigo usando la guía local."
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
                val prompt = "Eres CHAT DJ'S VIP, asistente general en español y especialista en DJ. Responde con claridad, enseña paso a paso, distingue hechos de dudas y no inventes menús, compatibilidades ni especificaciones. Para software DJ pregunta versión y modelo si hacen falta. Si hay riesgos eléctricos o de equipo, prioriza seguridad. Pregunta del usuario: $question"
                val body = JSONObject().put(
                    "contents", JSONArray().put(
                        JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                    )
                ).put("generationConfig", JSONObject().put("temperature", 0.6).put("maxOutputTokens", 1200))
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
                "No pude conectar con la IA gratuita: ${error.message ?: "error de conexión"}.\\n\\nRespuesta local:\\n" + djKnowledgeReply(question)
            }
            Handler(Looper.getMainLooper()).post { onResult(result) }
        }.start()
    }
}
