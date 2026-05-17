package com.azuratech.azuratime.arch

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MviArchitectureTest {
    @Test
    fun `all ViewModels in core and features must have onEvent function`() {
        val viewModelFiles = File("app/src/main/java").walkTopDown()
            .filter { it.name.endsWith("ViewModel.kt") && it.name != "AzuraViewModelFactory.kt" }
            .toList()

        viewModelFiles.forEach { file ->
            val content = file.readText()
            assertTrue(
                "${file.name} violates MVI standard: missing 'fun onEvent'",
                content.contains("fun onEvent(") || content.contains("// @Deprecated"),
            )
        }
    }

    @Test
    fun `no wildcard imports in Kotlin files`() {
        val kotlinFiles = File("app/src/main/java").walkTopDown()
            .filter { it.name.endsWith(".kt") }
            .toList()

        kotlinFiles.forEach { file ->
            val content = file.readText()
            assertTrue(
                "${file.name} contains wildcard imports",
                !content.lines().any { it.trim().matches(Regex("import .*\\.\\*")) },
            )
        }
    }
}
