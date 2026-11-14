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

    @Test fun recognizesExpandedOfflineTechnicalTopics() {
        assertEquals(LocalBrainIntent.KNOWN_TOPIC, LocalBrainEngine.classifyIntent("¿Qué es Python?"))
        assertEquals(LocalBrainIntent.KNOWN_TOPIC, LocalBrainEngine.classifyIntent("Explícame qué es una API"))
        assertEquals(LocalBrainIntent.KNOWN_TOPIC, LocalBrainEngine.classifyIntent("¿Para qué sirve Jetpack Compose?"))
        assertEquals(LocalBrainIntent.KNOWN_TOPIC, LocalBrainEngine.classifyIntent("¿Qué diferencia hay entre WAV y MP3?"))
    }

    @Test fun combinesMultipleLocalKnowledgeCardsForComparisons() {
        val answer = LocalBrainEngine.answerLocally("¿Qué diferencia hay entre WAV y MP3?", emptyList(), "")
        assertTrue("La comparación debe incluir la ficha MP3. Respuesta: $answer", answer.contains("MP3 es un formato"))
        assertTrue("La comparación debe incluir la ficha WAV. Respuesta: $answer", answer.contains("WAV suele almacenar"))
    }

    @Test fun directsGeneralQuestionsToTheInstalledLocalModel() {
        val question = "¿Cómo funciona un LLM local offline?"
        assertEquals(LocalBrainIntent.UNKNOWN, LocalBrainEngine.classifyIntent(question))
        val answer = LocalBrainEngine.answerLocally(question, emptyList(), "")
        assertTrue(answer.contains("Todavía no tengo un modelo generativo universal instalado"))
        assertTrue(answer.contains("Importa el modelo LFM2.5-350M INT4"))
    }

    @Test fun recognizesMoreAppDevelopmentRequests() {
        assertEquals(LocalBrainIntent.APP_DEVELOPMENT, LocalBrainEngine.classifyIntent("Ayúdame a depurar código"))
        assertEquals(LocalBrainIntent.APP_DEVELOPMENT, LocalBrainEngine.classifyIntent("Quiero generar APK"))
    }

    @Test fun routesAudioMasteringQuestionsToDjKnowledge() {
        assertEquals(LocalBrainIntent.DJ, LocalBrainEngine.classifyIntent("¿Cómo hago la masterización y evito clipping?"))
    }

    @Test fun keepsUnknownTopicsHonestAfterKnowledgeExpansion() {
        val answer = LocalBrainEngine.answerLocally("Explícame física cuántica avanzada", emptyList(), "")
        assertTrue(answer.contains("Todavía no tengo un modelo generativo universal instalado"))
    }

    @Test fun doesNotPretendUnknownTopicsAreKnown() {
        val answer = LocalBrainEngine.answerLocally("Explícame un tema que no está en la base", emptyList(), "")
        assertTrue(answer.contains("Todavía no tengo un modelo generativo universal instalado"))
    }
}
