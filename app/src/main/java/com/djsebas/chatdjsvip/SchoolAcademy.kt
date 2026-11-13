package com.djsebas.chatdjsvip

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

private data class AcademyTopic(val name: String, val description: String)
private data class AcademyLevel(val name: String, val subtitle: String, val topics: List<AcademyTopic>)
private data class AcademyQuestion(val prompt: String, val options: List<String>, val answer: Int, val explanation: String)

private val academyLevels = listOf(
    AcademyLevel("Principiante", "Fundamentos y primeros pasos", listOf(
        AcademyTopic("Controladoras, CDJ y mezcladoras", "Conoce decks, canales, ganancia, EQ, cue y conexiones."),
        AcademyTopic("Serato DJ", "Biblioteca, crates, puntos Cue, loops y beatmatch."),
        AcademyTopic("Preparación de biblioteca", "Organiza música, BPM, tonalidad, playlists y respaldos."),
        AcademyTopic("djay", "Mezcla desde móvil o tablet y conoce las funciones compatibles.")
    )),
    AcademyLevel("Intermedio", "Técnica, software y creatividad", listOf(
        AcademyTopic("rekordbox", "Analiza pistas, prepara USB, hot cues y memory cues."),
        AcademyTopic("VirtualDJ", "Domina decks, sampler, stems, efectos y controladoras."),
        AcademyTopic("Traktor", "Configura audio, beatgrids, loops, FX y MIDI."),
        AcademyTopic("Efectos, loops y creatividad", "Usa filtros, echo, reverb y loops con intención.")
    )),
    AcademyLevel("Profesional", "Mezcla avanzada y trabajo en eventos", listOf(
        AcademyTopic("Tornamesas y vinilos", "Conexión PHONO/LINE, cápsula, aguja, pitch y scratch."),
        AcademyTopic("Mezcla armónica y tonalidad", "Compatibilidad tonal, Camelot y selección musical."),
        AcademyTopic("Transiciones profesionales", "Fraseo, energía y transiciones largas o rápidas."),
        AcademyTopic("Preparación de eventos", "Planifica sesiones, sonido, respaldos y contingencias.")
    ))
)

private val academyExams = listOf(
    listOf(
        AcademyQuestion("¿Para qué sirve la ganancia (trim) de un canal?", listOf("Cambiar la tonalidad", "Ajustar el nivel de entrada", "Aumentar el BPM automáticamente"), 1, "La ganancia ajusta el nivel de entrada del canal. Evita que los medidores lleguen a rojo."),
        AcademyQuestion("¿Qué escuchas al activar CUE en un canal?", listOf("El audio de todas las bocinas", "Una grabación automática", "La señal de preescucha en audífonos"), 2, "CUE permite preparar la siguiente canción sin enviarla al público."),
        AcademyQuestion("¿Qué dato indica aproximadamente la velocidad de una canción?", listOf("EQ", "BPM", "USB"), 1, "BPM significa beats por minuto y describe el tempo."),
        AcademyQuestion("¿Qué debes revisar antes de conectar una controladora?", listOf("Solo el color del cable", "Subir todo el volumen al máximo", "Salidas, entradas y compatibilidad"), 2, "Identifica entradas, salidas y compatibilidad; conecta con volúmenes bajos."),
        AcademyQuestion("¿Qué es un Hot Cue?", listOf("Un cable de audio", "Un punto de acceso rápido dentro de la pista", "Un tipo de bocina"), 1, "Los Hot Cues permiten saltar rápidamente a puntos preparados de una canción.")
    ),
    listOf(
        AcademyQuestion("¿Qué debes hacer si el beatgrid está desfasado?", listOf("Subir el volumen", "Revisarlo y corregirlo manualmente", "Cambiar el nombre del archivo"), 1, "El análisis automático puede fallar; revisa el grid escuchando y observando los golpes."),
        AcademyQuestion("¿Para qué sirve un loop?", listOf("Eliminar el archivo", "Cambiar la salida Master", "Repetir una sección musical"), 2, "Un loop repite una sección y puede ayudar a extender una frase."),
        AcademyQuestion("¿Qué es la preescucha?", listOf("Grabar el evento", "Escuchar la pista entrante antes de enviarla al Master", "Subir los graves"), 1, "La preescucha ayuda a preparar tempo, fraseo y entrada."),
        AcademyQuestion("¿Cómo conviene usar efectos durante una mezcla?", listOf("Todos al máximo", "Sin escuchar el resultado", "Con moderación y controlando niveles"), 2, "Un efecto bien sincronizado y moderado aporta creatividad sin ensuciar la mezcla."),
        AcademyQuestion("¿Qué debes confirmar antes de exportar un USB para CDJ?", listOf("Que tenga el mayor volumen posible", "Compatibilidad y prueba en el equipo destino", "Que esté vacío"), 1, "Comprueba el formato y compatibilidad del equipo y prueba el USB antes del evento.")
    ),
    listOf(
        AcademyQuestion("¿Qué es el fraseo musical?", listOf("El volumen de audífonos", "El nombre de una controladora", "La organización de la música en frases y compases"), 2, "Reconocer frases ayuda a realizar entradas y salidas musicales naturales."),
        AcademyQuestion("¿Qué debes hacer con la fuerza de apoyo de una aguja?", listOf("Adivinar el valor", "Seguir el rango indicado por el fabricante", "Ponerla siempre al máximo"), 1, "La fuerza depende de la cápsula; consulta sus especificaciones para evitar daños."),
        AcademyQuestion("¿Qué representa la rueda Camelot?", listOf("Una lista de reproducción", "Una forma de relacionar tonalidades compatibles", "Un medidor de volumen"), 1, "Camelot facilita explorar tonalidades compatibles, aunque el oído y la música mandan."),
        AcademyQuestion("¿Qué es lo más importante al preparar un evento?", listOf("Depender de internet", "Usar todos los efectos", "Plan musical, revisión técnica y respaldo"), 2, "Un DJ profesional prepara música, equipo, conexiones y alternativas ante fallas."),
        AcademyQuestion("¿Cómo evitas saturación en el Master?", listOf("Llevando todos los medidores a rojo", "Controlando ganancias y niveles", "Subiendo todos los faders al máximo"), 1, "Mantén margen de nivel y evita clipping en cada etapa de la señal.")
    )
)

@Composable
fun SchoolAcademyScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("chat_djs_vip_school", Context.MODE_PRIVATE) }
    var completed by remember {
        val raw = prefs.getString("completed_topics", null)
        mutableStateOf(if (raw.isNullOrBlank()) emptySet() else runCatching {
            val json = JSONArray(raw)
            List(json.length()) { json.getString(it) }.toSet()
        }.getOrDefault(emptySet()))
    }
    var scores by remember {
        mutableStateOf(listOf(0, 0, 0).mapIndexed { index, _ -> prefs.getInt("exam_score_$index", -1) })
    }
    var selectedTopic by remember { mutableStateOf<AcademyTopic?>(null) }
    var studentQuestion by remember { mutableStateOf("") }
    var teacherAnswer by remember { mutableStateOf("") }
    var askingTeacher by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var quizLevel by remember { mutableIntStateOf(-1) }
    var quizIndex by remember { mutableIntStateOf(0) }
    var quizCorrect by remember { mutableIntStateOf(0) }
    var quizChoice by remember { mutableIntStateOf(-1) }
    var quizFeedback by remember { mutableStateOf("") }
    var resultMessage by remember { mutableStateOf<String?>(null) }
    val modelReady = remember(context) { OnDeviceLlmEngine.hasInstalledModel(context) }

    fun saveCompleted(updated: Set<String>) {
        completed = updated
        val json = JSONArray()
        updated.forEach { json.put(it) }
        prefs.edit().putString("completed_topics", json.toString()).apply()
    }
    fun levelUnlocked(index: Int): Boolean = index == 0 || scores[index - 1] >= 70
    fun levelTopicsDone(index: Int): Boolean = academyLevels[index].topics.all { it.name in completed }
    fun saveScore(index: Int, score: Int) {
        val updated = scores.toMutableList()
        updated[index] = score
        scores = updated
        prefs.edit().putInt("exam_score_$index", score).apply()
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("ESCUELA DJ • ACADEMIA", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Aprende, practica, presenta exámenes y obtén certificados internos.", color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        val total = academyLevels.sumOf { it.topics.size }
        Text("Avance general: ${completed.size} / $total clases", color = Accent, fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(
            progress = { completed.size.coerceAtMost(total).toFloat() / total },
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 12.dp),
            color = Accent, trackColor = Panel
        )
        OutlinedTextField(
            value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(),
            singleLine = true, placeholder = { Text("Buscar clase o técnica…") }
        )
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(academyLevels) { level ->
                val index = academyLevels.indexOf(level)
                val unlocked = levelUnlocked(index)
                val done = level.topics.count { it.name in completed }
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (unlocked) Icons.Default.School else Icons.Default.Lock, null, tint = if (unlocked) Accent else Muted)
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(level.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                Text(level.subtitle, color = Muted, fontSize = 12.sp)
                            }
                            Text("$done/4", color = Accent, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { done / 4f }, modifier = Modifier.fillMaxWidth().padding(vertical = 9.dp),
                            color = Accent, trackColor = Ink
                        )
                        if (!unlocked) {
                            Text("Aprueba el examen anterior con 70% o más para desbloquear este nivel.", color = Muted, fontSize = 12.sp)
                        } else {
                            level.topics.filter { it.name.contains(search, true) || it.description.contains(search, true) }
                                .forEach { topic ->
                                    Row(
                                        Modifier.fillMaxWidth().clickable {
                                            selectedTopic = topic
                                            studentQuestion = ""
                                            teacherAnswer = ""
                                        }.padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(if (topic.name in completed) Icons.Default.CheckCircle else Icons.Default.School, null, tint = Accent)
                                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                            Text(topic.name, fontWeight = FontWeight.SemiBold)
                                            Text(topic.description, color = Muted, fontSize = 11.sp)
                                        }
                                        Text(if (topic.name in completed) "Lista" else "Abrir", color = Accent, fontSize = 11.sp)
                                    }
                                }
                            if (done == 4) {
                                Button(
                                    onClick = {
                                        quizLevel = index
                                        quizIndex = 0
                                        quizCorrect = 0
                                        quizChoice = -1
                                        quizFeedback = ""
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Ink)
                                ) { Text(if (scores[index] >= 70) "Repetir examen" else "Presentar examen") }
                            } else {
                                Text("Completa las 4 clases y sus prácticas para habilitar el examen.", color = Muted, fontSize = 11.sp)
                            }
                            if (scores[index] >= 70) {
                                Surface(color = Color(0xFF123D36), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EmojiEvents, null, tint = Accent)
                                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                            Text("Certificado interno: ${level.name}", color = Accent, fontWeight = FontWeight.Bold)
                                            Text("Aprobado con ${scores[index]}%. CHAT DJ'S VIP • Escuela DJ", color = Color.White, fontSize = 11.sp)
                                        }
                                        TextButton(onClick = {
                                            val certificate = "CERTIFICADO INTERNO DE FINALIZACIÓN\nEscuela DJ • CHAT DJ'S VIP\nNivel: ${level.name}\nCalificación: ${scores[index]}%\nEmitido por la academia integrada en CHAT DJ'S VIP. No es una acreditación oficial."
                                            val send = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Certificado Escuela DJ")
                                                putExtra(Intent.EXTRA_TEXT, certificate)
                                            }
                                            runCatching { context.startActivity(Intent.createChooser(send, "Compartir certificado")) }
                                        }) { Text("Compartir") }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedTopic?.let { topic ->
        AlertDialog(
            onDismissRequest = { if (!askingTeacher) selectedTopic = null },
            title = { Text(topic.name) },
            text = {
                Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                    Text(lessonFor(topic.name), color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("PRÁCTICA", color = Accent, fontWeight = FontWeight.Bold)
                    Text("Realiza los pasos de la lección con tu equipo o software. Después marca la clase como completada para registrar tu avance.", color = Color.White, fontSize = 12.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("PROFESOR DJ CON CEREBRO LOCAL", color = Accent, fontWeight = FontWeight.Bold)
                    Text(if (modelReady) "Conectado al modelo local instalado." else "Modelo local no detectado; se usará la guía básica.", color = Muted, fontSize = 11.sp)
                    OutlinedTextField(
                        value = studentQuestion, onValueChange = { studentQuestion = it },
                        modifier = Modifier.fillMaxWidth(), placeholder = { Text("Pregunta sobre esta práctica…") },
                        enabled = !askingTeacher, minLines = 2
                    )
                    if (askingTeacher) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(18.dp), color = Accent, strokeWidth = 2.dp)
                            Text(" El profesor está respondiendo…", color = Muted, fontSize = 11.sp)
                        }
                    }
                    if (teacherAnswer.isNotBlank()) {
                        Surface(color = Color(0xFF123D36), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Text(teacherAnswer, Modifier.padding(10.dp), color = Color.White, fontSize = 12.sp)
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
Eres el profesor de la Escuela DJ de CHAT DJ'S VIP. Enseña en español mexicano, con paciencia.
Nivel: ${academyLevels.indexOfFirst { level -> topic in level.topics }.let { academyLevels[it].name }}
Tema: ${topic.name}
Lección: ${lessonFor(topic.name)}
Pregunta: $question
Responde paso a paso y propone una práctica verificable. No inventes botones, compatibilidad ni valores técnicos. Si falta marca, modelo o versión, pregúntalos. Usa el cerebro local ya integrado.
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
                    TextButton(onClick = {
                        saveCompleted(completed + topic.name)
                        selectedTopic = null
                    }) { Text(if (topic.name in completed) "Clase completada ✓" else "Marcar práctica completada") }
                }
            },
            dismissButton = { TextButton(onClick = { if (!askingTeacher) selectedTopic = null }) { Text("Cerrar") } },
            containerColor = Panel, titleContentColor = Color.White, textContentColor = Color.White
        )
    }

    if (quizLevel in academyLevels.indices) {
        val questions = academyExams[quizLevel]
        val question = questions[quizIndex]
        AlertDialog(
            onDismissRequest = { quizLevel = -1 },
            title = { Text("Examen • ${academyLevels[quizLevel].name} • ${quizIndex + 1}/${questions.size}") },
            text = {
                Column {
                    Text(question.prompt, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    question.options.forEachIndexed { optionIndex, option ->
                        Surface(
                            color = if (quizChoice == optionIndex) Color(0xFF174C43) else Panel,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable {
                                if (quizChoice == -1) {
                                    quizChoice = optionIndex
                                    if (optionIndex == question.answer) quizCorrect++
                                    quizFeedback = (if (optionIndex == question.answer) "¡Correcto! " else "No es correcto. ") + question.explanation
                                }
                            }
                        ) { Text(option, Modifier.padding(11.dp), color = Color.White, fontSize = 13.sp) }
                    }
                    if (quizFeedback.isNotBlank()) Text(quizFeedback, color = Accent, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                }
            },
            confirmButton = {
                TextButton(
                    enabled = quizChoice != -1,
                    onClick = {
                        if (quizIndex == questions.lastIndex) {
                            val score = (quizCorrect * 100) / questions.size
                            saveScore(quizLevel, score)
                            resultMessage = "Resultado: $score%. ${if (score >= 70) "¡Aprobaste! Tu certificado interno está disponible en este nivel." else "Necesitas 70% para aprobar. Puedes repasar las clases y repetir el examen."}"
                            quizLevel = -1
                        } else {
                            quizIndex++
                            quizChoice = -1
                            quizFeedback = ""
                        }
                    }
                ) { Text(if (quizIndex == questions.lastIndex) "Terminar examen" else "Siguiente pregunta") }
            },
            dismissButton = { TextButton(onClick = { quizLevel = -1 }) { Text("Salir") } },
            containerColor = Panel
        )
    }
    resultMessage?.let { result ->
        AlertDialog(
            onDismissRequest = { resultMessage = null },
            title = { Text("Evaluación terminada") },
            text = { Text(result) },
            confirmButton = { TextButton(onClick = { resultMessage = null }) { Text("Aceptar") } },
            containerColor = Panel
        )
    }
}
