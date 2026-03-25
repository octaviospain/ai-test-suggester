package net.transgressoft.lab.tools

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName
import java.io.File

/**
 * Tests for [DirectoryLister] covering recursive listing, error cases, and filtering.
 */
@DisplayName("DirectoryLister")
class DirectoryListerTest : FunSpec({

    test("DirectoryLister listKotlinFiles returns all .kt files recursively in directory") {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        tempDir.deleteOnExit()
        File(tempDir, "Root.kt").apply { writeText("class Root"); deleteOnExit() }
        val sub = File(tempDir, "sub").apply { mkdirs(); deleteOnExit() }
        File(sub, "Child.kt").apply { writeText("class Child"); deleteOnExit() }

        val result = DirectoryLister.listKotlinFiles(tempDir.absolutePath)

        result.shouldBeSuccess()
        result.getOrThrow() shouldHaveSize 2
    }

    test("DirectoryLister listKotlinFiles returns failure for non-existent directory") {
        val result = DirectoryLister.listKotlinFiles("/nonexistent/dir")

        result.shouldBeFailure()
        result.exceptionOrNull()!!.message shouldContain "not found"
    }

    test("DirectoryLister listKotlinFiles returns failure when path is a file not a directory") {
        val file = kotlin.io.path.createTempFile(suffix = ".kt").toFile()
        file.writeText("class Foo")
        file.deleteOnExit()

        val result = DirectoryLister.listKotlinFiles(file.absolutePath)

        result.shouldBeFailure()
        result.exceptionOrNull()!!.message shouldContain "Not a directory"
    }

    test("DirectoryLister listKotlinFiles returns empty list for directory with no .kt files") {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        tempDir.deleteOnExit()
        File(tempDir, "readme.md").apply { writeText("# README"); deleteOnExit() }

        val result = DirectoryLister.listKotlinFiles(tempDir.absolutePath)

        result.shouldBeSuccess()
        result.getOrThrow().shouldBeEmpty()
    }

    test("DirectoryLister listKotlinFiles excludes non-.kt files from results") {
        val tempDir = kotlin.io.path.createTempDirectory().toFile()
        tempDir.deleteOnExit()
        File(tempDir, "Valid.kt").apply { writeText("class Valid"); deleteOnExit() }
        File(tempDir, "Other.java").apply { writeText("class Other"); deleteOnExit() }
        File(tempDir, "data.json").apply { writeText("{}"); deleteOnExit() }

        val result = DirectoryLister.listKotlinFiles(tempDir.absolutePath)

        result.shouldBeSuccess()
        result.getOrThrow() shouldHaveSize 1
    }
})
