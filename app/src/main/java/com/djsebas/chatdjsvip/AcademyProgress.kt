package com.djsebas.chatdjsvip

internal data class AcademyProgressSummary(
    val completionPercent: Int,
    val passedExams: Int,
    val averageExamScore: Int?,
    val practicalCount: Int,
    val averagePracticalScore: Int?
)

internal fun summarizeAcademyProgress(
    completedTopics: Set<String>,
    totalTopics: Int,
    examScores: List<Int>,
    practicalScores: Map<String, Int>
): AcademyProgressSummary {
    val validExams = examScores.filter { it in 0..100 }
    val validPracticals = practicalScores.values.filter { it in 0..100 }
    return AcademyProgressSummary(
        completionPercent = if (totalTopics > 0) (completedTopics.size * 100 / totalTopics).coerceIn(0, 100) else 0,
        passedExams = validExams.count { it >= 70 },
        averageExamScore = validExams.takeIf { it.isNotEmpty() }?.average()?.toInt(),
        practicalCount = validPracticals.size,
        averagePracticalScore = validPracticals.takeIf { it.isNotEmpty() }?.average()?.toInt()
    )
}
