package com.codenotes.plugin.model

import java.util.UUID

class CodeReviewEntity {
    var id: String = UUID.randomUUID().toString()
    var meetingName: String = ""
    var meetingDate: String = ""
    var location: String = ""
    var startTime: String = ""
    var endTime: String = ""
    var host: String = ""
    var recorder: String = ""
    var attendees: String = ""
    var topic: String = ""
    var sendTo: String = ""
    var copyTo: String = ""
    var scope: String = ""
    var conclusion: String = ""
    var notes: String = ""
    var status: String = CodeReviewStatus.OPEN.name
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()

    override fun toString(): String = meetingName.ifBlank { "CodeReview($id)" }
}

class CodeReviewIssueEntity {
    var id: String = UUID.randomUUID().toString()
    var reviewId: String = ""
    var noteId: String = ""
    var title: String = ""
    var description: String = ""
    var filePath: String = ""
    var lineStart: Int = -1
    var lineEnd: Int = -1
    var symbolQualifiedName: String = ""
    var issueType: String = NoteType.REVIEW.name
    var severity: String = TodoPriority.MEDIUM.name
    var status: String = TodoStatus.TODO.name
    var owner: String = ""
    var dueDate: String = ""
    var suggestion: String = ""
    var resolution: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()

    override fun toString(): String = title.ifBlank { "CodeReviewIssue($id)" }
}

enum class CodeReviewStatus { OPEN, IN_REVIEW, DONE, ARCHIVED }

