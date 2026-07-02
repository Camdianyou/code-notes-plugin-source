package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.util.LocalizedEnumLabels

enum class CodeAnchorMarkerKind { NOTE, REVIEW_ISSUE }

data class CodeAnchorMarker(
    val id: String,
    val kind: CodeAnchorMarkerKind,
    val title: String,
    val summary: String,
    val filePath: String,
    val lineStart: Int,
    val lineEnd: Int,
    val textHash: String,
    val anchorType: String,
    val symbolLanguage: String,
    val symbolKind: String,
    val symbolQualifiedName: String,
    val symbolSignature: String,
    val fallbackLine: Int,
    val fallbackTextHash: String,
    val type: String,
    val priority: String
) {
    companion object {
        fun fromNotesAndIssues(
            relativePath: String,
            notes: List<NoteEntity>,
            issues: List<CodeReviewIssueEntity>
        ): List<CodeAnchorMarker> {
            val linkedNoteIds = notes.map { it.id }.toSet()
            val noteMarkers = notes
                .filter { it.filePath == relativePath && it.lineStart >= 0 }
                .map { it.toMarker() }
            val issueMarkers = issues
                .filter { it.noteId.isBlank() || it.noteId !in linkedNoteIds }
                .filter { it.filePath == relativePath && it.lineStart >= 0 }
                .map { it.toMarker() }
            return (noteMarkers + issueMarkers).sortedWith(compareBy({ it.lineStart }, { it.title }, { it.id }))
        }

        fun priorityCode(value: String): TodoPriority =
            LocalizedEnumLabels.priorityCode(value) ?: TodoPriority.MEDIUM

        fun typeIcon(value: String): String =
            (LocalizedEnumLabels.noteTypeCode(value) ?: NoteType.COMMENT).icon

        private fun NoteEntity.toMarker(): CodeAnchorMarker =
            CodeAnchorMarker(
                id = id,
                kind = CodeAnchorMarkerKind.NOTE,
                title = title.ifBlank { summary },
                summary = summary,
                filePath = filePath,
                lineStart = lineStart,
                lineEnd = lineEnd,
                textHash = textHash,
                anchorType = anchorType,
                symbolLanguage = symbolLanguage,
                symbolKind = symbolKind,
                symbolQualifiedName = symbolQualifiedName,
                symbolSignature = symbolSignature,
                fallbackLine = fallbackLine,
                fallbackTextHash = fallbackTextHash,
                type = type,
                priority = priority
            )

        private fun CodeReviewIssueEntity.toMarker(): CodeAnchorMarker =
            CodeAnchorMarker(
                id = id,
                kind = CodeAnchorMarkerKind.REVIEW_ISSUE,
                title = title,
                summary = description,
                filePath = filePath,
                lineStart = lineStart,
                lineEnd = lineEnd,
                textHash = "",
                anchorType = "",
                symbolLanguage = "",
                symbolKind = "",
                symbolQualifiedName = symbolQualifiedName,
                symbolSignature = "",
                fallbackLine = lineStart,
                fallbackTextHash = "",
                type = issueType,
                priority = severity
            )
    }
}
