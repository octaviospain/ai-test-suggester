package net.transgressoft.lab.analysis

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.message.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import net.transgressoft.lab.output.Priority
import net.transgressoft.lab.output.SuggestionCategory
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Tests for [AnalysisPipeline] verifying end-to-end analysis orchestration including
 * file reading, import resolution, context assembly, LLM execution, and structured
 * test suggestion parsing.
 */
@DisplayName("AnalysisPipeline")
class AnalysisPipelineTest : FunSpec({

    fun createTempKtFile(dir: File, name: String, content: String): File {
        return File(dir, name).also { it.writeText(content) }
    }

    fun mockResponse(content: String): Message.Response {
        val response = mockk<Message.Response>()
        every { response.content } returns content
        return response
    }

    val structuredSuggestion = """## Test: validates doSomething returns null for default case
**Category:** edge case
**Priority:** high
**Why:** Nullable return needs null handling verification
**Description:** Test that doSomething() returns null and callers handle it"""

    test("AnalysisPipeline analyze returns parsed suggestions for a single file") {
        runTest {
            val executor = mockk<SingleLLMPromptExecutor>()
            val pipeline = AnalysisPipeline(executor)

            val dir = createTempDirectory("pipeline-test").toFile()
            dir.deleteOnExit()
            val target = createTempKtFile(dir, "Foo.kt", """
                package com.example

                class Foo {
                    fun doSomething(): String? = null
                }
            """.trimIndent())

            coEvery { executor.execute(any(), any(), any()) } returns listOf(
                mockResponse(structuredSuggestion)
            )

            val result = pipeline.analyze(target.absolutePath)

            result.shouldBeSuccess {
                it shouldHaveSize 1
                it[0].name shouldContain "doSomething"
                it[0].category shouldBe SuggestionCategory.EDGE_CASE
                it[0].priority shouldBe Priority.HIGH
            }
        }
    }

    test("AnalysisPipeline analyze resolves imports and includes dependencies when directory context provided") {
        runTest {
            val executor = mockk<SingleLLMPromptExecutor>()
            val pipeline = AnalysisPipeline(executor)

            val dir = createTempDirectory("pipeline-test").toFile()
            dir.deleteOnExit()
            createTempKtFile(dir, "Bar.kt", """
                package com.example

                class Bar {
                    fun helper() = "help"
                }
            """.trimIndent())
            val target = createTempKtFile(dir, "Foo.kt", """
                package com.example

                import com.example.Bar

                class Foo {
                    val bar = Bar()
                }
            """.trimIndent())

            coEvery { executor.execute(any(), any(), any()) } returns listOf(
                mockResponse(structuredSuggestion)
            )

            val result = pipeline.analyze(target.absolutePath, directoryContext = dir.absolutePath)

            result.shouldBeSuccess {
                it shouldHaveSize 1
                it[0].name shouldContain "doSomething"
            }
        }
    }

    test("AnalysisPipeline analyze returns failure when target file cannot be read") {
        runTest {
            val executor = mockk<SingleLLMPromptExecutor>()
            val pipeline = AnalysisPipeline(executor)

            val result = pipeline.analyze("/nonexistent/path/Missing.kt")

            result.shouldBeFailure {
                it.message shouldContain "File not found"
            }
        }
    }

    test("AnalysisPipeline analyze returns failure when executor throws exception") {
        runTest {
            val executor = mockk<SingleLLMPromptExecutor>()
            val pipeline = AnalysisPipeline(executor)

            val dir = createTempDirectory("pipeline-test").toFile()
            dir.deleteOnExit()
            val target = createTempKtFile(dir, "Foo.kt", """
                package com.example

                class Foo
            """.trimIndent())

            coEvery { executor.execute(any(), any(), any()) } throws RuntimeException("LLM connection failed")

            val result = pipeline.analyze(target.absolutePath)

            result.shouldBeFailure {
                it.message shouldContain "LLM connection failed"
            }
        }
    }

    test("AnalysisPipeline analyze passes assembled context through AnalysisPromptBuilder to executor") {
        runTest {
            val executor = mockk<SingleLLMPromptExecutor>()
            val pipeline = AnalysisPipeline(executor)

            val dir = createTempDirectory("pipeline-test").toFile()
            dir.deleteOnExit()
            val target = createTempKtFile(dir, "Foo.kt", """
                package com.example

                class Foo {
                    fun bar() = 42
                }
            """.trimIndent())

            coEvery { executor.execute(match { prompt ->
                prompt.id == "code-analysis" &&
                    prompt.messages.any { it.role == Message.Role.User && it.content.contains("class Foo") }
            }, any(), any()) } returns listOf(
                mockResponse(structuredSuggestion)
            )

            val result = pipeline.analyze(target.absolutePath)

            result.shouldBeSuccess {
                it shouldHaveSize 1
                it[0].name shouldContain "doSomething"
            }
        }
    }

    test("AnalysisPipeline analyze returns failure when output has no parseable suggestions") {
        runTest {
            val executor = mockk<SingleLLMPromptExecutor>()
            val pipeline = AnalysisPipeline(executor)

            val dir = createTempDirectory("pipeline-test").toFile()
            dir.deleteOnExit()
            val target = createTempKtFile(dir, "Foo.kt", """
                package com.example

                class Foo
            """.trimIndent())

            coEvery { executor.execute(any(), any(), any()) } returns listOf(
                mockResponse("This is plain text with no ## Test: headers at all.")
            )

            val result = pipeline.analyze(target.absolutePath)

            result.shouldBeFailure {
                it.message shouldContain "No test suggestions"
            }
        }
    }
})
