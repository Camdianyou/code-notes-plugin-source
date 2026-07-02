package com.codenotes.plugin.toolwindow

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ToolbarSourceTest {

    @Test
    fun `toolbars omit open buttons while list double click navigation remains`() {
        val notesPanel = source("com/codenotes/plugin/toolwindow/CodeNotesPanel.kt")
        val reviewPanel = source("com/codenotes/plugin/toolwindow/CodeReviewPanel.kt")

        assertFalse(notesPanel.contains("panel.action.open"))
        assertFalse(reviewPanel.contains("review.action.open"))
        assertTrue(notesPanel.contains("if (e.clickCount == 2) navigateToSelected()"))
        assertTrue(reviewPanel.contains("if (e.clickCount == 2) navigateToSelectedIssue()"))
    }

    private fun source(path: String): String =
        javaClass.classLoader.getResource(path)?.readText()
            ?: java.io.File("src/main/kotlin/$path").readText()
}
