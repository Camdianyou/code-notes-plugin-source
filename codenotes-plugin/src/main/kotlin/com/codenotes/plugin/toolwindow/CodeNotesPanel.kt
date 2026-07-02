package com.codenotes.plugin.toolwindow

import com.codenotes.plugin.attachments.AttachmentService
import com.codenotes.plugin.anchor.SymbolAnchorService
import com.codenotes.plugin.events.NoteChangeBus
import com.codenotes.plugin.events.NoteChangeEvent
import com.codenotes.plugin.events.NoteChangeListener
import com.codenotes.plugin.io.NoteBackupService
import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteQuery
import com.codenotes.plugin.model.NoteScope
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.SymbolAnchor
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.repository.CodeReviewRepository
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.review.CodeReviewDefaults
import com.codenotes.plugin.review.CodeReviewIssueFactory
import com.codenotes.plugin.util.AnchorUtil
import com.codenotes.plugin.util.CodeNotesBundle
import com.codenotes.plugin.util.LocalizedEnumLabels
import com.codenotes.plugin.util.MarkdownPreview
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.GridLayout
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.DefaultListCellRenderer
import javax.swing.DefaultListModel
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JEditorPane
import javax.swing.JFileChooser
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.JTabbedPane
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent

class CodeNotesPanel(private val project: Project) : JPanel(BorderLayout()), Disposable {

    private val repository = NoteRepository.getInstance(project)
    private val searchField = SearchTextField()
    private val filterModel = DefaultListModel<FilterItem>()
    private val filterList = JBList(filterModel)
    private val noteModel = DefaultListModel<NoteEntity>()
    private val noteList = JBList(noteModel)

    private val typeCombo = JComboBox(NoteType.entries.toTypedArray())
    private val titleField = JBTextField()
    private val summaryField = JBTextField()
    private val descriptionArea = JBTextArea()
    private val tagsField = JBTextField()
    private val priorityCombo = JComboBox(TodoPriority.entries.toTypedArray())
    private val statusCombo = JComboBox(TodoStatus.entries.toTypedArray())
    private val dueDateField = JBTextField()
    private val favoriteBox = JBCheckBox(CodeNotesBundle.message("panel.field.favorite"))
    private val anchorLabel = JLabel("")
    private val updatedLabel = JLabel("")
    private val previewPane = JEditorPane("text/html", "")
    private val attachmentModel = DefaultListModel<String>()
    private val attachmentList = JBList(attachmentModel)

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")
    private var selectedFilter = FilterItem.all()
    private var loading = false

    init {
        project.messageBus.connect(this).subscribe(NoteChangeBus.TOPIC, object : NoteChangeListener {
            override fun notesChanged(event: NoteChangeEvent) = refreshAll(keepSelection = true)
        })

        searchField.textEditor.emptyText.text = CodeNotesBundle.message("panel.search.placeholder")
        searchField.addDocumentListener(object : com.intellij.ui.DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) = refreshNotes(keepSelection = true)
        })

        filterList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        filterList.cellRenderer = FilterRenderer()
        filterList.addListSelectionListener {
            if (!it.valueIsAdjusting) {
                selectedFilter = filterList.selectedValue ?: FilterItem.all()
                refreshNotes(keepSelection = false)
            }
        }

        noteList.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        noteList.cellRenderer = NoteRenderer()
        noteList.emptyText.text = CodeNotesBundle.message("panel.empty.list")
        noteList.addListSelectionListener {
            if (!it.valueIsAdjusting) loadSelectedNote()
        }
        noteList.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) navigateToSelected()
            }
        })

        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        previewPane.isEditable = false
        attachmentList.emptyText.text = CodeNotesBundle.message("panel.empty.list")
        typeCombo.renderer = localizedRenderer<NoteType> { LocalizedEnumLabels.noteType(it) }
        priorityCombo.renderer = localizedRenderer<TodoPriority> { LocalizedEnumLabels.priority(it) }
        statusCombo.renderer = localizedRenderer<TodoStatus> { LocalizedEnumLabels.status(it) }

        add(toolbar(), BorderLayout.NORTH)
        add(workspace(), BorderLayout.CENTER)
        refreshAll(keepSelection = false)
    }

    private fun toolbar(): JPanel {
        val panel = JPanel(BorderLayout(8, 0))
        val buttons = JPanel()
        buttons.add(JButton(CodeNotesBundle.message("panel.action.new")).apply { addActionListener { createProjectNote() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.action.save")).apply { addActionListener { saveSelectedNote() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.action.delete")).apply { addActionListener { deleteSelectedNote() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.action.open")).apply { addActionListener { navigateToSelected() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.action.addToReview")).apply { addActionListener { addSelectedNoteToReview() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.action.export")).apply { addActionListener { exportNotes() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.action.import")).apply { addActionListener { importNotes() } })
        panel.border = EmptyBorder(6, 8, 6, 8)
        panel.add(searchField, BorderLayout.CENTER)
        panel.add(buttons, BorderLayout.EAST)
        return panel
    }

    private fun workspace(): JSplitPane {
        val left = JPanel(BorderLayout())
        left.preferredSize = Dimension(170, 300)
        left.add(JBScrollPane(filterList), BorderLayout.CENTER)

        val middle = JPanel(BorderLayout())
        middle.preferredSize = Dimension(280, 300)
        middle.add(JBScrollPane(noteList), BorderLayout.CENTER)

        val right = detailPanel()
        val first = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, middle)
        first.resizeWeight = 0.25
        val root = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, first, right)
        root.resizeWeight = 0.45
        return root
    }

    private fun detailPanel(): JPanel {
        val fields = JPanel(GridLayout(0, 2, 6, 6))
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.type")))
        fields.add(typeCombo)
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.title")))
        fields.add(titleField)
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.summary")))
        fields.add(summaryField)
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.tags")))
        fields.add(tagsField)
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.priority")))
        fields.add(priorityCombo)
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.status")))
        fields.add(statusCombo)
        fields.add(JLabel(CodeNotesBundle.message("dialog.field.dueDate")))
        fields.add(dueDateField)
        fields.add(JLabel(CodeNotesBundle.message("panel.field.flags")))
        fields.add(favoriteBox)
        fields.add(JLabel(CodeNotesBundle.message("panel.field.anchor")))
        fields.add(anchorLabel)
        fields.add(JLabel(CodeNotesBundle.message("panel.field.updated")))
        fields.add(updatedLabel)

        val tabs = JTabbedPane()
        tabs.addTab(CodeNotesBundle.message("panel.tab.markdown"), JBScrollPane(descriptionArea))
        tabs.addTab(CodeNotesBundle.message("panel.tab.preview"), JBScrollPane(previewPane))
        tabs.addChangeListener {
            if (tabs.selectedIndex == 1) {
                previewPane.text = MarkdownPreview.toHtml(descriptionArea.text)
            }
        }

        val panel = JPanel(BorderLayout(0, 8))
        panel.border = EmptyBorder(10, 12, 10, 12)
        panel.add(fields, BorderLayout.NORTH)
        panel.add(tabs, BorderLayout.CENTER)
        panel.add(attachmentPanel(), BorderLayout.SOUTH)
        return panel
    }

    private fun attachmentPanel(): JPanel {
        val buttons = JPanel()
        buttons.add(JButton(CodeNotesBundle.message("panel.attachments.add")).apply { addActionListener { addAttachment() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.attachments.open")).apply { addActionListener { openAttachment() } })
        buttons.add(JButton(CodeNotesBundle.message("panel.attachments.remove")).apply { addActionListener { removeAttachment() } })
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(100, 120)
        panel.border = EmptyBorder(4, 0, 0, 0)
        panel.add(JLabel(CodeNotesBundle.message("panel.attachments.title")), BorderLayout.NORTH)
        panel.add(JBScrollPane(attachmentList), BorderLayout.CENTER)
        panel.add(buttons, BorderLayout.SOUTH)
        return panel
    }

    private fun refreshAll(keepSelection: Boolean) {
        refreshFilters()
        refreshNotes(keepSelection)
    }

    private fun refreshFilters() {
        val previous = selectedFilter.key
        filterModel.clear()
        val notes = repository.allNotes()
        filterModel.addElement(FilterItem.all())
        filterModel.addElement(
            FilterItem("favorite", CodeNotesBundle.message("panel.filter.favorites"), favoritesOnly = true)
        )
        TodoStatus.entries.forEach { status ->
            filterModel.addElement(FilterItem("status:${status.name}", LocalizedEnumLabels.status(status), statuses = setOf(status.name)))
        }
        notes.flatMap { it.tags.split(',') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .forEach { tag -> filterModel.addElement(FilterItem("tag:$tag", "#$tag", tags = setOf(tag))) }

        val index = (0 until filterModel.size()).firstOrNull { filterModel[it].key == previous } ?: 0
        filterList.selectedIndex = index
        selectedFilter = filterModel[index]
    }

    private fun refreshNotes(keepSelection: Boolean) {
        val selectedId = if (keepSelection) noteList.selectedValue?.id else null
        val query = NoteQuery(
            text = searchField.text.trim(),
            tags = selectedFilter.tags,
            statuses = selectedFilter.statuses,
            favoritesOnly = selectedFilter.favoritesOnly
        )
        noteModel.clear()
        repository.query(query).forEach { noteModel.addElement(it) }
        val selectedIndex = selectedId?.let { id ->
            (0 until noteModel.size()).firstOrNull { noteModel[it].id == id }
        } ?: 0
        if (noteModel.size() > 0) noteList.selectedIndex = selectedIndex.coerceIn(0, noteModel.size() - 1)
    }

    private fun loadSelectedNote() {
        val note = noteList.selectedValue ?: return
        loading = true
        typeCombo.selectedItem = LocalizedEnumLabels.noteTypeCode(note.type) ?: NoteType.COMMENT
        titleField.text = note.title
        summaryField.text = note.summary
        descriptionArea.text = note.description
        tagsField.text = note.tags
        priorityCombo.selectedItem = LocalizedEnumLabels.priorityCode(note.priority) ?: TodoPriority.MEDIUM
        statusCombo.selectedItem = LocalizedEnumLabels.statusCode(note.status) ?: TodoStatus.TODO
        dueDateField.text = note.dueDate
        favoriteBox.isSelected = note.favorite
        anchorLabel.text = when (note.anchorType) {
            NoteAnchor.SYMBOL.name -> "${note.symbolLanguage} ${symbolKindLabel(note.symbolKind)} ${note.symbolQualifiedName}"
            NoteAnchor.PROJECT.name -> CodeNotesBundle.message("panel.anchor.project")
            else -> "${note.filePath}:${note.lineStart + 1}"
        }
        updatedLabel.text = dateFormat.format(Date(note.updatedAt))
        previewPane.text = MarkdownPreview.toHtml(note.description)
        attachmentModel.clear()
        note.attachments.forEach { attachment ->
            attachmentModel.addElement("${attachment.fileName} (${attachment.sizeBytes} bytes)")
        }
        loading = false
    }

    private fun createProjectNote() {
        val note = NoteEntity().apply {
            anchorType = NoteAnchor.PROJECT.name
            scope = NoteScope.PROJECT.name
            title = CodeNotesBundle.message("panel.default.untitled")
            type = NoteType.COMMENT.name
            author = System.getProperty("user.name") ?: ""
        }
        repository.add(note)
        refreshAll(keepSelection = true)
        noteList.selectedIndex = (0 until noteModel.size()).firstOrNull { noteModel[it].id == note.id } ?: 0
    }

    private fun saveSelectedNote() {
        if (loading) return
        val note = noteList.selectedValue ?: return
        note.type = (typeCombo.selectedItem as NoteType).name
        note.title = titleField.text.trim()
        note.summary = summaryField.text.trim()
        note.description = descriptionArea.text
        note.tags = tagsField.text.trim()
        note.priority = (priorityCombo.selectedItem as TodoPriority).name
        note.status = (statusCombo.selectedItem as TodoStatus).name
        note.dueDate = dueDateField.text.trim()
        note.favorite = favoriteBox.isSelected
        repository.update(note)
        previewPane.text = MarkdownPreview.toHtml(note.description)
    }

    private fun addAttachment() {
        val note = noteList.selectedValue ?: return
        val chooser = JFileChooser()
        chooser.isMultiSelectionEnabled = true
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            chooser.selectedFiles.mapNotNull { AttachmentService.addAttachment(project, it) }
                .forEach { note.attachments.add(it) }
            repository.update(note)
            loadSelectedNote()
        }
    }

    private fun openAttachment() {
        val note = noteList.selectedValue ?: return
        val attachment = note.attachments.getOrNull(attachmentList.selectedIndex) ?: return
        AttachmentService.open(project, attachment)
    }

    private fun removeAttachment() {
        val note = noteList.selectedValue ?: return
        val index = attachmentList.selectedIndex
        val attachment = note.attachments.getOrNull(index) ?: return
        AttachmentService.delete(project, attachment)
        note.attachments.removeAt(index)
        repository.update(note)
        loadSelectedNote()
    }

    private fun exportNotes() {
        val chooser = JFileChooser()
        chooser.selectedFile = java.io.File("codeNotes-backup.xml")
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            NoteBackupService.exportTo(project, chooser.selectedFile)
        }
    }

    private fun importNotes() {
        val chooser = JFileChooser()
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            val state = NoteBackupService.importFrom(chooser.selectedFile)
            repository.replaceAll(state.notes, state.folders, state.codeReviews, state.codeReviewIssues)
        }
    }

    private fun deleteSelectedNote() {
        val note = noteList.selectedValue ?: return
        repository.delete(note.id)
    }

    private fun addSelectedNoteToReview() {
        val notes = noteList.selectedValuesList.ifEmpty {
            noteList.selectedValue?.let { listOf(it) } ?: emptyList()
        }
        if (notes.isEmpty()) return
        val reviewRepository = CodeReviewRepository.getInstance(project)
        val review = CodeReviewDefaults.getOrCreateDefaultReview(reviewRepository)
        notes.forEach { note ->
            reviewRepository.addIssue(CodeReviewIssueFactory.fromNote(review.id, note))
        }
        com.intellij.notification.NotificationGroupManager.getInstance()
            .getNotificationGroup("CodeNotes.Notifications")
            .createNotification(CodeNotesBundle.message("notification.reviewIssueAdded"), com.intellij.notification.NotificationType.INFORMATION)
            .notify(project)
    }

    private fun navigateToSelected() {
        val note = noteList.selectedValue ?: return
        if (note.filePath.isBlank()) return

        val basePath = project.basePath ?: return
        val vFile = LocalFileSystem.getInstance().findFileByPath("$basePath/${note.filePath}") ?: return
        FileEditorManager.getInstance(project).openFile(vFile, true)
        val editor = FileEditorManager.getInstance(project).selectedTextEditor
        val line = (if (note.anchorType == NoteAnchor.SYMBOL.name) {
            SymbolAnchorService.resolve(project, SymbolAnchor().apply {
                language = note.symbolLanguage
                symbolKind = note.symbolKind
                qualifiedName = note.symbolQualifiedName
                signature = note.symbolSignature
                filePath = note.filePath
                fallbackLine = note.fallbackLine
                fallbackHash = note.fallbackTextHash
            })
        } else null) ?: editor?.let { AnchorUtil.relocateLineRange(it.document, note.lineStart, note.lineEnd, note.textHash) }
            ?: note.lineStart.coerceAtLeast(0)

        val descriptor = OpenFileDescriptor(project, vFile, line, 0)
        SwingUtilities.invokeLater { descriptor.navigate(true) }
    }

    private data class FilterItem(
        val key: String,
        val label: String,
        val tags: Set<String> = emptySet(),
        val statuses: Set<String> = emptySet(),
        val favoritesOnly: Boolean = false
    ) {
        companion object {
            fun all() = FilterItem("all", CodeNotesBundle.message("panel.filter.all"))
        }
    }

    private inner class FilterRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            component.text = (value as? FilterItem)?.label ?: ""
            component.border = EmptyBorder(6, 10, 6, 8)
            return component
        }
    }

    private inner class NoteRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            val note = value as? NoteEntity
            if (note != null) {
                val title = note.title.ifBlank { CodeNotesBundle.message("panel.default.untitled") }
                val location = if (note.symbolQualifiedName.isNotBlank()) note.symbolQualifiedName else note.filePath
                val type = LocalizedEnumLabels.noteType(note.type)
                val icon = (LocalizedEnumLabels.noteTypeCode(note.type) ?: NoteType.COMMENT).icon
                component.text = "$icon $title  ·  $type  ·  $location"
            }
            component.border = EmptyBorder(6, 8, 6, 8)
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

    private fun symbolKindLabel(kind: String): String =
        when (kind) {
            "CLASS" -> CodeNotesBundle.message("symbol.kind.class")
            "METHOD" -> CodeNotesBundle.message("symbol.kind.method")
            "FIELD" -> CodeNotesBundle.message("symbol.kind.field")
            else -> kind
        }

    override fun dispose() {
    }
}
