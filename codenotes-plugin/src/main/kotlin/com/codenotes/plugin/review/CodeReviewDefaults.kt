package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.repository.CodeReviewRepository
import java.text.SimpleDateFormat
import java.util.Date

object CodeReviewDefaults {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")

    fun getOrCreateDefaultReview(repository: CodeReviewRepository): CodeReviewEntity {
        val today = today()
        repository.activeReviews().firstOrNull { it.meetingDate == today }?.let { return it }
        repository.latestActiveReview()?.let { return it }
        val user = System.getProperty("user.name") ?: ""
        return CodeReviewEntity().apply {
            meetingName = "$today 代码走查"
            meetingDate = today
            host = user
            recorder = user
            topic = "代码走查"
        }.also { repository.addReview(it) }
    }

    fun newTodayReview(): CodeReviewEntity {
        val today = today()
        val user = System.getProperty("user.name") ?: ""
        return CodeReviewEntity().apply {
            meetingName = "$today 代码走查"
            meetingDate = today
            host = user
            recorder = user
            topic = "代码走查"
        }
    }

    fun normalizeIssueDefaults(issue: com.codenotes.plugin.model.CodeReviewIssueEntity) {
        if (issue.issueType.isBlank()) issue.issueType = NoteType.REVIEW.name
        if (issue.severity.isBlank()) issue.severity = TodoPriority.MEDIUM.name
        if (issue.status.isBlank()) issue.status = TodoStatus.TODO.name
    }

    private fun today(): String = dateFormat.format(Date())
}

