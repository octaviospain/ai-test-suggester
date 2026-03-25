package net.transgressoft.lab.output

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName

/**
 * Tests for [OutputParser] covering field extraction from LLM markdown output,
 * missing field defaults, multi-line descriptions, and empty input handling.
 */
@DisplayName("OutputParser")
class OutputParserTest : FunSpec({

    test("OutputParser parse extracts all fields from single well-formed suggestion") {
        val input = """
## Test: verifies email uniqueness constraint
**Category:** edge case
**Priority:** high
**Description:** Duplicate emails could corrupt user data. Insert two users with the same email and verify constraint violation
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        val suggestions = result.getOrThrow()
        suggestions shouldHaveSize 1
        suggestions[0].name shouldBe "verifies email uniqueness constraint"
        suggestions[0].category shouldBe SuggestionCategory.EDGE_CASE
        suggestions[0].priority shouldBe Priority.HIGH
        suggestions[0].description shouldBe "Duplicate emails could corrupt user data. Insert two users with the same email and verify constraint violation"
    }

    test("OutputParser parse extracts multiple suggestions from multiple sections") {
        val input = """
## Test: handles null input gracefully
**Category:** error handling
**Priority:** high
**Description:** Null input crashes the parser. Pass null to parse and expect failure result

## Test: processes empty list
**Category:** boundary condition
**Priority:** medium
**Description:** Empty collections are common edge cases. Pass empty list and verify empty output
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        val suggestions = result.getOrThrow()
        suggestions shouldHaveSize 2
        suggestions[0].name shouldBe "handles null input gracefully"
        suggestions[1].name shouldBe "processes empty list"
    }

    test("OutputParser parse defaults missing Category to UNCATEGORIZED") {
        val input = """
## Test: some test
**Priority:** low
**Description:** desc
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        result.getOrThrow()[0].category shouldBe SuggestionCategory.UNCATEGORIZED
    }

    test("OutputParser parse defaults missing Priority to MEDIUM") {
        val input = """
## Test: some test
**Category:** happy path
**Description:** desc
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        result.getOrThrow()[0].priority shouldBe Priority.MEDIUM
    }

    test("OutputParser parse defaults missing Description to empty string") {
        val input = """
## Test: some test
**Category:** happy path
**Priority:** high
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        result.getOrThrow()[0].description shouldBe ""
    }

    test("OutputParser parse captures multi-line Description until next field header") {
        val input = """
## Test: complex test
**Category:** edge case
**Priority:** high
**Description:** First line of description.
Second line continues the description.
Third line with more details.
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        val desc = result.getOrThrow()[0].description
        desc shouldContain "First line of description."
        desc shouldContain "Second line continues the description."
        desc shouldContain "Third line with more details."
    }

    test("OutputParser parse returns failure for empty string with no sections") {
        val result = OutputParser.parse("")

        result.shouldBeFailure()
        result.exceptionOrNull()!!.message shouldBe "No test suggestions found in output"
    }

    test("OutputParser parse returns failure for text without any ## Test: sections") {
        val result = OutputParser.parse("Some random text without any test sections.")

        result.shouldBeFailure()
        result.exceptionOrNull()!!.message shouldBe "No test suggestions found in output"
    }

    test("OutputParser parse ignores preamble text before first ## Test: section") {
        val input = """
Here is some preamble text that should be ignored.
Some more introductory content.

## Test: actual test suggestion
**Category:** happy path
**Priority:** medium
**Description:** description here
        """.trimIndent()

        val result = OutputParser.parse(input)

        result.shouldBeSuccess()
        val suggestions = result.getOrThrow()
        suggestions shouldHaveSize 1
        suggestions[0].name shouldBe "actual test suggestion"
    }
})
