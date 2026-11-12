package com.djsebas.chatdjsvip

data class ProjectBuildCheck(
    val title: String,
    val passed: Boolean,
    val detail: String
)

data class ProjectBuildReport(
    val isAndroidProject: Boolean,
    val readyForBuildAttempt: Boolean,
    val checks: List<ProjectBuildCheck>,
    val buildCommand: String,
    val summary: String
)

object ProjectBuildValidator {
    fun validate(files: List<ImportedProjectFile>): ProjectBuildReport {
        val normalized = files.map { it.path.replace('\\', '/').lowercase() }
        val paths = normalized.toSet()
        fun has(vararg candidates: String) = candidates.any { it.lowercase() in paths }
        fun content(path: String): String = files.firstOrNull {
            it.path.replace('\\', '/').equals(path, ignoreCase = true)
        }?.content.orEmpty()

        val settingsPath = listOf("settings.gradle.kts", "settings.gradle").firstOrNull { it in paths }
        val rootBuildPath = listOf("build.gradle.kts", "build.gradle").firstOrNull { it in paths }
        val appBuildPath = listOf("app/build.gradle.kts", "app/build.gradle").firstOrNull { it in paths }
        val settings = settingsPath != null
        val rootBuild = rootBuildPath != null
        val appBuild = appBuildPath != null
        val manifestPath = listOf(
            "app/src/main/AndroidManifest.xml",
            "src/main/AndroidManifest.xml",
            "AndroidManifest.xml"
        ).firstOrNull { it in paths }
        val manifestText = manifestPath?.let(::content).orEmpty()
        val manifestValid = manifestPath != null &&
            manifestText.contains("<manifest", ignoreCase = true) &&
            manifestText.contains(">", ignoreCase = true)
        val source = paths.any {
            (it.startsWith("app/src/main/") || it.startsWith("src/main/")) &&
                (it.endsWith(".kt") || it.endsWith(".java"))
        }
        val wrapperScript = has("gradlew")
        val wrapperProperties = has("gradle/wrapper/gradle-wrapper.properties")
        val wrapperJar = has("gradle/wrapper/gradle-wrapper.jar")
        val wrapper = wrapperScript && wrapperProperties && wrapperJar
        val properties = has("gradle.properties")
        val settingsText = settingsPath?.let(::content).orEmpty()
        val includesApp = Regex("""include\s*\(?\s*["']?:app["']?""")
            .containsMatchIn(settingsText) || Regex("""include\s*\(\s*["']app["']""")
            .containsMatchIn(settingsText)
        val appBuildText = appBuildPath?.let(::content).orEmpty()
        val androidPlugin = Regex("""com\.android\.application""")
            .containsMatchIn(appBuildText) ||
            appBuildText.contains("com.android.tools.build:gradle")
        val duplicatePaths = normalized.size != paths.size
        val androidProject = settings || appBuild || manifestPath != null

        val checks = listOf(
            ProjectBuildCheck("Configuración raíz de Gradle", settings && rootBuild,
                if (settings && rootBuild) "Se encontraron settings y build.gradle raíz." else "Falta settings.gradle(.kts) o build.gradle(.kts) en la raíz."),
            ProjectBuildCheck("Módulo app incluido", settings && includesApp,
                if (includesApp) "settings.gradle declara el módulo :app." else "No se detectó la inclusión explícita de :app en settings.gradle."),
            ProjectBuildCheck("Plugin Android", appBuild && androidPlugin,
                if (androidPlugin) "El módulo app declara el plugin de aplicación Android." else "No se detectó com.android.application en app/build.gradle(.kts)."),
            ProjectBuildCheck("AndroidManifest.xml", manifestValid,
                if (manifestValid) "Se encontró un manifiesto con estructura XML inicial." else "Falta un manifiesto reconocible o su contenido parece incompleto."),
            ProjectBuildCheck("Código fuente Android", source,
                if (source) "Se encontró al menos un archivo Kotlin o Java en src/main." else "No se detectó código Kotlin/Java en src/main."),
            ProjectBuildCheck("Gradle Wrapper", wrapper,
                if (wrapper) "Se encontraron gradlew, gradle-wrapper.properties y gradle-wrapper.jar." else "Falta gradlew, gradle-wrapper.properties o gradle-wrapper.jar."),
            ProjectBuildCheck("Rutas de archivos únicas", !duplicatePaths,
                if (!duplicatePaths) "No se detectaron rutas duplicadas ignorando mayúsculas." else "Hay rutas duplicadas que pueden sobrescribirse al exportar."),
            ProjectBuildCheck("gradle.properties", properties,
                if (properties) "Se encontró gradle.properties." else "Opcional: agrega gradle.properties para fijar ajustes de Gradle.")
        )
        val requiredIndices = listOf(0, 1, 2, 3, 4, 5, 6)
        val ready = androidProject && requiredIndices.all { checks[it].passed }
        val summary = when {
            !androidProject -> "No se reconoce todavía un proyecto Android. Importa un ZIP de un proyecto Android."
            ready -> "La estructura y los archivos esenciales parecen listos para intentar una compilación. Esta revisión no ejecuta Gradle ni garantiza que compile."
            else -> "Se detectó un proyecto Android, pero faltan archivos o configuraciones. Corrige los puntos pendientes antes de intentar compilar."
        }
        return ProjectBuildReport(
            isAndroidProject = androidProject,
            readyForBuildAttempt = ready,
            checks = checks,
            buildCommand = "./gradlew assembleDebug",
            summary = summary
        )
    }
}
