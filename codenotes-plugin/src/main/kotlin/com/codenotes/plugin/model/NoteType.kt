package com.codenotes.plugin.model

/**
 * The kind of knowledge a note represents. Kept as a flat enum (rather than
 * a hierarchy) so it stays trivial to persist with IntelliJ's XmlSerializer
 * and trivial to extend later (new phases can just add new constants).
 */
enum class NoteType(val icon: String) {
    COMMENT("💬"),
    BUG("🐞"),
    QUESTION("❓"),
    OPTIMIZATION("⚡"),
    REVIEW("🔍"),
    WARNING("⚠️"),
    IMPORTANT("❗"),
    ARCHITECTURE("🏛"),
    TEMPORARY("⏳"),
    PERMANENT("📌"),
    TODO("✅"),
    DECISION("📋");

    companion object {
        fun safeValueOf(name: String?): NoteType =
            entries.firstOrNull { it.name == name } ?: COMMENT
    }
}

enum class TodoPriority { LOW, MEDIUM, HIGH, CRITICAL }

enum class TodoStatus { TODO, DOING, BLOCKED, WAITING, DONE, ARCHIVED }
