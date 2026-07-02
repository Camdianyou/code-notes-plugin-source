package com.codenotes.plugin.toolwindow

import com.codenotes.plugin.util.CodeNotesBundle
import com.codenotes.plugin.util.CodeNotesIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.JTabbedPane

class CodeNotesRootPanel(project: Project) : JPanel(BorderLayout()), Disposable {
    private val notesPanel = CodeNotesPanel(project)
    private val reviewPanel = CodeReviewPanel(project)

    init {
        val tabs = JTabbedPane()
        tabs.addTab(CodeNotesBundle.message("root.tab.notes"), CodeNotesIcons.Notes, notesPanel)
        tabs.addTab(CodeNotesBundle.message("root.tab.reviews"), CodeNotesIcons.Reviews, reviewPanel)
        add(tabs, BorderLayout.CENTER)
    }

    override fun dispose() {
        notesPanel.dispose()
        reviewPanel.dispose()
    }
}
