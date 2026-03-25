package net.transgressoft.lab.config

import ai.koog.prompt.llm.LLMCapability
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName

/**
 * Verifies model definitions are correctly configured: IDs, context window,
 * and capability exclusions (no Tools/ToolChoice).
 */
@DisplayName("Models")
class ModelsTest : FunSpec({

    test("Models has correct id for qwen2.5-coder model") {
        Models.QWEN_CODER_14B.id shouldBe "qwen2.5-coder:14b"
    }

    test("Models configures 16K context window for qwen model") {
        Models.QWEN_CODER_14B.contextLength shouldBe 16_384L
    }

    test("Models excludes Tools capability from qwen model") {
        val caps = Models.QWEN_CODER_14B.capabilities.shouldNotBeNull()
        caps shouldNotContain LLMCapability.Tools
    }

    test("Models excludes ToolChoice capability from qwen model") {
        val caps = Models.QWEN_CODER_14B.capabilities.shouldNotBeNull()
        caps shouldNotContain LLMCapability.ToolChoice
    }

    test("Models includes only Temperature and Completion capabilities") {
        val expected = listOf(LLMCapability.Temperature, LLMCapability.Completion)
        Models.QWEN_CODER_14B.capabilities.shouldNotBeNull() shouldContainExactly expected
    }

    test("Models lists qwen in REQUIRED_MODELS") {
        Models.REQUIRED_MODELS shouldHaveSize 1
        Models.REQUIRED_MODELS shouldContainExactly listOf(
            Models.QWEN_CODER_14B
        )
    }
})
