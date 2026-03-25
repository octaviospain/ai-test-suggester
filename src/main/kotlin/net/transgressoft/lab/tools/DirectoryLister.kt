package net.transgressoft.lab.tools

import java.io.File

/**
 * Lists Kotlin source files within a directory tree.
 *
 * Recursively walks the given directory and collects all files with a `.kt` extension.
 * Returns the file list wrapped in [Result] for safe error handling.
 *
 * Usage:
 * ```
 * val files = DirectoryLister.listKotlinFiles("/path/to/project/src")
 * files.onSuccess { it.forEach { f -> println(f.name) } }
 * files.onFailure { println("Error: ${it.message}") }
 * ```
 */
object DirectoryLister {

    /**
     * Lists all `.kt` files recursively under the given [dirPath].
     *
     * @param dirPath absolute or relative path to a directory
     * @return [Result.success] with a list of `.kt` [File] objects, or [Result.failure]
     *         if the path does not exist or is not a directory
     */
    fun listKotlinFiles(dirPath: String): Result<List<File>> = runCatching {
        val dir = File(dirPath)
        require(dir.exists()) { "Directory not found: $dirPath" }
        require(dir.isDirectory) { "Not a directory: $dirPath" }
        dir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .toList()
    }
}
