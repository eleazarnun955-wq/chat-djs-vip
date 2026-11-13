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
        AcademyQuestion("Durante la prueba de sonido, el medidor del canal llega a rojo aunque el fader está a la mitad. ¿Qué haces primero?", listOf("Bajas la ganancia (trim) y vuelves a comprobar el nivel", "Subes el Master para compensar", "Aumentas los graves"), 0, "El trim controla el nivel de entrada. Ajusta la ganancia antes del fader y evita clipping en cada etapa."),
        AcademyQuestion("Vas a preparar una canción nueva mientras la pista actual sigue sonando al público. ¿Qué función utilizas?", listOf("El botón Sync sin escuchar", "CUE para preescucharla en audífonos", "El control Master"), 1, "CUE envía la señal a los audífonos para preparar la entrada sin mandarla al público."),
        AcademyQuestion("La siguiente canción tiene 124 BPM y la que suena está a 128 BPM. ¿Qué indica esa diferencia?", listOf("Que debes subir el volumen cuatro puntos", "Que las canciones tienen la misma tonalidad", "Que debes ajustar el tempo para igualar los golpes"), 2, "La diferencia inicial es de 4 BPM. Ajusta el pitch o usa Sync con criterio y confirma escuchando."),
        AcademyQuestion("Conectas una controladora y no se escucha nada por las bocinas. ¿Cuál es la revisión más segura?", listOf("Subir todos los controles al máximo", "Revisar salida Master, cables, entrada de bocinas y volumen a nivel bajo", "Desconectar todo mientras suena"), 1, "Sigue la ruta de señal desde la salida Master hasta la entrada de las bocinas y prueba con niveles bajos."),
        AcademyQuestion("En una canción preparaste un punto para entrar directamente al primer golpe del coro. ¿Qué función te ayuda?", listOf("Hot Cue", "Ganancia", "Filtro pasa-altas"), 0, "Un Hot Cue guarda un punto de acceso rápido dentro de la pista."),
        AcademyQuestion("Al conectar tus audífonos no escuchas la pista de preescucha. ¿Qué revisas primero?", listOf("La salida Phones y la asignación Cue", "El color de la carátula", "El brillo de la pantalla"), 0, "Comprueba la salida física de audífonos, el nivel Phones y que el canal tenga CUE activado."),
        AcademyQuestion("El sonido se escucha distorsionado incluso con el Master bajo. ¿Qué debes comprobar?", listOf("Solo el volumen del teléfono", "Ganancia de canal y niveles de entrada", "La tonalidad de la canción"), 1, "La distorsión puede originarse antes del Master. Revisa trim, entrada y medidores de cada canal."),
        AcademyQuestion("Tu biblioteca muestra una canción, pero el software indica archivo perdido. ¿Qué haces?", listOf("Borras toda la colección", "Formateas la memoria", "Localizas el archivo y corriges su ruta"), 2, "Verifica que el archivo exista y vuelve a enlazar su ubicación sin borrar la biblioteca."),
        AcademyQuestion("Antes de comenzar un evento, ¿qué prueba evita sorpresas con la música?", listOf("Probar reproducción, salidas y audífonos con niveles bajos", "Subir todo al máximo", "Esperar a que llegue el público"), 0, "Haz una prueba de señal completa y confirma que Master y preescucha funcionen."),
        AcademyQuestion("¿Qué haces si dos canciones tienen BPM parecidos pero sus golpes se separan?", listOf("Aumentas el volumen", "Ajustas ligeramente el pitch y vuelves a escuchar", "Cambias el balance estéreo"), 1, "BPM cercanos no garantizan sincronía. Ajusta el tempo relativo y confirma durante varios compases.")
    ),
    listOf(
        AcademyQuestion("En rekordbox el beatgrid queda adelantado respecto a los golpes reales de una canción con tempo variable. ¿Qué haces?", listOf("Ignoras el desfase y activas Sync", "Subes el volumen", "Corriges el beatgrid manualmente y verificas escuchando"), 2, "En pistas con tempo variable el análisis automático puede fallar. Ajusta los marcadores y comprueba varios compases."),
        AcademyQuestion("Durante una transición necesitas extender ocho compases porque la siguiente pista aún no está lista. ¿Qué recurso usarías?", listOf("Un loop bien colocado y sincronizado", "El Master al máximo", "Desactivar la preescucha"), 0, "Un loop permite repetir una sección musical; cuida que sus límites coincidan con el fraseo."),
        AcademyQuestion("Escuchas la pista entrante en audífonos, pero el público no la oye. ¿Qué situación es más probable?", listOf("El archivo está dañado necesariamente", "Está en preescucha CUE y aún no la envías al Master", "El BPM está demasiado alto"), 1, "La preescucha es independiente de la mezcla principal. Abre el canal o mueve el crossfader según tu configuración."),
        AcademyQuestion("Un efecto Echo está embarrando el final de la mezcla. ¿Cuál es la mejor corrección?", listOf("Activar todos los efectos", "Subir la resonancia al máximo", "Reducir mezcla y feedback, sincronizarlo y retirarlo a tiempo"), 2, "Los efectos deben acompañar el fraseo. Controla wet/dry y feedback para no saturar ni llenar demasiado el espacio."),
        AcademyQuestion("Preparaste un USB para tocar en CDJ de un evento. ¿Qué reduce más el riesgo de incompatibilidad?", listOf("Probar el USB en el equipo destino y llevar respaldo", "Cambiar los nombres de todas las pistas al azar", "Confiar únicamente en que el ordenador lo reconoce"), 0, "Verifica formato, exportación y lectura en el equipo que usarás. Lleva una segunda copia."),
        AcademyQuestion("El controlador deja de responder durante la sesión, pero el audio principal sigue sonando. ¿Qué haces?", listOf("Desconectas todos los cables de inmediato", "Mantienes la reproducción y activas el respaldo de forma segura", "Reinicias el equipo sin avisar"), 1, "Prioriza la continuidad del audio. Cambia al respaldo y revisa USB o alimentación cuando sea seguro."),
        AcademyQuestion("Un loop de cuatro tiempos no cierra correctamente y produce un salto audible. ¿Qué corriges?", listOf("El volumen Master", "El color de la forma de onda", "Los puntos de inicio y fin y el beatgrid"), 2, "Alinea los límites del loop con los golpes y revisa cuantización y beatgrid."),
        AcademyQuestion("El micrófono comienza a acoplarse cuando el presentador se acerca a una bocina. ¿Qué haces?", listOf("Bajas el nivel, cambias la posición y ajustas EQ con moderación", "Subes la ganancia del micrófono", "Apuntas el micrófono hacia la bocina"), 0, "Reduce la realimentación bajando nivel y evitando que el micrófono apunte al sistema de PA."),
        AcademyQuestion("El efecto Echo deja una cola demasiado larga al terminar una frase vocal. ¿Cómo lo controlas?", listOf("Aumentas feedback", "Reduces feedback y mezcla del efecto y lo retiras a tiempo", "Activas varios efectos adicionales"), 1, "Controla wet/dry y feedback. El efecto debe acompañar el fraseo sin tapar la siguiente sección."),
        AcademyQuestion("Una pista tiene cambios de tempo y el Sync comienza a desalinearla. ¿Qué conviene hacer?", listOf("Ignorar el desfase", "Subir el Master", "Revisar el beatgrid y corregir manualmente los tramos necesarios"), 2, "En música con tempo variable revisa varios puntos del beatgrid y confirma la alineación de oído.")
    ),
    listOf(
        AcademyQuestion("En una mezcla larga, la pista entrante tiene una frase de 16 compases y la saliente está por iniciar otra. ¿Qué estrategia es más musical?", listOf("Entrar en cualquier golpe", "Alinear el inicio de frase y controlar la energía de ambas pistas", "Subir ambos canales al rojo"), 1, "Alinear frases ayuda a que cambios, voces y golpes importantes coincidan de forma natural."),
        AcademyQuestion("Una tornamesa suena muy bajo y con poco grave al conectarla a una entrada LINE. La cápsula requiere preamplificación phono. ¿Qué necesitas?", listOf("Un cable USB", "Subir el Master al máximo", "Conectar a PHONO o usar un preamplificador phono adecuado"), 2, "La señal de cápsula necesita preamplificación y ecualización RIAA. Usa PHONO o un preamp externo compatible."),
        AcademyQuestion("Dos pistas están en tonalidades cercanas según Camelot, pero al mezclarlas las voces chocan. ¿Qué decisión es mejor?", listOf("Confiar ciegamente en el número Camelot", "Escuchar, probar otra sección o elegir otra pista", "Aumentar todos los graves"), 1, "La compatibilidad tonal orienta, pero no garantiza una mezcla agradable. El oído y el arreglo musical tienen prioridad."),
        AcademyQuestion("En un evento falla el ordenador principal. ¿Qué preparación permite continuar con menos interrupción?", listOf("Depender de una conexión a internet", "Esperar a que vuelva sin avisar", "Tener música y equipo de respaldo probado, además de una lista alternativa"), 2, "Un plan de contingencia incluye biblioteca duplicada, medio alternativo y revisión previa de conexiones."),
        AcademyQuestion("El indicador Master marca clipping cuando entran dos canciones a la vez. ¿Qué haces?", listOf("Reduces ganancias y equilibras canales antes de llegar al Master", "Aumentas el Master todavía más", "Subes graves y agudos en ambos canales"), 0, "Controla niveles desde la entrada y deja margen en el bus Master. El rojo indica riesgo de distorsión."),
        AcademyQuestion("El limitador del sistema PA se activa repetidamente durante el drop. ¿Qué haces?", listOf("Ignoras el indicador", "Reduces la salida y revisas la ganancia en toda la cadena", "Aumentas todavía más los graves"), 1, "El limitador indica que el sistema está llegando a su límite. Corrige niveles desde la fuente hasta el PA."),
        AcademyQuestion("Dos pistas muestran el mismo BPM, pero sus golpes no coinciden. ¿Qué revisas?", listOf("Solo la tonalidad", "El color de las ondas", "La fase, el downbeat y el beatgrid"), 2, "Un BPM igual no garantiza sincronía. Comprueba fase, downbeat y posibles cambios de tempo."),
        AcademyQuestion("Una conexión larga entre mezcladora y PA presenta interferencias. ¿Qué conexión es preferible si ambos equipos la admiten?", listOf("Una conexión balanceada XLR adecuada", "Un cable de audífonos largo", "Un cable no balanceado improvisado"), 0, "Una conexión balanceada compatible ayuda a rechazar interferencias en recorridos largos."),
        AcademyQuestion("Falla la energía del equipo principal en medio de un evento. ¿Qué plan ayuda a continuar?", listOf("Esperar sin avisar", "Usar un respaldo de energía y reproducción previamente probado", "Buscar una red Wi-Fi"), 1, "Prepara antes del evento respaldo de energía cuando corresponda, música duplicada y una ruta alternativa de reproducción."),
        AcademyQuestion("El canal distorsiona aunque el medidor Master no está en rojo. ¿Qué inspeccionas?", listOf("Solo las bocinas", "La iluminación del escenario", "Trim, EQ, efectos y medidor del canal"), 2, "El clipping puede originarse en una etapa previa al Master. Revisa cada punto de la cadena de señal."),

    )
)

@Composable
fun SchoolAcademyScreen(modifier: Modifier = Modifier, onOpenLibrary: () -> Unit = {}) {
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
    var practicalOpen by remember { mutableStateOf(false) }
    var practicalChecks by remember { mutableStateOf(setOf<Int>()) }
    var practicalResult by remember { mutableStateOf<String?>(null) }
    var practicalScores by remember {
        mutableStateOf(academyLevels.flatMap { it.topics }.associate { topic ->
            topic.name to prefs.getInt("practical_score_${topic.name}", -1)
        })
    }
    val practicalSteps = listOf(
        "Prepara la pista entrante y confirma que el archivo y la salida de audio funcionan.",
        "Preescucha con CUE en audífonos sin enviar todavía la pista al público.",
        "Ajusta BPM y comprueba el beatmatch escuchando varios compases.",
        "Ajusta la ganancia de entrada evitando que los medidores lleguen a rojo.",
        "Alinea el fraseo, realiza la transición y verifica que el Master no distorsione."
    )
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
        OutlinedButton(onClick = onOpenLibrary, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.LibraryMusic, contentDescription = null)
            Text("  Abrir biblioteca musical para practicar")
        }
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
                    Text("Realiza los pasos de la lección con tu equipo o software. Debes aprobar la evaluación práctica con 70% o más para registrar esta clase como completada.", color = Color.White, fontSize = 12.sp)
                    val savedPracticalScore = practicalScores[topic.name] ?: -1
                    if (savedPracticalScore >= 0) {
                        Text("Última evaluación práctica: $savedPracticalScore% ${if (savedPracticalScore >= 70) "• Aprobada" else "• Necesitas repetirla"}", color = if (savedPracticalScore >= 70) Accent else Color(0xFFFFB74D), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
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
                    TextButton(onClick = {
                        practicalChecks = emptySet()
                        practicalResult = null
                        practicalOpen = true
                    }) { Text("Iniciar evaluación práctica") }
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
                    TextButton(
                        enabled = topic.name in completed || (practicalScores[topic.name] ?: -1) >= 70,
                        onClick = {
                            saveCompleted(completed + topic.name)
                            selectedTopic = null
                        }
                    ) {
                        Text(when {
                            topic.name in completed -> "Clase completada ✓"
                            (practicalScores[topic.name] ?: -1) >= 70 -> "Registrar clase completada"
                            else -> "Aprueba la práctica para continuar"
                        })
                    }
                }
            },
            dismissButton = { TextButton(onClick = { if (!askingTeacher) selectedTopic = null }) { Text("Cerrar") } },
            containerColor = Panel, titleContentColor = Color.White, textContentColor = Color.White
        )
    }

    if (practicalOpen) {
        AlertDialog(
            onDismissRequest = { practicalOpen = false },
            title = { Text("Evaluación práctica DJ") },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState())) {
                    Text("Simulación: prepara y mezcla una pista entrante de forma segura. Marca cada acción solo cuando la hayas realizado.", color = Color.White, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(progress = { practicalChecks.size / 5f }, modifier = Modifier.fillMaxWidth(), color = Accent, trackColor = Ink)
                    Text("Pasos completados: ${practicalChecks.size}/5", color = Accent, fontWeight = FontWeight.Bold)
                    practicalSteps.forEachIndexed { index, step ->
                        Row(Modifier.fillMaxWidth().clickable {
                            practicalChecks = if (index in practicalChecks) practicalChecks - index else practicalChecks + index
                        }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = index in practicalChecks, onCheckedChange = { checked ->
                                practicalChecks = if (checked) practicalChecks + index else practicalChecks - index
                            })
                            Text(step, color = Color.White, fontSize = 12.sp)
                        }
                    }
                    practicalResult?.let {
                        Surface(color = Color(0xFF123D36), shape = RoundedCornerShape(10.dp)) {
                            Text(it, Modifier.padding(10.dp), color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val score = practicalChecks.size * 20
                    selectedTopic?.let { topic ->
                        practicalScores = practicalScores + (topic.name to score)
                        prefs.edit().putInt("practical_score_${topic.name}", score).apply()
                    }
                    practicalResult = "Calificación práctica: $score/100. " + when {
                        score == 100 -> "Excelente: completaste todos los pasos de la mezcla."
                        score >= 70 -> "Buen trabajo. Repasa los pasos que faltaron y vuelve a practicar."
                        else -> "Necesitas practicar la preparación, el control de niveles y la transición antes de repetir."
                    }
                }) { Text("Calificar práctica") }
            },
            dismissButton = { TextButton(onClick = { practicalOpen = false }) { Text("Cerrar") } },
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
