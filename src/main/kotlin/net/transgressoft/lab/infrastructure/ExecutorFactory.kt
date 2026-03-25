package net.transgressoft.lab.infrastructure

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.ollama.client.ContextWindowStrategy
import ai.koog.prompt.executor.ollama.client.OllamaClient

/**
 * Creates properly configured Ollama infrastructure components.
 *
 * Constructs [OllamaClient] with a fixed 16K context window strategy (sending `num_ctx`
 * on every request) and wraps it in a [SingleLLMPromptExecutor]. This avoids the
 * `simpleOllamaAIExecutor()` convenience function which defaults to
 * [ContextWindowStrategy.None] and would leave Ollama at its 2048-token default.
 */
object ExecutorFactory {

    private const val DEFAULT_BASE_URL = "http://localhost:11434"
    private const val CONTEXT_WINDOW_SIZE = 16_384L

    fun createClient(baseUrl: String = DEFAULT_BASE_URL): OllamaClient {
        return OllamaClient(
            baseUrl = baseUrl,
            contextWindowStrategy = ContextWindowStrategy.Companion.Fixed(CONTEXT_WINDOW_SIZE)
        )
    }

    fun createExecutor(client: OllamaClient): SingleLLMPromptExecutor {
        return SingleLLMPromptExecutor(client)
    }
}
