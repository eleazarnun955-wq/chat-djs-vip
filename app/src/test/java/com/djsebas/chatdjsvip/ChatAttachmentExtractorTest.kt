package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatAttachmentExtractorTest {
    @Test
    fun keepsShortTextUnchanged() {
        assertEquals("Contenido breve", limitAttachmentText("Contenido breve"))
    }

    @Test
    fun truncatesLongTextWithNotice() {
        val result = limitAttachmentText("x".repeat(120_050))
        assertTrue(result.startsWith("x".repeat(120_000)))
        assertTrue(result.endsWith("[Contenido recortado por el límite de 120 KB de texto.]"))
    }
}
