package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalBrainEngineTest {
    @Test fun normalizesSpanishAccents() {
        assertEquals("que recuerdas", LocalBrainEngine.normalize("¿Qué recuerdas?"))
    }

    @Test fun recognizesMemoryCommands() {
        assertEquals(LocalBrainIntent.MEMORY_SAVE, LocalBrainEngine.classifyIntent("Recuerda: mi equipo es Pioneer"))
        assertEquals(LocalBrainIntent.MEMORY_LIST, LocalBrainEngine.classifyIntent("¿Qué recuerdas?"))
        assertEquals(LocalBrainIntent.MEMORY_CLEAR, LocalBrainEngine.classifyIntent("olvida memoria"))
        assertEquals(LocalBrainIntent.MEMORY_SEARCH, LocalBrainEngine.classifyIntent("busca en memoria: Pioneer"))
    }

    @Test fun routesDjQuestionsToDjKnowledge() {
        assertEquals(LocalBrainIntent.DJ, LocalBrainEngine.classifyIntent("¿Cómo ajusto los BPM para beatmatching?"))
    }

    @Test fun recognizesDevelopmentAndTechnicalKnowledge() {
        assertEquals(LocalBrainIntent.KNOWN_TOPIC, LocalBrainEngine.classifyIntent("¿Qué es Kotlin?"))
        assertEquals(LocalBrainIntent.APP_DEVELOPMENT, LocalBrainEngine.classifyIntent("Quiero crear una app Android Studio"))
    }

    @Test fun recognizesFollowUpRequests() {
        assertEquals(LocalBrainIntent.FOLLOW_UP, LocalBrainEngine.classifyIntent("continúa"))
    }

    @Test fun savesAndSearchesMemoryWithoutNetwork() {
        var saved = ""
        LocalBrainEngine.answerLocally("Recuerda: mi controlador es Pioneer", emptyList(), "") { saved = it }
        assertTrue(saved.contains("Pioneer"))
        assertTrue(LocalBrainEngine.answerLocally("busca en memoria: Pioneer", emptyList(), saved).contains("Pioneer"))
    }

    @Test fun clearsOnlyTheLocalMemoryValue() {
        var saved = "mi equipo es Pioneer"
        LocalBrainEngine.answerLocally("olvida memoria", emptyList(), saved) { saved = it }
        assertEquals("", saved)
    }

    @Test fun doesNotPretendUnknownTopicsAreKnown() {
        val answer = LocalBrainEngine.answerLocally("Explícame un tema que no está en la base", emptyList(), "")
        assertTrue(answer.contains("No tengo una ficha de conocimiento local suficiente"))
    }
}
