package com.codenotes.plugin.util

import kotlin.test.Test
import kotlin.test.assertEquals

class AnchorUtilTest {

    @Test
    fun `relocates a multi line anchor by matching the whole snippet`() {
        val originalSnippet = listOf(
            "fun calculateTotal(items: List<Item>): Int {",
            "    return items.sumOf { it.price }",
            "}"
        )
        val changedDocument = listOf(
            "package demo",
            "",
            "class Cart {",
            "    fun calculateTotal(items: List<Item>): Int {",
            "        return items.sumOf { it.price }",
            "    }",
            "}"
        )

        val line = AnchorUtil.relocateLineRange(
            lines = changedDocument,
            storedLineStart = 0,
            storedLineEnd = 2,
            textHash = AnchorUtil.hashOf(originalSnippet.joinToString("\n")),
            window = 10
        )

        assertEquals(3, line)
    }
}
