package com.codenotes.plugin.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.security.MessageDigest

object AnchorUtil {

    fun hashOf(text: String): String {
        val normalized = text
            .lineSequence()
            .map { it.trim() }
            .joinToString("\n")
            .trim()
        val digest = MessageDigest.getInstance("SHA-256").digest(normalized.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun relativePath(project: Project, file: VirtualFile): String {
        val base = project.basePath ?: return file.path
        val path = file.path
        return if (path.startsWith(base)) path.removePrefix(base).trimStart('/') else path
    }

    /**
     * Attempts to find the current line range for a note whose anchor text
     * hash was recorded at [storedLineStart]. Searches outward from the
     * stored position within [window] lines to tolerate edits above/below
     * the anchor. Returns the resolved 0-based line, or storedLineStart if
     * no match is found (caller should treat that as "possibly stale").
     */
    fun relocateLine(document: Document, storedLineStart: Int, textHash: String, window: Int = 200): Int {
        if (textHash.isBlank()) return storedLineStart.coerceIn(0, maxOf(document.lineCount - 1, 0))
        return relocateLineRange(document, storedLineStart, storedLineStart, textHash, window)
    }

    fun relocateLineRange(
        document: Document,
        storedLineStart: Int,
        storedLineEnd: Int,
        textHash: String,
        window: Int = 200
    ): Int {
        val lines = (0 until document.lineCount).map { line ->
            val startOffset = document.getLineStartOffset(line)
            val endOffset = document.getLineEndOffset(line)
            document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        }
        return relocateLineRange(lines, storedLineStart, storedLineEnd, textHash, window)
    }

    fun relocateLineRange(
        lines: List<String>,
        storedLineStart: Int,
        storedLineEnd: Int,
        textHash: String,
        window: Int = 200
    ): Int {
        if (lines.isEmpty()) return 0
        if (textHash.isBlank()) return storedLineStart.coerceIn(0, lines.lastIndex)
        val lineCount = lines.size
        val start = storedLineStart.coerceIn(0, lines.lastIndex)
        val lineSpan = maxOf(1, storedLineEnd - storedLineStart + 1)
        if (rangeHashMatches(lines, start, lineSpan, textHash)) return start

        for (offset in 1..window) {
            val down = start + offset
            if (down < lineCount && rangeHashMatches(lines, down, lineSpan, textHash)) return down
            val up = start - offset
            if (up >= 0 && rangeHashMatches(lines, up, lineSpan, textHash)) return up
        }
        return start
    }

    private fun rangeHashMatches(lines: List<String>, lineStart: Int, lineSpan: Int, textHash: String): Boolean {
        if (lineStart < 0 || lineStart + lineSpan > lines.size) return false
        return hashOf(lines.subList(lineStart, lineStart + lineSpan).joinToString("\n")) == textHash
    }

    private fun lineOfHashMatches(document: Document, line: Int, textHash: String): Boolean {
        if (line < 0 || line >= document.lineCount) return false
        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        val text = document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        return hashOf(text) == textHash
    }
}
