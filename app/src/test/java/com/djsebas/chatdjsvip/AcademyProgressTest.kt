package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcademyProgressTest {
    @Test
    fun summarizesCompletionAndPassingScores() {
        val summary = summarizeAcademyProgress(
            completedTopics = setOf("A", "B", "C"),
            totalTopics = 12,
            examScores = listOf(80, 65, -1),
            practicalScores = mapOf("A" to 100, "B" to 80, "C" to -1)
        )
        assertEquals(25, summary.completionPercent)
        assertEquals(1, summary.passedExams)
        assertEquals(72, summary.averageExamScore)
        assertEquals(2, summary.practicalCount)
        assertEquals(90, summary.averagePracticalScore)
    }

    @Test
    fun handlesEmptyAndInvalidProgressSafely() {
        val summary = summarizeAcademyProgress(emptySet(), 0, listOf(-1, 120), mapOf("A" to -1))
        assertEquals(0, summary.completionPercent)
        assertEquals(0, summary.passedExams)
        assertNull(summary.averageExamScore)
        assertEquals(0, summary.practicalCount)
        assertNull(summary.averagePracticalScore)
    }
}
