package net.transgressoft.lab.output

/**
 * Renders parsed [TestSuggestion] instances into sorted plain text output.
 *
 * Provides two formatting modes: [formatSingle] for a single file's suggestions and
 * [formatDirectory] for multiple files with per-file section headers. Suggestions are
 * always sorted by [Priority] ordinal (high first, then medium, then low). Output is
 * plain markdown text with no ANSI escape codes.
 *
 * Usage:
 * ```
 * val output = OutputFormatter.formatSingle("MyClass.kt", suggestions)
 * println(output)
 *
 * val dirOutput = OutputFormatter.formatDirectory(mapOf("A.kt" to suggestionsA, "B.kt" to suggestionsB))
 * println(dirOutput)
 * ```
 */
object OutputFormatter {

    /**
     * Formats a single file's test suggestions as sorted plain text with a summary header.
     *
     * @param fileName the source file name (used for context, not included in output header)
     * @param suggestions list of parsed test suggestions to render
     * @return formatted plain text string with summary header followed by sorted suggestions
     */
    fun formatSingle(fileName: String, suggestions: List<TestSuggestion>): String = buildString {
        appendLine(summaryHeader(suggestions))
        appendLine()
        sortedSuggestions(suggestions).forEach { appendSuggestion(this, it) }
    }

    /**
     * Formats multiple files' test suggestions with per-file headers and an overall summary.
     *
     * @param fileResults map of file names to their parsed test suggestions
     * @return formatted plain text string with overall summary, per-file headers, and sorted suggestions
     */
    fun formatDirectory(fileResults: Map<String, List<TestSuggestion>>): String = buildString {
        val allSuggestions = fileResults.values.flatten()
        appendLine(summaryHeader(allSuggestions))
        appendLine()
        for ((fileName, suggestions) in fileResults) {
            appendLine("# $fileName (${suggestions.size} suggestions)")
            appendLine()
            sortedSuggestions(suggestions).forEach { appendSuggestion(this, it) }
        }
    }

    private fun summaryHeader(suggestions: List<TestSuggestion>): String {
        val byPriority = suggestions.groupBy { it.priority }
        val breakdown = Priority.entries
            .mapNotNull { p -> byPriority[p]?.size?.let { "${it} ${p.label}" } }
            .joinToString(", ")
        return "**${suggestions.size} suggestions** ($breakdown)"
    }

    private fun sortedSuggestions(suggestions: List<TestSuggestion>): List<TestSuggestion> =
        suggestions.sortedBy { it.priority.ordinal }

    private fun appendSuggestion(sb: StringBuilder, s: TestSuggestion) {
        sb.appendLine("## Test: ${s.name}")
        sb.appendLine("**Category:** ${s.category.label}")
        sb.appendLine("**Priority:** ${s.priority.label}")
        sb.appendLine("**Description:** ${s.description}")
        sb.appendLine()
    }
}
