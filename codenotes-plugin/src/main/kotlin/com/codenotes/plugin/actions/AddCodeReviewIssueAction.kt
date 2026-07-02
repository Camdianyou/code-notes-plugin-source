package com.codenotes.plugin.actions

import com.codenotes.plugin.anchor.SymbolAnchorService
import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteScope
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.repository.CodeReviewRepository
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.review.CodeReviewIssueFactory
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
import java.text.SimpleDateFormat
import java.util.Date

class AddCodeReviewIssueAction : AnAction(
    { CodeNotesBundle.message("action.addReviewIssue.text") },
    { CodeNotesBundle.message("action.addReviewIssue.description") },
    AllIcons.General.BalloonInformation
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.getData(CommonDataKeys.EDITOR) != null
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor: Editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val file: VirtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val note = createAnchoredNote(project, editor, file)
        val dialog = NoteEditorDialog(project, null)
        if (!dialog.showAndGet()) return
        dialog.applyTo(note)
        if (note.type == NoteType.COMMENT.name) note.type = NoteType.REVIEW.name

        NoteRepository.getInstance(project).add(note)
        val reviewRepository = CodeReviewRepository.getInstance(project)
        val review = reviewRepository.latestActiveReview() ?: createDefaultReview(reviewRepository)
        reviewRepository.addIssue(CodeReviewIssueFactory.fromNote(review.id, note))

        NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeNotes.Notifications")
            .createNotification(CodeNotesBundle.message("notification.reviewIssueAdded"), NotificationType.INFORMATION)
            .notify(project)
    }

    private fun createAnchoredNote(project: com.intellij.openapi.project.Project, editor: Editor, file: VirtualFile): NoteEntity {
        val selectionModel = editor.selectionModel
        val document = editor.document
        val hasSelection = selectionModel.hasSelection()
        val startLine = if (hasSelection) document.getLineNumber(selectionModel.selectionStart) else editor.caretModel.logicalPosition.line
        val endLine = if (hasSelection) document.getLineNumber(selectionModel.selectionEnd) else startLine
        val anchorText = document.getText(TextRange(document.getLineStartOffset(startLine), document.getLineEndOffset(endLine)))

        return NoteEntity().apply {
            filePath = AnchorUtil.relativePath(project, file)
            lineStart = startLine
            lineEnd = endLine
            textHash = AnchorUtil.hashOf(anchorText)
            fallbackLine = startLine
            fallbackTextHash = textHash
            scope = if (hasSelection) NoteScope.SELECTION.name else NoteScope.LINE.name
            anchorType = if (hasSelection) NoteAnchor.SELECTION.name else NoteAnchor.LINE.name
            type = NoteType.REVIEW.name
            author = System.getProperty("user.name") ?: ""
            SymbolAnchorService.createAnchor(project, editor, file)?.let { anchor ->
                anchorType = NoteAnchor.SYMBOL.name
                scope = when (anchor.symbolKind) {
                    "CLASS" -> NoteScope.CLASS.name
                    "METHOD" -> NoteScope.METHOD.name
                    "FIELD" -> NoteScope.FIELD.name
                    else -> scope
                }
                symbolLanguage = anchor.language
                symbolKind = anchor.symbolKind
                symbolQualifiedName = anchor.qualifiedName
                symbolSignature = anchor.signature
                fallbackLine = anchor.fallbackLine
                fallbackTextHash = anchor.fallbackHash
            }
        }
    }

    private fun createDefaultReview(repository: CodeReviewRepository): CodeReviewEntity {
        val today = SimpleDateFormat("yyyy-MM-dd").format(Date())
        val user = System.getProperty("user.name") ?: ""
        val review = CodeReviewEntity().apply {
            meetingName = CodeNotesBundle.message("review.default.meetingName")
            meetingDate = today
            host = user
            recorder = user
            topic = CodeNotesBundle.message("review.default.topic")
        }
        repository.addReview(review)
        return review
    }
}
