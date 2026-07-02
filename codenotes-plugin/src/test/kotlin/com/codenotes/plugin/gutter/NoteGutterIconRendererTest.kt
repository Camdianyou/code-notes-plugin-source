package com.codenotes.plugin.gutter

import com.codenotes.plugin.model.TodoPriority
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteGutterIconRendererTest {

    @Test
    fun `maps priority codes and Chinese labels to gutter icon keys`() {
        assertEquals("LOW", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.LOW.name))
        assertEquals("MEDIUM", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.MEDIUM.name))
        assertEquals("HIGH", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.HIGH.name))
        assertEquals("CRITICAL", NoteGutterIconRenderer.iconKeyForPriority(TodoPriority.CRITICAL.name))
        assertEquals("HIGH", NoteGutterIconRenderer.iconKeyForPriority("\u9AD8"))
        assertEquals("MEDIUM", NoteGutterIconRenderer.iconKeyForPriority(""))
    }
}
