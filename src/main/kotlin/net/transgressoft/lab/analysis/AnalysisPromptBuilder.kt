package net.transgressoft.lab.analysis

import ai.koog.prompt.dsl.Prompt
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.params.LLMParams

/**
 * Constructs Koog prompts for structured test suggestion generation.
 *
 * Builds a [Prompt] with a system message containing Kotlin-specific analysis guidance
 * (sealed class exhaustiveness, null safety, coroutine cancellation, data class behavior,
 * extension function scoping) and instructions to produce structured markdown suggestions
 * in `## Test:` format. The assembled source context is included as the user message.
 *
 * Usage:
 * ```
 * val context = ContextAssembler.assemble(targetFile, deps)
 * val prompt = AnalysisPromptBuilder.build(context)
 * val responses = executor.execute(prompt, Models.QWEN_CODER_14B, emptyList())
 * ```
 */
object AnalysisPromptBuilder {

    internal const val SYSTEM_PROMPT = """You are a Kotlin test suggestion expert. Analyze the provided source code and produce structured test suggestions.

## Instructions
Prioritize high-risk areas (error conditions, edge cases, boundary values) over happy paths. Happy paths should still be covered but with lower priority.

Let the code complexity determine how many suggestions you produce. Focus on high-value tests.

Do NOT suggest trivial tests like 'test constructor' or 'test it works' -- each suggestion must reference specific business logic, edge cases, or domain concepts from the analyzed code.

## Kotlin-Specific Guidance

Pay special attention to these Kotlin-specific concerns:

- **Sealed class exhaustiveness**: Missing `when` branches, incomplete pattern matching, future subclass additions that break exhaustiveness
- **Nullable returns and null safety**: Safe calls (`?.`), forced unwraps (`!!`), platform types from Java interop, nullable receiver functions, null propagation chains
- **Coroutine concerns**: Cancellation handling, structured concurrency violations, dispatcher usage (blocking on Main, CPU-bound on IO), `withContext` correctness, suspend function exception propagation
- **Data class behavior**: `equals`/`hashCode` contract (especially with mutable fields or array properties), `copy` semantics (shallow copy pitfalls), destructuring order dependency
- **Extension function scoping**: Receiver scope leakage, shadowing of member functions, null receiver extensions, dispatch vs extension receiver resolution

## Test Naming Convention
Test names MUST use PascalCase with no spaces, underscores, or separators. Start with a verb or context noun.
Good examples: `TestRoomTypeBaseRateValues`, `TestCorporateEligibilityForPenthouseRoom`, `ValidateNullReturnFromRepository`
Bad examples: `Verify Total Price with Negative Adjustments`, `Test_BookingStatus_ConfirmedStatus`, `test null handling`

## Output Format
For each test suggestion, use EXACTLY this format:

## Test: [PascalCase test name with no spaces or underscores]
**Category:** [one of: happy path, edge case, error handling, boundary condition]
**Priority:** [one of: high, medium, low]
**Description:** [why this test matters, detailed description of test scenario, inputs, and expected behavior]

Output ONLY the test suggestions in the format above. Do not include any other text outside of suggestions."""

    /**
     * Builds a Koog [Prompt] for analysis of the given assembled context.
     *
     * @param assembledContext markdown-structured source context from [ContextAssembler]
     * @return a [Prompt] with system guidance and the context as user message
     */
    fun build(assembledContext: String): Prompt = prompt(
        id = "code-analysis",
        params = LLMParams(temperature = 0.3)
    ) {
        system(SYSTEM_PROMPT)
        user(assembledContext)
    }
}
