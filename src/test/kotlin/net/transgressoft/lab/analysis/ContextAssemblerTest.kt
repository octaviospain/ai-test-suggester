package net.transgressoft.lab.analysis

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Tests for [ContextAssembler] covering target file inclusion, dependency budgeting,
 * overflow summarization, and token estimation.
 */
@DisplayName("ContextAssembler")
class ContextAssemblerTest : FunSpec({

    fun createTempKtFile(dir: File, name: String, content: String): File {
        return File(dir, name).also { it.writeText(content) }
    }

    test("ContextAssembler assemble includes target file in full with markdown header and path metadata") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        val target = createTempKtFile(dir, "Foo.kt", """
            package com.example

            class Foo {
                fun doSomething() = 42
            }
        """.trimIndent())

        val result = ContextAssembler.assemble(target, emptyList())

        result shouldContain "## Target File: Foo.kt"
        result shouldContain "Package: com.example"
        result shouldContain "Path: ${target.path}"
        result shouldContain "class Foo"
        result shouldContain "fun doSomething()"
    }

    test("ContextAssembler assemble includes dependency files with markdown headers when within budget") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        val target = createTempKtFile(dir, "Foo.kt", """
            package com.example

            class Foo
        """.trimIndent())
        val dep = createTempKtFile(dir, "Bar.kt", """
            package com.example

            class Bar {
                fun helper() = "help"
            }
        """.trimIndent())

        val result = ContextAssembler.assemble(target, listOf(dep), contextWindowTokens = 5000)

        result shouldContain "## Dependency: Bar.kt"
        result shouldContain "class Bar"
        result shouldContain "fun helper()"
    }

    test("ContextAssembler assemble switches to one-line summary for dependencies that exceed token budget") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        val target = createTempKtFile(dir, "Foo.kt", """
            package com.example

            class Foo {
                fun doSomething() = 42
            }
        """.trimIndent())
        // Large dependency that won't fit in tight budget
        val largeDep = createTempKtFile(dir, "BigService.kt", """
            package com.example.service

            class BigService {
                fun processData() = "data"
                fun validateInput() = true
                fun transformResult() = listOf(1, 2, 3)
            }
        """.trimIndent())

        // Very tight budget forces summary mode
        val result = ContextAssembler.assemble(target, listOf(largeDep), contextWindowTokens = 100)

        result shouldContain "## Dependency (summary):"
        result shouldContain "BigService"
        // The dependency section should be a summary, not full source
        val depSection = result.substringAfter("## Dependency (summary):")
        depSection shouldNotContain "```kotlin"
        depSection shouldNotContain "fun transformResult()"
    }

    test("ContextAssembler assemble produces output with no dependencies when dependencyFiles is empty") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        val target = createTempKtFile(dir, "Solo.kt", """
            package com.example

            class Solo
        """.trimIndent())

        val result = ContextAssembler.assemble(target, emptyList())

        result shouldContain "## Target File: Solo.kt"
        result shouldNotContain "## Dependency"
    }

    test("ContextAssembler assemble never truncates the target file regardless of size") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        // Create a large target file
        val largeContent = buildString {
            appendLine("package com.example")
            appendLine()
            appendLine("class LargeClass {")
            repeat(100) { i ->
                appendLine("    fun method$i() = $i")
            }
            appendLine("}")
        }
        val target = createTempKtFile(dir, "LargeClass.kt", largeContent)

        // Very small budget -- target must still be included in full
        val result = ContextAssembler.assemble(target, emptyList(), contextWindowTokens = 50)

        result shouldContain "fun method0()"
        result shouldContain "fun method99()"
    }

    test("ContextAssembler assemble estimates tokens as characters divided by 4") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        val target = createTempKtFile(dir, "A.kt", "package x\n\nclass A")
        // Dependency with exactly 400 chars => 100 tokens
        val depContent = "package y\n\n" + "x".repeat(389)
        val dep = createTempKtFile(dir, "B.kt", depContent)

        // Budget: target tokens + reserved(3000) + 100 dep tokens should just fit at ~3200
        // With very tight budget, the dep source (100 tokens) should exceed remaining
        val result = ContextAssembler.assemble(target, listOf(dep), contextWindowTokens = 3100)

        // With 3100 total, minus 3000 reserved, only 100 left for all content
        // Target alone uses ~5 tokens, so dep (100 tokens in section) won't fit
        result shouldContain "## Dependency (summary):"
    }

    test("ContextAssembler assemble reserves tokens for system prompt and response") {
        val dir = createTempDirectory("ctx-test").toFile()
        dir.deleteOnExit()
        val target = createTempKtFile(dir, "T.kt", "package p\n\nclass T")
        val dep = createTempKtFile(dir, "D.kt", "package p\n\nclass D { fun m() = 1 }")

        // With generous budget, dependency should fit
        val resultGenerous = ContextAssembler.assemble(target, listOf(dep), contextWindowTokens = 10000)
        resultGenerous shouldContain "## Dependency: D.kt"
        resultGenerous shouldContain "```kotlin"

        // With budget barely above reserved (3000), dep should be summarized
        val resultTight = ContextAssembler.assemble(target, listOf(dep), contextWindowTokens = 3050)
        resultTight shouldContain "## Dependency (summary):"
    }
})
