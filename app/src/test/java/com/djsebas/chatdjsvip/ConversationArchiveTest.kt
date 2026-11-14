package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationArchiveTest {
    @Test fun skipsEmptyConversation() {
        assertEquals("[]", archiveConversation("[]", listOf("VIP: Hola"), 10L))
    }
    @Test fun archivesAndRestoresConversation() {
        val messages = listOf("VIP: Hola", "Tú: Ayúdame con mi mezcla", "VIP: Claro")
        val restored = parseConversationArchive(archiveConversation("[]", messages, 123L)).single()
        assertEquals("Ayúdame con mi mezcla", restored.title)
        assertEquals(messages, restored.messages)
    }
    @Test fun capsArchiveAtFortyAndDeletesEntry() {
        var raw = "[]"
        repeat(45) { n -> raw = archiveConversation(raw, listOf("Tú: Chat $n"), 1000L + n) }
        val parsed = parseConversationArchive(raw)
        assertEquals(40, parsed.size)
        assertTrue(parsed.none { it.title == "Chat 0" })
        assertEquals(39, parseConversationArchive(removeArchivedConversation(raw, parsed.first().id)).size)
    }
    @Test fun malformedDataDoesNotCrash() {
        assertTrue(parseConversationArchive("not-json").isEmpty())
    }
}
