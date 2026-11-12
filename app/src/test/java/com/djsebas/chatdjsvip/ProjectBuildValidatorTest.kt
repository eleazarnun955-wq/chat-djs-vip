package com.djsebas.chatdjsvip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectBuildValidatorTest {
    private fun validManifest() = """<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="com.example.demo">
        <application>
            <activity android:name=".MainActivity">
                <intent-filter>
                    <action android:name="android.intent.action.MAIN" />
                    <category android:name="android.intent.category.LAUNCHER" />
                </intent-filter>
            </activity>
        </application>
    </manifest>"""

    private fun completeProject(manifest: String = validManifest()) = listOf(
        ImportedProjectFile("settings.gradle.kts", """rootProject.name = "Demo"
include(":app")"""),
        ImportedProjectFile("build.gradle.kts", "plugins {}"),
        ImportedProjectFile("app/build.gradle.kts", """plugins { id("com.android.application") }
android {
    namespace = "com.example.demo"
    defaultConfig { applicationId = "com.example.demo" }
}"""),
        ImportedProjectFile("app/src/main/AndroidManifest.xml", manifest),
        ImportedProjectFile("app/src/main/java/com/example/MainActivity.kt", "class MainActivity"),
        ImportedProjectFile("gradlew", "#!/bin/sh"),
        ImportedProjectFile("gradle/wrapper/gradle-wrapper.properties", "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10-bin.zip"),
        ImportedProjectFile("gradle/wrapper/gradle-wrapper.jar", "binary-placeholder")
    )

    @Test fun reportsMissingAndroidBuildFiles() {
        val report = ProjectBuildValidator.validate(listOf(ImportedProjectFile("app/src/main/AndroidManifest.xml", "<manifest />")))
        assertTrue(report.isAndroidProject)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Plugin Android" && !it.passed })
    }

    @Test fun recognizesCompleteAndroidGradleProject() {
        val report = ProjectBuildValidator.validate(completeProject())
        assertTrue(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Actividad de inicio" && it.passed })
        assertTrue(report.checks.any { it.title == "Identidad de aplicación" && it.passed })
        assertTrue(report.buildCommand.contains("assembleDebug"))
    }

    @Test fun rejectsMalformedManifestXml() {
        val report = ProjectBuildValidator.validate(completeProject("<manifest><application></manifest>"))
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Manifiesto XML" && !it.passed })
    }

    @Test fun recognizesLauncherDeclaredOnActivityAlias() {
        val manifest = """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application>
                <activity android:name=".MainActivity" />
                <activity-alias android:name=".Launcher" android:targetActivity=".MainActivity">
                    <intent-filter>
                        <action android:name="android.intent.action.MAIN" />
                        <category android:name="android.intent.category.LAUNCHER" />
                    </intent-filter>
                </activity-alias>
            </application>
        </manifest>"""
        val report = ProjectBuildValidator.validate(completeProject(manifest))
        assertTrue(report.checks.any { it.title == "Actividad de inicio" && it.passed })
    }

    @Test fun acceptsAndroidPluginFromVersionCatalogAlias() {
        val files = completeProject().map { file ->
            if (file.path == "app/build.gradle.kts") file.copy(content = """plugins { alias(libs.plugins.android.application) }
android {
    namespace = "com.example.demo"
    defaultConfig { applicationId = "com.example.demo" }
}""") else file
        }
        val report = ProjectBuildValidator.validate(files)
        assertTrue(report.checks.any { it.title == "Plugin Android" && it.passed })
    }

    @Test fun rejectsInsecureGradleDistributionUrl() {
        val files = completeProject().map { file ->
            if (file.path == "gradle/wrapper/gradle-wrapper.properties") {
                file.copy(content = "distributionUrl=http\\://services.gradle.org/distributions/gradle-8.10-bin.zip")
            } else file
        }
        val report = ProjectBuildValidator.validate(files)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Distribución Gradle segura" && !it.passed })
    }

    @Test fun warnsWhenLauncherIsMissing() {
        val manifest = """<manifest xmlns:android="http://schemas.android.com/apk/res/android">
            <application><activity android:name=".MainActivity" /></application>
        </manifest>"""
        val report = ProjectBuildValidator.validate(completeProject(manifest))
        assertTrue(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Actividad de inicio" && !it.passed })
    }

    @Test fun rejectsProjectWithoutAndroidPluginOrWrapperJar() {
        val files = listOf(
            ImportedProjectFile("settings.gradle", """include(":app")"""),
            ImportedProjectFile("build.gradle", "allprojects {}"),
            ImportedProjectFile("app/build.gradle", """plugins { id("java") }"""),
            ImportedProjectFile("app/src/main/AndroidManifest.xml", "<manifest />"),
            ImportedProjectFile("app/src/main/java/Main.java", "class Main {}"),
            ImportedProjectFile("gradlew", "#!/bin/sh"),
            ImportedProjectFile("gradle/wrapper/gradle-wrapper.properties", "distributionUrl=...")
        )
        val report = ProjectBuildValidator.validate(files)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Gradle Wrapper" && !it.passed })
    }

    @Test fun detectsDuplicatePathsIgnoringCase() {
        val files = listOf(
            ImportedProjectFile("settings.gradle", """include(":app")"""),
            ImportedProjectFile("SETTINGS.GRADLE", """include(":app")""")
        )
        val report = ProjectBuildValidator.validate(files)
        assertTrue(report.checks.any { it.title == "Rutas de archivos únicas" && !it.passed })
    }
    @Test fun rejectsProjectWhenAppModuleIsNotIncluded() {
        val files = completeProject().map { file ->
            if (file.path == "settings.gradle.kts") file.copy(content = """rootProject.name = "Demo" """) else file
        }
        val report = ProjectBuildValidator.validate(files)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Módulo app incluido" && !it.passed })
    }

    @Test fun rejectsProjectWhenAndroidApplicationPluginIsMissing() {
        val files = completeProject().map { file ->
            if (file.path == "app/build.gradle.kts") file.copy(content = """plugins { id("org.jetbrains.kotlin.jvm") }""") else file
        }
        val report = ProjectBuildValidator.validate(files)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Plugin Android" && !it.passed })
    }

}
