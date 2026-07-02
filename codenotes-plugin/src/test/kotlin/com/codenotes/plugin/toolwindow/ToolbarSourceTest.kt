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

    @Test
    fun `toolbars use refresh and omit manual save buttons`() {
        val notesPanel = source("com/codenotes/plugin/toolwindow/CodeNotesPanel.kt")
        val reviewPanel = source("com/codenotes/plugin/toolwindow/CodeReviewPanel.kt")

        assertTrue(notesPanel.contains("panel.action.refresh"))
        assertTrue(reviewPanel.contains("review.action.refresh"))
        assertFalse(notesPanel.contains("panel.action.save"))
        assertFalse(reviewPanel.contains("review.action.saveReview"))
        assertFalse(reviewPanel.contains("review.action.saveIssue"))
        assertTrue(notesPanel.contains("installAutoSaveListeners()"))
        assertTrue(reviewPanel.contains("installAutoSaveListeners()"))
    }

    @Test
    fun `code review meeting editor omits conclusion input but keeps scope input`() {
        val reviewPanel = source("com/codenotes/plugin/toolwindow/CodeReviewPanel.kt")

        assertTrue(reviewPanel.contains("review.field.scope"))
        assertFalse(reviewPanel.contains("review.field.conclusion"))
        assertFalse(reviewPanel.contains("conclusionArea"))
        assertFalse(reviewPanel.contains("review.conclusion"))
    }

    private fun source(path: String): String =
        javaClass.classLoader.getResource(path)?.readText()
            ?: java.io.File("src/main/kotlin/$path").readText()
}
