package com.codenotes.plugin.ui

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.review.CodeReviewDefaults
import com.codenotes.plugin.review.CodeReviewValidationResult
import com.codenotes.plugin.util.CodeNotesBundle
import com.codenotes.plugin.util.LocalizedEnumLabels
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTable
import javax.swing.table.DefaultTableModel

class CodeReviewExportDialog(
    project: Project,
    private val review: CodeReviewEntity,
    private val issues: List<CodeReviewIssueEntity>,
    validation: CodeReviewValidationResult
) : DialogWrapper(project) {

    private val meetingNameField = JBTextField(review.meetingName)
    private val meetingDateField = JBTextField(review.meetingDate)
    private val hostField = JBTextField(review.host)
    private val recorderField = JBTextField(review.recorder)
    private val attendeesField = JBTextField(review.attendees)
    private val topicField = JBTextField(review.topic)
    private val issueModel = DefaultTableModel(
        arrayOf(
            CodeNotesBundle.message("review.export.issue.id"),
            CodeNotesBundle.message("review.field.issueTitle"),
            CodeNotesBundle.message("review.field.description"),
            CodeNotesBundle.message("review.field.issueType"),
            CodeNotesBundle.message("review.field.severity"),
            CodeNotesBundle.message("review.field.status"),
            CodeNotesBundle.message("review.field.owner"),
            CodeNotesBundle.message("review.field.suggestion")
        ),
        0
    )
    private val issueTable = JTable(issueModel)

    init {
        title = CodeNotesBundle.message("review.export.dialog.title")
        validation.missingIssueFields.keys
            .mapNotNull { issueId -> issues.firstOrNull { it.id == issueId } }
            .forEach { issue ->
                CodeReviewDefaults.normalizeIssueDefaults(issue)
                issueModel.addRow(
                    arrayOf(
                        issue.id,
                        issue.title,
                        issue.description,
                        LocalizedEnumLabels.noteType(issue.issueType),
                        LocalizedEnumLabels.priority(issue.severity),
                        LocalizedEnumLabels.status(issue.status),
                        issue.owner,
                        issue.suggestion
                    )
                )
            }
        issueTable.preferredScrollableViewportSize = Dimension(760, 160)
        issueTable.removeColumn(issueTable.columnModel.getColumn(0))
        init()
    }

    override fun createCenterPanel(): JComponent {
        val reviewPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(CodeNotesBundle.message("review.field.meetingName"), meetingNameField)
            .addLabeledComponent(CodeNotesBundle.message("review.field.meetingDate"), meetingDateField)
            .addLabeledComponent(CodeNotesBundle.message("review.field.host"), hostField)
            .addLabeledComponent(CodeNotesBundle.message("review.field.recorder"), recorderField)
            .addLabeledComponent(CodeNotesBundle.message("review.field.attendees"), attendeesField)
            .addLabeledComponent(CodeNotesBundle.message("review.field.topic"), topicField)
            .panel

        val panel = JPanel(BorderLayout(0, 8))
        panel.add(reviewPanel, BorderLayout.NORTH)
        panel.add(JLabel(CodeNotesBundle.message("review.export.dialog.issueHint")), BorderLayout.CENTER)
        panel.add(JBScrollPane(issueTable), BorderLayout.SOUTH)
        return panel
    }

    fun applyToEntities() {
        review.meetingName = meetingNameField.text.trim()
        review.meetingDate = meetingDateField.text.trim()
        review.host = hostField.text.trim()
        review.recorder = recorderField.text.trim()
        review.attendees = attendeesField.text.trim()
        review.topic = topicField.text.trim()
        for (row in 0 until issueModel.rowCount) {
            val issueId = issueModel.getValueAt(row, 0).toString()
            val issue = issues.firstOrNull { it.id == issueId } ?: continue
            issue.title = issueModel.getValueAt(row, 1)?.toString()?.trim().orEmpty()
            issue.description = issueModel.getValueAt(row, 2)?.toString()?.trim().orEmpty()
            issue.issueType = (LocalizedEnumLabels.noteTypeCode(issueModel.getValueAt(row, 3)?.toString().orEmpty()) ?: NoteType.REVIEW).name
            issue.severity = (LocalizedEnumLabels.priorityCode(issueModel.getValueAt(row, 4)?.toString().orEmpty()) ?: TodoPriority.MEDIUM).name
            issue.status = (LocalizedEnumLabels.statusCode(issueModel.getValueAt(row, 5)?.toString().orEmpty()) ?: TodoStatus.TODO).name
            issue.owner = issueModel.getValueAt(row, 6)?.toString()?.trim().orEmpty()
            issue.suggestion = issueModel.getValueAt(row, 7)?.toString()?.trim().orEmpty()
        }
    }
}
