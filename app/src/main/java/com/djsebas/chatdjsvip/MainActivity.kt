package com.djsebas.chatdjsvip

import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.net.Uri
import android.media.MediaPlayer
import android.media.PlaybackParams
import java.util.Locale
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONArray
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

val Ink = Color(0xFF101820)
val Panel = Color(0xFF18232D)
val Accent = Color(0xFF16C79A)
val Muted = Color(0xFF9AAAB7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ChatDjsVipApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDjsVipApp() {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("chat_djs_vip", android.content.Context.MODE_PRIVATE) }
    var section by remember { mutableStateOf("Chat") }
    var showClearChat by remember { mutableStateOf(false) }
    var showConversationHistory by remember { mutableStateOf(false) }
    var conversationArchive by remember { mutableStateOf(preferences.getString("conversation_archive_v1", "[]").orEmpty()) }
    var prompt by remember { mutableStateOf("") }
    var messages by remember {
        val stored = preferences.getString("messages", null)
        mutableStateOf(
            if (stored.isNullOrBlank()) listOf("¡Hola! Soy CHAT DJ'S VIP, tu asistente universal local. Importa un modelo para responder preguntas generales sin conexión.")
            else runCatching {
                val json = JSONArray(stored)
                List(json.length()) { index -> json.getString(index) }
            }.getOrElse { listOf("¡Hola! Soy CHAT DJ'S VIP. Importa un modelo local para activar conocimientos generales sin conexión.") }
        )
    }
    LaunchedEffect(messages) {
        val json = JSONArray()
        messages.forEach { json.put(it) }
        preferences.edit().putString("messages", json.toString()).apply()
    }
    MaterialTheme(colorScheme = darkColorScheme(
        primary = Accent, background = Ink, surface = Panel,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Ink, topBar = {
            TopAppBar(title = {
                Column {
                    Text("CHAT DJ'S VIP", fontWeight = FontWeight.Bold, fontSize = 19.sp)
                    Text(if (section == "Chat") "Asistente universal sin conexión" else "Aprende DJ desde cero", color = Muted, fontSize = 12.sp)
                }
            }, navigationIcon = {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Accent, modifier = Modifier.padding(start = 16.dp).size(28.dp))
            }, actions = {
                if (section == "Chat") {
                    IconButton(onClick = { showConversationHistory = true }) { Icon(Icons.Default.History, contentDescription = "Conversaciones anteriores", tint = Accent) }
                    IconButton(onClick = {
                        conversationArchive = archiveConversation(conversationArchive, messages, System.currentTimeMillis())
                        preferences.edit().putString("conversation_archive_v1", conversationArchive).apply()
                        messages = listOf("¡Hola! Soy CHAT DJ'S VIP. ¿Qué hacemos hoy?")
                        prompt = ""
                    }) { Icon(Icons.Default.AddComment, contentDescription = "Nueva conversación", tint = Accent) }
                    IconButton(onClick = { showClearChat = true }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Limpiar conversación", tint = Accent) }
                }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Panel))
        }, bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(selected = section == "Chat", onClick = { section = "Chat" }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(selected = section == "Escuela DJ", onClick = { section = "Escuela DJ" }, icon = { Icon(Icons.Default.School, null) }, label = { Text("Escuela DJ") })
                NavigationBarItem(selected = section == "Biblioteca", onClick = { section = "Biblioteca" }, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text("Biblioteca") })
                NavigationBarItem(selected = section == "Crear apps", onClick = { section = "Crear apps" }, icon = { Icon(Icons.Default.Code, null) }, label = { Text("Crear apps") })
                NavigationBarItem(selected = false, onClick = { context.startActivity(Intent(context, DjProduccionesActivity::class.java)) }, icon = { Icon(Icons.Default.MusicNote, null) }, label = { Text("Producciones") })
            }
        }) { padding ->
            when (section) {
                "Chat" -> ChatScreen(messages, prompt, { prompt = it }, { text, imageUri ->
                    if (text.isNotBlank() || imageUri != null) {
                        val shownText = text.ifBlank { "Analiza esta imagen y explícame qué observas." }
                        messages = messages + "Tú: $shownText" + "VIP: Consultando..."
                        prompt = ""
                        OnDeviceLlmEngine.reply(context, shownText, messages, imageUri) { answer ->
                            (context as? android.app.Activity)?.runOnUiThread {
                                messages = messages.dropLast(1) + "VIP: $answer"
                            }
                        }
                    }
                }, searchWeb = { query ->
                    val question = query.trim()
                    if (question.isNotBlank()) {
                        messages = messages + "Tú: Busca información actual sobre: $question" + "VIP: Consultando fuentes web..."
                        OnlineSearchEngine.search(context, question) { result ->
                            val sourceMessage = "VIP: Resultados web:\n$result"
                            messages = messages.dropLast(1) + sourceMessage
                            if (OnDeviceLlmEngine.hasInstalledModel(context)) {
                                messages = messages + "VIP: Analizando los resultados..."
                                OnDeviceLlmEngine.reply(
                                    context,
                                    "Responde en español usando estos resultados web como contexto. Distingue los datos de las posibles limitaciones y no inventes información.\n\nPregunta: $question\n\n$result",
                                    messages
                                ) { answer ->
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        messages = messages.dropLast(1) + "VIP: $answer"
                                    }
                                }
                            }
                        }
                    }
                }, modifier = Modifier.padding(padding))
                "Escuela DJ" -> SchoolAcademyScreen(Modifier.padding(padding), onOpenLibrary = { section = "Biblioteca" })
                "Biblioteca" -> LibraryScreen(Modifier.padding(padding))
                else -> AppStudioScreen(Modifier.padding(padding))
            }
        }
        if (showConversationHistory) {
            val savedConversations = parseConversationArchive(conversationArchive)
            AlertDialog(
                onDismissRequest = { showConversationHistory = false },
                title = { Text("Conversaciones anteriores") },
                text = {
                    if (savedConversations.isEmpty()) {
                        Text("Todavía no hay conversaciones archivadas. Al iniciar un chat nuevo, el anterior se guardará aquí.")
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(savedConversations, key = { it.id }) { saved ->
                                Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f).clickable {
                                            messages = saved.messages
                                            showConversationHistory = false
                                            section = "Chat"
                                        }.padding(4.dp)) {
                                            Text(saved.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
                                            Text("${saved.messages.count { it.startsWith("Tú:") }} mensajes tuyos · ${saved.messages.size} elementos", color = Muted, fontSize = 11.sp)
                                        }
                                        IconButton(onClick = {
                                            conversationArchive = removeArchivedConversation(conversationArchive, saved.id)
                                            preferences.edit().putString("conversation_archive_v1", conversationArchive).apply()
                                        }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar conversación", tint = Muted) }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = { TextButton(onClick = { showConversationHistory = false }) { Text("Cerrar") } },
                containerColor = Panel
            )
        }
        if (showClearChat) {
            AlertDialog(
                onDismissRequest = { showClearChat = false },
                title = { Text("Limpiar conversación") },
                text = { Text("¿Quieres borrar el historial guardado de este chat? Esta acción no se puede deshacer.") },
                confirmButton = { TextButton(onClick = {
                    messages = listOf("¡Hola! Soy CHAT DJ'S VIP. Importa el modelo local para activar conocimientos generales sin conexión.")
                    showClearChat = false
                }) { Text("Borrar historial") } },
                dismissButton = { TextButton(onClick = { showClearChat = false }) { Text("Cancelar") } },
                containerColor = Panel
            )
        }
    }
}

@Composable
private fun ChatScreen(messages: List<String>, prompt: String, onPrompt: (String) -> Unit, send: (String, Uri?) -> Unit, searchWeb: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    var modelInstalled by remember { mutableStateOf(OnDeviceLlmEngine.hasInstalledModel(context)) }
    var importingModel by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importingModel = true
            OnDeviceLlmEngine.importModel(context, uri) { success, message ->
                importingModel = false
                modelInstalled = success || OnDeviceLlmEngine.hasInstalledModel(context)
                if (success) {
                    importError = null
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                } else {
                    importError = message
                }
            }
        }
    }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedImage = uri
        }
    }
    val textFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            ChatAttachmentExtractor.extract(context, uri) { imported ->
                imported.onSuccess { content ->
                    onPrompt((prompt + "\n\nContenido del documento adjunto:\n" + content).trim())
                    android.widget.Toast.makeText(context, "Documento leído y agregado al mensaje.", android.widget.Toast.LENGTH_LONG).show()
                }.onFailure { error ->
                    android.widget.Toast.makeText(context, error.message ?: "No se pudo leer el documento.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    var speechEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    var speechReady by remember { mutableStateOf(false) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            speechReady = status == TextToSpeech.SUCCESS
        }
        speechEngine = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            speechEngine = null
            speechReady = false
        }
    }
    LaunchedEffect(speechReady) {
        if (speechReady) speechEngine?.language = Locale("es", "MX")
    }
    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) onPrompt(spoken)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla con CHAT DJ'S VIP")
            }
            runCatching { speechLauncher.launch(intent) }
        }
    }
    val exportChatLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            val content = messages.joinToString("\n\n")
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo")
            }.isSuccess
            android.widget.Toast.makeText(
                context,
                if (saved) "Conversación exportada correctamente" else "No se pudo guardar la conversación",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    val chatListState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) chatListState.animateScrollToItem(messages.lastIndex)
    }
    Column(modifier.fillMaxSize().padding(14.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (modelInstalled) "Cerebro universal: modelo importado" else "Cerebro universal: falta instalar modelo",
                        color = if (modelInstalled) Accent else Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("LFM2.5 350M INT4 • 248 MB • respuestas locales", color = Muted, fontSize = 10.sp)
                }
                TextButton(
                    onClick = { modelPicker.launch(arrayOf("*/*")) },
                    enabled = !importingModel
                ) {
                    if (importingModel) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Accent,
                            strokeWidth = 2.dp
                        )
                        Text(" Importando…")
                    } else {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Accent)
                        Text(" Importar modelo")
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    runCatching {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/litert-community/LFM2.5-350M/resolve/main/LFM2.5-350M_int4.litertlm")))
                    }
                }) { Text("Obtener modelo") }
                IconButton(
                    onClick = {
                        val query = prompt.ifBlank {
                            messages.asReversed().firstOrNull { it.startsWith("Tú:") }?.removePrefix("Tú:")?.trim().orEmpty()
                        }
                        if (query.isNotBlank()) searchWeb(query)
                        else android.widget.Toast.makeText(context, "Escribe una pregunta para buscar.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar en web", tint = Accent)
                }
                TextButton(
                    onClick = { exportChatLauncher.launch("Chat-DJs-VIP.txt") },
                    enabled = messages.isNotEmpty()
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = Accent)
                    Text("  Exportar conversación")
                }
            }
        }
        LazyColumn(state = chatListState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { msg ->
                val user = msg.startsWith("Tú:")
                Surface(color = if (user) Color(0xFF174C43) else Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, Modifier.weight(1f).padding(2.dp), color = Color.White, fontSize = 15.sp)
                        if (msg.startsWith("VIP:")) {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Respuesta CHAT DJ'S VIP", msg.removePrefix("VIP:").trim()))
                                android.widget.Toast.makeText(context, "Respuesta copiada", android.widget.Toast.LENGTH_SHORT).show()
                            }) { Icon(Icons.Default.ContentCopy, "Copiar respuesta", tint = Accent) }
                            IconButton(onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg.removePrefix("VIP:").trim())
                                }
                                runCatching { context.startActivity(Intent.createChooser(shareIntent, "Compartir respuesta")) }
                            }) { Icon(Icons.Default.Share, "Compartir respuesta", tint = Accent) }
                            IconButton(onClick = { speechEngine?.speak(msg.removePrefix("VIP:").trim(), TextToSpeech.QUEUE_FLUSH, null, "vip_reply") }) { Icon(Icons.Default.VolumeUp, "Escuchar respuesta", tint = Accent) }
                        }
                    }
                }
            }
        }
        if (selectedImage != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Icon(Icons.Default.Image, null, tint = Accent)
                Text("Imagen adjunta", color = Accent, modifier = Modifier.weight(1f).padding(start = 8.dp))
                TextButton(onClick = { selectedImage = null }) { Text("Quitar") }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Panel, CircleShape).padding(6.dp)) {
            TextField(value = prompt, onValueChange = onPrompt, modifier = Modifier.weight(1f), placeholder = { Text("Pregunta lo que quieras…", color = Muted) }, colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ))
            IconButton(onClick = { textFilePicker.launch(arrayOf("text/*", "application/json", "application/xml", "application/pdf")) }) {
                Icon(Icons.Default.AttachFile, "Adjuntar texto, código o PDF", tint = Accent)
            }
            IconButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                Icon(Icons.Default.Image, "Adjuntar imagen", tint = Accent)
            }
            IconButton(onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Default.Mic, "Hablar", tint = Accent)
            }
            IconButton(onClick = { send(prompt, selectedImage); selectedImage = null }) { Icon(Icons.Default.Send, "Enviar", tint = Accent) }
        }
    }
    importError?.let { diagnostic ->
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("No se pudo activar el cerebro local") },
            text = { Text(diagnostic) },
            confirmButton = {
                TextButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Diagnóstico CHAT DJ\u0027S VIP")
                        putExtra(Intent.EXTRA_TEXT, diagnostic)
                    }
                    runCatching { context.startActivity(Intent.createChooser(share, "Compartir diagnóstico")) }
                }) { Text("Compartir diagnóstico") }
            },
            dismissButton = {
                TextButton(onClick = { importError = null }) { Text("Cerrar") }
            },
            containerColor = Panel
        )
    }
}

private fun assistantReply(input: String): String = djKnowledgeReply(input)

@Composable
private fun SchoolScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val progressPrefs = remember(context) { context.getSharedPreferences("chat_djs_vip_school", android.content.Context.MODE_PRIVATE) }
    var completedTopics by remember {
        val saved = progressPrefs.getString("completed_topics", null)
        mutableStateOf(if (saved.isNullOrBlank()) emptySet() else runCatching {
            val json = JSONArray(saved)
            List(json.length()) { index -> json.getString(index) }.toSet()
        }.getOrDefault(emptySet()))
    }
    fun saveProgress(updated: Set<String>) {
        completedTopics = updated
        val json = JSONArray()
        updated.forEach { json.put(it) }
        progressPrefs.edit().putString("completed_topics", json.toString()).apply()
    }
    val topics = listOf(
        "Serato DJ" to "Biblioteca, crates, puntos Cue, loops, beatmatch y mezcla armónica.",
        "rekordbox" to "Preparación USB, análisis de pistas, hot cues, memory cues y exportación para CDJ.",
        "VirtualDJ" to "Decks, sampler, stems, efectos, automix y configuración de controladoras.",
        "Traktor" to "Decks, beatgrids, Remix Decks, efectos y mapeo MIDI.",
        "djay" to "Mezcla en móvil/tablet, Neural Mix, sincronización y controladores compatibles.",
        "Tornamesas y vinilos" to "Conexión PHONO/LINE, cápsula, aguja, contrapeso, antiskating, pitch y scratch.",
        "Controladoras, CDJ y mezcladoras" to "Ganancia, EQ, filtros, canales, crossfader, monitores y conexiones.",
        "Mezcla armónica y tonalidad" to "Compatibilidad tonal, Camelot, cambios de tonalidad y selección musical.",
        "Transiciones profesionales" to "Mezclas largas y cortas, cambios de energía, fraseo y transiciones por género.",
        "Preparación de biblioteca" to "Análisis BPM/Key, beatgrids, cues, playlists, etiquetas y copias de seguridad.",
        "Efectos, loops y creatividad" to "Echo, reverb, filtros, rolls, loops manuales y uso moderado de FX.",
        "Preparación de eventos" to "Estructura de sesión, lectura de pista, niveles, respaldo y plan ante fallas."
    )
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var searchText by remember { mutableStateOf("") }
    var studentQuestion by remember { mutableStateOf("") }
    var teacherAnswer by remember { mutableStateOf("") }
    var askingTeacher by remember { mutableStateOf(false) }
    val modelReady = remember(context) { OnDeviceLlmEngine.hasInstalledModel(context) }
    val filteredTopics = topics.filter { (name, desc) ->
        name.contains(searchText, ignoreCase = true) || desc.contains(searchText, ignoreCase = true)
    }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("ESCUELA DJ", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Aprende desde principiante hasta nivel profesional.", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
        Text("Progreso: ${completedTopics.size} de ${topics.size} temas completados", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
        LinearProgressIndicator(
            progress = { completedTopics.size.toFloat() / topics.size.coerceAtLeast(1) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            color = Accent,
            trackColor = Panel
        )
        OutlinedTextField(
            value = searchText,
            onValueChange = { searchText = it },
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            singleLine = true,
            placeholder = { Text("Buscar tema, técnica o programa…") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Accent) },
            trailingIcon = {
                if (searchText.isNotEmpty()) IconButton(onClick = { searchText = "" }) {
                    Icon(Icons.Default.Close, contentDescription = "Borrar búsqueda")
                }
            }
        )
        Text("${filteredTopics.size} temas disponibles", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        if (filteredTopics.isEmpty()) {
            Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, contentDescription = null, tint = Muted, modifier = Modifier.size(30.dp))
                    Text("No encontré ese tema", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    Text("Prueba con BPM, mezcla, Serato o transiciones.", color = Muted, fontSize = 12.sp)
                }
            }
        } else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(filteredTopics) { (name, desc) ->
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable {
                    selectedTopic = name
                    studentQuestion = ""
                    teacherAnswer = ""
                }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (name in completedTopics) Icons.Default.CheckCircle else Icons.Default.School, null, tint = Accent, modifier = Modifier.size(28.dp))
                        Column(Modifier.weight(1f).padding(start = 12.dp)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(desc, color = Muted, fontSize = 13.sp)
                            if (name in completedTopics) Text("Tema completado", color = Accent, fontSize = 11.sp)
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = "Abrir lección", tint = Accent)
                    }
                }
            }
        }
    }
    selectedTopic?.let { topic ->
        AlertDialog(
            onDismissRequest = { if (!askingTeacher) selectedTopic = null },
            title = { Text("Clase: $topic") },
            text = {
                Column {
                    Text(lessonFor(topic), fontSize = 14.sp, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Text("PREGÚNTALE AL PROFESOR DJ", fontWeight = FontWeight.Bold, color = Accent, fontSize = 13.sp)
                    Text(
                        if (modelReady) "Tu cerebro local puede explicar esta lección y resolver tus dudas."
                        else "Cerebro local no detectado: puedes consultar la guía básica incluida.",
                        color = Muted, fontSize = 11.sp
                    )
                    OutlinedTextField(
                        value = studentQuestion,
                        onValueChange = { studentQuestion = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ej. ¿Cómo ajusto el beatgrid?") },
                        enabled = !askingTeacher,
                        minLines = 2
                    )
                    if (askingTeacher) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                            Text(" El profesor está preparando la explicación…", color = Muted, fontSize = 12.sp)
                        }
                    }
                    if (teacherAnswer.isNotBlank()) {
                        Surface(color = Color(0xFF123D36), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(10.dp)) {
                                Text("RESPUESTA DEL PROFESOR", color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(teacherAnswer, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(
                        enabled = studentQuestion.isNotBlank() && !askingTeacher,
                        onClick = {
                            val question = studentQuestion.trim()
                            askingTeacher = true
                            teacherAnswer = ""
                            val prompt = """
Eres el profesor de la Escuela DJ de CHAT DJ'S VIP. Enseña en español mexicano, con paciencia y de forma práctica.
Tema de la clase: $topic
Material de la lección:
${lessonFor(topic)}
Duda del alumno: $question
Responde con pasos claros y un ejercicio práctico. Si pregunta por botones, menús, conexiones o ajustes, primero solicita marca/modelo y versión cuando falten. No inventes controles, valores de calibración ni compatibilidad. Distingue software (Serato DJ, rekordbox, VirtualDJ, Traktor y djay) y equipo físico. Si no estás seguro, dilo.
                            """.trimIndent()
                            if (modelReady) {
                                OnDeviceLlmEngine.reply(context, prompt, emptyList()) { answer ->
                                    teacherAnswer = answer
                                    askingTeacher = false
                                }
                            } else {
                                teacherAnswer = djKnowledgeReply(question)
                                askingTeacher = false
                            }
                        }
                    ) { Text("Preguntar al profesor") }
                    TextButton(
                        onClick = {
                            saveProgress(if (topic in completedTopics) completedTopics - topic else completedTopics + topic)
                            selectedTopic = null
                        }
                    ) { Text(if (topic in completedTopics) "Quitar tema completado" else "Marcar como aprendido") }
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!askingTeacher) selectedTopic = null }) { Text("Cerrar") }
            },
            containerColor = Panel,
            titleContentColor = Color.White,
            textContentColor = Color.White
        )
    }
}

fun lessonFor(topic: String): String = when (topic) {
    "Controladoras, CDJ y mezcladoras" -> """MAPA DEL EQUIPO DJ Y PRÁCTICA GUIADA
OBJETIVO: reconocer el recorrido de la señal y operar con seguridad.
1. DECKS: cada deck reproduce una pista. Localiza Play/Pause, Cue, jog wheel, pitch fader y controles de loops.
2. CANAL: sigue la señal desde Trim/Gain hasta EQ, fader y crossfader. El Trim ajusta entrada; el fader controla cuánto aporta el canal a la mezcla.
3. MASTER: identifica Master Out y conecta a bocinas activas o amplificador compatible. No conectes una salida de potencia a una entrada de línea.
4. AUDÍFONOS: conecta PHONES, pulsa CUE del canal entrante y ajusta el nivel de audífonos. Comprueba que no se envía al público.
5. MEDIDORES: reproduce una sección fuerte y ajusta Trim sin mantener los medidores en rojo. Deja margen en Master.
6. PRÁCTICA: con volúmenes bajos, reproduce una pista, preescucha otra y cambia entre canales. Anota qué control ajusta entrada, mezcla y salida.
RETO: explica con tus palabras la ruta Deck → canal → Master → bocinas. Si me dices marca y modelo, adapto el mapa a tu equipo.
ERRORES A EVITAR: conectar con volumen alto, confundir PHONO con LINE y corregir saturación subiendo el Master."""
    "Controladoras, CDJ y mezcladoras DJ" -> """MAPA DEL EQUIPO DJ Y PRÁCTICA GUIADA
OBJETIVO: reconocer decks, canales, salidas y monitoreo. Identifica Play/Pause, Cue, jog, pitch, Trim, EQ, fader, crossfader y Master.
1. Cada deck reproduce una pista independiente.
2. La señal entra al canal; Trim ajusta nivel de entrada, EQ modifica frecuencias y el fader regula el nivel del canal.
3. Master Out alimenta bocinas activas o un amplificador compatible.
4. PHONES permite preescuchar; activa CUE en el canal entrante y ajusta el nivel de audífonos.
5. Reproduce una sección fuerte y evita que los medidores permanezcan en rojo.
6. PRÁCTICA: reproduce una pista, prepara otra con CUE y realiza un cambio suave con niveles bajos.
RETO: describe la ruta Deck → canal → Master → bocinas. Dime marca y modelo para adaptar el diagrama.
"""
    "Serato DJ" -> """SERATO DJ • CONFIGURACIÓN, PREPARACIÓN Y MEZCLA
1. COMPATIBILIDAD: confirma que tu modelo de controlador y versión de Serato estén soportados. Instala software y driver oficial si corresponde.
2. AUDIO: abre Setup y revisa que el hardware aparezca. Comprueba Master, Headphones y asignación de salidas; prueba primero a volumen bajo.
3. BIBLIOTECA: importa música desde una carpeta estable del dispositivo. No muevas los archivos después de analizarlos; crea crates por género, energía y evento.
4. ANÁLISIS: analiza BPM y tonalidad. Reproduce el inicio y una parte fuerte para comprobar BPM y beatgrid; corrige manualmente si los marcadores no coinciden con los golpes.
5. PREPARACIÓN: coloca Hot Cues en intro, entrada vocal, drop y outro. Usa loops para extender una sección y verifica que el loop cierre a tiempo.
6. MEZCLA: activa CUE en el deck entrante, iguala tempo, alinea golpes, espera una frase compatible y realiza el cambio de graves con EQ y faders.
PRÁCTICA: prepara dos canciones de tempo parecido. Haz una transición de 32 compases, grábala y evalúa sincronía, fraseo, niveles y claridad.
DIAGNÓSTICO: si no hay preescucha, revisa Phones, CUE y Output Routing; si hay cortes, revisa driver, cable y carga del equipo.
Dime versión de Serato y modelo exacto para adaptar cada paso a tu configuración."""
    "rekordbox" -> """REKORDBOX • COLECCIÓN, ANÁLISIS Y USB
1. MODO DE TRABAJO: distingue Export para preparar medios compatibles y Performance para mezclar con el equipo/software admitido por tu licencia y versión.
2. COLECCIÓN: importa pistas desde carpetas organizadas. Mantén una ubicación estable y haz respaldo de los archivos originales.
3. ANÁLISIS: analiza BPM, tonalidad y forma de onda. Revisa beatgrid al inicio y en cambios de tempo; ajusta downbeat y grid cuando sea necesario.
4. CUES: coloca Memory Cues para marcar secciones y Hot Cues para saltos inmediatos. Nombra los puntos de forma consistente.
5. PLAYLISTS: crea listas por evento, género y energía. Revisa que los archivos no estén perdidos y que cada pista se reproduzca correctamente.
6. EXPORTACIÓN: confirma el formato y compatibilidad de la memoria con el reproductor destino. Exporta desde la función oficial, espera a que finalice y expulsa de forma segura.
PRÁCTICA: prepara una playlist de 10 pistas, revisa beatgrid y cues de tres canciones y exporta a una memoria de prueba.
VERIFICACIÓN: prueba esa memoria en el CDJ/XDJ exacto del evento; no supongas que todos los modelos aceptan los mismos formatos o funciones.
Dime versión de rekordbox y modelo de reproductor para personalizar el procedimiento."""
    "VirtualDJ" -> """VIRTUALDJ • CONTROLADOR, AUDIO Y PERFORMANCE
1. HARDWARE: conecta el controlador compatible y comprueba en Settings > Controllers que el dispositivo sea reconocido. Evita modificar el mapeo antes de respaldarlo.
2. AUDIO: en Settings > Audio selecciona una configuración compatible con tu interfaz. Asigna Master y Headphones según las salidas físicas reales.
3. BIBLIOTECA: añade carpetas musicales, analiza BPM/Key y organiza listas por género, energía y ocasión. Verifica que las rutas de archivos sigan disponibles.
4. DECKS: carga dos pistas, revisa BPM y beatgrid, y utiliza audífonos para preparar la pista entrante.
5. HERRAMIENTAS: practica Hot Cues, loops y efectos por separado. Si usas stems o sampler, comprueba consumo de CPU y evita saturar la salida.
6. TRANSICIÓN: alinea golpes y frases; cambia EQ y faders gradualmente. Graba una práctica para escuchar errores.
PRÁCTICA: prepara dos pistas, crea tres puntos Cue útiles y realiza una transición de 16 o 32 compases.
DIAGNÓSTICO: si no hay audífonos, revisa la configuración de salidas y el modo de audio; si el controlador no aparece, verifica cable, permisos y compatibilidad.
Dime versión de VirtualDJ y modelo de controlador para ajustar esta ruta."""
    "Traktor" -> """TRAKTOR • AUDIO, COLLECTION Y CONTROL
1. INTERFAZ: en Preferences > Audio Setup selecciona el dispositivo correcto. Ajusta Sample Rate y latencia de forma compatible con tu equipo.
2. ROUTING: en Output Routing asigna Master y Monitor/Headphones a los canales físicos correctos. Prueba cada salida a volumen bajo.
3. COLECCIÓN: importa pistas a Collection, analiza BPM y revisa beatgrids. Corrige el grid cuando el tempo o el downbeat estén mal detectados.
4. DECKS: identifica el tipo de deck y practica carga, reproducción, Cue y pitch. Comprende la diferencia entre tempo y fase antes de depender de Sync.
5. HOTCUES, LOOPS Y FX: prepara puntos musicales, prueba loops y aplica un efecto a la vez. Controla el nivel para evitar saturación.
6. MIDI: confirma el mapeo del controlador y exporta una copia antes de modificar asignaciones.
PRÁCTICA: prepara dos pistas, corrige un beatgrid, crea puntos de entrada y salida y realiza una transición sin Sync; repítela usando Sync y compara.
DIAGNÓSTICO: si hay cortes, aumenta gradualmente el búfer; si falla Monitor, revisa Output Routing y la interfaz seleccionada.
Dime versión de Traktor, sistema operativo e interfaz/controlador para adaptar los pasos."""
    "djay" -> "RUTA DE APRENDIZAJE\n1. Conecta un controlador compatible y concede permisos de audio.\n2. Selecciona la salida principal y la preescucha si tu dispositivo lo permite.\n3. Carga pistas, revisa BPM y forma de onda.\n4. Practica cue points, loops y ecualización.\n5. Explora Neural Mix si tu versión y dispositivo lo admiten.\n6. Verifica compatibilidad exacta del controlador antes de comprarlo."
    "Tornamesas y vinilos" -> "CONFIGURACIÓN SEGURA\n1. Coloca la tornamesa en una superficie firme y nivelada.\n2. Instala cápsula y aguja siguiendo el fabricante.\n3. Ajusta contrapeso y fuerza de apoyo al rango recomendado por la cápsula; no adivines el valor.\n4. Ajusta antiskating según el manual.\n5. Conecta PHONO a entrada PHONO con tierra; usa LINE solo si el preamplificador está activado y la mezcladora recibe LINE.\n6. Limpia el vinilo y practica cueing sin forzar la aguja."
    "Mezcla armónica y tonalidad" -> "MEZCLA ARMÓNICA\n1. Analiza la tonalidad de ambas canciones, pero confirma de oído: el análisis puede equivocarse.\n2. Aprende la rueda Camelot: combina primero la misma tonalidad o tonalidades vecinas compatibles.\n3. Prueba la mezcla en una sección con pocos elementos melódicos.\n4. Evita superponer dos melodías o voces que choquen.\n5. Si cambias de tonalidad, hazlo con una transición breve o una sección rítmica.\n6. Prioriza fraseo, energía y gusto musical por encima de una regla matemática."
    "Transiciones profesionales" -> "TRANSICIONES PROFESIONALES\n1. Define qué función cumple cada canción: abrir, mantener energía, subir o cerrar.\n2. Cuenta frases y prepara la entrada en una sección musical compatible.\n3. Para una mezcla larga, introduce primero medios/agudos y controla los graves.\n4. Para un cambio rápido, prepara un punto de entrada limpio y evita cortar una frase importante.\n5. Escucha la mezcla en audífonos y en Master; evita saturar.\n6. Graba prácticas y anota qué transición funcionó y por qué."
    "Preparación de biblioteca" -> "PREPARACIÓN DE BIBLIOTECA\n1. Conserva archivos originales y trabaja con copias organizadas.\n2. Usa nombres consistentes y etiquetas de artista, género, BPM y tonalidad.\n3. Analiza BPM/Key y revisa manualmente beatgrids en canciones con tempo variable.\n4. Crea playlists por género, energía, ocasión y momento del evento.\n5. Coloca Hot Cues en intros, drops, breaks y puntos de salida.\n6. Haz copia de seguridad de música, playlists y ajustes antes de un evento."
    "Efectos, loops y creatividad" -> "EFECTOS Y LOOPS\n1. Aprende primero el sonido limpio y la mezcla sin efectos.\n2. Usa filtro para despejar frecuencias durante una transición, sin exagerar.\n3. Prueba Echo o Reverb en el final de una frase y retira la pista con control.\n4. Practica loops de 1, 2, 4 y 8 compases para extender una sección.\n5. Comprueba que el efecto esté sincronizado y que el nivel de retorno no sature.\n6. Usa un efecto principal a la vez hasta dominar su duración y salida."
    "Preparación de eventos" -> "PREPARACIÓN DE EVENTOS\n1. Confirma horario, duración, estilos, solicitudes y restricciones del evento.\n2. Prepara varias playlists y una ruta alternativa por si cambia el ambiente.\n3. Revisa cables, adaptadores, audífonos, alimentación y compatibilidad.\n4. Prueba sonido a volumen bajo y ajusta niveles sin llegar al rojo.\n5. Lleva respaldo de música en otro dispositivo y evita depender de internet.\n6. Antes de comenzar, confirma que puedes reproducir, preescuchar y grabar si lo necesitas."
    else -> "CONCEPTOS ESENCIALES\n1. Identifica entradas y salidas antes de conectar.\n2. Conecta Master a bocinas activas o amplificador adecuado.\n3. Usa CUE para escuchar en audífonos sin enviar esa señal al público.\n4. Ajusta ganancia sin llevar los medidores a rojo.\n5. Ecualiza con cambios graduales y evita saturación.\n6. Baja volúmenes antes de conectar o desconectar equipos.\nDime marca y modelo para darte un diagrama de conexión específico."
}

@Composable
private fun LibraryScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("chat_djs_vip", android.content.Context.MODE_PRIVATE) }
    var tracks by remember {
        val saved = prefs.getString("audio_library", null)
        mutableStateOf(
            if (saved.isNullOrBlank()) emptyList()
            else runCatching {
                val json = JSONArray(saved)
                List(json.length()) { index -> json.getString(index) }
            }.getOrDefault(emptyList())
        )
    }
    var currentUri by remember { mutableStateOf<String?>(null) }
    var currentTitle by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var player by remember { mutableStateOf<MediaPlayer?>(null) }
    var searchText by remember { mutableStateOf("") }
    var showFavoritesOnly by remember { mutableStateOf(false) }
    var showPracticeOnly by remember { mutableStateOf(false) }
    var practiceTracks by remember {
        val saved = prefs.getString("practice_audio_tracks", null)
        mutableStateOf(if (saved.isNullOrBlank()) emptySet() else runCatching { val json = JSONArray(saved); List(json.length()) { json.getString(it) }.toSet() }.getOrDefault(emptySet()))
    }
    var sortAscending by remember { mutableStateOf(prefs.getBoolean("audio_sort_ascending", true)) }
    var favorites by remember {
        val saved = prefs.getString("favorite_audio_tracks", null)
        mutableStateOf(
            if (saved.isNullOrBlank()) emptySet()
            else runCatching {
                val json = JSONArray(saved)
                List(json.length()) { index -> json.getString(index) }.toSet()
            }.getOrDefault(emptySet())
        )
    }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(0) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }
    var isSeeking by remember { mutableStateOf(false) }
    var playbackSpeed by remember { mutableFloatStateOf(prefs.getFloat("audio_playback_speed", 1f).coerceIn(0.5f, 2f)) }
    var repeatCurrent by remember { mutableStateOf(prefs.getBoolean("audio_repeat_current", false)) }
    var autoPlayNext by remember { mutableStateOf(prefs.getBoolean("audio_auto_play_next", true)) }
    var playbackQueue by remember {
        val savedQueue = prefs.getString("audio_playback_queue", null)
        mutableStateOf(
            if (savedQueue.isNullOrBlank()) emptyList()
            else runCatching {
                val json = JSONArray(savedQueue)
                restorePlaybackQueue(List(json.length()) { index -> json.getString(index) }, tracks)
            }.getOrDefault(emptyList())
        )
    }
    var shuffleEnabled by remember { mutableStateOf(prefs.getBoolean("audio_shuffle_enabled", false)) }
    var showQueueDialog by remember { mutableStateOf(false) }
    LaunchedEffect(playbackQueue) {
        val json = JSONArray()
        playbackQueue.forEach { json.put(it) }
        prefs.edit().putString("audio_playback_queue", json.toString()).apply()
    }
    LaunchedEffect(tracks) {
        val validQueue = restorePlaybackQueue(playbackQueue, tracks)
        if (validQueue != playbackQueue) playbackQueue = validQueue
    }
    val latestCurrentUri by rememberUpdatedState(currentUri)
    val latestPlaybackQueue by rememberUpdatedState(playbackQueue)

    LaunchedEffect(currentUri, isPlaying, isLoading) {
        while (currentUri != null && isPlaying && !isLoading) {
            if (!isSeeking) {
                currentPositionMs = runCatching { player?.currentPosition ?: currentPositionMs }.getOrDefault(currentPositionMs)
            }
            delay(400)
        }
    }

    val filteredTracks = tracks.filter { uriString ->
        val uri = Uri.parse(uriString)
        val title = runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Audio"
        val metadata = readLibraryTrackMetadata(context, uri)
        val query = searchText.trim()
        (title.contains(query, ignoreCase = true) || metadata.artist?.contains(query, ignoreCase = true) == true || metadata.format?.contains(query, ignoreCase = true) == true) &&
            (!showFavoritesOnly || uriString in favorites) &&
            (!showPracticeOnly || uriString in practiceTracks)
    }

    val allSortedTracks = tracks.sortedBy { uriString ->
        val uri = Uri.parse(uriString)
        (runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "Audio").lowercase(Locale.ROOT)
    }.let { if (sortAscending) it else it.reversed() }
    val latestAllSortedTracks by rememberUpdatedState(allSortedTracks)
    val latestRepeatCurrent by rememberUpdatedState(repeatCurrent)
    val latestAutoPlayNext by rememberUpdatedState(autoPlayNext)
    val latestPlaybackSpeed by rememberUpdatedState(playbackSpeed)

    val sortedTracks = filteredTracks.sortedBy { uriString ->
        val uri = Uri.parse(uriString)
        (runCatching {
            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment ?: "Audio").lowercase(Locale.ROOT)
    }.let { if (sortAscending) it else it.reversed() }

    fun toggleFavorite(uriString: String) {
        favorites = if (uriString in favorites) favorites - uriString else favorites + uriString
        val json = JSONArray()
        favorites.forEach { json.put(it) }
        prefs.edit().putString("favorite_audio_tracks", json.toString()).apply()
    }

    fun togglePracticeTrack(uriString: String) {
        practiceTracks = if (uriString in practiceTracks) practiceTracks - uriString else practiceTracks + uriString
        val json = JSONArray()
        practiceTracks.forEach { json.put(it) }
        prefs.edit().putString("practice_audio_tracks", json.toString()).apply()
    }

    fun saveTracks(updated: List<String>) {
        tracks = updated
        val json = JSONArray()
        updated.forEach { json.put(it) }
        prefs.edit().putString("audio_library", json.toString()).apply()
    }

    fun stopPlayback() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        currentUri = null
        currentTitle = null
        isPlaying = false
        isLoading = false
        currentPositionMs = 0
        durationMs = 0
        seekPositionMs = 0f
        isSeeking = false
    }

    fun playTrack(uriString: String, title: String) {
        stopPlayback()
        currentUri = uriString
        currentTitle = title
        isLoading = true
        runCatching {
            MediaPlayer().also { mp ->
                player = mp
                mp.setDataSource(context, Uri.parse(uriString))
                mp.setOnPreparedListener {
                    durationMs = it.duration.coerceAtLeast(0)
                    currentPositionMs = 0
                    seekPositionMs = 0f
                    runCatching { it.setPlaybackParams(PlaybackParams().setSpeed(latestPlaybackSpeed)) }
                    isLoading = false
                    isPlaying = true
                    it.start()
                }
                mp.setOnCompletionListener {
                    if (latestRepeatCurrent) {
                        runCatching {
                            it.seekTo(0)
                            it.start()
                        }
                        currentPositionMs = 0
                        seekPositionMs = 0f
                        isPlaying = true
                    } else {
                        val nextUri = nextTrackAfterCompletion(
                            completedUri = latestCurrentUri,
                            queue = latestPlaybackQueue,
                            library = latestAllSortedTracks,
                            autoPlayNext = latestAutoPlayNext
                        )
                        if (nextUri != null) {
                            val next = Uri.parse(nextUri)
                            val nextTitle = runCatching {
                                context.contentResolver.query(next, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) cursor.getString(0) else null
                                }
                            }.getOrNull() ?: next.lastPathSegment?.substringAfterLast('/') ?: "Audio"
                            // No sobrescribir el estado de carga de la siguiente pista.
                            playTrack(nextUri, nextTitle)
                        } else {
                            runCatching { it.seekTo(0) }
                            currentPositionMs = durationMs
                            isPlaying = false
                            isLoading = false
                        }
                    }
                }
                mp.setOnErrorListener { _, _, _ ->
                    stopPlayback()
                    android.widget.Toast.makeText(context, "No se pudo reproducir este archivo. Comprueba que siga disponible y sea compatible.", android.widget.Toast.LENGTH_LONG).show()
                    true
                }
                mp.prepareAsync()
            }
        }.onFailure {
            stopPlayback()
            android.widget.Toast.makeText(context, "No se pudo abrir esta canción. Revisa el permiso del archivo o su formato.", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun playAdjacentTrack(direction: Int) {
        val activeQueue = playbackQueue.filter { it in allSortedTracks }.ifEmpty { allSortedTracks }
        val currentIndex = activeQueue.indexOf(currentUri)
        val targetUri = activeQueue.getOrNull(currentIndex + direction) ?: return
        val target = Uri.parse(targetUri)
        val targetTitle = runCatching {
            context.contentResolver.query(target, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull() ?: target.lastPathSegment?.substringAfterLast('/') ?: "Audio"
        playTrack(targetUri, targetTitle)
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isNotEmpty()) {
            val added = uris.mapNotNull { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                uri.toString().takeIf { it !in tracks }
            }.distinct()
            if (added.isNotEmpty()) {
                saveTracks(tracks + added)
                android.widget.Toast.makeText(
                    context,
                    if (added.size == 1) "Se agregó 1 canción" else "Se agregaron ${added.size} canciones",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(context, "Las canciones seleccionadas ya estaban en tu biblioteca", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { player?.release() }; player = null }
    }

    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Biblioteca musical", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Selecciona varias canciones en una sola operación. La biblioteca conserva tus archivos y permite buscar, marcar favoritas y reproducirlos.", color = Muted)
        Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LibraryMusic, null)
            Text("  Agregar una o varias canciones")
        }
        if (tracks.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showFavoritesOnly && !showPracticeOnly,
                    onClick = { showFavoritesOnly = false; showPracticeOnly = false },
                    label = { Text("Todas (${tracks.size})") },
                    leadingIcon = { Icon(Icons.Default.LibraryMusic, null) }
                )
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = { showFavoritesOnly = true; showPracticeOnly = false },
                    label = { Text("Favoritas (${favorites.count { it in tracks }})") },
                    leadingIcon = { Icon(Icons.Default.Favorite, null) }
                )
                FilterChip(
                    selected = showPracticeOnly,
                    onClick = { showPracticeOnly = !showPracticeOnly; showFavoritesOnly = false },
                    label = { Text("Prácticas (${practiceTracks.count { it in tracks }})") },
                    leadingIcon = { Icon(Icons.Default.School, null) }
                )
                TextButton(onClick = { sortAscending = !sortAscending; prefs.edit().putBoolean("audio_sort_ascending", sortAscending).apply() }) {
                    Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = Accent)
                    Text(if (sortAscending) "A–Z" else "Z–A")
                }
                TextButton(onClick = {
                    shuffleEnabled = !shuffleEnabled
                    prefs.edit().putBoolean("audio_shuffle_enabled", shuffleEnabled).apply()
                    val activeUri = currentUri
                    playbackQueue = if (shuffleEnabled) {
                        val remaining = allSortedTracks.filter { it != activeUri }.shuffled()
                        if (activeUri != null && activeUri in allSortedTracks) listOf(activeUri) + remaining else remaining
                    } else emptyList()
                    if (shuffleEnabled && activeUri == null) {
                        playbackQueue.firstOrNull()?.let { firstUri ->
                            val first = Uri.parse(firstUri)
                            val firstTitle = runCatching {
                                context.contentResolver.query(first, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                    if (cursor.moveToFirst()) cursor.getString(0) else null
                                }
                            }.getOrNull() ?: first.lastPathSegment?.substringAfterLast('/') ?: "Audio"
                            playTrack(firstUri, firstTitle)
                        }
                    }
                }) {
                    Text(if (shuffleEnabled) "Aleatorio: Sí" else "Aleatorio: No")
                }
            }
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar por canción, artista o formato…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Accent) },
                trailingIcon = {
                    if (searchText.isNotEmpty()) IconButton(onClick = { searchText = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                    }
                }
            )
        }
        if (tracks.isEmpty()) {
            Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(36.dp))
                    Text("Tu biblioteca está vacía", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text("Agrega archivos MP3, WAV, M4A u otros formatos compatibles con Android.", color = Muted, fontSize = 12.sp)
                }
            }
        } else {
            Text("${filteredTracks.size} de ${tracks.size} canción(es)", color = Accent, fontWeight = FontWeight.SemiBold)
            if (filteredTracks.isEmpty()) {
                Text("No hay canciones que coincidan con la búsqueda.", color = Muted, fontSize = 13.sp)
            } else LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(sortedTracks) { uriString ->
                    val uri = Uri.parse(uriString)
                    val title = remember(uriString) {
                        runCatching {
                            context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                if (cursor.moveToFirst()) cursor.getString(0) else null
                            }
                        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Audio"
                    }
                    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MusicNote, null, tint = Accent)
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(title, fontWeight = FontWeight.Medium, maxLines = 1)
                                val metadata = remember(uriString) { readLibraryTrackMetadata(context, uri) }
                                Text(
                                    listOfNotNull(metadata.artist, metadata.duration, metadata.format).joinToString(" • ").ifBlank {
                                        if (currentUri == uriString && isLoading) "Cargando…" else if (currentUri == uriString && isPlaying) "Reproduciendo" else "En biblioteca"
                                    },
                                    color = Muted,
                                    fontSize = 10.sp,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = {
                                if (currentUri == uriString && isPlaying) {
                                    runCatching { player?.pause() }
                                    isPlaying = false
                                } else if (currentUri == uriString && !isPlaying && !isLoading && player != null) {
                                    runCatching { player?.start() }
                                    isPlaying = true
                                } else {
                                    playbackQueue = if (shuffleEnabled) {
                                        listOf(uriString) + allSortedTracks.filter { it != uriString }.shuffled()
                                    } else if (uriString in playbackQueue) {
                                        playbackQueue
                                    } else emptyList()
                                    playTrack(uriString, title)
                                }
                            }) {
                                Icon(if (currentUri == uriString && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pausar" else "Reproducir", tint = Accent)
                            }
                            IconButton(onClick = {
                                val updatedQueue = addTrackToPlaybackQueue(currentUri, playbackQueue, allSortedTracks, uriString)
                                if (uriString == currentUri || uriString in playbackQueue) {
                                    android.widget.Toast.makeText(context, "La canción ya está en la cola", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    playbackQueue = updatedQueue
                                    android.widget.Toast.makeText(context, "Canción añadida a la cola", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Add, contentDescription = "Añadir a la cola", tint = Accent)
                            }
                            IconButton(onClick = { toggleFavorite(uriString) }) {
                                Icon(
                                    if (uriString in favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = if (uriString in favorites) "Quitar de favoritas" else "Agregar a favoritas",
                                    tint = if (uriString in favorites) Color(0xFFFF6B81) else Muted
                                )
                            }
                            IconButton(onClick = { togglePracticeTrack(uriString) }) {
                                Icon(
                                    if (uriString in practiceTracks) Icons.Default.CheckCircle else Icons.Default.Add,
                                    contentDescription = if (uriString in practiceTracks) "Quitar de prácticas" else "Agregar a prácticas DJ",
                                    tint = if (uriString in practiceTracks) Accent else Muted
                                )
                            }
                            IconButton(onClick = {
                                runCatching {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "audio/*"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Compartir canción"))
                                }.onFailure {
                                    android.widget.Toast.makeText(context, "No se pudo compartir este audio", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir canción", tint = Accent)
                            }
                            IconButton(onClick = {
                                if (currentUri == uriString) stopPlayback()
                                if (uriString in favorites) {
                                    favorites = favorites - uriString
                                    val favoriteJson = JSONArray()
                                    favorites.forEach { favoriteJson.put(it) }
                                    prefs.edit().putString("favorite_audio_tracks", favoriteJson.toString()).apply()
                                }
                                saveTracks(tracks.filterNot { it == uriString })
                            }) {
                                Icon(Icons.Default.DeleteOutline, "Quitar canción", tint = Muted)
                            }
                        }
                    }
                }
            }
        }
        if (currentTitle != null) {
            Surface(color = Color(0xFF174C43), shape = RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = Accent)
                        Text(currentTitle.orEmpty(), Modifier.weight(1f).padding(start = 8.dp), maxLines = 1, fontWeight = FontWeight.Medium)
                        TextButton(onClick = { showQueueDialog = true }) { Text("Cola", color = Accent) }
                        if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                        IconButton(onClick = {
                            if (currentPositionMs > 3000) {
                                runCatching { player?.seekTo(0) }
                                currentPositionMs = 0
                                seekPositionMs = 0f
                            } else {
                                playAdjacentTrack(-1)
                            }
                        }, enabled = !isLoading && currentUri != null) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Canción anterior", tint = Accent)
                        }
                        IconButton(onClick = { playAdjacentTrack(1) }, enabled = !isLoading && currentUri != null) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Siguiente canción", tint = Accent)
                        }
                        IconButton(onClick = {
                            if (isPlaying) {
                                runCatching { player?.pause() }
                                isPlaying = false
                            } else if (!isLoading) {
                                runCatching { player?.start() }
                                isPlaying = true
                            }
                        }, enabled = !isLoading) {
                            Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (isPlaying) "Pausar" else "Reproducir", tint = Accent)
                        }
                        TextButton(onClick = { stopPlayback() }) { Text("Detener") }
                        TextButton(onClick = { repeatCurrent = !repeatCurrent; prefs.edit().putBoolean("audio_repeat_current", repeatCurrent).apply() }) { Text(if (repeatCurrent) "Repetir: Sí" else "Repetir: No", color = if (repeatCurrent) Accent else Color.White) }
                        TextButton(onClick = { autoPlayNext = !autoPlayNext; prefs.edit().putBoolean("audio_auto_play_next", autoPlayNext).apply() }) { Text(if (autoPlayNext) "Siguiente: Sí" else "Siguiente: No", color = if (autoPlayNext) Accent else Color.White) }
                    }
                    Slider(
                        value = if (isSeeking) seekPositionMs else currentPositionMs.toFloat(),
                        onValueChange = {
                            seekPositionMs = it
                            isSeeking = true
                        },
                        onValueChangeFinished = {
                            val target = seekPositionMs.toInt().coerceIn(0, durationMs.coerceAtLeast(0))
                            runCatching { player?.seekTo(target) }
                            currentPositionMs = target
                            isSeeking = false
                        },
                        valueRange = 0f..durationMs.coerceAtLeast(1).toFloat(),
                        enabled = durationMs > 0 && !isLoading,
                        modifier = Modifier.fillMaxWidth().height(30.dp),
                        colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatAudioTime(if (isSeeking) seekPositionMs.toInt() else currentPositionMs), color = Color.White, fontSize = 11.sp)
                        Text(formatAudioTime(durationMs), color = Muted, fontSize = 11.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Velocidad", color = Muted, fontSize = 12.sp)
                        Slider(
                            value = playbackSpeed,
                            onValueChange = { playbackSpeed = it },
                            onValueChangeFinished = {
                                runCatching { player?.setPlaybackParams(PlaybackParams().setSpeed(playbackSpeed)) }
                                prefs.edit().putFloat("audio_playback_speed", playbackSpeed).apply()
                            },
                            valueRange = 0.5f..2f,
                            steps = 5,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            enabled = !isLoading,
                            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent)
                        )
                        Text(String.format(Locale.getDefault(), "%.2fx", playbackSpeed), color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
    if (showQueueDialog) {
        val activeQueue = playbackQueue.filter { it in allSortedTracks }.ifEmpty { allSortedTracks }
        AlertDialog(
            onDismissRequest = { showQueueDialog = false },
            title = { Text("Cola de reproducción (${activeQueue.size})") },
            text = {
                if (activeQueue.isEmpty()) {
                    Text("No hay canciones disponibles en la cola.")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    playbackQueue = listOfNotNull(currentUri)
                                    android.widget.Toast.makeText(context, "Se quitaron las canciones pendientes", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                enabled = activeQueue.any { it != currentUri }
                            ) { Text("Vaciar pendientes", color = Accent) }
                        }
                        LazyColumn(modifier = Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(activeQueue) { queuedUri ->
                                val queueIndex = activeQueue.indexOf(queuedUri)
                                val queued = Uri.parse(queuedUri)
                                val queuedTitle = runCatching {
                                    context.contentResolver.query(queued, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                                        if (cursor.moveToFirst()) cursor.getString(0) else null
                                    }
                                }.getOrNull() ?: queued.lastPathSegment?.substringAfterLast('/') ?: "Audio"
                                Surface(
                                    color = if (queuedUri == currentUri) Color(0xFF174C43) else Panel,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f).clickable {
                                            playbackQueue = activeQueue
                                            playTrack(queuedUri, queuedTitle)
                                            showQueueDialog = false
                                        }.padding(6.dp)) {
                                            Text(queuedTitle, maxLines = 1)
                                            if (queuedUri == currentUri) Text("Reproduciendo", color = Accent, fontSize = 10.sp)
                                        }
                                        IconButton(
                                            onClick = {
                                                val reordered = activeQueue.toMutableList()
                                                if (queueIndex > 0) {
                                                    val item = reordered.removeAt(queueIndex)
                                                    reordered.add(queueIndex - 1, item)
                                                    playbackQueue = reordered
                                                }
                                            },
                                            enabled = queueIndex > 0
                                        ) { Icon(Icons.Default.KeyboardArrowUp, "Subir", tint = Accent) }
                                        IconButton(
                                            onClick = {
                                                val reordered = activeQueue.toMutableList()
                                                if (queueIndex < reordered.lastIndex) {
                                                    val item = reordered.removeAt(queueIndex)
                                                    reordered.add(queueIndex + 1, item)
                                                    playbackQueue = reordered
                                                }
                                            },
                                            enabled = queueIndex < activeQueue.lastIndex
                                        ) { Icon(Icons.Default.KeyboardArrowDown, "Bajar", tint = Accent) }
                                        IconButton(
                                            onClick = {
                                                if (queuedUri != currentUri) {
                                                    playbackQueue = activeQueue.filterNot { it == queuedUri }
                                                }
                                            },
                                            enabled = queuedUri != currentUri
                                        ) { Icon(Icons.Default.Close, "Quitar de la cola", tint = Muted) }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showQueueDialog = false }) { Text("Cerrar") } },
            containerColor = Panel
        )
    }
}


private fun formatAudioTime(milliseconds: Int): String {
    val totalSeconds = (milliseconds.coerceAtLeast(0) / 1000)
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}


private data class LibraryTrackMetadata(val artist: String?, val duration: String?, val format: String?)

private fun readLibraryTrackMetadata(context: android.content.Context, uri: Uri): LibraryTrackMetadata {
    val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
    val format = mime?.substringAfter("/")?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
        ?: uri.lastPathSegment?.substringAfterLast('.', "")?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() }
    val retriever = android.media.MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        val artist = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
        val durationMs = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
        val duration = durationMs?.let { millis ->
            val seconds = (millis / 1000).toInt()
            String.format(Locale.getDefault(), "%d:%02d", seconds / 60, seconds % 60)
        }
        LibraryTrackMetadata(artist, duration, format)
    } catch (_: Exception) {
        LibraryTrackMetadata(null, null, format)
    } finally {
        runCatching { retriever.release() }
    }
}
