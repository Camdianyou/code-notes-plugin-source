package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.ui.NoteEditorDialog
import com.codenotes.plugin.util.CodeNotesBundle
import com.codenotes.plugin.util.CodeNotesIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import javax.swing.Icon

class NoteGutterIconRenderer(
    private val project: Project,
    private val noteId: String
) : GutterIconRenderer() {

    override fun getIcon(): Icon = CodeNotesIcons.GutterNote

    override fun getTooltipText(): String {
        val note = NoteRepository.getInstance(project).findById(noteId) ?: return ""
        val typeLabel = NoteType.safeValueOf(note.type).icon
        val title = note.title.ifBlank { "(untitled)" }
        return "${CodeNotesBundle.message("gutter.tooltip.prefix")} $typeLabel $title\n${note.summary}"
    }

    override fun isNavigateAction(): Boolean = true

    override fun getClickAction() = object : com.intellij.openapi.actionSystem.AnAction() {
        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
            val note: NoteEntity = NoteRepository.getInstance(project).findById(noteId) ?: return
            val dialog = NoteEditorDialog(project, note)
            if (dialog.showAndGet()) {
                dialog.applyTo(note)
                NoteRepository.getInstance(project).update(note)
            }
        }
    }

    override fun equals(other: Any?): Boolean = other is NoteGutterIconRenderer && other.noteId == noteId
    override fun hashCode(): Int = noteId.hashCode()
}
