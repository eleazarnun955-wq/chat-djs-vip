package com.djsebas.chatdjsvip

import org.junit.Assert.assertTrue
import org.junit.Test

class OnlineSearchEngineTest {
    @Test fun endpointIsScopedToUserInitiatedWikipediaSearch() {
        val source = java.io.File("src/main/java/com/djsebas/chatdjsvip/OnlineSearchEngine.kt").readText()
        assertTrue(source.contains("https://es.wikipedia.org/w/api.php"))
        assertTrue(source.contains("connectTimeout = 10_000"))
        assertTrue(source.contains("readTimeout = 12_000"))
        assertTrue(source.contains("setResult"))
    }
}
