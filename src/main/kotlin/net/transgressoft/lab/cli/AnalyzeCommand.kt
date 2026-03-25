package net.transgressoft.lab.cli

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import net.transgressoft.lab.analysis.AnalysisPipeline
import net.transgressoft.lab.infrastructure.OllamaHealthCheck
import net.transgressoft.lab.infrastructure.OllamaValidationException
import net.transgressoft.lab.output.OutputFormatter
import net.transgressoft.lab.output.TestSuggestion
import java.io.File
import java.nio.file.Path

/**
 * Clikt CLI command that analyzes Kotlin source files and suggests meaningful test cases
 * using local LLM reasoning via the [AnalysisPipeline].
 *
 * Supports two modes:
 * - **Single file mode**: Pass a `.kt` file path to analyze one file.
 * - **Directory mode**: Pass a directory path to recursively discover and analyze all
 *   non-test `.kt` files, with partial failure tolerance.
 *
 * @param pipeline the analysis pipeline used for LLM-based analysis
 * @param healthCheck optional Ollama health check; null in tests, real in production
 */
class AnalyzeCommand(
    private val pipeline: AnalysisPipeline,
    private val healthCheck: OllamaHealthCheck? = null,
) : SuspendingCliktCommand(name = "test-suggester") {

    override fun help(context: Context): String =
        "Analyzes Kotlin source code and suggests meaningful test cases using local LLM reasoning."

    val path: Path by argument("<path>", help = "Kotlin file or directory to analyze")
        .path(mustExist = true, mustBeReadable = true)

    val verbose: Boolean by option("--verbose", "-v", help = "Show progress during analysis")
        .flag()

    override suspend fun run() {
        if (healthCheck != null) {
            try {
                healthCheck.validate()
            } catch (e: OllamaValidationException) {
                throw PrintMessage("Error: ${e.message}", statusCode = 1, printError = true)
            }
        }

        val file = path.toFile()
        if (file.isFile) {
            if (file.extension != "kt") {
                throw PrintMessage("Error: Not a Kotlin file: ${file.name} (expected .kt extension)", statusCode = 1, printError = true)
            }
            analyzeSingleFile(file)
        } else if (file.isDirectory) {
            val ktFiles = discoverKotlinFiles(file)
            if (ktFiles.isEmpty()) {
                throw PrintMessage("Error: No Kotlin files found in: ${file.absolutePath}", statusCode = 1, printError = true)
            }
            analyzeDirectory(ktFiles, file.absolutePath)
        }
    }

    private fun discoverKotlinFiles(dir: File): List<File> =
        dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filterNot { shouldExclude(it) }
            .toList()

    private fun shouldExclude(file: File): Boolean {
        val name = file.name
        val absolutePath = file.absolutePath
        return name.endsWith("Test.kt") ||
            name.endsWith("Spec.kt") ||
            absolutePath.contains("/test/") ||
            absolutePath.contains("/tests/")
    }

    private suspend fun analyzeSingleFile(file: File) {
        if (verbose) {
            echo("[${file.name}] Analyzing...")
        }
        val result = pipeline.analyze(file.absolutePath)
        result.onSuccess { suggestions ->
            if (verbose) {
                echo("[${file.name}] Done (${suggestions.size} suggestions)")
            }
            echo(OutputFormatter.formatSingle(file.name, suggestions))
        }.onFailure {
            throw PrintMessage("Error: Analysis failed for ${file.name}: ${it.message}", statusCode = 1, printError = true)
        }
    }

    private suspend fun analyzeDirectory(files: List<File>, dirPath: String) {
        val results = mutableMapOf<String, List<TestSuggestion>>()
        val warnings = mutableListOf<String>()

        for (file in files) {
            if (verbose) {
                echo("[${file.name}] Analyzing...")
            }
            val result = pipeline.analyze(file.absolutePath, directoryContext = dirPath)
            result.onSuccess { suggestions ->
                if (verbose) {
                    echo("[${file.name}] Done (${suggestions.size} suggestions)")
                }
                results[file.name] = suggestions
            }.onFailure { e ->
                warnings.add("${file.name}: ${e.message}")
            }
        }

        if (results.isNotEmpty()) {
            echo(OutputFormatter.formatDirectory(results))
            if (warnings.isNotEmpty()) {
                echo("Warnings:")
                warnings.forEach { echo("  - $it") }
            }
        } else {
            throw PrintMessage("Error: All files failed analysis", statusCode = 1, printError = true)
        }
    }
}
