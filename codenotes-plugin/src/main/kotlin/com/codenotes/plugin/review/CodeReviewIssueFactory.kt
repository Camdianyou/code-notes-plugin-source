package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.NoteEntity

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
            issueType = note.type
            severity = note.priority
            status = note.status
            dueDate = note.dueDate
            suggestion = note.summary
            if (note.anchorType == NoteAnchor.SYMBOL.name && symbolQualifiedName.isBlank()) {
                symbolQualifiedName = note.elementFqName
            }
        }
}

