package net.transgressoft.lab.analysis

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DisplayName
import java.io.File
import kotlin.io.path.createTempDirectory

/**
 * Tests for [ImportResolver] covering import extraction, star-import filtering,
 * alias handling, and file-based resolution by class name and package declaration.
 */
@DisplayName("ImportResolver")
class ImportResolverTest : FunSpec({

    test("ImportResolver extractImports extracts fully-qualified import paths from Kotlin source") {
        val source = """
            package com.example

            import com.example.model.User
            import com.example.service.AuthService

            class Foo
        """.trimIndent()

        val imports = ImportResolver.extractImports(source)

        imports.shouldContainExactly("com.example.model.User", "com.example.service.AuthService")
    }

    test("ImportResolver extractImports skips star imports") {
        val source = """
            package com.example

            import com.example.model.*
            import com.example.service.AuthService
            import kotlin.collections.*

            class Foo
        """.trimIndent()

        val imports = ImportResolver.extractImports(source)

        imports.shouldContainExactly("com.example.service.AuthService")
    }

    test("ImportResolver extractImports handles aliased imports") {
        val source = """
            package com.example

            import com.example.model.User as AppUser
            import com.example.service.AuthService

            class Foo
        """.trimIndent()

        val imports = ImportResolver.extractImports(source)

        imports.shouldContainExactlyInAnyOrder("com.example.model.User", "com.example.service.AuthService")
    }

    test("ImportResolver extractImports returns empty list when source has no imports") {
        val source = """
            package com.example

            class Foo {
                fun bar() = 42
            }
        """.trimIndent()

        val imports = ImportResolver.extractImports(source)

        imports.shouldBeEmpty()
    }

    test("ImportResolver resolveImport finds file matching class name and package declaration") {
        val dir = createTempDirectory("import-test").toFile()
        dir.deleteOnExit()
        val subDir = File(dir, "model").also { it.mkdirs() }
        val userFile = File(subDir, "User.kt").also {
            it.writeText("package com.example.model\n\ndata class User(val name: String)")
            it.deleteOnExit()
        }

        val result = ImportResolver.resolveImport("com.example.model.User", listOf(userFile))

        result shouldBe userFile
    }

    test("ImportResolver resolveImport returns null when no matching file exists") {
        val dir = createTempDirectory("import-test").toFile()
        dir.deleteOnExit()
        val otherFile = File(dir, "Other.kt").also {
            it.writeText("package com.example\n\nclass Other")
            it.deleteOnExit()
        }

        val result = ImportResolver.resolveImport("com.example.model.User", listOf(otherFile))

        result.shouldBeNull()
    }

    test("ImportResolver resolveImport returns null for import paths that match file name but wrong package") {
        val dir = createTempDirectory("import-test").toFile()
        dir.deleteOnExit()
        val userFile = File(dir, "User.kt").also {
            it.writeText("package com.other.pkg\n\ndata class User(val id: Int)")
            it.deleteOnExit()
        }

        val result = ImportResolver.resolveImport("com.example.model.User", listOf(userFile))

        result.shouldBeNull()
    }

    test("ImportResolver resolveImport handles files with multiple classes by matching file name") {
        val dir = createTempDirectory("import-test").toFile()
        dir.deleteOnExit()
        val modelsFile = File(dir, "Models.kt").also {
            it.writeText("""
                package com.example.config

                class Models {
                    companion object {
                        const val VALUE = 42
                    }
                }

                class OtherModel
            """.trimIndent())
            it.deleteOnExit()
        }

        val result = ImportResolver.resolveImport("com.example.config.Models", listOf(modelsFile))

        result shouldBe modelsFile
    }
})
