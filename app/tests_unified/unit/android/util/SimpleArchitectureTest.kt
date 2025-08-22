package com.multisensor.recording.util

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

class SimpleArchitectureTest {

    @Test
    fun `UI layer should not depend on database layer directly`() {
        val uiFiles = getKotlinFilesInPackage("ui")
        val forbiddenImports = listOf(
            "import androidx.room",
            "import com.multisensor.recording.persistence.dao",
            "import com.multisensor.recording.persistence.entity"
        )

        val violations = mutableListOf<String>()
        uiFiles.forEach { file ->
            val content = file.readText()
            forbiddenImports.forEach { forbiddenImport ->
                if (content.contains(forbiddenImport)) {
                    violations.add("${file.name} contains forbidden import: $forbiddenImport")
                }
            }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `Controllers should not access UI components directly`() {
        val controllerFiles = getKotlinFilesInPackage("controllers")
        val forbiddenImports = listOf(
            "import androidx.compose",
            "import com.multisensor.recording.ui"
        )

        val violations = mutableListOf<String>()
        controllerFiles.forEach { file ->
            val content = file.readText()
            forbiddenImports.forEach { forbiddenImport ->
                if (content.contains(forbiddenImport)) {
                    violations.add("${file.name} contains forbidden import: $forbiddenImport")
                }
            }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `Recording layer should not depend on UI layer`() {
        val recordingFiles = getKotlinFilesInPackage("recording")
        val forbiddenImports = listOf(
            "import androidx.compose",
            "import com.multisensor.recording.ui"
        )

        val violations = mutableListOf<String>()
        recordingFiles.forEach { file ->
            val content = file.readText()
            forbiddenImports.forEach { forbiddenImport ->
                if (content.contains(forbiddenImport)) {
                    violations.add("${file.name} contains forbidden import: $forbiddenImport")
                }
            }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `Network layer should not depend on UI layer`() {
        val networkFiles = getKotlinFilesInPackage("network")
        val forbiddenImports = listOf(
            "import androidx.compose",
            "import com.multisensor.recording.ui"
        )

        val violations = mutableListOf<String>()
        networkFiles.forEach { file ->
            val content = file.readText()
            forbiddenImports.forEach { forbiddenImport ->
                if (content.contains(forbiddenImport)) {
                    violations.add("${file.name} contains forbidden import: $forbiddenImport")
                }
            }
        }

        assertThat(violations).isEmpty()
    }

    @Test
    fun `Should not use Android Log directly`() {
        val allKotlinFiles = getAllKotlinFiles()

        val violations = mutableListOf<String>()
        allKotlinFiles.forEach { file ->
            val content = file.readText()
            if (content.contains("Log.d(") || content.contains("Log.e(") || content.contains("Log.w(")) {
                violations.add("${file.name} uses Android Log directly instead of Logger")
            }
        }

        assertThat(violations).isEmpty()
    }

    private fun getKotlinFilesInPackage(packageName: String): List<File> {
        val srcDir = File("src/main/java/com/multisensor/recording/$packageName")
        if (!srcDir.exists()) return emptyList()

        return srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }

    private fun getAllKotlinFiles(): List<File> {
        val srcDir = File("src/main/java/com/multisensor/recording")
        if (!srcDir.exists()) return emptyList()

        return srcDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }
}