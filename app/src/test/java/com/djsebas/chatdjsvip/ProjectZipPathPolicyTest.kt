package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectZipPathPolicyTest {
    @Test
    fun acceptsAndNormalizesSafeRelativePaths() {
        assertEquals("app/src/MainActivity.kt", ProjectZipPathPolicy.safePath("app/src/MainActivity.kt"))
        assertEquals("app/src/MainActivity.kt", ProjectZipPathPolicy.safePath("app\\src\\MainActivity.kt"))
    }

    @Test
    fun rejectsTraversalAbsoluteAndDrivePaths() {
        assertNull(ProjectZipPathPolicy.safePath("../secret.txt"))
        assertNull(ProjectZipPathPolicy.safePath("app/../../secret.txt"))
        assertNull(ProjectZipPathPolicy.safePath("/etc/passwd"))
        assertNull(ProjectZipPathPolicy.safePath("C:/Windows/file.txt"))
        assertNull(ProjectZipPathPolicy.safePath("app//file.kt"))
        assertNull(ProjectZipPathPolicy.safePath("app/./file.kt"))
    }

    @Test
    fun sanitizesUnsafeCharactersAndRecognizesTextFiles() {
        assertEquals("app/my_file.kt", ProjectZipPathPolicy.safePath("app/my file.kt"))
        assertTrue(ProjectZipPathPolicy.isTextFile("app/Main.kt"))
        assertFalse(ProjectZipPathPolicy.isTextFile("app/image.png"))
    }
}
