package com.djsebas.chatdjsvip

import android.os.Bundle
import android.content.Intent
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.net.Uri
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
            }, actions = { IconButton(onClick = { apiKeyInput = securePreferences?.getString("gemini_api_key", "").orEmpty(); showAiSettings = true }) { Icon(Icons.Default.Settings, contentDescription = "Configurar IA", tint = Accent) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Panel))
        }, bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(selected = section == "Chat", onClick = { section = "Chat" }, icon = { Icon(Icons.Default.Chat, null) }, label = { Text("Chat") })
                NavigationBarItem(selected = section == "Escuela DJ", onClick = { section = "Escuela DJ" }, icon = { Icon(Icons.Default.School, null) }, label = { Text("Escuela DJ") })
                NavigationBarItem(selected = section == "Biblioteca", onClick = { section = "Biblioteca" }, icon = { Icon(Icons.Default.LibraryMusic, null) }, label = { Text("Biblioteca") })
            }
        }) { padding ->
            when (section) {
                "Chat" -> ChatScreen(messages, prompt, { prompt = it }, { text ->
                    if (text.isNotBlank()) {
                        messages = messages + "Tú: $text" + "VIP: Consultando..."
                        prompt = ""
                        GeminiClient.reply(context, text) { answer ->
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
private fun ChatScreen(messages: List<String>, prompt: String, onPrompt: (String) -> Unit, send: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var speechEngine by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) speechEngine?.language = Locale("es", "MX")
        }
        speechEngine = engine
        onDispose { engine.stop(); engine.shutdown(); speechEngine = null }
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
    Column(modifier.fillMaxSize().padding(14.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { msg ->
                val user = msg.startsWith("Tú:")
                Surface(color = if (user) Color(0xFF174C43) else Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(msg, Modifier.weight(1f).padding(2.dp), color = Color.White, fontSize = 15.sp)
                        if (msg.startsWith("VIP:")) IconButton(onClick = { speechEngine?.speak(msg.removePrefix("VIP:").trim(), TextToSpeech.QUEUE_FLUSH, null, "vip_reply") }) { Icon(Icons.Default.VolumeUp, "Escuchar respuesta", tint = Accent) }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Panel, CircleShape).padding(6.dp)) {
            TextField(value = prompt, onValueChange = onPrompt, modifier = Modifier.weight(1f), placeholder = { Text("Pregunta lo que quieras…", color = Muted) }, colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ))
            IconButton(onClick = { permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO) }) {
                Icon(Icons.Default.Mic, "Hablar", tint = Accent)
            }
            IconButton(onClick = { send(prompt) }) { Icon(Icons.Default.Send, "Enviar", tint = Accent) }
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
        "Controladoras, CDJ y mezcladoras" to "Ganancia, EQ, filtros, canales, crossfader, monitores y conexiones."
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
    "Serato DJ" -> "RUTA DE APRENDIZAJE\\n1. Instala la versión compatible y conecta tu controlador apagado.\\n2. Abre Serato y revisa Setup: audio, dispositivo y firmware.\\n3. Importa música desde Files y analiza BPM y tonalidad.\\n4. Organiza crates y revisa que cada pista tenga beatgrid correcto.\\n5. Marca Hot Cues y prueba loops de 4 u 8 compases.\\n6. Preescucha con CUE, iguala tempo y mezcla usando ganancia y EQ.\\nDime tu versión y modelo de controlador para adaptar los nombres de botones."
    "rekordbox" -> "RUTA DE APRENDIZAJE\\n1. Elige el modo Export para preparar USB o Performance para mezclar.\\n2. Importa y analiza las pistas.\\n3. Corrige beatgrids si el análisis detectó mal los golpes.\\n4. Configura Memory Cues y Hot Cues.\\n5. Exporta la colección a una memoria USB compatible y expúlsala de forma segura.\\n6. Prueba el USB en tu CDJ antes de un evento. Las opciones varían según versión y equipo."
    "VirtualDJ" -> "RUTA DE APRENDIZAJE\\n1. Configura el controlador en Settings > Controllers.\\n2. Selecciona la tarjeta de sonido y asigna salidas Master y Headphones.\\n3. Carga pistas en los decks y analiza BPM/Key.\\n4. Usa auriculares para preescuchar el deck entrante.\\n5. Practica loops, hot cues y transiciones con EQ.\\n6. Guarda cambios de mapeo solo después de probarlos."
    "Traktor" -> "RUTA DE APRENDIZAJE\\n1. Configura Audio Device y latencia en Preferences > Audio Setup.\\n2. Asigna salidas Master y Monitor.\\n3. Importa música a la Collection y analiza beatgrids.\\n4. Ajusta Sync solo después de entender fase y tempo.\\n5. Practica Hotcues, loops y FX con niveles moderados.\\n6. Comprueba el mapeo MIDI y guarda una copia antes de modificarlo."
    "djay" -> "RUTA DE APRENDIZAJE\\n1. Conecta un controlador compatible y concede permisos de audio.\\n2. Selecciona la salida principal y la preescucha si tu dispositivo lo permite.\\n3. Carga pistas, revisa BPM y forma de onda.\\n4. Practica cue points, loops y ecualización.\\n5. Explora Neural Mix si tu versión y dispositivo lo admiten.\\n6. Verifica compatibilidad exacta del controlador antes de comprarlo."
    "Tornamesas y vinilos" -> "CONFIGURACIÓN SEGURA\\n1. Coloca la tornamesa en una superficie firme y nivelada.\\n2. Instala cápsula y aguja siguiendo el fabricante.\\n3. Ajusta contrapeso y fuerza de apoyo al rango recomendado por la cápsula; no adivines el valor.\\n4. Ajusta antiskating según el manual.\\n5. Conecta PHONO a entrada PHONO con tierra; usa LINE solo si el preamplificador está activado y la mezcladora recibe LINE.\\n6. Limpia el vinilo y practica cueing sin forzar la aguja."
    else -> "CONCEPTOS ESENCIALES\\n1. Identifica entradas y salidas antes de conectar.\\n2. Conecta Master a bocinas activas o amplificador adecuado.\\n3. Usa CUE para escuchar en audífonos sin enviar esa señal al público.\\n4. Ajusta ganancia sin llevar los medidores a rojo.\\n5. Ecualiza con cambios graduales y evita saturación.\\n6. Baja volúmenes antes de conectar o desconectar equipos.\\nDime marca y modelo para darte un diagrama de conexión específico."
}

@Composable
private fun LibraryScreen(modifier: Modifier = Modifier) {
    var selectedFile by remember { mutableStateOf<String?>(null) }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            selectedFile = uri.lastPathSegment ?: uri.toString()
        }
    }
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Biblioteca", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Abre documentos y archivos desde tu teléfono para consultarlos durante tu aprendizaje.", color = Muted)
        Button(onClick = { filePicker.launch(arrayOf("*/*")) }) {
            Icon(Icons.Default.AttachFile, null)
            Text("  Abrir archivo del teléfono")
        }
        selectedFile?.let { Text("Archivo seleccionado: $it", color = Accent, fontSize = 12.sp) }
        Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmarks, null, tint = Accent)
                Text("Material de aprendizaje", Modifier.padding(start = 12.dp))
            }
        }
    }
}
