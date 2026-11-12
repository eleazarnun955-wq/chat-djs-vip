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
        val paths = files.map { it.path.replace('\\\\', '/').lowercase() }.toSet()
        fun has(vararg candidates: String) = candidates.any { it.lowercase() in paths }
        val settings = has("settings.gradle", "settings.gradle.kts")
        val rootBuild = has("build.gradle", "build.gradle.kts")
        val appBuild = has("app/build.gradle", "app/build.gradle.kts")
        val manifest = has("app/src/main/AndroidManifest.xml", "src/main/AndroidManifest.xml", "AndroidManifest.xml")
        val source = paths.any {
            (it.startsWith("app/src/main/") || it.startsWith("src/main/")) &&
                (it.endsWith(".kt") || it.endsWith(".java"))
        }
        val wrapper = has("gradlew") && has("gradle/wrapper/gradle-wrapper.properties")
        val properties = has("gradle.properties")
        val androidProject = settings || appBuild || manifest
        val checks = listOf(
            ProjectBuildCheck("Configuración raíz de Gradle", settings && rootBuild,
                if (settings && rootBuild) "Se encontraron settings y build.gradle raíz." else "Faltan settings.gradle(.kts) o build.gradle(.kts) en la raíz."),
            ProjectBuildCheck("Módulo Android", appBuild,
                if (appBuild) "Se encontró la configuración Gradle del módulo app." else "Falta app/build.gradle(.kts)."),
            ProjectBuildCheck("AndroidManifest.xml", manifest,
                if (manifest) "Se encontró el manifiesto Android." else "Falta el manifiesto de la aplicación."),
            ProjectBuildCheck("Código fuente Android", source,
                if (source) "Se encontró al menos un archivo Kotlin o Java en src/main." else "No se detectó código Kotlin/Java en src/main."),
            ProjectBuildCheck("Gradle Wrapper", wrapper,
                if (wrapper) "Se encontraron gradlew y gradle-wrapper.properties." else "Falta el wrapper; añádelo para builds reproducibles."),
            ProjectBuildCheck("gradle.properties", properties,
                if (properties) "Se encontró gradle.properties." else "Opcional: agrega gradle.properties para fijar ajustes de Gradle.")
        )
        val requiredPassed = checks.take(4).all { it.passed }
        val ready = androidProject && requiredPassed && wrapper
        val summary = when {
            !androidProject -> "No se reconoce todavía un proyecto Android completo. Importa un ZIP de un proyecto Android."
            ready -> "La estructura básica parece lista para intentar una compilación externa. Esto no significa que ya se haya compilado."
            else -> "Se detectaron elementos Android, pero faltan componentes. Revisa los puntos pendientes antes de compilar."
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
