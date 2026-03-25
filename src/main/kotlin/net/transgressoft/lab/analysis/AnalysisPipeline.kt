package net.transgressoft.lab.analysis

import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import net.transgressoft.lab.config.Models
import net.transgressoft.lab.output.OutputParser
import net.transgressoft.lab.output.TestSuggestion
import net.transgressoft.lab.tools.DirectoryLister
import net.transgressoft.lab.tools.FileReader
import java.io.File

/**
 * Orchestrates the full analysis pipeline: reading a target Kotlin file,
 * resolving its imports, assembling prompt context, executing the LLM prompt via
 * qwen2.5-coder, and parsing the response into structured test suggestions.
 *
 * The pipeline supports two modes:
 * - **Single file mode** (`directoryContext = null`): Only the target file is analyzed,
 *   with no import resolution.
 * - **Directory mode** (`directoryContext` provided): Imports from the target file are
 *   resolved against `.kt` files within the specified directory tree and included as
 *   dependency context.
 *
 * Usage:
 * ```
 * val executor = ExecutorFactory.createExecutor(ExecutorFactory.createClient())
 * val pipeline = AnalysisPipeline(executor)
 * val result = pipeline.analyze("/path/to/MyClass.kt", directoryContext = "/path/to/src")
 * result.onSuccess { suggestions -> suggestions.forEach { println(it.name) } }
 * result.onFailure { println("Analysis failed: ${it.message}") }
 * ```
 *
 * @param executor the Koog prompt executor for sending prompts to the LLM
 */
open class AnalysisPipeline(private val executor: SingleLLMPromptExecutor) {

    /**
     * Analyzes a Kotlin source file by sending it through the LLM and parsing the
     * response into a list of structured test suggestions.
     *
     * @param targetPath path to the Kotlin source file to analyze
     * @param directoryContext optional directory path for import resolution; when null,
     *        the file is analyzed in isolation without dependency context
     * @return [Result.success] with a list of parsed test suggestions, or [Result.failure] if
     *         file reading, LLM execution, or suggestion parsing fails
     */
    open suspend fun analyze(targetPath: String, directoryContext: String? = null): Result<List<TestSuggestion>> {
        val sourceContent = FileReader.read(targetPath).getOrElse { return Result.failure(it) }

        val targetFile = File(targetPath)

        val dependencyFiles = if (directoryContext != null) {
            resolveDependencies(sourceContent, directoryContext)
        } else {
            emptyList()
        }

        val assembledContext = ContextAssembler.assemble(targetFile, dependencyFiles)
        val prompt = AnalysisPromptBuilder.build(assembledContext)

        val rawOutput = runCatching {
            val responses = executor.execute(prompt, Models.QWEN_CODER_14B, emptyList())
            responses.joinToString("") { it.content }
        }.getOrElse { return Result.failure(it) }

        return OutputParser.parse(rawOutput.trim())
    }

    private fun resolveDependencies(sourceContent: String, directoryContext: String): List<File> {
        val directoryFiles = DirectoryLister.listKotlinFiles(directoryContext).getOrElse { return emptyList() }
        val imports = ImportResolver.extractImports(sourceContent)
        return imports.mapNotNull { ImportResolver.resolveImport(it, directoryFiles) }
    }
}
