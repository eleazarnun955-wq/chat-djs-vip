package com.djsebas.chatdjsvip

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectBuildValidatorTest {
    @Test
    fun reportsMissingAndroidBuildFiles() {
        val report = ProjectBuildValidator.validate(
            listOf(ImportedProjectFile("app/src/main/AndroidManifest.xml", "<manifest />"))
        )
        assertTrue(report.isAndroidProject)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Plugin Android" && !it.passed })
    }

    @Test
    fun recognizesBasicAndroidGradleProjectWithCompleteWrapper() {
        val files = listOf(
            ImportedProjectFile("settings.gradle.kts", """rootProject.name = "Demo"
include(":app")"""),
            ImportedProjectFile("build.gradle.kts", "plugins {}"),
            ImportedProjectFile("app/build.gradle.kts", """plugins { id("com.android.application") }"""),
            ImportedProjectFile("app/src/main/AndroidManifest.xml", """<manifest xmlns:android="http://schemas.android.com/apk/res/android"><application /></manifest>"""),
            ImportedProjectFile("app/src/main/java/com/example/MainActivity.kt", "class MainActivity"),
            ImportedProjectFile("gradlew", "#!/bin/sh"),
            ImportedProjectFile("gradle/wrapper/gradle-wrapper.properties", "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10-bin.zip"),
            ImportedProjectFile("gradle/wrapper/gradle-wrapper.jar", "binary-placeholder")
        )
        val report = ProjectBuildValidator.validate(files)
        assertTrue(report.readyForBuildAttempt)
        assertTrue(report.summary.contains("listos para intentar"))
        assertTrue(report.buildCommand.contains("assembleDebug"))
    }

    @Test
    fun rejectsProjectWithoutAndroidPluginOrWrapperJar() {
        val files = listOf(
            ImportedProjectFile("settings.gradle", """include(":app")"""),
            ImportedProjectFile("build.gradle", "allprojects {}"),
            ImportedProjectFile("app/build.gradle", "plugins { id(" + "\"java" + "\") }"),
            ImportedProjectFile("app/src/main/AndroidManifest.xml", "<manifest />"),
            ImportedProjectFile("app/src/main/java/Main.java", "class Main {}"),
            ImportedProjectFile("gradlew", "#!/bin/sh"),
            ImportedProjectFile("gradle/wrapper/gradle-wrapper.properties", "distributionUrl=...")
        )
        val report = ProjectBuildValidator.validate(files)
        assertFalse(report.readyForBuildAttempt)
        assertTrue(report.checks.any { it.title == "Gradle Wrapper" && !it.passed })
    }

    @Test
    fun detectsDuplicatePathsIgnoringCase() {
        val files = listOf(
            ImportedProjectFile("settings.gradle", """include(":app")"""),
            ImportedProjectFile("SETTINGS.GRADLE", """include(":app")""")
        )
        val report = ProjectBuildValidator.validate(files)
        assertTrue(report.checks.any { it.title == "Rutas de archivos únicas" && !it.passed })
    }
}
