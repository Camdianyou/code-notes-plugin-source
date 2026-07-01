package com.codenotes.plugin.ui

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.util.CodeNotesBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.Dimension
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Single rich-ish editor dialog covering title/type/summary/description/tags
 * and, when the type is TODO, priority/status/due-date. This intentionally
 * uses a plain text area for the description (Markdown source) rather than a
 * full WYSIWYG/Mermaid/LaTeX renderer — that richer editor experience from
 * the spec is a larger, separable Phase-2 component (a dedicated editor
 * panel/FileEditor) that can replace this text area without touching the
 * storage or action layers.
 */
class NoteEditorDialog(
    project: Project,
    private val existing: NoteEntity?
) : DialogWrapper(project, true) {

    private val typeCombo = JComboBox(NoteType.entries.toTypedArray())
    private val titleField = JBTextField()
    private val summaryField = JBTextField()
    private val descriptionArea = JBTextArea(8, 50)
    private val tagsField = JBTextField()
    private val priorityCombo = JComboBox(TodoPriority.entries.toTypedArray())
    private val statusCombo = JComboBox(TodoStatus.entries.toTypedArray())
    private val dueDateField = JBTextField()

    init {
        title = if (existing == null) CodeNotesBundle.message("dialog.title.new")
                else CodeNotesBundle.message("dialog.title.edit")
        existing?.let {
            typeCombo.selectedItem = NoteType.safeValueOf(it.type)
            titleField.text = it.title
            summaryField.text = it.summary
            descriptionArea.text = it.description
            tagsField.text = it.tags
            priorityCombo.selectedItem = runCatching { TodoPriority.valueOf(it.priority) }.getOrDefault(TodoPriority.MEDIUM)
            statusCombo.selectedItem = runCatching { TodoStatus.valueOf(it.status) }.getOrDefault(TodoStatus.TODO)
            dueDateField.text = it.dueDate
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        val scrollPane = JBScrollPane(descriptionArea)
        scrollPane.preferredSize = Dimension(500, 160)

        val panel: JPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.type"), typeCombo)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.title"), titleField)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.summary"), summaryField)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.description"), scrollPane)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.tags"), tagsField)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.priority"), priorityCombo)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.status"), statusCombo)
            .addLabeledComponent(CodeNotesBundle.message("dialog.field.dueDate"), dueDateField)
            .panel
        return panel
    }

    fun applyTo(note: NoteEntity) {
        note.type = (typeCombo.selectedItem as NoteType).name
        note.title = titleField.text.trim()
        note.summary = summaryField.text.trim()
        note.description = descriptionArea.text
        note.tags = tagsField.text.trim()
        note.priority = (priorityCombo.selectedItem as TodoPriority).name
        note.status = (statusCombo.selectedItem as TodoStatus).name
        note.dueDate = dueDateField.text.trim()
    }
}
