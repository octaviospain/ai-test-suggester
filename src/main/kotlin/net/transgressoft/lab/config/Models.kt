package net.transgressoft.lab.config

import ai.koog.prompt.executor.ollama.client.OllamaModels
import ai.koog.prompt.llm.LLMCapability.Completion
import ai.koog.prompt.llm.LLMCapability.Temperature
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel

/**
 * LLModel definitions for the single-model test suggestion pipeline.
 *
 * Holds the model configuration for Qwen 2.5 Coder, which handles both code analysis
 * and structured output generation. No tool-calling capabilities are used -- all file I/O
 * is handled programmatically in application code.
 *
 * Usage:
 * ```
 * Models.registerAll()
 * val model = Models.QWEN_CODER_14B
 * ```
 */
object Models {

    const val CONTEXT_WINDOW = 16_384L

    val QWEN_CODER_14B = LLModel(
        provider = LLMProvider.Ollama,
        id = "qwen2.5-coder:14b",
        capabilities = listOf(Temperature, Completion),
        contextLength = CONTEXT_WINDOW
    )

    val REQUIRED_MODELS = listOf(QWEN_CODER_14B)

    /**
     * Registers all required models with Koog's [OllamaModels] registry so they can
     * be resolved by the framework internals.
     */
    fun registerAll() {
        REQUIRED_MODELS.forEach { OllamaModels.addCustomModel(it) }
    }
}
