package com.djsebas.chatdjsvip

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Delete
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
    var projectName by remember { mutableStateOf("Mi proyecto") }
    var pendingDeleteProject by remember { mutableStateOf<String?>(null) }
    var importedFiles by remember { mutableStateOf<List<ImportedProjectFile>>(emptyList()) }
    var selectedFilePath by remember { mutableStateOf<String?>(null) }
    var editorContent by remember { mutableStateOf("") }
    var newFilePath by remember { mutableStateOf("") }
    var pendingDeleteFilePath by remember { mutableStateOf<String?>(null) }
    var pendingFileSwitchPath by remember { mutableStateOf<String?>(null) }
    var fileSearch by remember { mutableStateOf("") }
    var fileTypeFilter by remember { mutableStateOf("Todos") }
    var buildReport by remember { mutableStateOf<ProjectBuildReport?>(null) }
    var taskMode by remember { mutableStateOf("Crear") }
    val savedProjects = remember(context) {
        val stored = preferences.getString("app_studio_projects", null)
        mutableStateListOf<String>().apply {
            if (!stored.isNullOrBlank()) runCatching {
                val json = JSONArray(stored)
                for (i in 0 until json.length()) add(json.getString(i))
            }
        }
    }
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
    val currentStoredContent = importedFiles.firstOrNull { it.path == selectedFilePath }?.content
    val isEditorDirty = selectedFilePath != null && currentStoredContent != null && editorContent != currentStoredContent
    val visibleFiles = importedFiles.filter { file ->
        val matchesSearch = fileSearch.isBlank() || file.path.contains(fileSearch.trim(), ignoreCase = true)
        val lowerPath = file.path.lowercase()
        val matchesType = when (fileTypeFilter) {
            "Kotlin" -> lowerPath.endsWith(".kt") || lowerPath.endsWith(".java")
            "XML" -> lowerPath.endsWith(".xml")
            "Gradle" -> lowerPath.endsWith(".gradle") || lowerPath.endsWith(".gradle.kts") || lowerPath.contains("gradle")
            "Otros" -> !(lowerPath.endsWith(".kt") || lowerPath.endsWith(".java") || lowerPath.endsWith(".xml") ||
                lowerPath.endsWith(".gradle") || lowerPath.endsWith(".gradle.kts") || lowerPath.contains("gradle"))
            else -> true
        }
        matchesSearch && matchesType
    }
    val selectProjectFile: (String) -> Unit = { path ->
        if (path != selectedFilePath && isEditorDirty) {
            pendingFileSwitchPath = path
        } else {
            selectedFilePath = path
            editorContent = importedFiles.firstOrNull { it.path == path }?.content.orEmpty()
        }
    }
    val saveProject: (List<ImportedProjectFile>) -> Unit = { filesToSave ->
        val cleanName = projectName.trim().take(60)
        if (cleanName.isNotBlank()) {
            val projectsJson = runCatching { org.json.JSONObject(preferences.getString("app_studio_project_data", "{}").orEmpty()) }
                .getOrElse { org.json.JSONObject() }
            val project = org.json.JSONObject().put("name", cleanName).put("target", target)
                .put("history", JSONArray(studioHistory.toList())).put("result", result)
                .put("files", JSONArray().apply { filesToSave.forEach { put(org.json.JSONObject().put("path", it.path).put("content", it.content)) } })
            projectsJson.put(cleanName, project)
            if (!savedProjects.contains(cleanName)) savedProjects.add(cleanName)
            preferences.edit().putString("app_studio_projects", JSONArray(savedProjects.toList()).toString())
                .putString("app_studio_project_data", projectsJson.toString()).apply()
            status = "Proyecto «$cleanName» guardado en este dispositivo."
        }
    }
    LaunchedEffect(editorContent, selectedFilePath, projectName, importedFiles) {
        val path = selectedFilePath
        val storedContent = importedFiles.firstOrNull { it.path == path }?.content
        if (path != null && storedContent != null && editorContent != storedContent && projectName.isNotBlank()) {
            kotlinx.coroutines.delay(800)
            if (selectedFilePath == path && editorContent != storedContent) {
                val snapshot = importedFiles.map { file ->
                    if (file.path == path) file.copy(content = editorContent) else file
                }
                importedFiles = snapshot
                saveProject(snapshot)
                status = "Guardado automático: $path"
            }
        }
    }
    val loadProject: (String) -> Unit = { name ->
        if (name == projectName) {
            status = "«$name» ya es el proyecto abierto; se conservaron tus cambios."
        } else {
            val snapshot = importedFiles.map { file -> if (file.path == selectedFilePath) file.copy(content = editorContent) else file }
            if (importedFiles.isNotEmpty()) saveProject(snapshot)
            val projectsJson = runCatching { org.json.JSONObject(preferences.getString("app_studio_project_data", "{}").orEmpty()) }
                .getOrElse { org.json.JSONObject() }
            val project = projectsJson.optJSONObject(name)
            if (project != null) {
                projectName = name
                target = project.optString("target", "Android")
                preferences.edit().putString("app_studio_target", target).apply()
                studioHistory.clear()
                val history = project.optJSONArray("history") ?: JSONArray()
                for (i in 0 until history.length()) studioHistory.add(history.optString(i))
                result = project.optString("result", "")
                importedFiles = buildList {
                    val files = project.optJSONArray("files") ?: JSONArray()
                    for (i in 0 until files.length()) {
                        val item = files.optJSONObject(i) ?: continue
                        add(ImportedProjectFile(item.optString("path"), item.optString("content")))
                    }
                }
                selectedFilePath = importedFiles.firstOrNull()?.path
                editorContent = importedFiles.firstOrNull()?.content.orEmpty()
                preferences.edit().putString("app_studio_history", JSONArray(studioHistory).toString())
                    .putString("app_studio_last_result", result).apply()
                status = "Proyecto «$name» abierto."
            } else {
                status = "No se pudo abrir el proyecto guardado."
            }
        }
    }
    val deleteProject: (String) -> Unit = { name ->
        val projectsJson = runCatching { org.json.JSONObject(preferences.getString("app_studio_project_data", "{}").orEmpty()) }
            .getOrElse { org.json.JSONObject() }
        projectsJson.remove(name)
        savedProjects.remove(name)
        preferences.edit().putString("app_studio_projects", JSONArray(savedProjects.toList()).toString())
            .putString("app_studio_project_data", projectsJson.toString()).apply()
        if (projectName == name) projectName = "Mi proyecto"
        status = "Proyecto guardado «$name» eliminado. El contenido que tienes abierto no se borró."
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val imported = ProjectZipImporter.import(context, uri)
            if (imported.success) {
                importedFiles = imported.files
                selectedFilePath = imported.files.firstOrNull()?.path
                editorContent = imported.files.firstOrNull()?.content.orEmpty()
                status = "Se importaron ${imported.files.size} archivos. Se incluirán como contexto para las próximas mejoras."
            } else status = "No se pudo importar: ${imported.error}"
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
            val filesToExport = importedFiles.map { file ->
                if (file.path == selectedFilePath) file.copy(content = editorContent) else file
            }
            val export = ProjectZipExporter.export(context, uri, result, filesToExport)
            status = if (export.success) "ZIP creado: ${export.fileCount} archivos del proyecto y código listos para extraer." else "No se pudo crear el ZIP: ${export.error}"
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
        Text("Proyecto actual", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(value = projectName, onValueChange = { projectName = it }, modifier = Modifier.weight(1f),
                singleLine = true, label = { Text("Nombre del proyecto") })
            Button(onClick = { saveProject(importedFiles.map { file -> if (file.path == selectedFilePath) file.copy(content = editorContent) else file }) }, enabled = !isGenerating && projectName.isNotBlank()) { Text("Guardar") }
        }
        if (savedProjects.isNotEmpty()) {
            Text("Mis proyectos guardados", fontWeight = FontWeight.SemiBold)
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                savedProjects.forEach { name ->
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        FilterChip(selected = projectName == name, onClick = { loadProject(name) }, label = { Text(name) })
                        IconButton(onClick = { pendingDeleteProject = name }, enabled = !isGenerating) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar proyecto $name")
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedButton(onClick = {
                val snapshot = importedFiles.map { file -> if (file.path == selectedFilePath) file.copy(content = editorContent) else file }
                if (isEditorDirty) saveProject(snapshot)
                importLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed"))
            }, enabled = !isGenerating) {
                Icon(Icons.Default.Download, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Importar proyecto ZIP")
            }
            Text(if (importedFiles.isEmpty()) "Sin archivos importados" else "${importedFiles.size} archivos cargados", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        if (importedFiles.isNotEmpty()) {
            Text("Explorador y editor de archivos", fontWeight = FontWeight.SemiBold)
            Text("Busca por nombre o ruta y filtra por tipo de archivo. Los cambios pendientes se protegen al cambiar de archivo.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            OutlinedTextField(
                value = fileSearch,
                onValueChange = { fileSearch = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Buscar archivo por nombre o ruta") },
                placeholder = { Text("MainActivity, manifest, gradle…") }
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Todos", "Kotlin", "XML", "Gradle", "Otros").forEach { type ->
                    FilterChip(
                        selected = fileTypeFilter == type,
                        onClick = { fileTypeFilter = type },
                        label = { Text(type) }
                    )
                }
            }
            Text("Mostrando ${visibleFiles.size} de ${importedFiles.size} archivos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newFilePath,
                    onValueChange = { newFilePath = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Ruta del archivo nuevo") },
                    placeholder = { Text("app/src/Main.kt") }
                )
                Button(onClick = {
                    val path = newFilePath.trim()
                    val segments = path.split("/")
                    val valid = path.isNotBlank() && !path.startsWith("/") && !path.contains("\\") &&
                        segments.none { it.isBlank() || it == "." || it == ".." } &&
                        !path.contains(":") && path.length <= 180
                    when {
                        !valid -> status = "Ruta no válida. Usa una ruta relativa sin .. ni segmentos vacíos."
                        importedFiles.any { it.path == path } -> status = "Ese archivo ya existe."
                        importedFiles.size >= 100 -> status = "El proyecto alcanzó el límite de 100 archivos."
                        else -> {
                            val currentFiles = importedFiles.map { file -> if (file.path == selectedFilePath) file.copy(content = editorContent) else file }
                            val updatedFiles = currentFiles + ImportedProjectFile(path, "")
                            importedFiles = updatedFiles
                            selectedFilePath = path
                            editorContent = ""
                            newFilePath = ""
                            saveProject(updatedFiles)
                            status = "Archivo $path creado y guardado en el proyecto."
                        }
                    }
                }, enabled = !isGenerating) { Text("Crear") }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                visibleFiles.forEach { file ->
                    TextButton(
                        onClick = { selectProjectFile(file.path) },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(if (selectedFilePath == file.path) "▸ ${file.path}" else file.path, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            selectedFilePath?.let { path ->
                Text(path, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                if (isEditorDirty) Text("Cambios sin guardar", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp)
                OutlinedTextField(
                    value = editorContent,
                    onValueChange = { editorContent = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 8,
                    maxLines = 24,
                    label = { Text("Contenido del archivo") }
                )
                Button(onClick = {
                    val updatedFiles = importedFiles.map { file ->
                        if (file.path == path) file.copy(content = editorContent) else file
                    }
                    importedFiles = updatedFiles
                    saveProject(updatedFiles)
                    status = "Cambios guardados en $path."
                }, enabled = !isGenerating, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Code, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Guardar cambios del archivo")
                }
                OutlinedButton(
                    onClick = { pendingDeleteFilePath = path },
                    enabled = !isGenerating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Eliminar este archivo")
                }
            }
        }
        if (importedFiles.isNotEmpty()) {
            OutlinedButton(
                onClick = { zipLauncher.launch("Proyecto-${projectName.trim().ifBlank { "CHAT-DJS-VIP" }}.zip") },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exportar archivos del proyecto en ZIP")
            }
            Text(
                "Puedes exportar tus archivos aunque todavía no hayas generado una respuesta de IA. Se incluirán los cambios actuales del editor.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Text("¿Qué tipo de aplicación quieres crear?", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = target == "Android", onClick = { target = "Android"; preferences.edit().putString("app_studio_target", "Android").apply() }, label = { Text("Android / APK") })
            FilterChip(selected = target == "Web", onClick = { target = "Web"; preferences.edit().putString("app_studio_target", "Web").apply() }, label = { Text("Página / Web app") })
        }
        if (target == "Android") {
            OutlinedButton(
                onClick = {
                    val currentFiles = importedFiles.map { file ->
                        if (file.path == selectedFilePath) file.copy(content = editorContent) else file
                    }
                    buildReport = ProjectBuildValidator.validate(currentFiles)
                    status = "Revisión de estructura Android terminada. No se ejecutó una compilación."
                },
                enabled = !isGenerating,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Revisar si el proyecto está listo para compilar") }
            buildReport?.let { report ->
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Diagnóstico de compilación", fontWeight = FontWeight.Bold)
                        Text(report.summary, fontSize = 13.sp)
                        report.checks.forEach { check ->
                            Text(
                                (if (check.passed) "✓ " else "• ") + check.title + ": " + check.detail,
                                fontSize = 12.sp,
                                color = if (check.passed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (report.isAndroidProject) {
                            Text("Comando sugerido: ${report.buildCommand}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
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
            val filesContext = importedFiles.joinToString("\n\n") { file -> "Archivo existente: ${file.path}\n```\n${file.content}\n```" }.take(120000)
            val prompt = """MODO_CREADOR_APPS:
Plataforma solicitada: $target.
Modo de trabajo: $taskMode.
${if (studioHistory.isEmpty()) "Requisitos iniciales del usuario:" else "Nueva instrucción del usuario para continuar el proyecto existente:"}
$userRequest

Archivos existentes importados; respeta estas rutas y conserva el código que no necesite cambios:
$filesContext

Modo Crear: genera estructura inicial funcional y archivos completos.
Modo Mejorar: conserva funciones existentes y limita cambios a archivos necesarios.
Modo Corregir: explica causa del error, entrega corrección y pruebas de regresión.
Modo Preparar build: revisa dependencias, configuración, permisos, comandos reproducibles y criterios de éxito; no afirmes compilación real sin ejecutarla.

Entrega una solución de desarrollo realista. Primero resume el alcance y la arquitectura. Después organiza la respuesta por archivos, indicando la ruta exacta y proporcionando el contenido completo de cada archivo esencial. IMPORTANTE: antes de cada bloque de código escribe una línea independiente con este formato exacto: Archivo: ruta/relativa/al/proyecto.ext. Usa rutas relativas seguras, sin .. ni rutas absolutas. Incluye dependencias, versiones compatibles, configuración, instrucciones de compilación y pruebas. Para Android usa Kotlin y Jetpack Compose salvo que exista una razón técnica clara para otra opción. Para web indica framework, estructura y comandos. Incluye manejo de errores, accesibilidad y seguridad. No inventes servicios ya conectados ni afirmes que compilaste o probaste algo. Si faltan datos imprescindibles, declara los supuestos y pregunta solo lo necesario. Si la respuesta es demasiado extensa, divide el trabajo en entregas numeradas y no omitas archivos esenciales. Si ya existe un proyecto en el historial, conserva sus decisiones y archivos previos y entrega cambios compatibles, indicando exactamente qué archivos reemplazar o crear."""
            isGenerating = true
            result = ""
            status = when (taskMode) {
                "Crear" -> "Analizando el proyecto localmente…"
                "Mejorar" -> "Revisando mejoras con el cerebro local…"
                "Corregir" -> "Diagnosticando con el cerebro local…"
                else -> "Revisando preparación de build localmente…"
            }
            val historySnapshot = studioHistory.toList()
            LocalBrainEngine.reply(context, prompt, historySnapshot) { answer ->
                studioHistory.add("Tú: $prompt")
                studioHistory.add("VIP: $answer")
                while (studioHistory.size > 24) studioHistory.removeAt(0)
                val updatedHistory = JSONArray(studioHistory.toList())
                val projectStore = runCatching {
                    org.json.JSONObject(preferences.getString("app_studio_project_data", "{}").orEmpty())
                }.getOrElse { org.json.JSONObject() }
                val cleanProjectName = projectName.trim().take(60).ifBlank { "Mi proyecto" }
                projectStore.put(cleanProjectName, org.json.JSONObject()
                    .put("name", cleanProjectName)
                    .put("target", target)
                    .put("history", updatedHistory)
                    .put("result", answer)
                    .put("files", JSONArray().apply { importedFiles.forEach { put(org.json.JSONObject().put("path", it.path).put("content", it.content)) } }))
                if (!savedProjects.contains(cleanProjectName)) savedProjects.add(cleanProjectName)
                preferences.edit()
                    .putString("app_studio_history", updatedHistory.toString())
                    .putString("app_studio_last_result", answer)
                    .putString("app_studio_target", target)
                    .putString("app_studio_projects", JSONArray(savedProjects.toList()).toString())
                    .putString("app_studio_project_data", projectStore.toString())
                    .apply()
                projectName = cleanProjectName
                result = answer
                isGenerating = false
                status = "Análisis local guardado automáticamente en «$cleanProjectName»."
            }
        }, enabled = requirements.isNotBlank() && !isGenerating, modifier = Modifier.fillMaxWidth()) {
            if (isGenerating) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text("Analizando…")
            } else {
                Icon(Icons.Default.Code, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(when (taskMode) {
                    "Crear" -> "Analizar aplicación localmente"
                    "Mejorar" -> "Analizar mejoras localmente"
                    "Corregir" -> "Diagnosticar errores localmente"
                    else -> "Revisar build localmente"
                })
            }
        }
        if (studioHistory.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contexto guardado: ${studioHistory.size / 2} intercambios", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                TextButton(onClick = {
                    val snapshot = importedFiles.map { file -> if (file.path == selectedFilePath) file.copy(content = editorContent) else file }
                    if (isEditorDirty) saveProject(snapshot)
                    studioHistory.clear()
                    preferences.edit().remove("app_studio_history").remove("app_studio_last_result").apply()
                    result = ""
                    status = "Contexto del estudio borrado. Puedes iniciar otro proyecto."
                }, enabled = !isGenerating) { Text("Nuevo proyecto") }
            }
        }
        pendingFileSwitchPath?.let { nextPath ->
            AlertDialog(
                onDismissRequest = { pendingFileSwitchPath = null },
                title = { Text("Cambios sin guardar") },
                text = { Text("El archivo actual tiene cambios que todavía no se han guardado. ¿Qué quieres hacer antes de abrir $nextPath?") },
                confirmButton = {
                    TextButton(onClick = {
                        val currentPath = selectedFilePath
                        val updatedFiles = importedFiles.map { file ->
                            if (file.path == currentPath) file.copy(content = editorContent) else file
                        }
                        importedFiles = updatedFiles
                        saveProject(updatedFiles)
                        selectedFilePath = nextPath
                        editorContent = updatedFiles.firstOrNull { it.path == nextPath }?.content.orEmpty()
                        pendingFileSwitchPath = null
                        status = "Cambios guardados y archivo $nextPath abierto."
                    }) { Text("Guardar y abrir") }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = {
                            selectedFilePath = nextPath
                            editorContent = importedFiles.firstOrNull { it.path == nextPath }?.content.orEmpty()
                            pendingFileSwitchPath = null
                            status = "Se descartaron los cambios no guardados del archivo anterior."
                        }) { Text("Descartar") }
                        TextButton(onClick = { pendingFileSwitchPath = null }) { Text("Cancelar") }
                    }
                }
            )
        }
        pendingDeleteFilePath?.let { path ->
            AlertDialog(
                onDismissRequest = { pendingDeleteFilePath = null },
                title = { Text("Eliminar archivo") },
                text = { Text(if (isEditorDirty && selectedFilePath == path) "Este archivo tiene cambios sin guardar. Si confirmas, se eliminarán junto con el archivo. ¿Continuar?" else "¿Eliminar $path del proyecto abierto? Esta acción no se puede deshacer.") },
                confirmButton = {
                    TextButton(onClick = {
                        val updatedFiles = importedFiles.filterNot { it.path == path }
                        importedFiles = updatedFiles
                        if (selectedFilePath == path) {
                            selectedFilePath = updatedFiles.firstOrNull()?.path
                            editorContent = updatedFiles.firstOrNull()?.content.orEmpty()
                        }
                        saveProject(updatedFiles)
                        status = "Archivo $path eliminado del proyecto."
                        pendingDeleteFilePath = null
                    }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteFilePath = null }) { Text("Cancelar") }
                }
            )
        }
        pendingDeleteProject?.let { name ->
            AlertDialog(
                onDismissRequest = { pendingDeleteProject = null },
                title = { Text("Eliminar proyecto") },
                text = { Text("¿Seguro que quieres eliminar «$name» de tus proyectos guardados? Esta acción no se puede deshacer. El contenido que tengas abierto permanecerá intacto.") },
                confirmButton = {
                    TextButton(onClick = { deleteProject(name); pendingDeleteProject = null }) { Text("Eliminar") }
                },
                dismissButton = {
                    TextButton(onClick = { pendingDeleteProject = null }) { Text("Cancelar") }
                }
            )
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
