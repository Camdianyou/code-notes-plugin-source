package com.codenotes.plugin.settings

import com.codenotes.plugin.util.CodeNotesBundle
import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import java.awt.BorderLayout
import javax.swing.ButtonGroup
import javax.swing.JButton
import javax.swing.JFileChooser
import javax.swing.JPanel
import javax.swing.JRadioButton

class CodeNotesConfigurable : Configurable {

    private val followIdeRadio = JRadioButton(CodeNotesBundle.message("settings.language.followIde"))
    private val englishRadio = JRadioButton(CodeNotesBundle.message("settings.language.english"))
    private val chineseRadio = JRadioButton(CodeNotesBundle.message("settings.language.chinese"))
    private val templatePathField = JBTextField()
    private var panel: JPanel? = null

    override fun getDisplayName(): String = CodeNotesBundle.message("settings.title")

    override fun createComponent(): JPanel {
        val group = ButtonGroup()
        group.add(followIdeRadio)
        group.add(englishRadio)
        group.add(chineseRadio)

        val storageLabel = JBLabel(CodeNotesBundle.message("settings.storage.label"))
        val templatePanel = JPanel(BorderLayout(6, 0))
        templatePanel.add(templatePathField, BorderLayout.CENTER)
        templatePanel.add(JButton(CodeNotesBundle.message("settings.template.browse")).apply {
            addActionListener {
                val chooser = JFileChooser()
                if (chooser.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION) {
                    templatePathField.text = chooser.selectedFile.absolutePath
                }
            }
        }, BorderLayout.EAST)

        panel = FormBuilder.createFormBuilder()
            .addLabeledComponent(CodeNotesBundle.message("settings.language.label"), followIdeRadio)
            .addComponent(englishRadio)
            .addComponent(chineseRadio)
            .addVerticalGap(12)
            .addLabeledComponent(CodeNotesBundle.message("settings.template.label"), templatePanel)
            .addComponent(storageLabel)
            .addComponentFillVertically(JPanel(), 0)
            .panel

        reset()
        return panel!!
    }

    override fun isModified(): Boolean {
        val current = CodeNotesSettingsState.getInstance().languageOverride
        val selected = selectedOverride()
        return current != selected ||
            CodeNotesSettingsState.getInstance().codeReviewTemplatePath != templatePathField.text.trim()
    }

    override fun apply() {
        CodeNotesSettingsState.getInstance().apply {
            languageOverride = selectedOverride()
            codeReviewTemplatePath = templatePathField.text.trim()
        }
    }

    override fun reset() {
        when (CodeNotesSettingsState.getInstance().languageOverride) {
            "en" -> englishRadio.isSelected = true
            "zh-CN" -> chineseRadio.isSelected = true
            else -> followIdeRadio.isSelected = true
        }
        templatePathField.text = CodeNotesSettingsState.getInstance().codeReviewTemplatePath
    }

    private fun selectedOverride(): String = when {
        englishRadio.isSelected -> "en"
        chineseRadio.isSelected -> "zh-CN"
        else -> ""
    }
}
