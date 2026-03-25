package net.transgressoft.lab.output

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.DisplayName

/**
 * Tests for [OutputFormatter] covering summary header generation, priority-based sorting,
 * single-file and directory formatting modes, and absence of ANSI escape codes.
 */
@DisplayName("OutputFormatter")
class OutputFormatterTest : FunSpec({

    val highSuggestion = TestSuggestion(
        name = "handles concurrent access",
        description = "Race conditions could corrupt state. Verify thread safety under concurrent writes",
        category = SuggestionCategory.EDGE_CASE,
        priority = Priority.HIGH,
    )

    val mediumSuggestion = TestSuggestion(
        name = "validates input format",
        description = "Users may send unexpected formats. Pass malformed input and verify rejection",
        category = SuggestionCategory.HAPPY_PATH,
        priority = Priority.MEDIUM,
    )

    val lowSuggestion = TestSuggestion(
        name = "logs warning on retry",
        description = "Observability for debugging. Trigger a retry and check log output",
        category = SuggestionCategory.ERROR_HANDLING,
        priority = Priority.LOW,
    )

    test("OutputFormatter formatSingle produces summary header with count and priority breakdown") {
        val output = OutputFormatter.formatSingle("MyClass.kt", listOf(highSuggestion, mediumSuggestion, lowSuggestion))

        output shouldContain "**3 suggestions** (1 high, 1 medium, 1 low)"
    }

    test("OutputFormatter formatSingle sorts suggestions by priority HIGH first then MEDIUM then LOW") {
        val output = OutputFormatter.formatSingle("MyClass.kt", listOf(lowSuggestion, highSuggestion, mediumSuggestion))

        val highIndex = output.indexOf("handles concurrent access")
        val mediumIndex = output.indexOf("validates input format")
        val lowIndex = output.indexOf("logs warning on retry")

        (highIndex < mediumIndex) shouldBe true
        (mediumIndex < lowIndex) shouldBe true
    }

    test("OutputFormatter formatSingle renders each suggestion with all fields") {
        val output = OutputFormatter.formatSingle("MyClass.kt", listOf(highSuggestion))

        output shouldContain "## Test: handles concurrent access"
        output shouldContain "**Category:** edge case"
        output shouldContain "**Priority:** high"
        output shouldContain "**Description:** Race conditions could corrupt state. Verify thread safety under concurrent writes"
    }

    test("OutputFormatter formatSingle output contains no ANSI escape codes") {
        val output = OutputFormatter.formatSingle("MyClass.kt", listOf(highSuggestion, mediumSuggestion))

        output shouldNotContain "\u001B"
        output shouldNotContain "\\033"
    }

    test("OutputFormatter formatDirectory produces per-file headers with suggestion count") {
        val fileResults = mapOf(
            "FileA.kt" to listOf(highSuggestion),
            "FileB.kt" to listOf(mediumSuggestion, lowSuggestion),
        )

        val output = OutputFormatter.formatDirectory(fileResults)

        output shouldContain "# FileA.kt (1 suggestions)"
        output shouldContain "# FileB.kt (2 suggestions)"
    }

    test("OutputFormatter formatDirectory includes overall summary header across all files") {
        val fileResults = mapOf(
            "FileA.kt" to listOf(highSuggestion),
            "FileB.kt" to listOf(mediumSuggestion),
        )

        val output = OutputFormatter.formatDirectory(fileResults)

        output shouldContain "**2 suggestions** (1 high, 1 medium)"
    }

    test("OutputFormatter formatDirectory sorts suggestions within each file by priority") {
        val fileResults = mapOf(
            "MyFile.kt" to listOf(lowSuggestion, highSuggestion, mediumSuggestion),
        )

        val output = OutputFormatter.formatDirectory(fileResults)

        val highIndex = output.indexOf("handles concurrent access")
        val mediumIndex = output.indexOf("validates input format")
        val lowIndex = output.indexOf("logs warning on retry")

        (highIndex < mediumIndex) shouldBe true
        (mediumIndex < lowIndex) shouldBe true
    }

    test("OutputFormatter formatSingle with empty list produces summary with zero count") {
        val output = OutputFormatter.formatSingle("Empty.kt", emptyList())

        output shouldContain "**0 suggestions** ()"
        output shouldNotContain "## Test:"
    }
})
