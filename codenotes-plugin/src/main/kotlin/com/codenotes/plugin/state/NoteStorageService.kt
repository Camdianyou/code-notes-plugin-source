package com.codenotes.plugin.state

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteFolder
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.util.xmlb.annotations.XCollection

/**
 * Simple bean wrapper required by PersistentStateComponent — a List of beans
 * needs to live inside another bean field for reliable XML (de)serialization.
 */
class NoteStorageState {
    @XCollection(style = XCollection.Style.v2)
    var notes: MutableList<NoteEntity> = mutableListOf()

    @XCollection(style = XCollection.Style.v2)
    var folders: MutableList<NoteFolder> = mutableListOf()
}

/**
 * Stores all notes for a project in `<project>/.idea/codeNotes.xml`.
 *
 * This is intentionally the simplest possible persistence backend (matching
 * the spec's ".idea / JSON / XML" local storage option). SQLite-backed,
 * cloud, and git-friendly Markdown export/import backends are natural
 * Phase-2/3 additions behind the same NoteRepository-style API surface —
 * kept small here on purpose so swapping backends later doesn't require
 * touching UI or action code.
 */
@Service(Service.Level.PROJECT)
@State(
    name = "CodeNotesStorage",
    storages = [Storage("codeNotes.xml")]
)
class NoteStorageService : PersistentStateComponent<NoteStorageState> {

    private var state = NoteStorageState()

    override fun getState(): NoteStorageState = state

    override fun loadState(state: NoteStorageState) {
        XmlSerializerUtil.copyBean(state, this.state)
    }

    fun getAllNotes(): List<NoteEntity> = state.notes.toList()

    fun replaceAll(notes: List<NoteEntity>, folders: List<NoteFolder> = state.folders) {
        state.notes = notes.toMutableList()
        state.folders = folders.toMutableList()
    }

    fun getFolders(): List<NoteFolder> = state.folders.toList()

    fun addFolder(folder: NoteFolder) {
        state.folders.add(folder)
    }

    fun getNotesForFile(projectRelativePath: String): List<NoteEntity> =
        state.notes.filter { it.filePath == projectRelativePath }

    fun addNote(note: NoteEntity) {
        state.notes.add(note)
    }

    fun updateNote(note: NoteEntity) {
        val idx = state.notes.indexOfFirst { it.id == note.id }
        if (idx >= 0) {
            note.updatedAt = System.currentTimeMillis()
            state.notes[idx] = note
        }
    }

    fun deleteNote(id: String) {
        state.notes.removeIf { it.id == id }
    }

    fun findById(id: String): NoteEntity? = state.notes.firstOrNull { it.id == id }

    companion object {
        fun getInstance(project: Project): NoteStorageService = project.service()
    }
}
