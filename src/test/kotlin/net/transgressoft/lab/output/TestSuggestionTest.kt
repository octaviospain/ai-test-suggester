package net.transgressoft.lab.output

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName

/**
 * Tests for [Priority] and [SuggestionCategory] enum parsing with case-insensitive
 * matching and fallback defaults.
 */
@DisplayName("TestSuggestion")
class TestSuggestionTest : FunSpec({

    test("Priority fromString returns HIGH for 'high'") {
        Priority.fromString("high") shouldBe Priority.HIGH
    }

    test("Priority fromString returns MEDIUM for 'medium'") {
        Priority.fromString("medium") shouldBe Priority.MEDIUM
    }

    test("Priority fromString returns LOW for 'low'") {
        Priority.fromString("low") shouldBe Priority.LOW
    }

    test("Priority fromString returns MEDIUM as default for unknown value") {
        Priority.fromString("unknown") shouldBe Priority.MEDIUM
    }

    test("Priority fromString handles mixed case 'High'") {
        Priority.fromString("High") shouldBe Priority.HIGH
    }

    test("Priority fromString handles uppercase 'HIGH'") {
        Priority.fromString("HIGH") shouldBe Priority.HIGH
    }

    test("SuggestionCategory fromString returns HAPPY_PATH for 'happy path'") {
        SuggestionCategory.fromString("happy path") shouldBe SuggestionCategory.HAPPY_PATH
    }

    test("SuggestionCategory fromString returns EDGE_CASE for 'edge case'") {
        SuggestionCategory.fromString("edge case") shouldBe SuggestionCategory.EDGE_CASE
    }

    test("SuggestionCategory fromString returns ERROR_HANDLING for 'error handling'") {
        SuggestionCategory.fromString("error handling") shouldBe SuggestionCategory.ERROR_HANDLING
    }

    test("SuggestionCategory fromString returns BOUNDARY_CONDITION for 'boundary condition'") {
        SuggestionCategory.fromString("boundary condition") shouldBe SuggestionCategory.BOUNDARY_CONDITION
    }

    test("SuggestionCategory fromString returns UNCATEGORIZED for unknown value") {
        SuggestionCategory.fromString("unknown") shouldBe SuggestionCategory.UNCATEGORIZED
    }

    test("SuggestionCategory fromString handles mixed case 'Edge Case'") {
        SuggestionCategory.fromString("Edge Case") shouldBe SuggestionCategory.EDGE_CASE
    }
})
