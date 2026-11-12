package com.djsebas.chatdjsvip

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class ProjectBuildCheck(val title: String, val passed: Boolean, val detail: String)
data class ProjectBuildReport(
    val isAndroidProject: Boolean,
    val readyForBuildAttempt: Boolean,
    val checks: List<ProjectBuildCheck>,
    val buildCommand: String,
    val summary: String
)

object ProjectBuildValidator {
    private data class ManifestInfo(
        val valid: Boolean, val hasApplication: Boolean, val hasLauncher: Boolean,
        val packageName: String?, val error: String?
    )

    private fun inspectManifest(xml: String): ManifestInfo {
        if (xml.isBlank()) return ManifestInfo(false, false, false, null, "El manifiesto está vacío.")
        return try {
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = true
                isExpandEntityReferences = false
                setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
                setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
                setFeature("http://xml.org/sax/features/external-general-entities", false)
                setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            }
            val builder = factory.newDocumentBuilder()
            builder.setEntityResolver { _, _ -> InputSource(StringReader("")) }
            val document = builder.parse(InputSource(StringReader(xml)))
            val root = document.documentElement
            if (root == null || root.tagName != "manifest") {
                return ManifestInfo(false, false, false, null, "La raíz XML debe ser <manifest>.")
            }
            val applications = root.getElementsByTagName("application")
            var launcher = false
            val activities = root.getElementsByTagName("activity")
            for (i in 0 until activities.length) {
                val activity = activities.item(i) as? org.w3c.dom.Element ?: continue
                val filters = activity.getElementsByTagName("intent-filter")
                for (j in 0 until filters.length) {
                    val filter = filters.item(j) as? org.w3c.dom.Element ?: continue
                    val actions = filter.getElementsByTagName("action")
                    val categories = filter.getElementsByTagName("category")
                    val hasMain = (0 until actions.length).any {
                        (actions.item(it) as? org.w3c.dom.Element)?.getAttributeNS(
                            "http://schemas.android.com/apk/res/android", "name"
                        ) == "android.intent.action.MAIN"
                    }
                    val hasLauncherCategory = (0 until categories.length).any {
                        (categories.item(it) as? org.w3c.dom.Element)?.getAttributeNS(
                            "http://schemas.android.com/apk/res/android", "name"
                        ) == "android.intent.category.LAUNCHER"
                    }
                    if (hasMain && hasLauncherCategory) launcher = true
                }
            }
            ManifestInfo(true, applications.length > 0, launcher,
                root.getAttribute("package").takeIf { it.isNotBlank() }, null)
        } catch (error: Exception) {
            ManifestInfo(false, false, false, null, error.message?.take(180) ?: "XML no válido.")
        }
    }

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
        val manifestPath = listOf("app/src/main/androidmanifest.xml", "src/main/androidmanifest.xml", "androidmanifest.xml")
            .firstOrNull { it in paths }
        val manifestInfo = manifestPath?.let { inspectManifest(content(it)) }
        val source = paths.any {
            (it.startsWith("app/src/main/") || it.startsWith("src/main/")) &&
                (it.endsWith(".kt") || it.endsWith(".java"))
        }
        val wrapper = has("gradlew") && has("gradle/wrapper/gradle-wrapper.properties") &&
            has("gradle/wrapper/gradle-wrapper.jar")
        val properties = has("gradle.properties")
        val settingsText = settingsPath?.let(::content).orEmpty()
        val includesApp = Regex("""include\s*\(?\s*["']?:app["']?""").containsMatchIn(settingsText) ||
            Regex("""include\s*\(\s*["']app["']""").containsMatchIn(settingsText)
        val appBuildText = appBuildPath?.let(::content).orEmpty()
        val androidPlugin = Regex("""com\.android\.application""").containsMatchIn(appBuildText) ||
            appBuildText.contains("com.android.tools.build:gradle")
        val namespace = Regex("""namespace\s*=\s*["']([^"']+)["']""").find(appBuildText)?.groupValues?.getOrNull(1)
        val applicationId = Regex("""applicationId\s*=\s*["']([^"']+)["']""").find(appBuildText)?.groupValues?.getOrNull(1)
        val duplicatePaths = normalized.size != paths.size
        val androidProject = settings || appBuild || manifestPath != null
        val checks = listOf(
            ProjectBuildCheck("Configuración raíz de Gradle", settings && rootBuild,
                if (settings && rootBuild) "Se encontraron settings y build.gradle raíz." else "Falta settings.gradle(.kts) o build.gradle(.kts) en la raíz."),
            ProjectBuildCheck("Módulo app incluido", settings && includesApp,
                if (includesApp) "settings.gradle declara el módulo :app." else "No se detectó la inclusión explícita de :app en settings.gradle."),
            ProjectBuildCheck("Plugin Android", appBuild && androidPlugin,
                if (androidPlugin) "El módulo app declara el plugin de aplicación Android." else "No se detectó com.android.application en app/build.gradle(.kts)."),
            ProjectBuildCheck("Manifiesto XML", manifestInfo?.valid == true,
                if (manifestInfo?.valid == true) "El XML es válido y su elemento raíz es <manifest>." else "Manifiesto ausente o XML inválido: ${manifestInfo?.error ?: "no se encontró el archivo"}."),
            ProjectBuildCheck("Elemento application", manifestInfo?.hasApplication == true,
                if (manifestInfo?.hasApplication == true) "Se encontró el elemento <application>." else "No se encontró <application>; revisa el manifiesto."),
            ProjectBuildCheck("Actividad de inicio", manifestInfo?.hasLauncher == true,
                if (manifestInfo?.hasLauncher == true) "Se detectó una actividad con MAIN y LAUNCHER." else "No se detectó una actividad con intent-filter MAIN/LAUNCHER."),
            ProjectBuildCheck("Identidad de aplicación", !namespace.isNullOrBlank() && !applicationId.isNullOrBlank(),
                if (!namespace.isNullOrBlank() && !applicationId.isNullOrBlank()) "namespace y applicationId están declarados." else "Revisa namespace y applicationId en app/build.gradle(.kts)."),
            ProjectBuildCheck("Código fuente Android", source,
                if (source) "Se encontró al menos un archivo Kotlin o Java en src/main." else "No se detectó código Kotlin/Java en src/main."),
            ProjectBuildCheck("Gradle Wrapper", wrapper,
                if (wrapper) "Se encontraron gradlew, gradle-wrapper.properties y gradle-wrapper.jar." else "Falta gradlew, gradle-wrapper.properties o gradle-wrapper.jar."),
            ProjectBuildCheck("Rutas de archivos únicas", !duplicatePaths,
                if (!duplicatePaths) "No se detectaron rutas duplicadas ignorando mayúsculas." else "Hay rutas duplicadas que pueden sobrescribirse al exportar."),
            ProjectBuildCheck("gradle.properties", properties,
                if (properties) "Se encontró gradle.properties." else "Opcional: agrega gradle.properties para fijar ajustes de Gradle.")
        )
        val required = setOf("Configuración raíz de Gradle", "Módulo app incluido", "Plugin Android",
            "Manifiesto XML", "Código fuente Android", "Gradle Wrapper", "Rutas de archivos únicas")
        val ready = androidProject && checks.filter { it.title in required }.all { it.passed }
        val summary = when {
            !androidProject -> "No se reconoce todavía un proyecto Android. Importa un ZIP de un proyecto Android."
            ready -> "La estructura esencial parece lista para intentar una compilación. Revisa también los avisos de manifiesto e identidad. Esta revisión no ejecuta Gradle ni garantiza que compile."
            else -> "Se detectó un proyecto Android, pero faltan archivos o configuraciones esenciales. Corrige los puntos pendientes antes de intentar compilar."
        }
        return ProjectBuildReport(androidProject, ready, checks, "./gradlew assembleDebug", summary)
    }
}
