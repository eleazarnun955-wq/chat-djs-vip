package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSearchEngineTest {
    @Test fun queryIsEncodedAndUsesWikipediaHttpsEndpoint() {
        val url = buildSearchUrl("cumbia dj")
        assertTrue(url.startsWith("https://es.wikipedia.org/w/api.php?"))
        assertTrue(url.contains("srsearch=cumbia+dj"))
        assertTrue(url.contains("srlimit=5"))
    }

    @Test fun queryWhitespaceIsTrimmed() {
        assertEquals(buildSearchUrl("cumbia"), buildSearchUrl("  cumbia  "))
    }
}
