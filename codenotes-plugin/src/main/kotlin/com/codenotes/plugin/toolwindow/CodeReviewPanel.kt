package com.codenotes.plugin.toolwindow

import com.codenotes.plugin.events.CodeReviewChangeBus
import com.codenotes.plugin.events.CodeReviewChangeEvent
import com.codenotes.plugin.events.CodeReviewChangeListener
import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.CodeReviewStatus
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.repository.CodeReviewRepository
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.review.CodeReviewDefaults
import com.codenotes.plugin.review.CodeReviewExportService
import com.codenotes.plugin.review.CodeReviewExportValidator
import com.codenotes.plugin.review.CodeReviewIssueFactory
import com.codenotes.plugin.ui.CodeReviewExportDialog
import com.codenotes.plugin.util.CodeNotesBundle
import com.codenotes.plugin.util.CodeNotesIcons
import com.codenotes.plugin.util.CodeNotesUi
import com.codenotes.plugin.util.LocalizedEnumLabels
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JComboBox
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.text.JTextComponent

class CodeReviewPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val reviewRepository = CodeReviewRepository.getInstance(project)
    private val noteRepository = NoteRepository.getInstance(project)
    private val reviewModel = DefaultListModel<CodeReviewEntity>()
    private val reviewList = JBList(reviewModel)
    private val issueModel = DefaultListModel<CodeReviewIssueEntity>()
    private val issueList = JBList(issueModel)

    private val meetingNameField = JBTextField()
    private val meetingDateField = JBTextField()
    private val locationField = JBTextField()
    private val startTimeField = JBTextField()
    private val endTimeField = JBTextField()
    private val hostField = JBTextField()
    private val recorderField = JBTextField()
    private val attendeesField = JBTextField()
    private val topicField = JBTextField()
    private val sendToField = JBTextField()
    private val copyToField = JBTextField()
    private val scopeArea = JBTextArea()
    private val notesArea = JBTextArea()
    private val reviewStatusCombo = JComboBox(CodeReviewStatus.entries.toTypedArray())

    private val issueTitleField = JBTextField()
    private val issueDescriptionArea = JBTextArea()
    private val issueFileField = JBTextField()
    private val issueLineField = JBTextField()
    private val issueSymbolField = JBTextField()
    private val issueTypeCombo = JComboBox(NoteType.entries.toTypedArray())
    private val severityCombo = JComboBox(TodoPriority.entries.toTypedArray())
    private val issueStatusCombo = JComboBox(TodoStatus.entries.toTypedArray())
    private val ownerField = JBTextField()
    private val dueDateField = JBTextField()
    private val suggestionArea = JBTextArea()
    private val resolutionArea = JBTextArea()
    private val linkedNoteLabel = JLabel("")

    private var loading = false
    private var editingReviewId: String? = null
    private var editingIssueId: String? = null
    private val reviewAutoSaveTimer = Timer(500) { saveSelectedReview() }.apply { isRepeats = false }
    private val issueAutoSaveTimer = Timer(500) { saveSelectedIssue() }.apply { isRepeats = false }

    init {
        project.messageBus.connect(this).subscribe(CodeReviewChangeBus.TOPIC, object : CodeReviewChangeListener {
            override fun codeReviewsChanged(event: CodeReviewChangeEvent) = refreshAll(keepSelection = true)
        })

        reviewList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        reviewList.cellRenderer = ReviewRenderer()
        reviewList.emptyText.text = CodeNotesBundle.message("review.empty.reviews")
        reviewList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                flushReviewAutoSave()
                flushIssueAutoSave()
                loadSelectedReview()
                refreshIssues(keepSelection = false)
            }
        }

        issueList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        issueList.cellRenderer = IssueRenderer()
        issueList.emptyText.text = CodeNotesBundle.message("review.empty.issues")
        issueList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                flushIssueAutoSave()
                loadSelectedIssue()
            }
        }
        issueList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) navigateToSelectedIssue()
            }
        })

        listOf(scopeArea, notesArea, issueDescriptionArea, suggestionArea, resolutionArea).forEach {
            it.lineWrap = true
            it.wrapStyleWord = true
        }
        reviewStatusCombo.renderer = localizedRenderer<CodeReviewStatus> { LocalizedEnumLabels.reviewStatus(it) }
        issueTypeCombo.renderer = localizedRenderer<NoteType> { LocalizedEnumLabels.noteType(it) }
        severityCombo.renderer = localizedRenderer<TodoPriority> { LocalizedEnumLabels.priority(it) }
        issueStatusCombo.renderer = localizedRenderer<TodoStatus> { LocalizedEnumLabels.status(it) }
        installAutoSaveListeners()

        add(toolbar(), BorderLayout.NORTH)
        add(workspace(), BorderLayout.CENTER)
        refreshAll(keepSelection = false)
    }

    private fun toolbar(): JPanel {
        val buttons = CodeNotesUi.toolbarPanel()
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.newReview"), CodeNotesIcons.Meeting, primary = true) { createReview() })
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.newIssue"), CodeNotesIcons.ReviewIssue, primary = true) { createIssue() })
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.export"), CodeNotesIcons.Export, primary = true) { exportSelectedReview() })
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.addNote"), CodeNotesIcons.AddNote) { addNoteIssue() })
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.refresh"), CodeNotesIcons.Refresh) { refreshAndClear() })
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.deleteReview"), CodeNotesIcons.Delete) { deleteSelectedReview() })
        buttons.add(CodeNotesUi.actionButton(CodeNotesBundle.message("review.action.deleteIssue"), CodeNotesIcons.Delete) { deleteSelectedIssue() })

        val panel = JPanel(BorderLayout())
        panel.border = EmptyBorder(6, 8, 6, 8)
        panel.add(buttons, BorderLayout.WEST)
        return panel
    }

    private fun workspace(): JSplitPane {
        val left = JPanel(BorderLayout())
        left.preferredSize = Dimension(240, 300)
        left.add(JBScrollPane(reviewList), BorderLayout.CENTER)

        val middle = JPanel(BorderLayout())
        middle.preferredSize = Dimension(320, 300)
        middle.add(JBScrollPane(issueList), BorderLayout.CENTER)

        val first = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, middle)
        first.resizeWeight = 0.35
        val root = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, first, detailTabs())
        root.resizeWeight = 0.55
        return root
    }

    private fun detailTabs(): JTabbedPane {
        val tabs = JTabbedPane()
        tabs.addTab(CodeNotesBundle.message("review.tab.meeting"), CodeNotesIcons.Meeting, reviewDetailPanel())
        tabs.addTab(CodeNotesBundle.message("review.tab.issue"), CodeNotesIcons.ReviewIssue, issueDetailPanel())
        return tabs
    }

    private fun reviewDetailPanel(): JPanel {
        val fields = JPanel(GridLayout(0, 2, 6, 6))
        fields.add(JLabel(CodeNotesBundle.message("review.field.meetingName"))); fields.add(meetingNameField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.meetingDate"))); fields.add(meetingDateField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.location"))); fields.add(locationField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.startTime"))); fields.add(startTimeField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.endTime"))); fields.add(endTimeField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.host"))); fields.add(hostField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.recorder"))); fields.add(recorderField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.attendees"))); fields.add(attendeesField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.topic"))); fields.add(topicField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.sendTo"))); fields.add(sendToField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.copyTo"))); fields.add(copyToField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.status"))); fields.add(reviewStatusCombo)

        val textTabs = JTabbedPane()
        textTabs.addTab(CodeNotesBundle.message("review.field.scope"), CodeNotesIcons.Filter, JBScrollPane(scopeArea))
        textTabs.addTab(CodeNotesBundle.message("review.field.notes"), CodeNotesIcons.Info, JBScrollPane(notesArea))

        val panel = CodeNotesUi.detailPanel()
        panel.add(
            CodeNotesUi.verticalSplit(
                CodeNotesUi.section(CodeNotesBundle.message("review.section.meeting"), CodeNotesIcons.Meeting, fields),
                CodeNotesUi.section(CodeNotesBundle.message("review.section.reviewContent"), CodeNotesIcons.Markdown, textTabs),
                0.35
            ),
            BorderLayout.CENTER
        )
        return panel
    }

    private fun issueDetailPanel(): JPanel {
        val fields = JPanel(GridLayout(0, 2, 6, 6))
        fields.add(JLabel(CodeNotesBundle.message("review.field.issueTitle"))); fields.add(issueTitleField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.filePath"))); fields.add(issueFileField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.line"))); fields.add(issueLineField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.symbol"))); fields.add(issueSymbolField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.issueType"))); fields.add(issueTypeCombo)
        fields.add(JLabel(CodeNotesBundle.message("review.field.severity"))); fields.add(severityCombo)
        fields.add(JLabel(CodeNotesBundle.message("review.field.status"))); fields.add(issueStatusCombo)
        fields.add(JLabel(CodeNotesBundle.message("review.field.owner"))); fields.add(ownerField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.dueDate"))); fields.add(dueDateField)
        fields.add(JLabel(CodeNotesBundle.message("review.field.linkedNote"))); fields.add(linkedNoteLabel)

        val textTabs = JTabbedPane()
        textTabs.addTab(CodeNotesBundle.message("review.field.description"), CodeNotesIcons.Markdown, JBScrollPane(issueDescriptionArea))
        textTabs.addTab(CodeNotesBundle.message("review.field.suggestion"), CodeNotesIcons.Info, JBScrollPane(suggestionArea))
        textTabs.addTab(CodeNotesBundle.message("review.field.resolution"), CodeNotesIcons.Status, JBScrollPane(resolutionArea))

        val panel = CodeNotesUi.detailPanel()
        panel.add(
            CodeNotesUi.verticalSplit(
                CodeNotesUi.section(CodeNotesBundle.message("review.section.issue"), CodeNotesIcons.ReviewIssue, fields),
                CodeNotesUi.section(CodeNotesBundle.message("review.section.followup"), CodeNotesIcons.Priority, textTabs),
                0.35
            ),
            BorderLayout.CENTER
        )
        return panel
    }

    private fun refreshAll(keepSelection: Boolean) {
        refreshReviews(keepSelection)
        refreshIssues(keepSelection)
    }

    private fun refreshAndClear() {
        loading = true
        reviewAutoSaveTimer.stop()
        issueAutoSaveTimer.stop()
        refreshReviews(keepSelection = false)
        reviewList.clearSelection()
        issueModel.clear()
        issueList.clearSelection()
        editingReviewId = null
        editingIssueId = null
        clearReviewForm()
        clearIssueForm()
        loading = false
    }

    private fun installAutoSaveListeners() {
        val reviewTextListener = object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = scheduleReviewAutoSave()
        }
        listOf<JTextComponent>(
            meetingNameField,
            meetingDateField,
            locationField,
            startTimeField,
            endTimeField,
            hostField,
            recorderField,
            attendeesField,
            topicField,
            sendToField,
            copyToField,
            scopeArea,
            notesArea
        ).forEach { it.document.addDocumentListener(reviewTextListener) }
        reviewStatusCombo.addActionListener { scheduleReviewAutoSave() }

        val issueTextListener = object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = scheduleIssueAutoSave()
        }
        listOf<JTextComponent>(
            issueTitleField,
            issueDescriptionArea,
            issueFileField,
            issueLineField,
            issueSymbolField,
            ownerField,
            dueDateField,
            suggestionArea,
            resolutionArea
        ).forEach { it.document.addDocumentListener(issueTextListener) }
        listOf(issueTypeCombo, severityCombo, issueStatusCombo).forEach { combo ->
            combo.addActionListener { scheduleIssueAutoSave() }
        }
    }

    private fun scheduleReviewAutoSave() {
        if (loading || editingReviewId == null) return
        reviewAutoSaveTimer.restart()
    }

    private fun scheduleIssueAutoSave() {
        if (loading || editingIssueId == null) return
        issueAutoSaveTimer.restart()
    }

    private fun flushReviewAutoSave() {
        if (reviewAutoSaveTimer.isRunning) {
            reviewAutoSaveTimer.stop()
            saveSelectedReview()
        }
    }

    private fun flushIssueAutoSave() {
        if (issueAutoSaveTimer.isRunning) {
            issueAutoSaveTimer.stop()
            saveSelectedIssue()
        }
    }

    private fun clearReviewForm() {
        meetingNameField.text = ""
        meetingDateField.text = ""
        locationField.text = ""
        startTimeField.text = ""
        endTimeField.text = ""
        hostField.text = ""
        recorderField.text = ""
        attendeesField.text = ""
        topicField.text = ""
        sendToField.text = ""
        copyToField.text = ""
        scopeArea.text = ""
        notesArea.text = ""
        reviewStatusCombo.selectedItem = CodeReviewStatus.OPEN
    }

    private fun clearIssueForm() {
        issueTitleField.text = ""
        issueDescriptionArea.text = ""
        issueFileField.text = ""
        issueLineField.text = ""
        issueSymbolField.text = ""
        issueTypeCombo.selectedItem = NoteType.REVIEW
        severityCombo.selectedItem = TodoPriority.MEDIUM
        issueStatusCombo.selectedItem = TodoStatus.TODO
        ownerField.text = ""
        dueDateField.text = ""
        suggestionArea.text = ""
        resolutionArea.text = ""
        linkedNoteLabel.text = ""
    }

    private fun refreshReviews(keepSelection: Boolean) {
        val selectedId = if (keepSelection) reviewList.selectedValue?.id else null
        reviewModel.clear()
        reviewRepository.allReviews().forEach { reviewModel.addElement(it) }
        val selectedIndex = selectedId?.let { id ->
            (0 until reviewModel.size()).firstOrNull { reviewModel[it].id == id }
        } ?: 0
        if (reviewModel.size() > 0) reviewList.selectedIndex = selectedIndex.coerceIn(0, reviewModel.size() - 1)
    }

    private fun refreshIssues(keepSelection: Boolean) {
        val review = reviewList.selectedValue
        val selectedId = if (keepSelection) issueList.selectedValue?.id else null
        issueModel.clear()
        if (review != null) {
            reviewRepository.issues(review.id).forEach { issueModel.addElement(it) }
        }
        val selectedIndex = selectedId?.let { id ->
            (0 until issueModel.size()).firstOrNull { issueModel[it].id == id }
        } ?: 0
        if (issueModel.size() > 0) issueList.selectedIndex = selectedIndex.coerceIn(0, issueModel.size() - 1)
    }

    private fun loadSelectedReview() {
        val review = reviewList.selectedValue ?: return
        loading = true
        editingReviewId = review.id
        meetingNameField.text = review.meetingName
        meetingDateField.text = review.meetingDate
        locationField.text = review.location
        startTimeField.text = review.startTime
        endTimeField.text = review.endTime
        hostField.text = review.host
        recorderField.text = review.recorder
        attendeesField.text = review.attendees
        topicField.text = review.topic
        sendToField.text = review.sendTo
        copyToField.text = review.copyTo
        scopeArea.text = review.scope
        notesArea.text = review.notes
        reviewStatusCombo.selectedItem = LocalizedEnumLabels.reviewStatusCode(review.status) ?: CodeReviewStatus.OPEN
        loading = false
    }

    private fun loadSelectedIssue() {
        val issue = issueList.selectedValue ?: return
        loading = true
        editingIssueId = issue.id
        issueTitleField.text = issue.title
        issueDescriptionArea.text = issue.description
        issueFileField.text = issue.filePath
        issueLineField.text = if (issue.lineStart >= 0) (issue.lineStart + 1).toString() else ""
        issueSymbolField.text = issue.symbolQualifiedName
        issueTypeCombo.selectedItem = LocalizedEnumLabels.noteTypeCode(issue.issueType) ?: NoteType.REVIEW
        severityCombo.selectedItem = LocalizedEnumLabels.priorityCode(issue.severity) ?: TodoPriority.MEDIUM
        issueStatusCombo.selectedItem = LocalizedEnumLabels.statusCode(issue.status) ?: TodoStatus.TODO
        ownerField.text = issue.owner
        dueDateField.text = issue.dueDate
        suggestionArea.text = issue.suggestion
        resolutionArea.text = issue.resolution
        linkedNoteLabel.text = if (issue.noteId.isBlank()) CodeNotesBundle.message("review.linkedNote.none") else issue.noteId
        loading = false
    }

    private fun createReview() {
        flushReviewAutoSave()
        flushIssueAutoSave()
        val review = CodeReviewDefaults.newTodayReview()
        reviewRepository.addReview(review)
        refreshReviews(keepSelection = true)
    }

    private fun saveSelectedReview() {
        if (loading) return
        val review = editingReviewId?.let { reviewRepository.findReview(it) } ?: reviewList.selectedValue ?: return
        review.meetingName = meetingNameField.text.trim()
        review.meetingDate = meetingDateField.text.trim()
        review.location = locationField.text.trim()
        review.startTime = startTimeField.text.trim()
        review.endTime = endTimeField.text.trim()
        review.host = hostField.text.trim()
        review.recorder = recorderField.text.trim()
        review.attendees = attendeesField.text.trim()
        review.topic = topicField.text.trim()
        review.sendTo = sendToField.text.trim()
        review.copyTo = copyToField.text.trim()
        review.scope = scopeArea.text.trim()
        review.notes = notesArea.text.trim()
        review.status = (reviewStatusCombo.selectedItem as CodeReviewStatus).name
        reviewRepository.updateReview(review)
    }

    private fun deleteSelectedReview() {
        reviewAutoSaveTimer.stop()
        issueAutoSaveTimer.stop()
        val review = reviewList.selectedValue ?: return
        reviewRepository.deleteReview(review.id)
        editingReviewId = null
        editingIssueId = null
        clearReviewForm()
        clearIssueForm()
    }

    private fun createIssue() {
        flushReviewAutoSave()
        flushIssueAutoSave()
        val review = reviewList.selectedValue ?: return
        val issue = CodeReviewIssueEntity().apply {
            reviewId = review.id
            title = CodeNotesBundle.message("review.default.issueTitle")
            issueType = NoteType.REVIEW.name
            severity = TodoPriority.MEDIUM.name
            status = TodoStatus.TODO.name
        }
        reviewRepository.addIssue(issue)
        refreshIssues(keepSelection = true)
    }

    private fun addNoteIssue() {
        flushReviewAutoSave()
        flushIssueAutoSave()
        val review = reviewList.selectedValue ?: CodeReviewDefaults.getOrCreateDefaultReview(reviewRepository)
        val notes = noteRepository.allNotes()
        if (notes.isEmpty()) return
        val selected = JOptionPane.showInputDialog(
            this,
            CodeNotesBundle.message("review.addNote.prompt"),
            CodeNotesBundle.message("review.action.addNote"),
            JOptionPane.PLAIN_MESSAGE,
            null,
            notes.toTypedArray(),
            notes.first()
        ) as? NoteEntity ?: return
        reviewRepository.addIssue(CodeReviewIssueFactory.fromNote(review.id, selected))
        refreshIssues(keepSelection = true)
    }

    private fun saveSelectedIssue() {
        if (loading) return
        val issue = editingIssueId?.let { reviewRepository.findIssue(it) } ?: issueList.selectedValue ?: return
        issue.title = issueTitleField.text.trim()
        issue.description = issueDescriptionArea.text.trim()
        issue.filePath = issueFileField.text.trim()
        issue.lineStart = issueLineField.text.trim().toIntOrNull()?.let { it - 1 } ?: -1
        issue.lineEnd = issue.lineStart
        issue.symbolQualifiedName = issueSymbolField.text.trim()
        issue.issueType = (issueTypeCombo.selectedItem as NoteType).name
        issue.severity = (severityCombo.selectedItem as TodoPriority).name
        issue.status = (issueStatusCombo.selectedItem as TodoStatus).name
        issue.owner = ownerField.text.trim()
        issue.dueDate = dueDateField.text.trim()
        issue.suggestion = suggestionArea.text.trim()
        issue.resolution = resolutionArea.text.trim()
        reviewRepository.updateIssue(issue)
    }

    private fun deleteSelectedIssue() {
        issueAutoSaveTimer.stop()
        val issue = issueList.selectedValue ?: return
        reviewRepository.deleteIssue(issue.id)
        editingIssueId = null
        clearIssueForm()
    }

    private fun exportSelectedReview() {
        flushReviewAutoSave()
        flushIssueAutoSave()
        saveSelectedReview()
        saveSelectedIssue()
        val review = reviewList.selectedValue ?: CodeReviewDefaults.getOrCreateDefaultReview(reviewRepository)
        val issues = reviewRepository.issues(review.id)
        issues.forEach { CodeReviewDefaults.normalizeIssueDefaults(it) }
        val validation = CodeReviewExportValidator.validate(review, issues)
        if (!validation.isValid) {
            val dialog = CodeReviewExportDialog(project, review, issues, validation)
            if (!dialog.showAndGet()) return
            dialog.applyToEntities()
            reviewRepository.updateReview(review)
            issues.forEach { reviewRepository.updateIssue(it) }
            if (!CodeReviewExportValidator.validate(review, issues).isValid) {
                JOptionPane.showMessageDialog(this, CodeNotesBundle.message("review.export.validationStillMissing"))
                return
            }
        }
        val chooser = JFileChooser()
        chooser.selectedFile = java.io.File("${review.meetingName.ifBlank { "代码走查报告" }}.xlsx")
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val target = chooser.selectedFile
            CodeReviewExportService.export(project, review, issues, target)
            NotificationGroupManager.getInstance()
                .getNotificationGroup("CodeNotes.Notifications")
                .createNotification(CodeNotesBundle.message("review.export.success"), NotificationType.INFORMATION)
                .addAction(object : NotificationAction(CodeNotesBundle.message("review.export.openFile")) {
                    override fun actionPerformed(e: AnActionEvent, notification: Notification) {
                        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(target)?.let { file ->
                            FileEditorManager.getInstance(project).openFile(file, true)
                        }
                    }
                })
                .notify(project)
        }
    }

    private fun navigateToSelectedIssue() {
        val issue = issueList.selectedValue ?: return
        val note = issue.noteId.takeIf { it.isNotBlank() }?.let { noteRepository.findById(it) }
        val filePath = note?.filePath ?: issue.filePath
        if (filePath.isBlank()) return
        val basePath = project.basePath ?: return
        val vFile = LocalFileSystem.getInstance().findFileByPath("$basePath/$filePath") ?: return
        FileEditorManager.getInstance(project).openFile(vFile, true)
        val line = (note?.lineStart ?: issue.lineStart).coerceAtLeast(0)
        val descriptor = OpenFileDescriptor(project, vFile, line, 0)
        SwingUtilities.invokeLater { descriptor.navigate(true) }
    }

    private inner class ReviewRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            val review = value as? CodeReviewEntity
            if (review != null) {
                val status = runCatching { CodeReviewStatus.valueOf(review.status) }
                    .map { LocalizedEnumLabels.reviewStatus(it) }
                    .getOrDefault(review.status)
                val title = review.meetingName.ifBlank { CodeNotesBundle.message("review.default.meetingName") }
                val date = review.meetingDate.ifBlank { CodeNotesBundle.message("review.meta.noDate") }
                component.text = CodeNotesUi.htmlTitle(title, "$status / $date")
            }
            CodeNotesUi.tuneListLabel(component, isSelected, CodeNotesIcons.Meeting)
            return component
        }
    }

    private inner class IssueRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            val issue = value as? CodeReviewIssueEntity
            if (issue != null) {
                val location = issue.symbolQualifiedName.ifBlank { issue.filePath }
                val severity = LocalizedEnumLabels.priority(issue.severity)
                val status = LocalizedEnumLabels.status(issue.status)
                val type = LocalizedEnumLabels.noteType(issue.issueType)
                val title = issue.title.ifBlank { CodeNotesBundle.message("review.default.issueTitle") }
                val meta = location.ifBlank { CodeNotesBundle.message("review.meta.noCode") }
                component.text = CodeNotesUi.htmlBadge(title, "$severity / $status / $type", meta)
            }
            CodeNotesUi.tuneListLabel(component, isSelected, CodeNotesIcons.ReviewIssue)
            return component
        }
    }

    private fun <T> localizedRenderer(label: (T) -> String) = object : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            @Suppress("UNCHECKED_CAST")
            component.text = value?.let { label(it as T) }.orEmpty()
            component.border = EmptyBorder(4, 8, 4, 8)
            return component
        }
    }

    override fun dispose() {
        reviewAutoSaveTimer.stop()
        issueAutoSaveTimer.stop()
    }
}
