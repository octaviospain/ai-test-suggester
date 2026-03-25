package net.transgressoft.lab

import com.github.ajalt.clikt.command.main
import net.transgressoft.lab.cli.AnalyzeCommand
import net.transgressoft.lab.config.Models
import net.transgressoft.lab.infrastructure.ExecutorFactory
import net.transgressoft.lab.infrastructure.OllamaHealthCheck

/**
 * Entry point for the test suggester CLI.
 *
 * Initializes the Ollama infrastructure (model registration, client, executor,
 * health check) and delegates all CLI argument parsing and analysis orchestration
 * to [AnalyzeCommand].
 */
suspend fun main(args: Array<String>) {
    Models.registerAll()

    val client = ExecutorFactory.createClient()
    val executor = ExecutorFactory.createExecutor(client)
    val healthCheck = OllamaHealthCheck(client)
    val pipeline = net.transgressoft.lab.analysis.AnalysisPipeline(executor)

    AnalyzeCommand(pipeline, healthCheck).main(args)
}
