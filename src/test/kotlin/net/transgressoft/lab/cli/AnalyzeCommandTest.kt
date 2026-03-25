package net.transgressoft.lab.cli

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import com.github.ajalt.clikt.command.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import net.transgressoft.lab.analysis.AnalysisPipeline
import net.transgressoft.lab.output.Priority
import net.transgressoft.lab.output.SuggestionCategory
import net.transgressoft.lab.output.TestSuggestion
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * A test double for [AnalysisPipeline] that returns configurable results per file path.
 * Avoids MockK issues with name-mangled suspend functions returning [Result] inline class.
 */
class FakePipeline(
    private val resultsByPath: Map<String, Result<List<TestSuggestion>>> = emptyMap(),
    private val defaultResult: Result<List<TestSuggestion>> = Result.success(emptyList()),
) : AnalysisPipeline(mockk<SingleLLMPromptExecutor>()) {

    var analyzeCalls = mutableListOf<Pair<String, String?>>()

    override suspend fun analyze(targetPath: String, directoryContext: String?): Result<List<TestSuggestion>> {
        analyzeCalls.add(targetPath to directoryContext)
        return resultsByPath.entries
            .firstOrNull { targetPath.contains(it.key) }
            ?.value
            ?: defaultResult
    }
}

/**
 * Tests for [AnalyzeCommand] verifying CLI argument parsing, error handling,
 * single file analysis, directory analysis, verbose mode, and partial failure behavior.
 */
@DisplayName("AnalyzeCommand")
class AnalyzeCommandTest : FunSpec({

    fun createTempDir(name: String): File =
        createTempDirectory(name).toFile().also { it.deleteOnExit() }

    fun createFile(dir: File, name: String, content: String = "class Stub"): File =
        File(dir, name).also { it.writeText(content) }

    val cannedSuggestions = listOf(
        TestSuggestion(
            name = "validates null return handling",
            description = "Nullable return needs verification. Test that null is handled properly",
            category = SuggestionCategory.EDGE_CASE,
            priority = Priority.HIGH,
        ),
        TestSuggestion(
            name = "verifies default behavior",
            description = "Core functionality must work. Test the happy path scenario",
            category = SuggestionCategory.HAPPY_PATH,
            priority = Priority.MEDIUM,
        ),
    )

    test("exits with error for non-existent path") {
        val pipeline = FakePipeline()
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test("/nonexistent/path/to/file.kt")

        result.statusCode shouldBe 1
    }

    test("exits with error for non-kt file") {
        val dir = createTempDir("cli-test-nonkt")
        val mdFile = createFile(dir, "README.md", "# Readme")
        val pipeline = FakePipeline()
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(mdFile.absolutePath)

        result.statusCode shouldBe 1
        result.stderr shouldContain "Not a Kotlin file"
    }

    test("exits with error for empty directory") {
        val dir = createTempDir("cli-test-empty")
        val pipeline = FakePipeline()
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(dir.absolutePath)

        result.statusCode shouldBe 1
        result.stderr shouldContain "No Kotlin files found"
    }

    test("analyzes single kt file and prints suggestions") {
        val dir = createTempDir("cli-test-single")
        val ktFile = createFile(dir, "MyClass.kt", "class MyClass { fun foo() = 42 }")
        val pipeline = FakePipeline(defaultResult = Result.success(cannedSuggestions))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(ktFile.absolutePath)

        result.statusCode shouldBe 0
        result.stdout shouldContain "validates null return handling"
        result.stdout shouldContain "2 suggestions"
    }

    test("analyzes directory and prints suggestions for each file") {
        val dir = createTempDir("cli-test-dir")
        createFile(dir, "Foo.kt", "class Foo")
        createFile(dir, "Bar.kt", "class Bar")
        val pipeline = FakePipeline(defaultResult = Result.success(cannedSuggestions))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(dir.absolutePath)

        result.statusCode shouldBe 0
        result.stdout shouldContain "suggestions"
    }

    test("excludes test files from directory discovery") {
        val dir = createTempDir("cli-test-exclude")
        createFile(dir, "Main.kt", "class Main")
        createFile(dir, "MainTest.kt", "class MainTest")
        val pipeline = FakePipeline(defaultResult = Result.success(cannedSuggestions))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(dir.absolutePath)

        result.statusCode shouldBe 0
        pipeline.analyzeCalls.size shouldBe 1
        pipeline.analyzeCalls[0].first shouldContain "Main.kt"
    }

    test("shows progress with verbose flag") {
        val dir = createTempDir("cli-test-verbose")
        val ktFile = createFile(dir, "FileName.kt", "class FileName")
        val pipeline = FakePipeline(defaultResult = Result.success(cannedSuggestions))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test("--verbose ${ktFile.absolutePath}")

        result.statusCode shouldBe 0
        result.stdout shouldContain "[FileName.kt] Analyzing..."
    }

    test("silent without verbose flag") {
        val dir = createTempDir("cli-test-silent")
        val ktFile = createFile(dir, "FileName.kt", "class FileName")
        val pipeline = FakePipeline(defaultResult = Result.success(cannedSuggestions))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(ktFile.absolutePath)

        result.statusCode shouldBe 0
        result.stdout shouldNotContain "Analyzing..."
    }

    test("skips failed files in directory mode and reports warnings") {
        val dir = createTempDir("cli-test-partial")
        createFile(dir, "Good.kt", "class Good")
        createFile(dir, "Bad.kt", "class Bad")
        val pipeline = FakePipeline(resultsByPath = mapOf(
            "Good.kt" to Result.success(cannedSuggestions),
            "Bad.kt" to Result.failure(RuntimeException("LLM error")),
        ))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(dir.absolutePath)

        result.statusCode shouldBe 0
        result.stdout shouldContain "suggestions"
        result.stdout shouldContain "Warnings:"
    }

    test("exits 1 when all files fail in directory mode") {
        val dir = createTempDir("cli-test-allfail")
        createFile(dir, "A.kt", "class A")
        createFile(dir, "B.kt", "class B")
        val pipeline = FakePipeline(defaultResult = Result.failure(RuntimeException("LLM error")))
        val command = AnalyzeCommand(pipeline, healthCheck = null)

        val result = command.test(dir.absolutePath)

        result.statusCode shouldBe 1
        result.stderr shouldContain "All files failed"
    }
})
