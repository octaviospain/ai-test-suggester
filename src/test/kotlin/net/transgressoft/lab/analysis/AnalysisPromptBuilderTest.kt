package net.transgressoft.lab.analysis

import ai.koog.prompt.message.Message
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldContainIgnoringCase
import org.junit.jupiter.api.DisplayName

/**
 * Tests for [AnalysisPromptBuilder] verifying prompt structure, Kotlin-specific
 * analysis guidance, and assembled context inclusion.
 */
@DisplayName("AnalysisPromptBuilder")
class AnalysisPromptBuilderTest : FunSpec({

    val sampleContext = "## Target File: Foo.kt\nPackage: com.example\n```kotlin\nclass Foo\n```"

    test("AnalysisPromptBuilder build creates a prompt with id 'code-analysis'") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        prompt.id shouldBe "code-analysis"
    }

    test("AnalysisPromptBuilder build includes system message with Kotlin-specific suggestion guidance") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContain "Kotlin"
        systemMessage.content shouldContain "test suggestion"
    }

    test("AnalysisPromptBuilder build system message mentions sealed class exhaustiveness") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContainIgnoringCase "sealed class"
        systemMessage.content shouldContainIgnoringCase "exhaustive"
    }

    test("AnalysisPromptBuilder build system message mentions nullable returns and null safety") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContain "nullable"
        systemMessage.content shouldContain "null"
    }

    test("AnalysisPromptBuilder build system message mentions coroutine cancellation") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContainIgnoringCase "coroutine"
        systemMessage.content shouldContainIgnoringCase "cancellation"
    }

    test("AnalysisPromptBuilder build system message mentions data class equality and copy") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContainIgnoringCase "data class"
        systemMessage.content shouldContain "equals"
        systemMessage.content shouldContain "copy"
    }

    test("AnalysisPromptBuilder build system message mentions extension function receiver scope") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContainIgnoringCase "extension function"
        systemMessage.content shouldContain "receiver"
    }

    test("AnalysisPromptBuilder build system message prioritizes edge cases and error conditions over happy paths") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContain "edge case"
        systemMessage.content shouldContain "error condition"
        systemMessage.content shouldContain "happy path"
    }

    test("AnalysisPromptBuilder build system message includes structured output format with ## Test headers") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContain "## Test:"
    }

    test("AnalysisPromptBuilder build system message forbids trivial suggestions") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContain "Do NOT suggest"
        systemMessage.content shouldContain "business logic"
    }

    test("AnalysisPromptBuilder build system message includes category and priority field instructions") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val systemMessage = prompt.messages.first { it.role == Message.Role.System }
        systemMessage.content shouldContain "**Category:**"
        systemMessage.content shouldContain "**Priority:**"
    }

    test("AnalysisPromptBuilder build includes assembled context as user message") {
        val prompt = AnalysisPromptBuilder.build(sampleContext)
        val userMessage = prompt.messages.first { it.role == Message.Role.User }
        userMessage.content shouldBe sampleContext
    }
})
