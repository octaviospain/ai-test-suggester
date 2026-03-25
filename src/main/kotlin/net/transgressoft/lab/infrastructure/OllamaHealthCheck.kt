package net.transgressoft.lab.infrastructure

import ai.koog.prompt.executor.clients.LLMClientException
import ai.koog.prompt.executor.ollama.client.OllamaClient
import net.transgressoft.lab.config.Models

/**
 * Performs lazy validation of Ollama connectivity and model availability.
 *
 * On the first call to [validate], checks that the Ollama server is reachable and
 * that all required models are pulled. Subsequent calls return immediately. Error
 * messages include actionable fix commands (e.g., `ollama pull`, `ollama serve`).
 */
class OllamaHealthCheck(private val client: OllamaClient) {

    private var validated = false

    suspend fun validate() {
        if (validated) return

        try {
            for (model in Models.REQUIRED_MODELS) {
                val card = client.getModelOrNull(model.id)
                    ?: throw OllamaValidationException(
                        "Model ${model.id} not found. Run: ollama pull ${model.id}"
                    )
            }
        } catch (e: OllamaValidationException) {
            throw e
        } catch (e: LLMClientException) {
            throw OllamaValidationException(
                "Cannot connect to Ollama at ${client.baseUrl}. Ensure Ollama is running: ollama serve"
            )
        }

        validated = true
    }
}
