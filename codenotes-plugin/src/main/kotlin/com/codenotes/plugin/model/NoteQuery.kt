package com.codenotes.plugin.model

class NoteQuery(
    var text: String = "",
    var folderId: String = "",
    var tags: Set<String> = emptySet(),
    var types: Set<String> = emptySet(),
    var statuses: Set<String> = emptySet(),
    var anchorTypes: Set<String> = emptySet(),
    var favoritesOnly: Boolean = false
) {
    fun applyTo(notes: List<NoteEntity>): List<NoteEntity> {
        val normalizedText = text.trim().lowercase()
        val normalizedTags = tags.map { it.trim().lowercase() }.filter { it.isNotBlank() }.toSet()
        return notes.asSequence()
            .filter { note -> normalizedText.isBlank() || note.matchesText(normalizedText) }
            .filter { note -> folderId.isBlank() || note.folderId == folderId }
            .filter { note -> normalizedTags.isEmpty() || normalizedTags.all { it in note.tagSet() } }
            .filter { note -> types.isEmpty() || note.type in types }
            .filter { note -> statuses.isEmpty() || note.status in statuses }
            .filter { note -> anchorTypes.isEmpty() || note.anchorType in anchorTypes }
            .filter { note -> !favoritesOnly || note.favorite }
            .sortedWith(compareByDescending<NoteEntity> { it.favorite }.thenByDescending { it.updatedAt })
            .toList()
    }

    private fun NoteEntity.matchesText(query: String): Boolean =
        title.lowercase().contains(query) ||
            summary.lowercase().contains(query) ||
            description.lowercase().contains(query) ||
            tags.lowercase().contains(query) ||
            filePath.lowercase().contains(query) ||
            symbolQualifiedName.lowercase().contains(query)

    private fun NoteEntity.tagSet(): Set<String> =
        tags.split(',')
            .map { it.trim().lowercase() }
            .filter { it.isNotBlank() }
            .toSet()
}

enum class TaskViewFilter { ALL, TODO, DOING, BLOCKED, WAITING, DONE, ARCHIVED }
