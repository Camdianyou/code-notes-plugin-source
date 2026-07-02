package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeAnchorMarkerTest {

    @Test
    fun `builds markers for notes and independent review issues without duplicating linked issues`() {
        val note = NoteEntity().apply {
            id = "note-1"
            title = "Null risk"
            summary = "Check nullable value"
            filePath = "src/Foo.kt"
            lineStart = 4
            lineEnd = 4
            type = NoteType.BUG.name
            priority = TodoPriority.HIGH.name
        }
        val linkedIssue = CodeReviewIssueEntity().apply {
            id = "issue-linked"
            noteId = "note-1"
            filePath = "src/Foo.kt"
            lineStart = 4
            title = "Linked"
        }
        val independentIssue = CodeReviewIssueEntity().apply {
            id = "issue-independent"
            filePath = "src/Foo.kt"
            lineStart = 9
            title = "Independent"
            issueType = NoteType.REVIEW.name
            severity = TodoPriority.LOW.name
        }

        val markers = CodeAnchorMarker.fromNotesAndIssues(
            "src/Foo.kt",
            listOf(note),
            listOf(linkedIssue, independentIssue)
        )

        assertEquals(listOf("note-1", "issue-independent"), markers.map { it.id })
        assertEquals(listOf(CodeAnchorMarkerKind.NOTE, CodeAnchorMarkerKind.REVIEW_ISSUE), markers.map { it.kind })
        assertTrue(markers.none { it.id == "issue-linked" })
        assertEquals(TodoPriority.HIGH, CodeAnchorMarker.priorityCode(markers[0].priority))
        assertEquals(TodoPriority.LOW, CodeAnchorMarker.priorityCode(markers[1].priority))
    }

    @Test
    fun `skips independent review issues without code line anchors`() {
        val issue = CodeReviewIssueEntity().apply {
            id = "issue-no-code"
            filePath = "src/Foo.kt"
            lineStart = -1
            title = "No line"
        }

        val markers = CodeAnchorMarker.fromNotesAndIssues("src/Foo.kt", emptyList(), listOf(issue))

        assertTrue(markers.isEmpty())
    }
}
