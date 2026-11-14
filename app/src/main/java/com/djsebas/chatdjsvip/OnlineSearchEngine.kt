package com.djsebas.chatdjsvip

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.text.Html
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.Executors

/**
 * Búsqueda web solicitada por el usuario. Consulta el endpoint público de búsqueda
 * de Wikipedia por HTTPS; el modelo local nunca se conecta por sí mismo.
 */
object OnlineSearchEngine {
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun search(context: Context, query: String, onResult: (String) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val result = runCatching {
                val url = URL(buildSearchUrl(query))
                val connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 12_000
                    setRequestProperty("User-Agent", "CHAT-DJS-VIP/3.8.4 (Android app; optional user-initiated search)")
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val status = connection.responseCode
                    check(status in 200..299) { "Wikipedia respondió HTTP $status." }
                    val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    val items = JSONObject(body).optJSONObject("query")?.optJSONArray("search")
                    if (items == null || items.length() == 0) {
                        "No encontré resultados en Wikipedia en español para «$query». Prueba con otras palabras."
                    } else {
                        buildString {
                            append("Búsqueda: ").append(query).append("\\n")
                            for (i in 0 until minOf(items.length(), 5)) {
                                val item = items.optJSONObject(i) ?: continue
                                val title = item.optString("title", "Sin título")
                                val snippet = Html.fromHtml(item.optString("snippet", ""), Html.FROM_HTML_MODE_LEGACY).toString()
                                val pageId = item.optLong("pageid", 0L)
                                append("\\n").append(i + 1).append(". ").append(title).append("\\n")
                                append(snippet).append("\\n")
                                if (pageId > 0) append("Fuente: https://es.wikipedia.org/?curid=").append(pageId).append("\\n")
                            }
                            append("\\nFuente: Wikipedia en español. Verifica datos importantes con fuentes adicionales.")
                        }
                    }
                } finally {
                    connection.disconnect()
                }
            }.getOrElse { error ->
                "No se pudo completar la búsqueda web: ${error.message ?: "error de conexión"}. Comprueba internet e inténtalo de nuevo."
            }
            mainHandler.post { onResult(result) }
        }
    }
}

internal fun buildSearchUrl(query: String): String {
    val encoded = URLEncoder.encode(query.trim(), "UTF-8")
    return "https://es.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=1&srlimit=5"
}
