package com.codenotes.plugin.util

import com.codenotes.plugin.model.CodeReviewStatus
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus

object LocalizedEnumLabels {
    private val noteTypeLabels = mapOf(
        NoteType.COMMENT to "\u8BC4\u8BBA",
        NoteType.BUG to "\u7F3A\u9677",
        NoteType.QUESTION to "\u95EE\u9898",
        NoteType.OPTIMIZATION to "\u4F18\u5316",
        NoteType.REVIEW to "\u8BC4\u5BA1",
        NoteType.WARNING to "\u8B66\u544A",
        NoteType.IMPORTANT to "\u91CD\u8981",
        NoteType.ARCHITECTURE to "\u67B6\u6784",
        NoteType.TEMPORARY to "\u4E34\u65F6",
        NoteType.PERMANENT to "\u6C38\u4E45",
        NoteType.TODO to "\u5F85\u529E",
        NoteType.DECISION to "\u51B3\u7B56"
    )

    private val priorityLabels = mapOf(
        TodoPriority.LOW to "\u4F4E",
        TodoPriority.MEDIUM to "\u4E2D",
        TodoPriority.HIGH to "\u9AD8",
        TodoPriority.CRITICAL to "\u7D27\u6025"
    )

    private val statusLabels = mapOf(
        TodoStatus.TODO to "\u5F85\u529E",
        TodoStatus.DOING to "\u8FDB\u884C\u4E2D",
        TodoStatus.BLOCKED to "\u963B\u585E",
        TodoStatus.WAITING to "\u7B49\u5F85",
        TodoStatus.DONE to "\u5DF2\u5B8C\u6210",
        TodoStatus.ARCHIVED to "\u5DF2\u5F52\u6863"
    )

    private val reviewStatusLabels = mapOf(
        CodeReviewStatus.OPEN to "\u672A\u5F00\u59CB",
        CodeReviewStatus.IN_REVIEW to "\u8D70\u67E5\u4E2D",
        CodeReviewStatus.DONE to "\u5DF2\u5B8C\u6210",
        CodeReviewStatus.ARCHIVED to "\u5DF2\u5F52\u6863"
    )

    fun noteType(type: NoteType): String = noteTypeLabels.getValue(type)

    fun noteType(value: String): String = noteTypeCode(value)
        ?.let { noteType(it) }
        ?: value

    fun noteTypeCode(value: String): NoteType? = enumCode(value, NoteType.entries, noteTypeLabels)

    fun priority(priority: TodoPriority): String = priorityLabels.getValue(priority)

    fun priority(value: String): String = priorityCode(value)
        ?.let { priority(it) }
        ?: value

    fun priorityCode(value: String): TodoPriority? = enumCode(value, TodoPriority.entries, priorityLabels)

    fun status(status: TodoStatus): String = statusLabels.getValue(status)

    fun status(value: String): String = statusCode(value)
        ?.let { status(it) }
        ?: value

    fun statusCode(value: String): TodoStatus? = enumCode(value, TodoStatus.entries, statusLabels)

    fun reviewStatus(status: CodeReviewStatus): String = reviewStatusLabels.getValue(status)

    fun reviewStatus(value: String): String = reviewStatusCode(value)
        ?.let { reviewStatus(it) }
        ?: value

    fun reviewStatusCode(value: String): CodeReviewStatus? = enumCode(value, CodeReviewStatus.entries, reviewStatusLabels)

    private fun <T : Enum<T>> enumCode(value: String, entries: List<T>, labels: Map<T, String>): T? {
        val normalized = value.trim()
        if (normalized.isBlank()) return null
        entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }?.let { return it }
        return labels.entries.firstOrNull { it.value == normalized }?.key
    }
}

