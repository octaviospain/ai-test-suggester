package net.transgressoft.lab.infrastructure

import ai.koog.prompt.executor.clients.LLMClientException
import ai.koog.prompt.executor.ollama.client.OllamaClient
import ai.koog.prompt.executor.ollama.client.OllamaModelCard
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName

/**
 * Verifies [OllamaHealthCheck] error paths and lazy validation behavior using
 * mocked [OllamaClient] -- no running Ollama instance required.
 */
@DisplayName("OllamaHealthCheck")
class OllamaHealthCheckTest : FunSpec({

    val client = mockk<OllamaClient>()

    val modelCard = mockk<OllamaModelCard>()

    beforeEach {
        clearMocks(client)
    }

    test("OllamaHealthCheck throws OllamaValidationException with model name when model is missing") {
        runTest {
            coEvery { client.getModelOrNull(any(), any()) } returns null
            val healthCheck = OllamaHealthCheck(client)

            val exception = shouldThrow<OllamaValidationException> {
                healthCheck.validate()
            }
            exception.message shouldContain "qwen2.5-coder:14b"
        }
    }

    test("OllamaHealthCheck includes 'ollama pull' fix command in missing model error") {
        runTest {
            coEvery { client.getModelOrNull(any(), any()) } returns null
            val healthCheck = OllamaHealthCheck(client)

            val exception = shouldThrow<OllamaValidationException> {
                healthCheck.validate()
            }
            exception.message shouldContain "ollama pull qwen2.5-coder:14b"
        }
    }

    test("OllamaHealthCheck throws OllamaValidationException when Ollama is unreachable") {
        runTest {
            coEvery { client.getModelOrNull(any(), any()) } throws LLMClientException("test", "Connection refused")
            every { client.baseUrl } returns "http://localhost:11434"
            val healthCheck = OllamaHealthCheck(client)

            shouldThrow<OllamaValidationException> {
                healthCheck.validate()
            }
        }
    }

    test("OllamaHealthCheck includes 'ollama serve' fix command in connection error") {
        runTest {
            coEvery { client.getModelOrNull(any(), any()) } throws LLMClientException("test", "Connection refused")
            every { client.baseUrl } returns "http://localhost:11434"
            val healthCheck = OllamaHealthCheck(client)

            val exception = shouldThrow<OllamaValidationException> {
                healthCheck.validate()
            }
            exception.message shouldContain "ollama serve"
        }
    }

    test("OllamaHealthCheck succeeds when all models are available") {
        runTest {
            coEvery { client.getModelOrNull(any(), any()) } returns modelCard
            val healthCheck = OllamaHealthCheck(client)

            shouldNotThrowAny {
                healthCheck.validate()
            }
        }
    }

    test("OllamaHealthCheck skips re-validation after successful first call") {
        runTest {
            coEvery { client.getModelOrNull(any(), any()) } returns modelCard
            val healthCheck = OllamaHealthCheck(client)

            healthCheck.validate()

            // Change mock to throw -- second call should still succeed (skipped)
            coEvery { client.getModelOrNull(any(), any()) } throws LLMClientException("test", "Connection refused")

            shouldNotThrowAny {
                healthCheck.validate()
            }

            // getModelOrNull should have been called only during the first validate()
            coVerify(exactly = 1) { client.getModelOrNull(any(), any()) }
        }
    }
})
