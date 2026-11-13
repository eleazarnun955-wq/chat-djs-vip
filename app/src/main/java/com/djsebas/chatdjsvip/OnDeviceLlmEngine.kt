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
    private var loadedBackendIsGpu: Boolean? = null

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
        val app = context.applicationContext
        val savedPath = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(MODEL_PATH, null)
        val modelFile = recoverModelFile(app)
        val path = savedPath?.takeIf { File(it).isFile } ?: modelFile.absolutePath
        return File(path).isFile && File(path).length() > 50L * 1024L * 1024L
    }

    fun importModel(context: Context, uri: Uri, onResult: (Boolean, String) -> Unit) {
        val app = context.applicationContext
        executor.execute {
            val modelDirectory = File(app.filesDir, MODEL_DIR)
            val target = File(modelDirectory, "assistant.litertlm")
            val backup = File(modelDirectory, "assistant.litertlm.backup")
            val staged = File(modelDirectory, "assistant.litertlm.part")
            var previousModelMoved = false
            var newModelPlaced = false
            var settingsSaved = false

            val result = runCatching {
                val displayName = app.contentResolver.query(
                    uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) cursor.getString(0) else null
                }.orEmpty()
                // No dependemos del nombre que reporte el proveedor SAF (Files/Descargas).
                // La validación real la hace LiteRT-LM al inicializar el contenido copiado.
                check(modelDirectory.exists() || modelDirectory.mkdirs()) {
                    "No se pudo preparar el almacenamiento privado para el modelo."
                }
                // Recupera primero una copia anterior si una ejecución previa se interrumpió.
                recoverModelFile(app)
                if (staged.exists()) {
                    check(staged.delete()) { "No se pudo limpiar una importación incompleta anterior." }
                }
                if (target.exists() && backup.exists()) {
                    check(backup.delete()) { "No se pudo limpiar una copia de respaldo antigua." }
                }

                app.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(staged).use { output ->
                        input.copyTo(output)
                        output.fd.sync()
                    }
                } ?: error("No se pudo leer el archivo seleccionado.")
                require(staged.isFile && staged.length() > 50L * 1024L * 1024L) {
                    "El archivo parece incompleto o no es un modelo de lenguaje compatible."
                }

                // Validar el archivo temporal antes de tocar el modelo que ya funciona.
                validateModelFile(app, staged)
                releaseLoadedModel()

                if (target.exists()) {
                    check(target.renameTo(backup)) {
                        "No se pudo proteger el cerebro anterior. No se reemplazó."
                    }
                    previousModelMoved = true
                }
                check(staged.renameTo(target)) {
                    "No se pudo colocar el nuevo modelo en el almacenamiento privado."
                }
                newModelPlaced = true

                val saved = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putString(MODEL_PATH, target.absolutePath)
                    .commit()
                check(saved) { "No se pudo guardar la configuración del nuevo modelo." }
                settingsSaved = true

                // La copia anterior solo se elimina después de confirmar el nuevo ajuste.
                if (backup.exists()) backup.delete()
                target.absolutePath
            }.recoverCatching { error ->
                // Revertir el cambio si falla el movimiento o el guardado de preferencias.
                val rollback = runCatching {
                    if (!settingsSaved && newModelPlaced && target.exists()) {
                        check(target.delete()) { "No se pudo retirar el modelo incompleto." }
                    }
                    if (!settingsSaved && previousModelMoved && backup.exists()) {
                        check(backup.renameTo(target)) {
                            "No se pudo restaurar automáticamente el cerebro anterior."
                        }
                    }
                }
                val rollbackError = rollback.exceptionOrNull()
                if (rollbackError != null) {
                    throw IllegalStateException(
                        "${error.message ?: "Falló la importación"} Además, la recuperación automática necesita atención: ${rollbackError.message}",
                        error
                    )
                }
                throw error
            }

            if (staged.exists()) staged.delete()
            val message = result.fold(
                onSuccess = {
                    "Cerebro local importado y validado. El modelo anterior solo se reemplazó después de comprobar el archivo. Envía una pregunta para iniciar la primera respuesta."
                },
                onFailure = {
                    "No se pudo importar el cerebro local: ${it.message ?: "error de archivo"}. Si ya había un modelo instalado, se intentó conservarlo o restaurarlo."
                }
            )
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
                val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val savedPath = prefs.getString(MODEL_PATH, null)
                val recoveredFile = recoverModelFile(app)
                val path = savedPath?.takeIf { File(it).isFile }
                    ?: recoveredFile.takeIf { it.isFile }?.absolutePath
                    ?: error("Aún no has importado el modelo generativo local. Descarga el archivo .litertlm e impórtalo desde el botón «Importar modelo».")
                if (savedPath != path) prefs.edit().putString(MODEL_PATH, path).apply()
                val historyForInitialization = previousMessages.toMutableList().apply {
                    val currentQuestionIndex = indexOfLast { it == "Tú: $question" }
                    if (currentQuestionIndex >= 0) removeAt(currentQuestionIndex)
                    removeAll { it.contains("Consultando...") }
                }
                if (conversation == null || loadedPath != path) {
                    // Compatibilidad prioritaria: usar CPU en teléfonos sin OpenCL fiable.
                    ensureLoaded(app, path, historyForInitialization, useGpu = false)
                }
                val savedFacts = app.getSharedPreferences("chat_djs_vip_local_brain", Context.MODE_PRIVATE)
                    .getString("facts", "").orEmpty().lines().filter { it.isNotBlank() }.takeLast(10).joinToString("; ")
                val modelPrompt = if (savedFacts.isBlank()) question
                    else "$question\n\nContexto privado guardado localmente por el usuario: $savedFacts"
                checkNotNull(conversation).sendMessage(modelPrompt).toString()
            }
            val answer = result.fold(
                onSuccess = { it.ifBlank { "El modelo local no generó una respuesta. Intenta formular la pregunta de otra manera." } },
                onFailure = { "No se pudo usar el cerebro generativo local: ${it.message ?: "error desconocido"}. Comprueba que el modelo sea compatible y que el teléfono tenga memoria disponible." }
            )
            mainHandler.post { onResult(answer) }
        }
    }

    private fun recoverModelFile(context: Context): File {
        val modelDirectory = File(context.filesDir, MODEL_DIR)
        val target = File(modelDirectory, "assistant.litertlm")
        val backup = File(modelDirectory, "assistant.litertlm.backup")
        if (!target.exists() && backup.exists()) {
            backup.renameTo(target)
        } else if (target.exists() && backup.exists()) {
            backup.delete()
        }
        return target
    }

    private fun validateModelFile(context: Context, modelFile: File) {
        var lastError: Throwable? = null
        for (useGpu in listOf(false)) {
            var candidateEngine: Engine? = null
            var initialized = false
            var candidateConversation: Conversation? = null
            try {
                val createdEngine = Engine(
                    EngineConfig(
                        modelPath = modelFile.absolutePath,
                        backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                        cacheDir = context.cacheDir.path
                    )
                )
                candidateEngine = createdEngine
                createdEngine.initialize()
                initialized = true
                candidateConversation = createdEngine.createConversation(
                    ConversationConfig(systemInstruction = Contents.of(SYSTEM_PROMPT))
                )
                candidateConversation.close()
                candidateConversation = null
                return
            } catch (error: Throwable) {
                if (isMemoryExhaustion(error)) throw error
                lastError = error
            } finally {
                runCatching { candidateConversation?.close() }
                if (initialized) runCatching { candidateEngine?.close() }
            }
        }
        throw IllegalArgumentException(
            "LiteRT-LM no pudo inicializar este archivo ni crear una conversación.",
            lastError
        )
    }

    private fun ensureLoaded(context: Context, path: String, history: List<String>, useGpu: Boolean) {
        if (conversation != null && loadedPath == path && loadedBackendIsGpu == useGpu) return
        releaseLoadedModel()
        val modelFile = File(path)
        require(modelFile.isFile && modelFile.length() > 50L * 1024L * 1024L) {
            "No se encuentra un modelo .litertlm válido."
        }
        val newEngine = Engine(
            EngineConfig(
                modelPath = modelFile.absolutePath,
                backend = if (useGpu) Backend.GPU() else Backend.CPU(),
                cacheDir = context.cacheDir.path
            )
        )
        var initialized = false
        try {
            newEngine.initialize()
            initialized = true
            val initialMessages = history.asSequence()
                .filter { it.startsWith("Tú:") || it.startsWith("VIP:") }
                .filterNot { it.contains("Consultando...") }
                .toList()
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
            loadedBackendIsGpu = useGpu
        } catch (error: Throwable) {
            if (initialized) runCatching { newEngine.close() }
            throw error
        }
    }

    private fun isMemoryExhaustion(error: Throwable): Boolean {
        var current: Throwable? = error
        repeat(8) {
            if (current is OutOfMemoryError) return true
            current = current?.cause
        }
        return false
    }

    private fun releaseLoadedModel() {
        runCatching { conversation?.close() }
        conversation = null
        runCatching { engine?.close() }
        engine = null
        loadedPath = null
        loadedBackendIsGpu = null
    }
}
