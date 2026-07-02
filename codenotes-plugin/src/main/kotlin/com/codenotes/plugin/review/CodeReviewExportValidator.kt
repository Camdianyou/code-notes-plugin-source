package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity

data class CodeReviewValidationResult(
    val missingReviewFields: List<String>,
    val missingIssueFields: Map<String, List<String>>
) {
    val isValid: Boolean
        get() = missingReviewFields.isEmpty() && missingIssueFields.isEmpty()
}

object CodeReviewExportValidator {
    fun validate(review: CodeReviewEntity, issues: List<CodeReviewIssueEntity>): CodeReviewValidationResult {
        val missingReviewFields = buildList {
            if (review.meetingName.isBlank()) add("\u4F1A\u8BAE\u540D\u79F0")
            if (review.meetingDate.isBlank()) add("\u4F1A\u8BAE\u65E5\u671F")
            if (review.host.isBlank()) add("\u4F1A\u8BAE\u4E3B\u6301\u4EBA")
            if (review.recorder.isBlank()) add("\u8BB0\u5F55\u4EBA")
            if (review.attendees.isBlank()) add("\u53C2\u4F1A\u4EBA\u5458")
            if (review.topic.isBlank()) add("\u4F1A\u8BAE\u4E3B\u9898")
        }
        val missingIssues = issues.mapNotNull { issue ->
            val missing = buildList {
                if (issue.title.isBlank()) add("\u6807\u9898")
                if (issue.description.isBlank()) add("\u95EE\u9898\u63CF\u8FF0")
                if (issue.filePath.isNotBlank() && issue.lineStart < 0 && issue.symbolQualifiedName.isBlank()) {
                    add("\u884C\u53F7\u6216\u7B26\u53F7")
                }
            }
            if (missing.isEmpty()) null else issue.id to missing
        }.toMap()
        return CodeReviewValidationResult(missingReviewFields, missingIssues)
    }
}

