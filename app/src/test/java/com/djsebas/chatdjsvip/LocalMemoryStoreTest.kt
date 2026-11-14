package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMemoryStoreTest {
    @Test
    fun addsFactsWithoutDuplicatesAndKeepsLatestForty() {
        val original = (1..40).joinToString("\n") { "Dato $it" }
        val updated = addLocalMemoryFact(original, "Dato 41")
        val facts = parseLocalMemoryFacts(updated)
        assertEquals(40, facts.size)
        assertEquals("Dato 2", facts.first())
        assertEquals("Dato 41", facts.last())
        assertEquals(updated, addLocalMemoryFact(updated, "dato 41"))
    }

    @Test
    fun removesOnlySelectedFact() {
        assertEquals("uno\ntres", removeLocalMemoryFact("uno\ndos\ntres", 1))
    }
}
