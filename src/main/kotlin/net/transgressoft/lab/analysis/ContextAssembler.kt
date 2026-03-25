package net.transgressoft.lab.analysis

import net.transgressoft.lab.config.Models
import java.io.File

/**
 * Builds markdown-structured prompt context from a target Kotlin source file and its
 * resolved dependencies, respecting a token budget.
 *
 * The target file is always included in full (never truncated). Dependencies are added
 * with full source code until the token budget is exhausted, at which point remaining
 * dependencies receive a one-line summary containing the class name and public method
 * signatures.
 *
 * Token estimation uses a character-based heuristic of ~4 characters per token.
 *
 * Usage:
 * ```
 * val context = ContextAssembler.assemble(targetFile, resolvedDeps)
 * // context is ready to be used as the user message in the thinker prompt
 * ```
 */
object ContextAssembler {

    private const val CHARS_PER_TOKEN = 4
    private const val RESERVED_TOKENS = 3000

    private fun estimateTokens(text: String): Int = text.length / CHARS_PER_TOKEN

    /**
     * Assembles a markdown-structured prompt context from the target file and its dependencies.
     *
     * @param targetFile the Kotlin source file to analyze (always included in full)
     * @param dependencyFiles resolved dependency files to include as context
     * @param contextWindowTokens total available token budget (defaults to [Models.CONTEXT_WINDOW])
     * @return markdown-formatted string with target and dependency sections
     */
    fun assemble(
        targetFile: File,
        dependencyFiles: List<File>,
        contextWindowTokens: Long = Models.CONTEXT_WINDOW
    ): String {
        val targetSource = targetFile.readText()
        val targetPackage = extractPackage(targetSource)

        val targetSection = buildString {
            appendLine("## Target File: ${targetFile.name}")
            appendLine("Package: $targetPackage")
            appendLine("Path: ${targetFile.path}")
            appendLine("```kotlin")
            appendLine(targetSource)
            appendLine("```")
        }

        var remainingTokens = contextWindowTokens - RESERVED_TOKENS - estimateTokens(targetSection)

        val result = StringBuilder(targetSection)

        for (depFile in dependencyFiles) {
            val depSource = depFile.readText()
            val depPackage = extractPackage(depSource)

            val fullSection = buildString {
                appendLine("## Dependency: ${depFile.name}")
                appendLine("Package: $depPackage")
                appendLine("Path: ${depFile.path}")
                appendLine("```kotlin")
                appendLine(depSource)
                appendLine("```")
            }

            val sectionTokens = estimateTokens(fullSection)

            if (sectionTokens <= remainingTokens) {
                result.append(fullSection)
                remainingTokens -= sectionTokens
            } else {
                result.appendLine("## Dependency (summary): ${summarizeDependency(depFile)}")
            }
        }

        return result.toString()
    }

    private fun extractPackage(source: String): String {
        return source.lineSequence()
            .firstOrNull { it.trimStart().startsWith("package ") }
            ?.trim()?.removePrefix("package ")?.trim()
            ?: "(default)"
    }

    /**
     * Produces a one-line summary of a dependency file: class name, path, and public method signatures.
     */
    private fun summarizeDependency(file: File): String {
        val source = file.readText()
        val className = file.nameWithoutExtension
        val methodRegex = Regex("""fun\s+(\w+)\s*\(""")
        val methods = methodRegex.findAll(source)
            .map { "${it.groupValues[1]}()" }
            .toList()

        val methodList = if (methods.isNotEmpty()) methods.joinToString(", ") else "no public methods"
        return "$className(${file.path}): $methodList"
    }
}
