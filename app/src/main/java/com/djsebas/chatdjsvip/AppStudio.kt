package com.djsebas.chatdjsvip

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AppStudioScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var requirements by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("Android") }
    var result by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Describe la aplicación que quieres crear.") }

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(result.toByteArray(Charsets.UTF_8))
                } ?: error("No se pudo abrir el archivo")
            }.isSuccess
            status = if (saved) "Proyecto guardado en tus archivos." else "No se pudo guardar el proyecto."
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row {
            Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            Text("ESTUDIO DE APLICACIONES", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }
        Text(
            "Describe tu idea y recibe una propuesta técnica y código inicial organizado por archivos. Para generar necesitas configurar Gemini en Ajustes del Chat.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
        Text("¿Qué tipo de aplicación quieres crear?", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = target == "Android", onClick = { target = "Android" }, label = { Text("Android / APK") })
            FilterChip(selected = target == "Web", onClick = { target = "Web" }, label = { Text("Página / Web app") })
        }
        OutlinedTextField(
            value = requirements,
            onValueChange = { requirements = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4,
            label = { Text("Describe tu aplicación") },
            placeholder = { Text("Ejemplo: una app para organizar eventos, con usuarios, calendario, fotografías y notificaciones…") }
        )
        Button(
            onClick = {
                isGenerating = true
                result = ""
                status = "Diseñando la solución…"
                val prompt = """MODO_CREADOR_APPS:
Plataforma solicitada: $target.
Requisitos del usuario:
${requirements.trim()}

Entrega una solución de desarrollo realista. Primero resume el alcance y la arquitectura. Después organiza la respuesta por archivos, indicando la ruta exacta y proporcionando el contenido completo de cada archivo esencial. Incluye dependencias, versiones compatibles, configuración, instrucciones de compilación y pruebas. Para Android usa Kotlin y Jetpack Compose salvo que exista una razón técnica clara para otra opción. Para web indica framework, estructura y comandos. Incluye manejo de errores, accesibilidad y seguridad. No inventes servicios ya conectados ni afirmes que compilaste o probaste algo. Si faltan datos imprescindibles, declara los supuestos y pregunta solo lo necesario. Si la respuesta es demasiado extensa, divide el trabajo en entregas numeradas y no omitas archivos esenciales."""
                GeminiClient.reply(context, prompt, emptyList()) { answer ->
                    result = answer
                    isGenerating = false
                    status = if (answer.startsWith("Para generar código")) "Falta configurar la clave de IA." else "Borrador generado. Revisa el código antes de compilar."
                }
            },
            enabled = requirements.isNotBlank() && !isGenerating,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Generando…")
            } else {
                Icon(Icons.Default.Code, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Diseñar y generar aplicación")
            }
        }
        Text(status, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
        if (result.isNotBlank()) {
            HorizontalDivider()
            Text("Propuesta y código generado", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                SelectionContainer {
                    Text(result, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { saveLauncher.launch("Proyecto-${target.lowercase()}.md") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Guardar")
                }
                OutlinedButton(
                    onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, result)
                        }
                        runCatching { context.startActivity(Intent.createChooser(share, "Compartir proyecto")) }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(Modifier.width(5.dp))
                    Text("Compartir")
                }
            }
            Text(
                "Este módulo genera especificaciones y código. La compilación, ejecución de pruebas y publicación automática requieren integrar un entorno de construcción seguro; todavía no se realizan desde esta pantalla.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
