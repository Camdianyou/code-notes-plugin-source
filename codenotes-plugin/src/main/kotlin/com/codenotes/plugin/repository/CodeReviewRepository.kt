package com.codenotes.plugin.repository

import com.codenotes.plugin.events.CodeReviewChangeBus
import com.codenotes.plugin.events.CodeReviewChangeEvent
import com.codenotes.plugin.events.CodeReviewChangeKind
import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.CodeReviewStatus
import com.codenotes.plugin.state.NoteStorageService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class CodeReviewRepository(private val project: Project) {

    private val storage: NoteStorageService
        get() = NoteStorageService.getInstance(project)

    fun allReviews(): List<CodeReviewEntity> =
        storage.getCodeReviews().sortedByDescending { it.updatedAt }

    fun activeReviews(): List<CodeReviewEntity> =
        allReviews().filter { it.status != CodeReviewStatus.ARCHIVED.name }

    fun latestActiveReview(): CodeReviewEntity? = activeReviews().firstOrNull()

    fun findReview(id: String): CodeReviewEntity? = storage.findCodeReviewById(id)

    fun issues(reviewId: String): List<CodeReviewIssueEntity> =
        storage.getCodeReviewIssues(reviewId).sortedByDescending { it.updatedAt }

    fun findIssue(id: String): CodeReviewIssueEntity? = storage.findCodeReviewIssueById(id)

    fun addReview(review: CodeReviewEntity) {
        val now = System.currentTimeMillis()
        review.createdAt = now
        review.updatedAt = now
        storage.addCodeReview(review)
        publish(CodeReviewChangeKind.ADDED, review.id)
    }

    fun updateReview(review: CodeReviewEntity) {
        review.updatedAt = System.currentTimeMillis()
        storage.updateCodeReview(review)
        publish(CodeReviewChangeKind.UPDATED, review.id)
    }

    fun deleteReview(id: String) {
        storage.deleteCodeReview(id)
        publish(CodeReviewChangeKind.DELETED, id)
    }

    fun addIssue(issue: CodeReviewIssueEntity) {
        val now = System.currentTimeMillis()
        issue.createdAt = now
        issue.updatedAt = now
        storage.addCodeReviewIssue(issue)
        touchReview(issue.reviewId)
        publish(CodeReviewChangeKind.ISSUE_ADDED, issue.reviewId, issue.id)
    }

    fun updateIssue(issue: CodeReviewIssueEntity) {
        issue.updatedAt = System.currentTimeMillis()
        storage.updateCodeReviewIssue(issue)
        touchReview(issue.reviewId)
        publish(CodeReviewChangeKind.ISSUE_UPDATED, issue.reviewId, issue.id)
    }

    fun deleteIssue(id: String) {
        val issue = storage.findCodeReviewIssueById(id)
        storage.deleteCodeReviewIssue(id)
        if (issue != null) touchReview(issue.reviewId)
        publish(CodeReviewChangeKind.ISSUE_DELETED, issue?.reviewId.orEmpty(), id)
    }

    private fun touchReview(reviewId: String) {
        val review = storage.findCodeReviewById(reviewId) ?: return
        review.updatedAt = System.currentTimeMillis()
        storage.updateCodeReview(review)
    }

    private fun publish(kind: CodeReviewChangeKind, reviewId: String = "", issueId: String = "") {
        project.messageBus.syncPublisher(CodeReviewChangeBus.TOPIC)
            .codeReviewsChanged(CodeReviewChangeEvent(kind, reviewId, issueId))
    }

    companion object {
        fun getInstance(project: Project): CodeReviewRepository = project.service()
    }
}

