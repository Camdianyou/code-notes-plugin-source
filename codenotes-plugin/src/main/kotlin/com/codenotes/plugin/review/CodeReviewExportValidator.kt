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
            if (review.meetingName.isBlank()) add("会议名称")
            if (review.meetingDate.isBlank()) add("会议日期")
            if (review.host.isBlank()) add("会议主持人")
            if (review.recorder.isBlank()) add("记录人")
            if (review.attendees.isBlank()) add("参会人员")
            if (review.topic.isBlank()) add("会议主题")
        }
        val missingIssues = issues.mapNotNull { issue ->
            val missing = buildList {
                if (issue.title.isBlank()) add("标题")
                if (issue.description.isBlank()) add("问题描述")
                if (issue.issueType.isBlank()) add("问题类型")
                if (issue.severity.isBlank()) add("严重程度")
                if (issue.status.isBlank()) add("状态")
                if (issue.filePath.isNotBlank() && issue.lineStart < 0 && issue.symbolQualifiedName.isBlank()) {
                    add("行号或符号")
                }
            }
            if (missing.isEmpty()) null else issue.id to missing
        }.toMap()
        return CodeReviewValidationResult(missingReviewFields, missingIssues)
    }
}

