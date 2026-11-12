package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import java.text.Normalizer
import java.util.Locale

/**
 * Cerebro local sin proveedores externos.
 * Usa clasificación de intención, fichas de conocimiento integradas,
 * contexto reciente y memoria explícita guardada en el dispositivo.
 */
object LocalBrainEngine {
    private const val PREFS = "chat_djs_vip_local_brain"
    private const val MEMORY_KEY = "facts"
    private const val MAX_MEMORY_FACTS = 40

    fun reply(
        context: Context,
        question: String,
        previousMessages: List<String>,
        imageUri: Uri? = null,
        onResult: (String) -> Unit
    ) {
        val appContext = context.applicationContext
        Thread {
            val answer = runCatching {
                val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val effectiveQuestion = resolveFollowUp(question, previousMessages)
                val memory = prefs.getString(MEMORY_KEY, "").orEmpty()
                if (imageUri != null) {
                    "El cerebro local no envía imágenes a servidores. Esta versión todavía no incluye un modelo visual offline, por lo que no puedo describir el contenido de la imagen con fiabilidad. Puedes describirla y analizaré esa descripción."
                } else {
                    answerLocally(effectiveQuestion, previousMessages, memory) { updated ->
                        prefs.edit().putString(MEMORY_KEY, updated).apply()
                    }
                }
            }.getOrElse { error ->
                "El cerebro local encontró un problema: " + (error.message ?: "error interno")
            }
            Handler(Looper.getMainLooper()).post { onResult(answer) }
        }.start()
    }

    internal fun answerLocally(
        question: String,
        history: List<String>,
        memory: String,
        saveMemory: (String) -> Unit = {}
    ): String {
        val q = question.trim()
        return when (classifyIntent(q)) {
            LocalBrainIntent.MEMORY_SAVE -> {
                val fact = q.substringAfter(':', "").trim()
                if (fact.isBlank()) {
                    "Escribe «Recuerda: dato» para guardar algo en este teléfono."
                } else {
                    val facts = (memory.lines().filter { it.isNotBlank() } + fact)
                        .distinctBy { normalize(it) }
                        .takeLast(MAX_MEMORY_FACTS)
                    saveMemory(facts.joinToString("\n"))
                    "Dato guardado en la memoria local de este teléfono."
                }
            }
            LocalBrainIntent.MEMORY_CLEAR -> {
                saveMemory("")
                "La memoria personal local quedó vacía. El historial del chat no se modificó."
            }
            LocalBrainIntent.MEMORY_LIST -> {
                val facts = memory.lines().filter { it.isNotBlank() }
                if (facts.isEmpty()) "Aún no hay datos guardados. Usa «Recuerda: ...»."
                else "Memoria local de este teléfono:\n" + facts.joinToString("\n") { "• $it" }
            }
            LocalBrainIntent.MEMORY_SEARCH -> {
                val term = q.substringAfter(':', "").trim()
                val matches = memory.lines().filter {
                    it.isNotBlank() && (term.isBlank() || normalize(it).contains(normalize(term)))
                }
                if (matches.isEmpty()) "No encontré ese dato en la memoria local."
                else "Coincidencias en tu memoria local:\n" + matches.joinToString("\n") { "• $it" }
            }
            LocalBrainIntent.GREETING ->
                "¡Hola! Soy CHAT DJ'S VIP con cerebro local. Puedo ayudarte con técnicas DJ, audio, conceptos de desarrollo, análisis paso a paso y memoria privada en este teléfono. No utilizo Gemini ni servicios de IA externos."
            LocalBrainIntent.DJ -> djKnowledgeReply(q)
            LocalBrainIntent.KNOWN_TOPIC -> localKnowledgeAnswer(q)
            LocalBrainIntent.APP_DEVELOPMENT -> appDevelopmentGuide(q)
            LocalBrainIntent.REASONING -> reasoningGuide(q, history, memory)
            LocalBrainIntent.FOLLOW_UP ->
                "Puedo continuar, pero no encontré una pregunta anterior clara. Escribe el tema o pega el fragmento que quieres ampliar."
            LocalBrainIntent.UNKNOWN -> generalLocalAnswer(q, history, memory)
        }
    }

    internal fun classifyIntent(question: String): LocalBrainIntent {
        val q = normalize(question)
        return when {
            q.startsWith("recuerda:") || q.startsWith("guarda en memoria:") || q.startsWith("memoriza:") ->
                LocalBrainIntent.MEMORY_SAVE
            q.startsWith("olvida memoria") || q.startsWith("borra memoria") || q.startsWith("limpia memoria") ->
                LocalBrainIntent.MEMORY_CLEAR
            q.startsWith("busca en memoria:") -> LocalBrainIntent.MEMORY_SEARCH
            q == "memoria" || q.contains("que recuerdas") || q.contains("mi memoria") ->
                LocalBrainIntent.MEMORY_LIST
            q in setOf("hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "hey") ->
                LocalBrainIntent.GREETING
            isFollowUp(q) -> LocalBrainIntent.FOLLOW_UP
            isDjQuestion(q) -> LocalBrainIntent.DJ
            isAppDevelopment(q) -> LocalBrainIntent.APP_DEVELOPMENT
            isReasoningRequest(q) -> LocalBrainIntent.REASONING
            hasKnownTopic(q) -> LocalBrainIntent.KNOWN_TOPIC
            else -> LocalBrainIntent.UNKNOWN
        }
    }

    internal fun normalize(value: String): String =
        Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace("[^\\p{L}\\p{N}: ]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .trim()

    private fun resolveFollowUp(question: String, messages: List<String>): String {
        val normalized = normalize(question)
        if (!isFollowUp(normalized)) return question
        val previousQuestion = messages.asReversed()
            .filter { it.startsWith("Tú:") }
            .map { it.removePrefix("Tú:").trim() }
            .firstOrNull { it.isNotBlank() && normalize(it) != normalized }
        return if (previousQuestion.isNullOrBlank()) question
        else previousQuestion + "\n\nSeguimiento: " + question
    }

    private fun isFollowUp(q: String) = q in setOf(
        "continua", "sigue", "mas", "explica mas", "dame mas detalles",
        "y luego", "y despues", "como sigo", "hazlo", "adelante",
        "desarrollalo", "amplia", "amplia la respuesta"
    )

    private fun isDjQuestion(q: String) = listOf(
        "bpm", "beatmatch", "beatmatching", "tempo", "sincronizar", "ecual",
        "graves", "medios", "agudos", "ganancia", "gain", "clipping", "cue",
        "preescucha", "audifonos", "auriculares", "rca", "xlr", "bocina",
        "altavoz", "tornamesa", "vinilo", "aguja", "scratch", "compas",
        "frase musical", "cdj", "controladora", "mezcladora", "mixer", "loop",
        "hot cue", "serato", "rekordbox", "virtualdj", "traktor", "djay",
        "no suena", "sin sonido", "latencia", "grabar sesion"
    ).any { it in q }

    private fun isAppDevelopment(q: String) = listOf(
        "crear app", "crear una app", "crear aplicacion", "crear una aplicacion",
        "desarrollar una app", "desarrollar aplicacion", "programar una app",
        "generar codigo", "modo creador apps", "construir una aplicacion",
        "hacer una aplicacion", "compilar proyecto", "error de gradle",
        "android studio", "modo creador apps", "repositorio"
    ).any { it in q }

    private fun isReasoningRequest(q: String) = listOf(
        "compara", "diferencia entre", "ventaja", "desventaja", "analiza",
        "explica por que", "explica porque", "calcula", "resuelve",
        "paso a paso", "planifica", "organiza", "evalua", "que conviene"
    ).any { it in q }

    private data class KnowledgeEntry(val keywords: List<String>, val answer: String)

    private val knowledge = listOf(
        KnowledgeEntry(listOf("html", "pagina web"),
            "HTML define la estructura de una página web mediante títulos, párrafos, enlaces, imágenes y formularios. CSS controla la presentación y JavaScript añade interactividad. HTML no es por sí solo un lenguaje de programación."),
        KnowledgeEntry(listOf("css", "estilos web"),
            "CSS define la apariencia de una página: colores, tipografía, espaciado y distribución. HTML aporta estructura; CSS presenta esa estructura; JavaScript añade comportamiento."),
        KnowledgeEntry(listOf("javascript", "js"),
            "JavaScript permite añadir lógica e interactividad a sitios web. Se ejecuta en navegadores y también en servidores con entornos como Node.js. No es lo mismo que Java."),
        KnowledgeEntry(listOf("kotlin"),
            "Kotlin es un lenguaje moderno usado ampliamente para Android. Se integra con Java y permite crear interfaces con Jetpack Compose. En una app revisa Activity, permisos, ciclo de vida, almacenamiento, Gradle y pruebas."),
        KnowledgeEntry(listOf("gradle", "build.gradle"),
            "Gradle automatiza la construcción de proyectos. En Android, build.gradle.kts declara plugins, SDK, dependencias y configuración. Ejecuta pruebas, Android Lint y assembleDebug antes de entregar una APK."),
        KnowledgeEntry(listOf("apk", "instalar aplicacion android"),
            "APK es el paquete instalable de Android. Comprueba compilación, identificador de paquete, firma e instalación de prueba. Cambiar applicationId crea una app independiente; conservarlo permite actualizar la misma app si la firma coincide."),
        KnowledgeEntry(listOf("github", "repositorio"),
            "GitHub aloja repositorios Git. Un commit guarda cambios; una rama permite trabajar aislado; un pull request propone integrar una rama; GitHub Actions ejecuta pruebas y genera artefactos. Revisa el diff y CI antes de integrar."),
        KnowledgeEntry(listOf("base de datos", "sqlite", "room"),
            "Una base de datos organiza información. En Android, Room ofrece una capa sobre SQLite con entidades, DAO y consultas. Define claves, índices, migraciones y políticas de respaldo; evita guardar contraseñas en texto plano."),
        KnowledgeEntry(listOf("permisos android", "manifest", "manifiesto android"),
            "Los permisos Android se declaran en AndroidManifest.xml. Algunos requieren autorización en tiempo de ejecución. Solicita solo los necesarios, explica su uso y maneja el caso en que el usuario los deniegue."),
        KnowledgeEntry(listOf("seguridad", "privacidad", "datos personales"),
            "Principios básicos: recopila solo los datos necesarios, guarda secretos fuera del código, valida entradas, limita acceso a archivos, usa HTTPS solo cuando exista una función de red justificada y explica permisos y opciones de eliminación.")
    )

    private fun hasKnownTopic(q: String) =
        knowledge.any { entry -> entry.keywords.any { normalize(it) in q } }

    private fun localKnowledgeAnswer(q: String): String =
        knowledge.map { entry -> entry to entry.keywords.count { normalize(it) in q } }
            .filter { it.second > 0 }
            .maxByOrNull { it.second }
            ?.first?.answer
            ?: "No encontré una ficha local suficiente para ese tema."

    private fun appDevelopmentGuide(q: String): String =
        "MODO DE DESARROLLO LOCAL\n" +
        "1. Define el objetivo, los usuarios y el resultado esperado.\n" +
        "2. Divide el proyecto en pantallas, funciones, datos y permisos.\n" +
        "3. Revisa los archivos existentes antes de modificar; conserva lo que ya funciona.\n" +
        "4. Implementa cambios pequeños y aislados.\n" +
        "5. Añade pruebas para las funciones nuevas y existentes.\n" +
        "6. Ejecuta Lint, compila y valida en un dispositivo antes de publicar.\n\n" +
        "Solicitud recibida: " + q + "\n" +
        "Límite: el cerebro local puede orientar y revisar texto compartido, pero no modifica ni compila por sí solo proyectos externos."

    private fun reasoningGuide(q: String, history: List<String>, memory: String): String {
        val context = history.takeLast(8).filter { it.isNotBlank() }.joinToString("\n").take(1200)
        val facts = memory.lines().filter { it.isNotBlank() }.takeLast(8).joinToString("; ")
        return "ANÁLISIS ESTRUCTURADO LOCAL\n" +
            "Objetivo: " + q + "\n" +
            "1. Hechos confirmados: separa datos disponibles de suposiciones.\n" +
            "2. Incógnitas: identifica qué información falta.\n" +
            "3. Restricciones: considera tiempo, presupuesto, compatibilidad y riesgos.\n" +
            "4. Alternativas: compara beneficios y desventajas.\n" +
            "5. Verificación: define una prueba observable para comprobar el resultado.\n" +
            (if (facts.isNotBlank()) "Memoria útil: " + facts + "\n" else "") +
            (if (context.isNotBlank()) "Contexto reciente: " + context + "\n" else "") +
            "Este análisis usa reglas locales; no consulta fuentes externas ni inventa datos."
    }

    private fun generalLocalAnswer(q: String, history: List<String>, memory: String): String {
        val previousTopic = history.asReversed().firstOrNull { it.startsWith("Tú:") }
            ?.removePrefix("Tú:")?.trim().orEmpty()
        val facts = memory.lines().filter { it.isNotBlank() }.takeLast(5).joinToString("; ")
        return "No tengo una ficha de conocimiento local suficiente para responder eso con fiabilidad.\n" +
            "Puedo trabajar sin internet con guías DJ, conceptos básicos de desarrollo, análisis estructurado y memoria local.\n" +
            (if (previousTopic.isNotBlank() && normalize(previousTopic) != normalize(q)) "Tema reciente: " + previousTopic + "\n" else "") +
            (if (facts.isNotBlank()) "Datos que guardaste: " + facts + "\n" else "") +
            "Para evitar inventar, comparte un poco más de contexto o el texto que quieres que analice."
    }
}

internal enum class LocalBrainIntent {
    MEMORY_SAVE, MEMORY_CLEAR, MEMORY_LIST, MEMORY_SEARCH,
    GREETING, DJ, KNOWN_TOPIC, APP_DEVELOPMENT, REASONING, FOLLOW_UP, UNKNOWN
}
