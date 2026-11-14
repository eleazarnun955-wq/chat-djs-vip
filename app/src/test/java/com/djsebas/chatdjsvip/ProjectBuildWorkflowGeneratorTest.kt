package com.djsebas.chatdjsvip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectBuildWorkflowGeneratorTest {
    private fun androidFiles() = listOf(
        ImportedProjectFile("settings.gradle.kts", """rootProject.name = "Demo"
include(":app")"""),
        ImportedProjectFile("app/build.gradle.kts", """plugins { id("com.android.application") }"""),
        ImportedProjectFile("app/src/main/AndroidManifest.xml", "<manifest />")
    )

    @Test fun createsBuildWorkflowForAndroidGradleProject() {
        val workflow = ProjectBuildWorkflowGenerator.generate(androidFiles())
        assertEquals(".github/workflows/android-build.yml", workflow?.path)
        assertTrue(workflow?.content.orEmpty().contains("assembleDebug"))
        assertTrue(workflow?.content.orEmpty().contains("actions/upload-artifact@v4"))
        assertTrue(workflow?.content.orEmpty().contains("workflow_dispatch"))
    }

    @Test fun doesNotCreateWorkflowForNonAndroidProject() {
        assertNull(ProjectBuildWorkflowGenerator.generate(listOf(
            ImportedProjectFile("package.json", """{"scripts":{"build":"vite build"}}""")
        )))
    }

    @Test fun doesNotOverwriteExistingWorkflow() {
        val files = androidFiles() + ImportedProjectFile(
            ".github/workflows/android-build.yml", "name: Custom"
        )
        assertNull(ProjectBuildWorkflowGenerator.generate(files))
    }
}
