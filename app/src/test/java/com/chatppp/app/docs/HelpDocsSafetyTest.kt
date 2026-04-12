package com.chatppp.app.docs

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class HelpDocsSafetyTest {
    @Test
    fun help_docs_use_placeholders_instead_of_secret_like_prefixes() {
        val repositoryRoot = findRepositoryRoot()
        val helpDocsDir = File(repositoryRoot, "docs/help")
        val flaggedFiles = helpDocsDir
            .walkTopDown()
            .filter { it.isFile }
            .filter { SECRET_LIKE_PATTERN.containsMatchIn(it.readText()) }
            .map { it.relativeTo(repositoryRoot).invariantSeparatorsPath }
            .toList()

        assertTrue(
            "Expected docs/help to avoid secret-like prefixes, but found: $flaggedFiles",
            flaggedFiles.isEmpty()
        )
    }

    @Test
    fun provider_templates_help_doc_exists() {
        val providerTemplatesDoc = File(findRepositoryRoot(), "docs/help/provider-templates.md")

        assertTrue(
            "Expected provider templates help doc at ${providerTemplatesDoc.path}",
            providerTemplatesDoc.exists()
        )
    }

    private fun findRepositoryRoot(): File {
        return generateSequence(File(".").absoluteFile) { current -> current.parentFile }
            .firstOrNull { File(it, "settings.gradle.kts").exists() }
            ?: error("Could not locate repository root from ${File(".").absolutePath}")
    }

    private companion object {
        val SECRET_LIKE_PATTERN = Regex("""\b(?:sk|ms)-""")
    }
}
