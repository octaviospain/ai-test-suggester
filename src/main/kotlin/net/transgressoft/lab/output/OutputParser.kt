package net.transgressoft.lab.output

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Regex-based best-effort parser that extracts structured [TestSuggestion] instances
 * from markdown-formatted LLM output.
 *
 * Splits the output on `## Test:` section headers and extracts fields (category, priority,
 * description) from each section. Missing fields are defaulted rather than causing
 * parse failures, following the best-effort parsing strategy.
 *
 * Usage:
 * ```
 * val result = OutputParser.parse(llmOutput)
 * result.onSuccess { suggestions -> suggestions.forEach { println(it.name) } }
 * result.onFailure { println("No suggestions found") }
 * ```
 */
object OutputParser {

    private val SUGGESTION_SPLIT = Regex("""(?=^## Test:\s)""", RegexOption.MULTILINE)
    private val NAME_REGEX = Regex("""^## Test:\s*(.+)$""", RegexOption.MULTILINE)
    private val CATEGORY_REGEX = Regex("""\*\*Category:\*\*\s*(.+)$""", RegexOption.MULTILINE)
    private val PRIORITY_REGEX = Regex("""\*\*Priority:\*\*\s*(.+)$""", RegexOption.MULTILINE)
    private val DESCRIPTION_REGEX = Regex("""\*\*Description:\*\*\s*([\s\S]+?)(?=\*\*\w+:\*\*|## Test:|$)""")

    /**
     * Parses the given LLM [output] into a list of [TestSuggestion] instances.
     *
     * @param output markdown-formatted string containing `## Test:` sections
     * @return [Result.success] with parsed suggestions, or [Result.failure] with
     *         [IllegalStateException] if no `## Test:` sections are found
     */
    fun parse(output: String): Result<List<TestSuggestion>> {
        val sections = output.split(SUGGESTION_SPLIT)
            .filter { it.trimStart().startsWith("## Test:") }

        if (sections.isEmpty()) {
            return Result.failure(IllegalStateException("No test suggestions found in output"))
        }

        val suggestions = sections.map { parseSection(it) }
        return Result.success(suggestions)
    }

    private fun parseSection(section: String): TestSuggestion {
        val name = extractField(section, NAME_REGEX) ?: "Unnamed test suggestion"
        val category = SuggestionCategory.fromString(
            extractField(section, CATEGORY_REGEX) ?: ""
        )
        val priority = Priority.fromString(
            extractField(section, PRIORITY_REGEX) ?: ""
        )
        val description = extractMultiLineField(section) ?: ""

        return TestSuggestion(
            name = name,
            description = description,
            category = category,
            priority = priority,
        )
    }

    private fun extractField(section: String, regex: Regex): String? {
        val match = regex.find(section)
        return match?.groupValues?.get(1)?.trim().also {
            if (it == null) logger.debug { "Missing field for pattern: ${regex.pattern}" }
        }
    }

    private fun extractMultiLineField(section: String): String? {
        val match = DESCRIPTION_REGEX.find(section)
        return match?.groupValues?.get(1)?.trim().also {
            if (it == null) logger.debug { "Missing Description field in section" }
        }
    }
}
