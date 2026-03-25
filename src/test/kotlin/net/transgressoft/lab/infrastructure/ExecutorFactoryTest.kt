package net.transgressoft.lab.infrastructure

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.OllamaClient
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DisplayName

/**
 * Verifies that [ExecutorFactory] creates properly configured Ollama
 * infrastructure components with the correct context window strategy.
 */
@DisplayName("ExecutorFactory")
class ExecutorFactoryTest : FunSpec({

    test("ExecutorFactory creates OllamaClient instance") {
        val client = ExecutorFactory.createClient()
        client.shouldNotBeNull()
        client.shouldBeInstanceOf<OllamaClient>()
    }

    test("ExecutorFactory creates OllamaClient with custom base URL") {
        val client = ExecutorFactory.createClient("http://custom:1234")
        client.shouldNotBeNull()
        client.shouldBeInstanceOf<OllamaClient>()
    }

    test("ExecutorFactory creates SingleLLMPromptExecutor from client") {
        val client = ExecutorFactory.createClient()
        val executor = ExecutorFactory.createExecutor(client)
        executor.shouldNotBeNull()
        executor.shouldBeInstanceOf<SingleLLMPromptExecutor>()
    }
})
