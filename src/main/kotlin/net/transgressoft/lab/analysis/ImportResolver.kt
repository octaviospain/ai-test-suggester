package net.transgressoft.lab.analysis

import java.io.File

/**
 * Extracts and resolves Kotlin import statements to project-internal source files.
 *
 * Parses import declarations from Kotlin source code and maps fully-qualified class names
 * to files within a given search scope. Star imports and external library imports are
 * silently skipped since they cannot be resolved to a single project file.
 *
 * Usage:
 * ```
 * val source = File("MyClass.kt").readText()
 * val imports = ImportResolver.extractImports(source)
 * val resolved = imports.mapNotNull { ImportResolver.resolveImport(it, projectFiles) }
 * ```
 */
object ImportResolver {

    private val IMPORT_REGEX = Regex("""^import\s+([\w.]+?)(\.\*)?(\s+as\s+\w+)?$""", RegexOption.MULTILINE)

    /**
     * Extracts fully-qualified import paths from Kotlin source code.
     *
     * Filters out star imports (e.g., `import com.example.*`) and strips alias suffixes
     * (e.g., `import com.Foo as Bar` yields `com.Foo`).
     *
     * @param source Kotlin source code as a string
     * @return list of fully-qualified class names from non-star import statements
     */
    fun extractImports(source: String): List<String> {
        return IMPORT_REGEX.findAll(source)
            .filter { it.groupValues[2].isEmpty() }
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Resolves a fully-qualified import path to a file within the given search scope.
     *
     * Matches by file name (last segment of the import path) and verifies the file's
     * `package` declaration matches the expected package prefix. Returns the first
     * matching file or `null` if no match is found.
     *
     * @param importPath fully-qualified class name (e.g., `com.example.model.User`)
     * @param searchFiles list of Kotlin source files to search within
     * @return the matching [File], or `null` if unresolvable
     */
    fun resolveImport(importPath: String, searchFiles: List<File>): File? {
        val className = importPath.substringAfterLast('.')
        val packagePath = importPath.substringBeforeLast('.')

        return searchFiles.firstOrNull { file ->
            file.nameWithoutExtension == className &&
                file.readText().lineSequence()
                    .firstOrNull { it.trimStart().startsWith("package ") }
                    ?.let { it.trim().removePrefix("package ").trim() == packagePath }
                    ?: false
        }
    }
}
