package com.codenotes.plugin.repository

import com.codenotes.plugin.events.NoteChangeBus
import com.codenotes.plugin.events.NoteChangeEvent
import com.codenotes.plugin.events.NoteChangeKind
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteFolder
import com.codenotes.plugin.model.NoteQuery
import com.codenotes.plugin.state.NoteStorageService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class NoteRepository(private val project: Project) {

    private val storage: NoteStorageService
        get() = NoteStorageService.getInstance(project)

    fun allNotes(): List<NoteEntity> = storage.getAllNotes()

    fun query(query: NoteQuery): List<NoteEntity> = query.applyTo(storage.getAllNotes())

    fun notesForFile(projectRelativePath: String): List<NoteEntity> =
        storage.getNotesForFile(projectRelativePath)

    fun findById(id: String): NoteEntity? = storage.findById(id)

    fun folders(): List<NoteFolder> = storage.getFolders()

    fun addFolder(folder: NoteFolder) {
        storage.addFolder(folder)
        publish(NoteChangeKind.REFRESHED)
    }

    fun add(note: NoteEntity) {
        val now = System.currentTimeMillis()
        note.createdAt = now
        note.updatedAt = now
        storage.addNote(note)
        publish(NoteChangeKind.ADDED, note)
    }

    fun update(note: NoteEntity) {
        note.updatedAt = System.currentTimeMillis()
        storage.updateNote(note)
        publish(NoteChangeKind.UPDATED, note)
    }

    fun delete(id: String) {
        val note = storage.findById(id)
        storage.deleteNote(id)
        publish(NoteChangeKind.DELETED, note, id)
    }

    fun replaceAll(
        notes: List<NoteEntity>,
        folders: List<NoteFolder>,
        codeReviews: List<com.codenotes.plugin.model.CodeReviewEntity> = storage.getCodeReviews(),
        codeReviewIssues: List<com.codenotes.plugin.model.CodeReviewIssueEntity> = storage.getAllCodeReviewIssues()
    ) {
        storage.replaceAll(notes, folders, codeReviews, codeReviewIssues)
        publish(NoteChangeKind.IMPORTED)
    }

    private fun publish(kind: NoteChangeKind, note: NoteEntity? = null, noteId: String = "") {
        project.messageBus.syncPublisher(NoteChangeBus.TOPIC)
            .notesChanged(NoteChangeEvent(kind, note?.id ?: noteId, note?.filePath ?: ""))
    }

    companion object {
        fun getInstance(project: Project): NoteRepository = project.service()
    }
}
