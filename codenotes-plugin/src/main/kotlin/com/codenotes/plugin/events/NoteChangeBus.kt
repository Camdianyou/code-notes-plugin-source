package com.codenotes.plugin.events

import com.intellij.util.messages.Topic

enum class NoteChangeKind { ADDED, UPDATED, DELETED, IMPORTED, REFRESHED }

class NoteChangeEvent(
    val kind: NoteChangeKind,
    val noteId: String = "",
    val filePath: String = ""
)

interface NoteChangeListener {
    fun notesChanged(event: NoteChangeEvent)
}

object NoteChangeBus {
    val TOPIC: Topic<NoteChangeListener> =
        Topic.create("Code Notes changes", NoteChangeListener::class.java)
}
