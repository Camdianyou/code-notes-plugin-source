package com.codenotes.plugin.model

import kotlin.test.Test
import kotlin.test.assertEquals

class NoteQueryTest {

    @Test
    fun `filters notes by query tags and task status`() {
        val bug = NoteEntity().apply {
            title = "Null pointer on login"
            summary = "Crashes after clicking submit"
            tags = "auth, critical"
            type = NoteType.BUG.name
            status = TodoStatus.BLOCKED.name
        }
        val decision = NoteEntity().apply {
            title = "Use XML storage"
            summary = "Keep project notes git friendly"
            tags = "storage"
            type = NoteType.DECISION.name
            status = TodoStatus.DONE.name
        }

        val result = NoteQuery(
            text = "login",
            tags = setOf("critical"),
            statuses = setOf(TodoStatus.BLOCKED.name)
        ).applyTo(listOf(decision, bug))

        assertEquals(listOf(bug.id), result.map { it.id })
    }
}
