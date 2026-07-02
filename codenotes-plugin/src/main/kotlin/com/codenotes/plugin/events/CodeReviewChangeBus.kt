package com.codenotes.plugin.events

import com.intellij.util.messages.Topic

data class CodeReviewChangeEvent(
    val kind: CodeReviewChangeKind,
    val reviewId: String = "",
    val issueId: String = ""
)

enum class CodeReviewChangeKind { ADDED, UPDATED, DELETED, ISSUE_ADDED, ISSUE_UPDATED, ISSUE_DELETED, REFRESHED, EXPORTED }

interface CodeReviewChangeListener {
    fun codeReviewsChanged(event: CodeReviewChangeEvent)
}

object CodeReviewChangeBus {
    @Topic.ProjectLevel
    val TOPIC: Topic<CodeReviewChangeListener> =
        Topic(CodeReviewChangeListener::class.java, Topic.BroadcastDirection.NONE)
}

