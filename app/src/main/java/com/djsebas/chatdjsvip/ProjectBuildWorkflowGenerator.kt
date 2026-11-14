package com.djsebas.chatdjsvip

/**
 * Adds a reproducible GitHub Actions build workflow to exported Android Gradle projects.
 * It only prepares the workflow; compilation happens after the user uploads the ZIP to GitHub.
 */
object ProjectBuildWorkflowGenerator {
    private const val WORKFLOW_PATH = ".github/workflows/android-build.yml"

    fun workflowPath(): String = WORKFLOW_PATH

    fun generate(files: List<ImportedProjectFile>): ImportedProjectFile? {
        val paths = files.map { it.path.replace('\\', '/').lowercase() }.toSet()
        val appBuild = files.firstOrNull {
            it.path.replace('\\', '/').equals("app/build.gradle.kts", ignoreCase = true) ||
                it.path.replace('\\', '/').equals("app/build.gradle", ignoreCase = true)
        }?.content.orEmpty()
        val rootBuild = paths.contains("settings.gradle.kts") || paths.contains("settings.gradle")
        val androidPlugin = appBuild.contains("com.android.application") ||
            appBuild.contains("com.android.tools.build:gradle")
        val manifest = paths.contains("app/src/main/androidmanifest.xml") ||
            paths.contains("src/main/androidmanifest.xml")
        if (!rootBuild || !androidPlugin || !manifest) return null
        if (paths.contains(WORKFLOW_PATH.lowercase())) return null

        val workflow = """name: Android APK
on:
  push:
    branches: [ main, master ]
  pull_request:
    branches: [ main, master ]
  workflow_dispatch:

permissions:
  contents: read

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Descargar proyecto
        uses: actions/checkout@v4
      - name: Configurar Java 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
      - name: Configurar Gradle
        uses: gradle/actions/setup-gradle@v4
        with:
          gradle-version: '8.9'
      - name: Compilar y probar APK
        shell: bash
        run: |
          set -euo pipefail
          if [ -f ./gradlew ]; then
            chmod +x ./gradlew
            ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
          else
            gradle --no-daemon testDebugUnitTest lintDebug assembleDebug
          fi
      - name: Publicar APK como artefacto descargable
        uses: actions/upload-artifact@v4
        with:
          name: android-apk
          path: app/build/outputs/apk/debug/*.apk
          if-no-files-found: error
          retention-days: 14
"""
        return ImportedProjectFile(WORKFLOW_PATH, workflow)
    }
}
