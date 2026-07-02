package com.codenotes.plugin.actions

import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteScope
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.util.AnchorUtil
import com.codenotes.plugin.util.CodeNotesBundle
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.TextRange
import java.awt.datatransfer.DataFlavor

class AddClipboardNoteAction : AnAction(
    { CodeNotesBundle.message("action.addClipboardNote.text") },
    { CodeNotesBundle.message("action.addClipboardNote.description") },
    AllIcons.Actions.MenuPaste
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible =
            e.project != null &&
                e.getData(CommonDataKeys.EDITOR) != null &&
                e.getData(CommonDataKeys.VIRTUAL_FILE) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val text: String = CopyPasteManager.getInstance()
            .getContents<String>(DataFlavor.stringFlavor)
            ?.takeIf { it.isNotBlank() }
            ?: return

        val document = editor.document
        val line = editor.caretModel.logicalPosition.line
        val lineText = document.getText(TextRange(document.getLineStartOffset(line), document.getLineEndOffset(line)))
        val note = NoteEntity().apply {
            filePath = AnchorUtil.relativePath(project, file)
            lineStart = line
            lineEnd = line
            textHash = AnchorUtil.hashOf(lineText)
            fallbackLine = line
            fallbackTextHash = textHash
            anchorType = NoteAnchor.LINE.name
            scope = NoteScope.LINE.name
            type = NoteType.COMMENT.name
            title = text.lineSequence().firstOrNull()?.take(80).orEmpty()
            description = text
            author = System.getProperty("user.name") ?: ""
        }
        NoteRepository.getInstance(project).add(note)
        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeNotes.Notifications")
            .createNotification(CodeNotesBundle.message("notification.noteAdded"), NotificationType.INFORMATION)
            .notify(project)
    }
}
