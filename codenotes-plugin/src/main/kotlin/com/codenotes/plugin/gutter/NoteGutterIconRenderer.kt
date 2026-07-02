package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.repository.CodeReviewRepository
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.ui.NoteEditorDialog
import com.codenotes.plugin.util.CodeNotesBundle
import com.codenotes.plugin.util.CodeNotesIcons
import com.codenotes.plugin.util.LocalizedEnumLabels
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.project.Project
import javax.swing.Icon

class NoteGutterIconRenderer(
    private val project: Project,
    private val marker: CodeAnchorMarker
) : GutterIconRenderer() {

    override fun getIcon(): Icon = iconForPriority(marker.priority)

    override fun getTooltipText(): String {
        val typeLabel = CodeAnchorMarker.typeIcon(marker.type)
        val title = marker.title.ifBlank { "(untitled)" }
        val priority = LocalizedEnumLabels.priority(priorityFor(marker.priority))
        val prefix = when (marker.kind) {
            CodeAnchorMarkerKind.NOTE -> CodeNotesBundle.message("gutter.tooltip.prefix")
            CodeAnchorMarkerKind.REVIEW_ISSUE -> "\u8D70\u67E5\u95EE\u9898\uFF1A"
        }
        return "$prefix $typeLabel $title / $priority\n${marker.summary}"
    }

    override fun isNavigateAction(): Boolean = true

    override fun getClickAction() = object : com.intellij.openapi.actionSystem.AnAction() {
        override fun actionPerformed(e: com.intellij.openapi.actionSystem.AnActionEvent) {
            when (marker.kind) {
                CodeAnchorMarkerKind.NOTE -> {
                    val note = NoteRepository.getInstance(project).findById(marker.id) ?: return
                    val dialog = NoteEditorDialog(project, note)
                    if (dialog.showAndGet()) {
                        dialog.applyTo(note)
                        NoteRepository.getInstance(project).update(note)
                    }
                }
                CodeAnchorMarkerKind.REVIEW_ISSUE -> {
                    CodeReviewRepository.getInstance(project).findIssue(marker.id) ?: return
                    com.intellij.openapi.wm.ToolWindowManager.getInstance(project)
                        .getToolWindow("Code Notes")
                        ?.activate(null)
                }
            }
        }
    }

    override fun equals(other: Any?): Boolean =
        other is NoteGutterIconRenderer && other.marker.id == marker.id && other.marker.kind == marker.kind

    override fun hashCode(): Int = 31 * marker.kind.hashCode() + marker.id.hashCode()

    companion object {
        fun iconKeyForPriority(priority: String): String = priorityFor(priority).name

        fun iconForPriority(priority: String): Icon =
            when (priorityFor(priority)) {
                TodoPriority.LOW -> CodeNotesIcons.GutterLow
                TodoPriority.MEDIUM -> CodeNotesIcons.GutterMedium
                TodoPriority.HIGH -> CodeNotesIcons.GutterHigh
                TodoPriority.CRITICAL -> CodeNotesIcons.GutterCritical
            }

        private fun priorityFor(priority: String): TodoPriority =
            LocalizedEnumLabels.priorityCode(priority) ?: TodoPriority.MEDIUM
    }
}
