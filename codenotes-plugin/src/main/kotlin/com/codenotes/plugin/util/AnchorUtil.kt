package com.codenotes.plugin.util

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.security.MessageDigest

object AnchorUtil {

    fun hashOf(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.trim().toByteArray())
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
        val lineCount = document.lineCount
        val start = storedLineStart.coerceIn(0, maxOf(lineCount - 1, 0))
        if (lineOfHashMatches(document, start, textHash)) return start

        for (offset in 1..window) {
            val down = start + offset
            if (down < lineCount && lineOfHashMatches(document, down, textHash)) return down
            val up = start - offset
            if (up >= 0 && lineOfHashMatches(document, up, textHash)) return up
        }
        return start
    }

    private fun lineOfHashMatches(document: Document, line: Int, textHash: String): Boolean {
        if (line < 0 || line >= document.lineCount) return false
        val startOffset = document.getLineStartOffset(line)
        val endOffset = document.getLineEndOffset(line)
        val text = document.getText(com.intellij.openapi.util.TextRange(startOffset, endOffset))
        return hashOf(text) == textHash
    }
}
