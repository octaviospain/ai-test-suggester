package net.transgressoft.lab.tools

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.result.shouldBeFailure
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.DisplayName

/**
 * Tests for [FileReader] covering valid reads, missing files, and non-Kotlin file rejection.
 */
@DisplayName("FileReader")
class FileReaderTest : FunSpec({

    test("FileReader read returns file contents for a valid .kt file") {
        val file = kotlin.io.path.createTempFile(suffix = ".kt").toFile()
        file.writeText("fun main() {}", Charsets.UTF_8)
        file.deleteOnExit()

        val result = FileReader.read(file.absolutePath)

        result.shouldBeSuccess()
        result.getOrThrow() shouldContain "fun main()"
    }

    test("FileReader read returns failure for non-existent file path") {
        val result = FileReader.read("/nonexistent/path/Fake.kt")

        result.shouldBeFailure()
        result.exceptionOrNull()!!.message shouldContain "not found"
    }

    test("FileReader read returns failure for non-.kt file extension") {
        val file = kotlin.io.path.createTempFile(suffix = ".java").toFile()
        file.writeText("class Foo {}")
        file.deleteOnExit()

        val result = FileReader.read(file.absolutePath)

        result.shouldBeFailure()
        result.exceptionOrNull()!!.message shouldContain "Not a Kotlin file"
    }

    test("FileReader read reads UTF-8 encoded content correctly") {
        val file = kotlin.io.path.createTempFile(suffix = ".kt").toFile()
        val utf8Content = "// Kommentar mit Umlauten: aou and symbols"
        file.writeText(utf8Content, Charsets.UTF_8)
        file.deleteOnExit()

        val result = FileReader.read(file.absolutePath)

        result.shouldBeSuccess()
        result.getOrThrow() shouldContain utf8Content
    }
})
