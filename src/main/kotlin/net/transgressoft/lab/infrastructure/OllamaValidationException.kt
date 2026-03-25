package net.transgressoft.lab.infrastructure

/**
 * Represents a failure during Ollama connectivity or model availability validation.
 *
 * Thrown when the application cannot connect to the Ollama server or when a required
 * model is not available. Messages include actionable fix commands for the user.
 */
class OllamaValidationException(message: String) : RuntimeException(message)
