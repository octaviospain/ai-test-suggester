package net.transgressoft.lab.output

/**
 * Priority level for a test suggestion, indicating how important it is to implement.
 *
 * Enum ordinal order (HIGH=0, MEDIUM=1, LOW=2) is used for sorting suggestions
 * with highest priority first.
 *
 * @property label lowercase string representation used for parsing LLM output
 */
enum class Priority(val label: String) {
    HIGH("high"),
    MEDIUM("medium"),
    LOW("low");

    companion object {
        /**
         * Parses a priority string with case-insensitive matching.
         * Returns [MEDIUM] as the default when the input does not match any known priority.
         */
        fun fromString(value: String): Priority =
            entries.firstOrNull { it.label.equals(value.trim(), ignoreCase = true) } ?: MEDIUM
    }
}

/**
 * Category classifying the type of test scenario a suggestion targets.
 *
 * @property label human-readable string representation used for parsing LLM output
 */
enum class SuggestionCategory(val label: String) {
    HAPPY_PATH("happy path"),
    EDGE_CASE("edge case"),
    ERROR_HANDLING("error handling"),
    BOUNDARY_CONDITION("boundary condition"),
    UNCATEGORIZED("uncategorized");

    companion object {
        /**
         * Parses a category string with case-insensitive matching.
         * Returns [UNCATEGORIZED] as the default when the input does not match any known category.
         */
        fun fromString(value: String): SuggestionCategory =
            entries.firstOrNull { it.label.equals(value.trim(), ignoreCase = true) } ?: UNCATEGORIZED
    }
}

/**
 * A structured test suggestion extracted from LLM output.
 *
 * @property name descriptive test name following common naming conventions
 * @property description detailed explanation of what the test verifies, why it matters, inputs, and expected behavior
 * @property category the type of test scenario (happy path, edge case, etc.)
 * @property priority importance level indicating how critical this test is to implement
 */
data class TestSuggestion(
    val name: String,
    val description: String,
    val category: SuggestionCategory,
    val priority: Priority,
)
