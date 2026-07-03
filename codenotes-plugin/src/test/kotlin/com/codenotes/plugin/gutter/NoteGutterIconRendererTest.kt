package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.TodoPriority
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteGutterIconRendererTest {

    @Test
    fun `maps priority codes and Chinese labels to gutter icon keys`() {
        assertEquals("PIN_LOW", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.LOW.name))
        assertEquals("PIN_MEDIUM", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.MEDIUM.name))
        assertEquals("PIN_HIGH", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.HIGH.name))
        assertEquals("PIN_CRITICAL", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.CRITICAL.name))
        assertEquals("PIN_HIGH", NoteGutterIconRenderer.iconKeyForPriority("\u9AD8"))
        assertEquals("PIN_MEDIUM", NoteGutterIconRenderer.iconKeyForPriority(""))
        assertEquals("PIN_MEDIUM", NoteGutterIconRenderer.iconKeyForPriority("unknown"))
    }
}
