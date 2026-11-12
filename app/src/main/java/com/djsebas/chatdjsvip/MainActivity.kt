package com.djsebas.chatdjsvip

import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.net.Uri
import android.media.MediaPlayer
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.MasterKey.KeyScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.util.Locale
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

private val Ink = Color(0xFF101820)
private val Panel = Color(0xFF18232D)
private val Accent = Color(0xFF16C79A)
private val Muted = Color(0xFF9AAAB7)

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
    var showAiSettings by remember { mutableStateOf(false) }
    var showClearChat by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    val securePreferences = remember(context) { runCatching { val masterKey = MasterKey.Builder(context).setKeyScheme(KeyScheme.AES256_GCM).build(); EncryptedSharedPreferences.create(context, "chat_djs_vip_secure", masterKey, PrefKeyEncryptionScheme.AES256_SIV, PrefValueEncryptionScheme.AES256_GCM) }.getOrNull() }
    var prompt by remember { mutableStateOf("") }
    var messages by remember {
        val stored = preferences.getString("messages", null)
        mutableStateOf(
            if (stored.isNullOrBlank()) listOf("¡Hola! Soy CHAT DJ'S VIP. Puedo ayudarte con tus dudas y enseñarte técnicas de DJ paso a paso.")
            else runCatching {
                val json = JSONArray(stored)
                List(json.length()) { index -> json.getString(index) }
            }.getOrElse { listOf("¡Hola! Soy CHAT DJ'S VIP. Puedo ayudarte con tus dudas y enseñarte técnicas de DJ paso a paso.") }
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
                    Text(if (section == "Chat") "Tu asistente inteligente" else "Aprende DJ desde cero", color = Muted, fontSize = 12.sp)
                }
            }, navigationIcon = {
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Accent, modifier = Modifier.padding(start = 16.dp).size(28.dp))
            }, actions = {
                if (section == "Chat") IconButton(onClick = { showClearChat = true }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Limpiar conversación", tint = Accent) }
                IconButton(onClick = { apiKeyInput = securePreferences?.getString("gemini_api_key", "").orEmpty(); showAiSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Configurar IA", tint = Accent) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Panel))
        }, bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(selected = section == "Chat", onClick = { section = "Chat" }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(selected = section == "Escuela DJ", onClick = { section = "Escuela DJ" }, icon = { Icon(Icons.Default.School, null) }, label = { Text("Escuela DJ") })
                NavigationBarItem(selected = section == "Biblioteca", onClick = { section = "Biblioteca" }, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text("Biblioteca") })
            }
        }) { padding ->
            when (section) {
                "Chat" -> ChatScreen(messages, prompt, { prompt = it }, { text, imageUri ->
                    if (text.isNotBlank() || imageUri != null) {
                        val shownText = text.ifBlank { "Analiza esta imagen y explícame qué observas." }
                        messages = messages + "Tú: $shownText" + "VIP: Consultando..."
                        prompt = ""
                        GeminiClient.reply(context, shownText, messages, imageUri) { answer ->
                            (context as? android.app.Activity)?.runOnUiThread {
                                messages = messages.dropLast(1) + "VIP: $answer"
                            }
                        }
                    }
                }, Modifier.padding(padding))
                "Escuela DJ" -> SchoolScreen(Modifier.padding(padding))
                else -> LibraryScreen(Modifier.padding(padding))
            }
        }
        if (showClearChat) {
            AlertDialog(
                onDismissRequest = { showClearChat = false },
                title = { Text("Limpiar conversación") },
                text = { Text("¿Quieres borrar el historial guardado de este chat? Esta acción no se puede deshacer.") },
                confirmButton = { TextButton(onClick = {
                    messages = listOf("¡Hola! Soy CHAT DJ'S VIP. Puedo ayudarte con tus dudas y enseñarte técnicas de DJ paso a paso.")
                    showClearChat = false
                }) { Text("Borrar historial") } },
                dismissButton = { TextButton(onClick = { showClearChat = false }) { Text("Cancelar") } },
                containerColor = Panel
            )
        }
        if (showAiSettings) {
            AlertDialog(
                onDismissRequest = { showAiSettings = false },
                title = { Text("IA gratuita (Gemini)") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("La app puede usar el nivel gratuito de Google AI Studio. Tiene límites de uso; no es ilimitado. No agregues un método de pago ni actives facturación si quieres mantener el costo en $0.")
                        OutlinedTextField(value = apiKeyInput, onValueChange = { apiKeyInput = it }, label = { Text("Clave API de Google AI Studio") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                        TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/api-keys"))) } }) { Text("Obtener clave gratuita") }
                        Text("Aviso: en el nivel gratuito Google puede usar el contenido enviado para mejorar sus productos. No compartas datos privados.", color = Muted, fontSize = 12.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        securePreferences?.edit()?.putString("gemini_api_key", apiKeyInput.trim())?.apply()
                        showAiSettings = false
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { showAiSettings = false }) { Text("Cancelar") } },
                containerColor = Panel
            )
        }
    }
}

@Composable
private fun ChatScreen(messages: List<String>, prompt: String, onPrompt: (String) -> Unit, send: (String, Uri?) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var selectedImage by remember { mutableStateOf<Uri?>(null) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            selectedImage = uri
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
    val chatListState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) chatListState.animateScrollToItem(messages.lastIndex)
    }
    Column(modifier.fillMaxSize().padding(14.dp)) {
        LazyColumn(state = chatListState, modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { msg ->
                val user = msg.startsWith("Tú:")
                Surface(color = if (user) Color(0xFF174C43) else Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, Modifier.weight(1f).padding(2.dp), color = Color.White, fontSize = 15.sp)
                        if (msg.startsWith("VIP:")) {
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
            IconButton(onClick = { imagePicker.launch(arrayOf("image/*")) }) {
                Icon(Icons.Default.Image, "Adjuntar imagen", tint = Accent)
            }
            IconButton(onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Default.Mic, "Hablar", tint = Accent)
            }
            IconButton(onClick = { send(prompt, selectedImage); selectedImage = null }) { Icon(Icons.Default.Send, "Enviar", tint = Accent) }
        }
    }
}

private fun assistantReply(input: String): String = djKnowledgeReply(input)

@Composable
private fun SchoolScreen(modifier: Modifier = Modifier) {
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
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("ESCUELA DJ", fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("Aprende desde principiante hasta nivel profesional.", color = Muted, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(topics) { (name, desc) ->
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().clickable { selectedTopic = name }) {
                    Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, null, tint = Accent, modifier = Modifier.size(28.dp))
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(name, fontWeight = FontWeight.Bold)
                            Text(desc, color = Muted, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
        selectedTopic?.let { topic ->
            AlertDialog(
                onDismissRequest = { selectedTopic = null },
                title = { Text(topic) },
                text = { Text(lessonFor(topic), fontSize = 14.sp) },
                confirmButton = { TextButton(onClick = { selectedTopic = null }) { Text("Entendido") } },
                containerColor = Panel,
                titleContentColor = Color.White,
                textContentColor = Color.White
            )
        }
    }
}

private fun lessonFor(topic: String): String = when (topic) {
    "Serato DJ" -> "RUTA DE APRENDIZAJE\n1. Instala la versión compatible y conecta tu controlador apagado.\n2. Abre Serato y revisa Setup: audio, dispositivo y firmware.\n3. Importa música desde Files y analiza BPM y tonalidad.\n4. Organiza crates y revisa que cada pista tenga beatgrid correcto.\n5. Marca Hot Cues y prueba loops de 4 u 8 compases.\n6. Preescucha con CUE, iguala tempo y mezcla usando ganancia y EQ.\nDime tu versión y modelo de controlador para adaptar los nombres de botones."
    "rekordbox" -> "RUTA DE APRENDIZAJE\n1. Elige el modo Export para preparar USB o Performance para mezclar.\n2. Importa y analiza las pistas.\n3. Corrige beatgrids si el análisis detectó mal los golpes.\n4. Configura Memory Cues y Hot Cues.\n5. Exporta la colección a una memoria USB compatible y expúlsala de forma segura.\n6. Prueba el USB en tu CDJ antes de un evento. Las opciones varían según versión y equipo."
    "VirtualDJ" -> "RUTA DE APRENDIZAJE\n1. Configura el controlador en Settings > Controllers.\n2. Selecciona la tarjeta de sonido y asigna salidas Master y Headphones.\n3. Carga pistas en los decks y analiza BPM/Key.\n4. Usa auriculares para preescuchar el deck entrante.\n5. Practica loops, hot cues y transiciones con EQ.\n6. Guarda cambios de mapeo solo después de probarlos."
    "Traktor" -> "RUTA DE APRENDIZAJE\n1. Configura Audio Device y latencia en Preferences > Audio Setup.\n2. Asigna salidas Master y Monitor.\n3. Importa música a la Collection y analiza beatgrids.\n4. Ajusta Sync solo después de entender fase y tempo.\n5. Practica Hotcues, loops y FX con niveles moderados.\n6. Comprueba el mapeo MIDI y guarda una copia antes de modificarlo."
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
                    isLoading = false
                    isPlaying = true
                    it.start()
                }
                mp.setOnCompletionListener {
                    runCatching { it.seekTo(0) }
                    isPlaying = false
                    isLoading = false
                }
                mp.setOnErrorListener { _, _, _ ->
                    isLoading = false
                    isPlaying = false
                    stopPlayback()
                    true
                }
                mp.prepareAsync()
            }
        }.onFailure {
            stopPlayback()
        }
    }

    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val value = uri.toString()
            if (value !in tracks) saveTracks(tracks + value)
        }
    }

    DisposableEffect(Unit) {
        onDispose { runCatching { player?.release() }; player = null }
    }

    Column(modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Biblioteca musical", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Agrega canciones de tu teléfono. La selección se conserva para volver a reproducirlas.", color = Muted)
        Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LibraryMusic, null)
            Text("  Agregar música del teléfono")
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
            Text("${tracks.size} canción(es)", color = Accent, fontWeight = FontWeight.SemiBold)
            LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tracks) { uriString ->
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
                                Text(if (currentUri == uriString && isLoading) "Cargando…" else if (currentUri == uriString && isPlaying) "Reproduciendo" else "En biblioteca", color = Muted, fontSize = 11.sp)
                            }
                            IconButton(onClick = {
                                if (currentUri == uriString && isPlaying) {
                                    runCatching { player?.pause() }
                                    isPlaying = false
                                } else if (currentUri == uriString && !isPlaying && !isLoading && player != null) {
                                    runCatching { player?.start() }
                                    isPlaying = true
                                } else {
                                    playTrack(uriString, title)
                                }
                            }) {
                                Icon(if (currentUri == uriString && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying) "Pausar" else "Reproducir", tint = Accent)
                            }
                            IconButton(onClick = {
                                if (currentUri == uriString) stopPlayback()
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
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, null, tint = Accent)
                    Text(currentTitle.orEmpty(), Modifier.weight(1f).padding(start = 8.dp), maxLines = 1)
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Accent, strokeWidth = 2.dp)
                    TextButton(onClick = { stopPlayback() }) { Text("Detener") }
                }
            }
        }
    }
}
