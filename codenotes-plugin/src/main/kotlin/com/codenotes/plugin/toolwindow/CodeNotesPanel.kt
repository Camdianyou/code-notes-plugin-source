package com.codenotes.plugin.toolwindow

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.state.NoteStorageService
import com.codenotes.plugin.ui.NoteEditorDialog
import com.codenotes.plugin.util.AnchorUtil
import com.codenotes.plugin.util.CodeNotesBundle
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

class CodeNotesPanel(private val project: Project) : JPanel(BorderLayout()) {

    private val tableModel = NotesTableModel()
    private val table = JBTable(tableModel)
    private val searchField = SearchTextField()

    init {
        searchField.textEditor.emptyText.text = CodeNotesBundle.message("panel.search.placeholder")
        searchField.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: javax.swing.event.DocumentEvent) = refresh()
        })

        table.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        table.setShowGrid(false)
        table.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) navigateToSelected()
            }
            override fun mousePressed(e: MouseEvent) = maybeShowPopup(e)
            override fun mouseReleased(e: MouseEvent) = maybeShowPopup(e)
        })

        val top = JPanel(BorderLayout())
        top.add(searchField, BorderLayout.CENTER)

        add(top, BorderLayout.NORTH)
        add(JBScrollPane(table), BorderLayout.CENTER)

        refresh()
    }

    fun refresh() {
        val all = NoteStorageService.getInstance(project).getAllNotes()
        val query = searchField.text.trim().lowercase()
        tableModel.notes = if (query.isEmpty()) {
            all.sortedByDescending { it.updatedAt }
        } else {
            all.filter {
                it.title.lowercase().contains(query) ||
                    it.summary.lowercase().contains(query) ||
                    it.description.lowercase().contains(query) ||
                    it.tags.lowercase().contains(query) ||
                    it.filePath.lowercase().contains(query)
            }.sortedByDescending { it.updatedAt }
        }
    }

    private fun maybeShowPopup(e: MouseEvent) {
        if (!e.isPopupTrigger) return
        val row = table.rowAtPoint(e.point)
        if (row < 0) return
        table.setRowSelectionInterval(row, row)
        val note = tableModel.noteAt(row) ?: return

        val group = DefaultActionGroup()
        group.add(object : com.intellij.openapi.actionSystem.AnAction(CodeNotesBundle.message("action.editNote.text")) {
            override fun actionPerformed(ev: com.intellij.openapi.actionSystem.AnActionEvent) = editNote(note)
        })
        group.add(object : com.intellij.openapi.actionSystem.AnAction(CodeNotesBundle.message("action.deleteNote.text")) {
            override fun actionPerformed(ev: com.intellij.openapi.actionSystem.AnActionEvent) = deleteNote(note)
        })

        val popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.TOOLWINDOW_POPUP, group)
        popupMenu.component.show(e.component, e.x, e.y)
    }

    private fun editNote(note: NoteEntity) {
        val dialog = NoteEditorDialog(project, note)
        if (dialog.showAndGet()) {
            dialog.applyTo(note)
            NoteStorageService.getInstance(project).updateNote(note)
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeNotes.Notifications")
                .createNotification(CodeNotesBundle.message("notification.noteUpdated"), NotificationType.INFORMATION)
                .notify(project)
            refresh()
        }
    }

    private fun deleteNote(note: NoteEntity) {
        NoteStorageService.getInstance(project).deleteNote(note.id)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeNotes.Notifications")
            .createNotification(CodeNotesBundle.message("notification.noteDeleted"), NotificationType.INFORMATION)
            .notify(project)
        refresh()
    }

    private fun navigateToSelected() {
        val row = table.selectedRow
        val note = tableModel.noteAt(row) ?: return
        if (note.filePath.isBlank()) return

        val basePath = project.basePath ?: return
        val vFile = LocalFileSystem.getInstance().findFileByPath("$basePath/${note.filePath}") ?: return

        var line = note.lineStart.coerceAtLeast(0)
        // Open the file first so we have a live Document to relocate the anchor against.
        FileEditorManager.getInstance(project).openFile(vFile, true)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor != null) {
            line = AnchorUtil.relocateLine(editor.document, note.lineStart, note.textHash)
        }

        val descriptor = OpenFileDescriptor(project, vFile, line, 0)
        SwingUtilities.invokeLater { descriptor.navigate(true) }
    }
}
