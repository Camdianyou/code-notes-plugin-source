package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.util.LocalizedEnumLabels

object CodeReviewIssueFactory {
    fun fromNote(reviewId: String, note: NoteEntity): CodeReviewIssueEntity =
        CodeReviewIssueEntity().apply {
            this.reviewId = reviewId
            noteId = note.id
            title = note.title.ifBlank { note.summary }
            description = note.description.ifBlank { note.summary }
            filePath = note.filePath
            lineStart = note.lineStart
            lineEnd = note.lineEnd
            symbolQualifiedName = note.symbolQualifiedName
            issueType = (LocalizedEnumLabels.noteTypeCode(note.type) ?: NoteType.REVIEW).name
            severity = (LocalizedEnumLabels.priorityCode(note.priority) ?: TodoPriority.MEDIUM).name
            status = (LocalizedEnumLabels.statusCode(note.status) ?: TodoStatus.TODO).name
            dueDate = note.dueDate
            suggestion = note.summary
            if (note.anchorType == NoteAnchor.SYMBOL.name && symbolQualifiedName.isBlank()) {
                symbolQualifiedName = note.elementFqName
            }
        }
}
