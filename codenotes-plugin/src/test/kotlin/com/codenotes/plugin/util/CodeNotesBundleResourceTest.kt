package com.codenotes.plugin.util

import java.util.Properties
import kotlin.test.Test
import kotlin.test.assertTrue

class CodeNotesBundleResourceTest {

    @Test
    fun `tool window localization keys exist in English and Chinese bundles`() {
        val requiredKeys = listOf(
            "panel.action.new",
            "panel.action.save",
            "panel.action.refresh",
            "panel.action.delete",
            "panel.action.open",
            "panel.action.export",
            "panel.action.import",
            "panel.filter.all",
            "panel.filter.favorites",
            "panel.field.flags",
            "panel.field.favorite",
            "panel.field.anchor",
            "panel.field.updated",
            "panel.tab.markdown",
            "panel.tab.preview",
            "panel.attachments.title",
            "panel.attachments.add",
            "panel.attachments.open",
            "panel.attachments.remove",
            "panel.default.untitled",
            "panel.anchor.project",
            "note.type.comment",
            "note.type.bug",
            "todo.priority.low",
            "todo.priority.medium",
            "todo.status.todo",
            "todo.status.archived",
            "review.action.refresh"
        )

        val english = loadProperties("messages/CodeNotesBundle.properties")
        val chinese = loadProperties("messages/CodeNotesBundle_zh_CN.properties")

        requiredKeys.forEach { key ->
            assertTrue(english.containsKey(key), "English bundle is missing $key")
            assertTrue(chinese.containsKey(key), "Chinese bundle is missing $key")
        }
    }

    private fun loadProperties(path: String): Properties {
        val properties = Properties()
        val stream = javaClass.classLoader.getResourceAsStream(path)
            ?: error("Missing resource $path")
        stream.use { properties.load(it.reader(Charsets.UTF_8)) }
        return properties
    }
}
