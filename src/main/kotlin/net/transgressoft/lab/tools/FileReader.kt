package net.transgressoft.lab.tools

import java.io.File

/**
 * Reads Kotlin source files from the file system.
 *
 * Validates that the target file exists and has a `.kt` extension before reading.
 * Returns file contents wrapped in [Result] for safe error handling.
 *
 * Usage:
 * ```
 * val content = FileReader.read("/path/to/MyClass.kt")
 * content.onSuccess { println(it) }
 * content.onFailure { println("Error: ${it.message}") }
 * ```
 */
object FileReader {

    /**
     * Reads the contents of a Kotlin source file at the given [path].
     *
     * @param path absolute or relative path to a `.kt` file
     * @return [Result.success] with the file contents, or [Result.failure] if the file
     *         does not exist or is not a Kotlin file
     */
    fun read(path: String): Result<String> = runCatching {
        val file = File(path)
        require(file.exists()) { "File not found: $path" }
        require(file.extension == "kt") { "Not a Kotlin file: $path" }
        file.readText(Charsets.UTF_8)
    }
}
