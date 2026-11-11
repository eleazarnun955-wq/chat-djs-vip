package com.djsebas.chatdjsvip

import android.os.Bundle
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
    var section by remember { mutableStateOf("Chat") }
    var prompt by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf("¡Hola! Soy CHAT DJ'S VIP. Puedo ayudarte con tus dudas y enseñarte técnicas de DJ paso a paso.")) }
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
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Panel))
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
                        messages = messages + "Tú: $text" + "VIP: " + assistantReply(text)
                        prompt = ""
                    }
                }, Modifier.padding(padding))
                "Escuela DJ" -> SchoolScreen(Modifier.padding(padding))
                else -> LibraryScreen(Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun ChatScreen(messages: List<String>, prompt: String, onPrompt: (String) -> Unit, send: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(14.dp)) {
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(messages) { msg ->
                val user = msg.startsWith("Tú:")
                Surface(color = if (user) Color(0xFF174C43) else Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(msg, Modifier.padding(14.dp), color = Color.White, fontSize = 15.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().background(Panel, CircleShape).padding(6.dp)) {
            TextField(value = prompt, onValueChange = onPrompt, modifier = Modifier.weight(1f), placeholder = { Text("Pregunta lo que quieras…", color = Muted) }, colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                focusedTextColor = Color.White, unfocusedTextColor = Color.White
            ))
            IconButton(onClick = { send(prompt) }) { Icon(Icons.Default.Send, "Enviar", tint = Accent) }
        }
    }
}

private fun assistantReply(input: String): String {
    val q = input.lowercase()
    return when {
        listOf("beatmatch", "mezclar", "mezcla", "bpm").any { it in q } ->
            "Para empezar a mezclar: 1) iguala los BPM de ambas canciones; 2) escucha la canción entrante con preescucha en audífonos; 3) localiza el primer golpe del compás; 4) ajusta el jog o pitch poco a poco; 5) introduce el canal con el ecualizador y evita subir dos graves al máximo. Dime qué equipo usas y te guío con sus controles exactos."
        listOf("serato", "rekordbox", "virtualdj", "traktor", "djay").any { it in q } ->
            "Puedo guiarte paso a paso con ese programa. Dime la versión y el modelo de tu controladora para no indicarte botones que no correspondan a tu equipo."
        listOf("tornamesa", "vinilo", "aguja", "scratch").any { it in q } ->
            "En tornamesa física primero revisa nivelación, fuerza de apoyo y antiskating según el manual de tu cápsula. Conecta la salida PHONO a una entrada PHONO (o usa LINE si la tornamesa tiene preamplificador activado). ¿Qué modelo tienes?"
        else -> "Puedo ayudarte con conceptos y formación DJ básica. Para responder con precisión, dime qué aplicación, versión o modelo de equipo utilizas. Esta primera versión funciona sin conexión a un servicio de IA; la conexión a modelos locales y proveedores en nube se integrará y probará por separado."
    }
}

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
    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Biblioteca", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text("Aquí se organizarán tus conversaciones, guías y archivos de aprendizaje DJ.", color = Muted)
        Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Bookmarks, null, tint = Accent)
                Text("Material de aprendizaje", Modifier.padding(start = 12.dp))
            }
        }
    }
}
