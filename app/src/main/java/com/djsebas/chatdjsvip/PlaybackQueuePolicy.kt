package com.djsebas.chatdjsvip

/**
 * Resuelve la siguiente pista al terminar la actual.
 * Una cola explícita se respeta mientras contenga pistas aún presentes en la biblioteca;
 * si no hay una cola válida, se utiliza el orden completo de la biblioteca.
 */
internal fun nextTrackAfterCompletion(
    completedUri: String?,
    queue: List<String>,
    library: List<String>,
    autoPlayNext: Boolean
): String? {
    if (!autoPlayNext) return null
    val activeQueue = queue.filter { it in library }.ifEmpty { library }
    val nextIndex = activeQueue.indexOf(completedUri) + 1
    return activeQueue.getOrNull(nextIndex)
}
