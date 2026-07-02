package com.codenotes.plugin.state

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
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

    @XCollection(style = XCollection.Style.v2)
    var codeReviews: MutableList<CodeReviewEntity> = mutableListOf()

    @XCollection(style = XCollection.Style.v2)
    var codeReviewIssues: MutableList<CodeReviewIssueEntity> = mutableListOf()
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

    fun replaceAll(
        notes: List<NoteEntity>,
        folders: List<NoteFolder> = state.folders,
        codeReviews: List<CodeReviewEntity> = state.codeReviews,
        codeReviewIssues: List<CodeReviewIssueEntity> = state.codeReviewIssues
    ) {
        state.notes = notes.toMutableList()
        state.folders = folders.toMutableList()
        state.codeReviews = codeReviews.toMutableList()
        state.codeReviewIssues = codeReviewIssues.toMutableList()
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

    fun getCodeReviews(): List<CodeReviewEntity> = state.codeReviews.toList()

    fun getCodeReviewIssues(reviewId: String): List<CodeReviewIssueEntity> =
        state.codeReviewIssues.filter { it.reviewId == reviewId }

    fun getAllCodeReviewIssues(): List<CodeReviewIssueEntity> = state.codeReviewIssues.toList()

    fun addCodeReview(review: CodeReviewEntity) {
        state.codeReviews.add(review)
    }

    fun updateCodeReview(review: CodeReviewEntity) {
        val idx = state.codeReviews.indexOfFirst { it.id == review.id }
        if (idx >= 0) {
            review.updatedAt = System.currentTimeMillis()
            state.codeReviews[idx] = review
        }
    }

    fun deleteCodeReview(id: String) {
        state.codeReviews.removeIf { it.id == id }
        state.codeReviewIssues.removeIf { it.reviewId == id }
    }

    fun findCodeReviewById(id: String): CodeReviewEntity? =
        state.codeReviews.firstOrNull { it.id == id }

    fun addCodeReviewIssue(issue: CodeReviewIssueEntity) {
        state.codeReviewIssues.add(issue)
    }

    fun updateCodeReviewIssue(issue: CodeReviewIssueEntity) {
        val idx = state.codeReviewIssues.indexOfFirst { it.id == issue.id }
        if (idx >= 0) {
            issue.updatedAt = System.currentTimeMillis()
            state.codeReviewIssues[idx] = issue
        }
    }

    fun deleteCodeReviewIssue(id: String) {
        state.codeReviewIssues.removeIf { it.id == id }
    }

    fun findCodeReviewIssueById(id: String): CodeReviewIssueEntity? =
        state.codeReviewIssues.firstOrNull { it.id == id }

    companion object {
        fun getInstance(project: Project): NoteStorageService = project.service()
    }
}
