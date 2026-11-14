package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeneratedProjectFileParserTest {
    @Test
    fun extractsMultipleFilesWithExactPaths() {
        val response = """
            Arquitectura breve.

            Archivo: app/src/main/MainActivity.kt
            ```kotlin
            package demo
            fun main() = Unit
            ```

            Archivo: app/build.gradle.kts
            ```kotlin
            plugins { id("com.android.application") }
            ```
        """.trimIndent()
        val files = GeneratedProjectFileParser.parse(response)
        assertEquals(2, files.size)
        assertEquals("app/src/main/MainActivity.kt", files[0].path)
        assertTrue(files[0].content.contains("fun main()"))
        assertEquals("app/build.gradle.kts", files[1].path)
    }

    @Test
    fun rejectsUnsafePathsAndDuplicatePaths() {
        val response = """
            Archivo: ../secret.kt
            ```kotlin
            secret
            ```

            Archivo: app/src/Main.kt
            ```kotlin
            first
            ```

            Archivo: app/src/Main.kt
            ```kotlin
            duplicate
            ```
        """.trimIndent()
        val files = GeneratedProjectFileParser.parse(response)
        assertEquals(1, files.size)
        assertEquals("first", files.single().content)
    }
}
