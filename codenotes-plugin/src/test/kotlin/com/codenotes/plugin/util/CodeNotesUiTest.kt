package com.codenotes.plugin.util

import javax.swing.JLabel
import javax.swing.JSplitPane
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CodeNotesUiTest {

    @Test
    fun `vertical split creates draggable top and bottom detail sections`() {
        val split = CodeNotesUi.verticalSplit(JLabel("top"), JLabel("bottom"), 0.35)

        assertEquals(JSplitPane.VERTICAL_SPLIT, split.orientation)
        assertEquals(0.35, split.resizeWeight)
        assertTrue(split.isOneTouchExpandable)
    }
}
