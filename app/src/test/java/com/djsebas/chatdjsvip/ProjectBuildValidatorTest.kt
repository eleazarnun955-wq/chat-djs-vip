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
        assertTrue(report.checks.any { it.title == "Módulo Android" && !it.passed })
    }

    @Test
    fun recognizesBasicAndroidGradleProjectWithWrapper() {
        val files = listOf(
            ImportedProjectFile("settings.gradle.kts", "rootProject.name = \"Demo\""),
            ImportedProjectFile("build.gradle.kts", "plugins {}"),
            ImportedProjectFile("app/build.gradle.kts", "plugins {}"),
            ImportedProjectFile("app/src/main/AndroidManifest.xml", "<manifest />"),
            ImportedProjectFile("app/src/main/java/com/example/MainActivity.kt", "class MainActivity"),
            ImportedProjectFile("gradlew", "#!/bin/sh"),
            ImportedProjectFile("gradle/wrapper/gradle-wrapper.properties", "distributionUrl=...")
        )
        val report = ProjectBuildValidator.validate(files)
        assertTrue(report.readyForBuildAttempt)
        assertTrue(report.summary.contains("lista para intentar"))
        assertTrue(report.buildCommand.contains("assembleDebug"))
    }
}
