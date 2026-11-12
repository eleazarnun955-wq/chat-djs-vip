package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper

/** Motor de conocimiento local: no usa red, claves API ni servicios externos. */
object LocalBrainEngine {
    private const val PREFS = "chat_djs_vip_local_brain"
    private const val MEMORY_KEY = "facts"

    fun reply(context: Context, question: String, previousMessages: List<String>, imageUri: Uri? = null, onResult: (String) -> Unit) {
        Thread {
            val answer = runCatching {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val memory = prefs.getString(MEMORY_KEY, "").orEmpty()
                val q = question.trim()
                val normalized = q.lowercase()
                when {
                    imageUri != null -> "El cerebro local no envía datos a servidores, pero esta versión aún no incluye un modelo visual local. Describe la imagen y analizaré esa descripción sin inventar detalles."
                    isMemoryRequest(normalized) -> {
                        val fact = q.substringAfter(':', "").trim()
                        if (fact.isBlank()) "Escribe «Recuerda: ...» para guardar un dato."
                        else {
                            val facts = (memory.lines().filter { it.isNotBlank() } + fact).distinct().takeLast(40)
                            prefs.edit().putString(MEMORY_KEY, facts.joinToString("\n")).apply()
                            "Dato guardado en la memoria local de este teléfono."
                        }
                    }
                    normalized.contains("qué recuerdas") || normalized.contains("que recuerdas") || normalized.contains("mi memoria") ->
                        if (memory.isBlank()) "Aún no hay datos guardados. Usa «Recuerda: ...»."
                        else "Memoria local:\n" + memory.lines().joinToString("\n") { "• $it" }
                    isDjQuestion(normalized) -> djKnowledgeReply(q)
                    isAppDevelopment(normalized) -> appDevelopmentGuide(q)
                    isReasoningRequest(normalized) -> reasoningGuide(q, previousMessages, memory)
                    normalized.contains("hola") || normalized == "buenas" || normalized == "buenos días" ->
                        "¡Hola! Soy CHAT DJ'S VIP con cerebro local. Puedo ayudarte con DJ, música, razonamiento estructurado y desarrollo de aplicaciones sin IA externa."
                    else -> generalLocalAnswer(q, previousMessages, memory)
                }
            }.getOrElse { error -> "El cerebro local encontró un problema: " + (error.message ?: "error interno") }
            Handler(Looper.getMainLooper()).post { onResult(answer) }
        }.start()
    }

    private fun isMemoryRequest(q: String) = q.startsWith("recuerda:") || q.startsWith("guarda en memoria:") || q.startsWith("memoriza:")
    private fun isDjQuestion(q: String) = listOf("bpm","beatmatch","tempo","sincronizar","ecual","eq","graves","medios","agudos","ganancia","gain","clipping","cue","preescucha","audifonos","conexión","conectar","rca","xlr","bocina","tornamesa","vinilo","aguja","scratch","frase","compás","estructura","cdj","controladora","mezcladora","mixer","loop","hot cue","serato","rekordbox","virtualdj","traktor","djay","no suena","sin sonido","latencia","grabar").any { it in q }
    private fun isAppDevelopment(q: String) = listOf("crear app","crear aplicación","crear aplicacion","programar","código","codigo","compil","android","kotlin","javascript","html","python","github","aplicación","aplicacion","software","proyecto").any { it in q }
    private fun isReasoningRequest(q: String) = listOf("compara","diferencia","ventaja","desventaja","analiza","explica por qué","explica porque","calcula","resuelve","paso a paso","planifica","organiza").any { it in q }

    private fun appDevelopmentGuide(q: String) = """
MODO DESARROLLO LOCAL
1. Define el objetivo y el resultado esperado.
2. Divide el proyecto en pantallas, funciones, datos y permisos.
3. Revisa archivos existentes antes de modificar; conserva las funciones que ya trabajan.
4. Implementa cambios pequeños y aislados.
5. Añade pruebas para funciones nuevas y existentes.
6. Compila y valida en el dispositivo antes de publicar.

Solicitud recibida: $q
Comparte el error exacto o los archivos implicados para un diagnóstico concreto. No afirmaré que compilé o publiqué algo si no se ejecutó realmente.
""".trimIndent()

    private fun reasoningGuide(q: String, history: List<String>, memory: String): String {
        val context = history.takeLast(8).filter { it.isNotBlank() }.joinToString("\n").take(1400)
        val facts = memory.lines().filter { it.isNotBlank() }.takeLast(8).joinToString("; ")
        return "ANÁLISIS ESTRUCTURADO LOCAL\n" +
            "Objetivo: " + q + "\n" +
            "1. Separa hechos confirmados de suposiciones.\n" +
            "2. Identifica restricciones, recursos, compatibilidad y riesgos.\n" +
            "3. Compara alternativas con ventajas y desventajas.\n" +
            "4. Elige la opción que cumpla requisitos con menor riesgo.\n" +
            "5. Define una prueba observable para verificar el resultado.\n" +
            (if (facts.isNotBlank()) "Memoria útil: " + facts + "\n" else "") +
            (if (context.isNotBlank()) "Contexto reciente: " + context + "\n" else "") +
            "Este motor aplica reglas y conocimiento integrado; todavía no es un modelo lingüístico generativo general."
    }

    private fun generalLocalAnswer(q: String, history: List<String>, memory: String): String {
        val context = history.takeLast(6).filter { it.isNotBlank() }.joinToString("\n").take(900)
        val facts = memory.lines().filter { it.isNotBlank() }.takeLast(5).joinToString("; ")
        return "Estoy funcionando localmente, sin Gemini, claves API ni llamadas a servicios externos.\n\n" +
            "Consulta: " + q + "\n" +
            "1. Define el resultado concreto que buscas.\n" +
            "2. Identifica los datos que faltan.\n" +
            "3. Avanza con acciones pequeñas y seguras.\n" +
            "4. Comprueba el resultado antes de darlo por terminado.\n" +
            (if (facts.isNotBlank()) "Datos guardados: " + facts + "\n" else "") +
            (if (context.isNotBlank()) "Contexto reciente: " + context + "\n" else "") +
            "Mi conocimiento local aún tiene límites. Si falta información, lo diré en lugar de inventarla."
    }
}
