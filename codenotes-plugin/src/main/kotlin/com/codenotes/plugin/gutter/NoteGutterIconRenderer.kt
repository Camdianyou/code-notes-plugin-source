package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.state.NoteStorageService
import com.codenotes.plugin.ui.NoteEditorDialog
import com.codenotes.plugin.util.CodeNotesBundle
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import javax.swing.Icon

class NoteGutterIconRenderer(
    private val project: Project,
    private val noteId: String
) : GutterIconRenderer() {

    override fun getIcon(): Icon = AllIcons.General.BalloonInformation

    override fun getTooltipText(): String {
        val note = NoteStorageService.getInstance(project).findById(noteId) ?: return ""
        val typeLabel = NoteType.safeValueOf(note.type).icon
        val title = note.title.ifBlank { "(untitled)" }
        return "${CodeNotesBundle.message("gutter.tooltip.prefix")} $typeLabel $title\n${note.summary}"
    }

    override fun isNavigateAction(): Boolean = true

    override fun getClickAction() = object : com.intellij.openapi.actionSystem.AnAction() {
        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
            val note: NoteEntity = NoteStorageService.getInstance(project).findById(noteId) ?: return
            val dialog = NoteEditorDialog(project, note)
            if (dialog.showAndGet()) {
                dialog.applyTo(note)
                NoteStorageService.getInstance(project).updateNote(note)
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is NoteGutterIconRenderer && other.noteId == noteId
    override fun hashCode(): Int = noteId.hashCode()
}
