package com.djsebas.chatdjsvip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray

@Composable
fun AppStudioScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences("chat_djs_vip", android.content.Context.MODE_PRIVATE)
    }
    var requirements by remember { mutableStateOf("") }
    var target by remember { mutableStateOf(preferences.getString("app_studio_target", "Android") ?: "Android") }
    var taskMode by remember { mutableStateOf("Crear") }
    var result by remember { mutableStateOf(preferences.getString("app_studio_last_result", "").orEmpty()) }
    var isGenerating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Describe la aplicación que quieres crear.") }
    val studioHistory = remember(context) {
        val stored = preferences.getString("app_studio_history", null)
        mutableStateListOf<String>().apply {
            if (!stored.isNullOrBlank()) runCatching {
                val json = JSONArray(stored)
                for (i in 0 until json.length()) add(json.getString(i))
            }
        }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { it.write(result.toByteArray(Charsets.UTF_8)) }
                    ?: error("No se pudo abrir el archivo")
            }.isSuccess
            status = if (saved) "Proyecto guardado en tus archivos." else "No se pudo guardar el proyecto."
        }
    }
    val zipLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) {
            val export = ProjectZipExporter.export(context, uri, result)
            status = if (export.success) "ZIP creado: ${export.fileCount} archivos listos para extraer." else "No se pudo crear el ZIP: ${export.error}"
        }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("ESTUDIO DE APLICACIONES", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Diseña aplicaciones Android y web, genera código organizado y continúa trabajando sobre el mismo proyecto. Puedes guardar la propuesta como documento o empaquetar los bloques de código en un ZIP.",
            color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp
        )
        Text("¿Qué tipo de aplicación quieres crear?", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = target == "Android", onClick = { target = "Android"; preferences.edit().putString("app_studio_target", "Android").apply() }, label = { Text("Android / APK") })
            FilterChip(selected = target == "Web", onClick = { target = "Web"; preferences.edit().putString("app_studio_target", "Web").apply() }, label = { Text("Página / Web app") })
        }
        Text("¿Qué quieres hacer ahora?", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Crear", "Mejorar", "Corregir", "Preparar build").forEach { mode ->
                FilterChip(selected = taskMode == mode, onClick = { taskMode = mode }, label = { Text(mode) })
            }
        }
        OutlinedTextField(
            value = requirements, onValueChange = { requirements = it }, modifier = Modifier.fillMaxWidth(), minLines = 4,
            label = { Text(if (studioHistory.isEmpty()) "Describe tu aplicación" else "Nueva instrucción o mejora") },
            placeholder = { Text(if (studioHistory.isEmpty()) "Ejemplo: una app para organizar eventos, con usuarios, calendario, fotografías y notificaciones…" else "Ejemplo: agrega inicio de sesión y conserva el diseño anterior…") }
        )
        Button(onClick = {
            val userRequest = requirements.trim()
            val prompt = """MODO_CREADOR_APPS:
Plataforma solicitada: $target.
${if (studioHistory.isEmpty()) "Requisitos iniciales del usuario:" else "Nueva instrucción del usuario para continuar el proyecto existente:"}
$userRequest

Entrega una solución de desarrollo realista. Primero resume el alcance y la arquitectura. Después organiza la respuesta por archivos, indicando la ruta exacta y proporcionando el contenido completo de cada archivo esencial. IMPORTANTE: antes de cada bloque de código escribe una línea independiente con este formato exacto: Archivo: ruta/relativa/al/proyecto.ext. Usa rutas relativas seguras, sin .. ni rutas absolutas. Incluye dependencias, versiones compatibles, configuración, instrucciones de compilación y pruebas. Para Android usa Kotlin y Jetpack Compose salvo que exista una razón técnica clara para otra opción. Para web indica framework, estructura y comandos. Incluye manejo de errores, accesibilidad y seguridad. No inventes servicios ya conectados ni afirmes que compilaste o probaste algo. Si faltan datos imprescindibles, declara los supuestos y pregunta solo lo necesario. Si la respuesta es demasiado extensa, divide el trabajo en entregas numeradas y no omitas archivos esenciales. Si ya existe un proyecto en el historial, conserva sus decisiones y archivos previos y entrega cambios compatibles, indicando exactamente qué archivos reemplazar o crear."""
            isGenerating = true
            result = ""
            status = when (taskMode) {\n                "Crear" -> "Diseñando la aplicación…"\n                "Mejorar" -> "Analizando mejoras compatibles…"\n                "Corregir" -> "Revisando posibles errores…"\n                else -> "Preparando configuración de compilación…"\n            }
            val historySnapshot = studioHistory.toList()
            GeminiClient.reply(context, prompt, historySnapshot) { answer ->
                studioHistory.add("Tú: $prompt")
                studioHistory.add("VIP: $answer")
                while (studioHistory.size > 24) studioHistory.removeAt(0)
                preferences.edit().putString("app_studio_history", JSONArray(studioHistory).toString())
                    .putString("app_studio_last_result", answer).putString("app_studio_target", target).apply()
                result = answer
                isGenerating = false
                status = if (answer.startsWith("Para generar código")) "Falta configurar la clave de IA." else "Respuesta generada y contexto del proyecto guardado."
            }
        }, enabled = requirements.isNotBlank() && !isGenerating, modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Generando…")
            } else {
                Icon(Icons.Default.Code, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(when (taskMode) {\n                    "Crear" -> "Diseñar y generar aplicación"\n                    "Mejorar" -> "Aplicar mejoras al proyecto"\n                    "Corregir" -> "Analizar y corregir errores"\n                    else -> "Preparar compilación y pruebas"\n                })
            }
        }
        if (studioHistory.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contexto guardado: ${studioHistory.size / 2} intercambios", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                TextButton(onClick = {
                    studioHistory.clear()
                    preferences.edit().remove("app_studio_history").remove("app_studio_last_result").apply()
                    result = ""
                    status = "Contexto del estudio borrado. Puedes iniciar otro proyecto."
                }, enabled = !isGenerating) { Text("Nuevo proyecto") }
            }
        }
        Text(status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        if (result.isNotBlank()) {
            HorizontalDivider()
            Text("Propuesta y código generado", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                SelectionContainer { Text(result, modifier = Modifier.padding(12.dp), fontSize = 13.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = { saveLauncher.launch("Proyecto-${target.lowercase()}.md") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Guardar")
                }
                OutlinedButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Código y proyecto CHAT DJ'S VIP", result))
                    status = "Código y contenido copiados al portapapeles."
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Copiar")
                }
                OutlinedButton(onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, result) }
                    runCatching { context.startActivity(Intent.createChooser(share, "Compartir proyecto")) }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Enviar")
                }
            }
            OutlinedButton(onClick = { zipLauncher.launch("Proyecto-CHAT-DJS-VIP.zip") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar archivos del proyecto en ZIP")
            }
            Text(
                "El ZIP contiene los bloques de código detectados y el documento completo. La compilación y ejecución de proyectos generados aún no están conectadas; esta pantalla prepara y organiza el trabajo, sin simular una compilación.",
                color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
            )
        }
    }
}
