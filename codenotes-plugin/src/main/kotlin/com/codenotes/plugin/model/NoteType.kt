package com.codenotes.plugin.model

/**
 * The kind of knowledge a note represents. Internal enum names stay stable for
 * XML compatibility; user-facing labels are provided by LocalizedEnumLabels.
 */
enum class NoteType(val icon: String) {
    COMMENT("\u8BC4"),
    BUG("\u7F3A"),
    QUESTION("\u95EE"),
    OPTIMIZATION("\u4F18"),
    REVIEW("\u5BA1"),
    WARNING("\u8B66"),
    IMPORTANT("\u91CD"),
    ARCHITECTURE("\u67B6"),
    TEMPORARY("\u4E34"),
    PERMANENT("\u4E45"),
    TODO("\u5F85"),
    DECISION("\u51B3");

    companion object {
        fun safeValueOf(name: String?): NoteType =
            entries.firstOrNull { it.name.equals(name.orEmpty(), ignoreCase = true) } ?: COMMENT
    }
}

enum class TodoPriority { LOW, MEDIUM, HIGH, CRITICAL }

enum class TodoStatus { TODO, DOING, BLOCKED, WAITING, DONE, ARCHIVED }

