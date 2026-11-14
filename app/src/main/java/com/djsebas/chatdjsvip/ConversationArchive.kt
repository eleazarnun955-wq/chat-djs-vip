package com.djsebas.chatdjsvip

import org.json.JSONArray
import org.json.JSONObject

internal data class ArchivedConversation(val id: String, val title: String, val messages: List<String>)
private const val MAX_ARCHIVED_CONVERSATIONS = 40

internal fun parseConversationArchive(raw: String?): List<ArchivedConversation> {
    if (raw.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(raw)
        buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                val rows = item.optJSONArray("messages") ?: continue
                val messages = List(rows.length()) { index -> rows.optString(index) }
                if (messages.isNotEmpty()) add(ArchivedConversation(id, item.optString("title", "Conversación"), messages))
            }
        }.sortedByDescending { it.id }
    }.getOrDefault(emptyList())
}

internal fun archiveConversation(raw: String?, messages: List<String>, timestamp: Long): String {
    if (messages.none { it.startsWith("Tú:") }) return raw ?: "[]"
    val existing = parseConversationArchive(raw).filterNot { it.id == timestamp.toString() }
    val first = messages.firstOrNull { it.startsWith("Tú:") }?.removePrefix("Tú:")?.trim().orEmpty()
    val title = first.replace(Regex("\\s+"), " ").take(64).ifBlank { "Conversación $timestamp" }
    val archived = (listOf(ArchivedConversation(timestamp.toString(), title, messages)) + existing).take(MAX_ARCHIVED_CONVERSATIONS)
    return JSONArray().apply {
        archived.forEach { c ->
            put(JSONObject().put("id", c.id).put("title", c.title)
                .put("messages", JSONArray().apply { c.messages.forEach { put(it) } }))
        }
    }.toString()
}

internal fun removeArchivedConversation(raw: String?, id: String): String = JSONArray().apply {
    parseConversationArchive(raw).filterNot { it.id == id }.forEach { c ->
        put(JSONObject().put("id", c.id).put("title", c.title)
            .put("messages", JSONArray().apply { c.messages.forEach { put(it) } }))
    }
}.toString()
