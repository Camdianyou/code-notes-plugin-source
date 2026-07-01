package com.codenotes.plugin.actions

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteScope
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.state.NoteStorageService
import com.codenotes.plugin.ui.NoteEditorDialog
import com.codenotes.plugin.util.AnchorUtil
import com.codenotes.plugin.util.CodeNotesBundle
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile

class AddNoteAction : AnAction(
    { CodeNotesBundle.message("action.addNote.text") },
    { CodeNotesBundle.message("action.addNote.description") },
    AllIcons.General.BalloonInformation
) {

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        e.presentation.isEnabledAndVisible = editor != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        val selectionModel = editor.selectionModel
        val document = editor.document

        val hasSelection = selectionModel.hasSelection()
        val startLine = if (hasSelection) document.getLineNumber(selectionModel.selectionStart)
                         else editor.caretModel.logicalPosition.line
        val endLine = if (hasSelection) document.getLineNumber(selectionModel.selectionEnd)
                      else startLine

        val anchorText = document.getText(
            TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(endLine))
        )

        val note = NoteEntity().apply {
            filePath = AnchorUtil.relativePath(project, file)
            lineStart = startLine
            lineEnd = endLine
            textHash = AnchorUtil.hashOf(anchorText)
            scope = if (hasSelection) NoteScope.SELECTION.name else NoteScope.LINE.name
            type = NoteType.COMMENT.name
            author = System.getProperty("user.name") ?: ""
        }

        val dialog = NoteEditorDialog(project, null)
        if (dialog.showAndGet()) {
            dialog.applyTo(note)
            NoteStorageService.getInstance(project).addNote(note)
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeNotes.Notifications")
                .createNotification(CodeNotesBundle.message("notification.noteAdded"), NotificationType.INFORMATION)
                .notify(project)
        }
    }
}
