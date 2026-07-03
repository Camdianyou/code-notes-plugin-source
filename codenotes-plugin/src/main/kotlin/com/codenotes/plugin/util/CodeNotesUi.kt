package com.codenotes.plugin.util

import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.Icon
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.SwingConstants

object CodeNotesUi {
    fun toolbarPanel(): JPanel =
        JPanel(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), 0)).apply {
            border = JBUI.Borders.empty(6, 8)
            isOpaque = false
        }

    fun actionButton(text: String, icon: Icon, primary: Boolean = false, action: () -> Unit): JButton =
        JButton(text, icon).apply {
            isFocusable = false
            margin = JBUI.insets(4, if (primary) 10 else 8)
            toolTipText = text
            putClientProperty("JButton.buttonType", if (primary) "default" else "small")
            addActionListener { action() }
        }

    fun detailPanel(): JPanel =
        JPanel(BorderLayout(0, JBUI.scale(8))).apply {
            border = JBUI.Borders.empty(8, 10)
            isOpaque = false
        }

    fun verticalSplit(top: JComponent, bottom: JComponent, resizeWeight: Double = 0.35): JSplitPane =
        JSplitPane(JSplitPane.VERTICAL_SPLIT, top, bottom).apply {
            this.resizeWeight = resizeWeight
            isOneTouchExpandable = true
            dividerSize = JBUI.scale(7)
            border = JBUI.Borders.empty()
        }

    fun section(title: String, icon: Icon, content: JComponent): JPanel =
        JPanel(BorderLayout(0, JBUI.scale(6))).apply {
            isOpaque = false
            border = BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
                JBUI.Borders.empty(0, 0, 8, 0)
            )
            add(sectionHeader(title, icon), BorderLayout.NORTH)
            add(content, BorderLayout.CENTER)
        }

    fun sectionHeader(title: String, icon: Icon): JLabel =
        JLabel(title, icon, SwingConstants.LEFT).apply {
            font = UIUtil.getLabelFont().deriveFont(Font.BOLD)
            iconTextGap = JBUI.scale(6)
            border = JBUI.Borders.empty(0, 0, 3, 0)
        }

    fun tuneListLabel(label: JLabel, selected: Boolean, icon: Icon? = null) {
        label.border = JBUI.Borders.empty(7, 10)
        label.icon = icon
        label.iconTextGap = JBUI.scale(8)
        label.isOpaque = selected
    }

    fun htmlTitle(title: String, meta: String): String =
        "<html><b>${escape(title)}</b><br><span style='color:#787878'>${escape(meta)}</span></html>"

    fun htmlBadge(title: String, badge: String, meta: String): String =
        "<html><b>${escape(title)}</b><br><span style='color:#787878'>${escape(badge)} &nbsp; ${escape(meta)}</span></html>"

    fun escape(value: String): String =
        value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;")

    fun compactGrid(component: Component): Component {
        if (component is JComponent) {
            component.border = JBUI.Borders.empty(2)
        }
        return component
    }
}
