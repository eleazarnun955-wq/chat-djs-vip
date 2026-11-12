package com.djsebas.chatdjsvip

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.Message
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.Executors

/**
 * Motor generativo local. No usa red ni proveedores externos.
 * El modelo .litertlm se importa una vez desde Archivos y se guarda en el
 * almacenamiento privado de la app, fuera del APK.
 */
object OnDeviceLlmEngine {
    private const val PREFS = "chat_djs_vip_local_model"
    private const val MODEL_PATH = "model_path"
    private const val MODEL_DIR = "models"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var loadedPath: String? = null

    private const val SYSTEM_PROMPT = """
Eres CHAT DJ'S VIP, un asistente universal, útil, claro y respetuoso.
Responde principalmente en español natural de México, pero comprende y puede responder en otros idiomas.
Puedes ayudar con ciencias, matemáticas, historia, geografía, educación, idiomas, tecnología, programación, salud general, derecho general, economía, negocios, vida cotidiana, creatividad, razonamiento y producción musical/DJ.
Explica paso a paso cuando sea útil, adapta el nivel al usuario y da ejemplos concretos.
No inventes hechos, citas, fuentes, resultados ni capacidades. Si no sabes algo o no estás seguro, dilo con claridad.
La aplicación funciona sin conexión: no afirmes conocer noticias, precios, clima ni datos actuales que no estén en el contexto.
En temas médicos, legales o financieros, ofrece información general y señala cuándo hace falta un profesional.
No afirmes que has ejecutado código, abierto archivos, instalado aplicaciones o realizado acciones si no ocurrió realmente.
Protege la privacidad y no solicites contraseñas ni datos sensibles innecesarios.
"""

    fun hasInstalledModel(context: Context): Boolean {
        val path = context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(MODEL_PATH, null)
        return !path.isNullOrBlank() && File(path).isFile && File(path).length() > 50L * 1024L * 1024L
    }

    fun importModel(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            var tempFile: File? = null
            val result = runCatching {
                val displayName = app.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }.orEmpty()
                require(displayName.endsWith(".litertlm", ignoreCase = true)) {
                    "Selecciona un archivo de modelo .litertlm."
                }

                val modelDirectory = File(app.filesDir, MODEL_DIR).apply { mkdirs() }
                tempFile = File(modelDirectory, "assistant.litertlm.part")
                val target = File(modelDirectory, "assistant.litertlm")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile!!).use { output -> input.copyTo(output) }
                } ?: error("No se pudo leer el archivo seleccionado.")
                require(tempFile!!.length() > 50L * 1024L * 1024L) {
                    "El archivo parece incompleto o no es un modelo de lenguaje."
                }
                if (target.exists()) target.delete()
                check(tempFile!!.renameTo(target)) { "No se pudo guardar el modelo en el teléfono." }
                app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(MODEL_PATH, target.absolutePath).apply()
                target.absolutePath
            }

            val message = result.fold(
                onSuccess = { path ->
                    val loaded = runCatching { ensureLoaded(app, path, emptyList()) }
                    if (loaded.isSuccess) "Modelo importado y cargado correctamente."
                    else "Modelo importado. No se pudo iniciar en este teléfono: ${loaded.exceptionOrNull()?.message ?: "error desconocido"}"
                },
                onFailure = { "No se pudo importar el modelo: ${it.message ?: "error de archivo"}" }
            )
            tempFile?.takeIf { it.exists() }?.delete()
            mainHandler.post { onResult(result.isSuccess, message) }
        }
    }

    fun reply(
        context: Context,
        question: String,
        previousMessages: List<String>,
        imageUri: Uri? = null,
        onResult: (String) -> Unit
    ) {
        val app = context.applicationContext
        val intent = LocalBrainEngine.classifyIntent(question)
        if (imageUri != null || intent in setOf(
                LocalBrainIntent.MEMORY_SAVE,
                LocalBrainIntent.MEMORY_CLEAR,
                LocalBrainIntent.MEMORY_LIST,
                LocalBrainIntent.MEMORY_SEARCH
            )
        ) {
            LocalBrainEngine.reply(app, question, previousMessages, imageUri, onResult)
            return
        }

        executor.execute {
            val result = runCatching {
                val path = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(MODEL_PATH, null)
                    ?: error("Aún no has importado el modelo generativo local. Descarga el archivo .litertlm e impórtalo desde el botón «Importar modelo».")
                val historyForInitialization = previousMessages.toMutableList().apply {
                    val currentQuestionIndex = indexOfLast { it == "Tú: $question" }
                    if (currentQuestionIndex >= 0) removeAt(currentQuestionIndex)
                    removeAll { it.contains("Consultando...") }
                }
                ensureLoaded(app, path, historyForInitialization)
                checkNotNull(conversation).sendMessage(question).toString()
            }
            val answer = result.fold(
                onSuccess = { it.ifBlank { "El modelo local no generó una respuesta. Intenta formular la pregunta de otra manera." } },
                onFailure = { "No se pudo usar el cerebro generativo local: ${it.message ?: "error desconocido"}. Comprueba que el modelo sea compatible y que el teléfono tenga memoria disponible." }
            )
            mainHandler.post { onResult(answer) }
        }
    }

    private fun ensureLoaded(context: Context, path: String, history: List<String>) {
        if (conversation != null && loadedPath == path) return
        releaseLoadedModel()
        val modelFile = File(path)
        require(modelFile.isFile && modelFile.length() > 50L * 1024L * 1024L) {
            "No se encuentra un modelo .litertlm válido."
        }
        val newEngine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = Backend.CPU(),
                cacheDir = context.cacheDir.path
            )
        )
        try {
            newEngine.initialize()
            val initialMessages = history.asSequence()
                .filter { it.startsWith("Tú:") || it.startsWith("VIP:") }
                .filterNot { it.contains("Consultando...") }
                .takeLast(10)
                .mapNotNull { line ->
                    when {
                        line.startsWith("Tú:") -> Message.user(line.removePrefix("Tú:").trim())
                        line.startsWith("VIP:") -> Message.model(line.removePrefix("VIP:").trim())
                        else -> null
                    }
                }.toList()
            val newConversation = newEngine.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of(SYSTEM_PROMPT),
                    initialMessages = initialMessages
                )
            )
            engine = newEngine
            conversation = newConversation
            loadedPath = path
        } catch (error: Throwable) {
            runCatching { newEngine.close() }
            throw error
        }
    }

    private fun releaseLoadedModel() {
        runCatching { conversation?.close() }
        conversation = null
        runCatching { engine?.close() }
        engine = null
        loadedPath = null
    }
}
