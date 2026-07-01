package com.codenotes.plugin.gutter

import com.codenotes.plugin.state.NoteStorageService
import com.codenotes.plugin.util.AnchorUtil
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project

/**
 * Paints a gutter icon for every note anchored to the file backing a newly
 * opened editor. Runs a best-effort line relocation (see AnchorUtil) so
 * icons stay roughly correct even if lines shifted above the anchor.
 */
class NoteGutterEditorListener : EditorFactoryListener {

    override fun editorCreated(event: EditorFactoryEvent) {
        val editor = event.editor
        val project: Project = editor.project ?: return
        val file = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        val relativePath = AnchorUtil.relativePath(project, file)

        val notes = NoteStorageService.getInstance(project).getNotesForFile(relativePath)
        if (notes.isEmpty()) return

        val document = editor.document
        for (note in notes) {
            if (note.lineStart < 0) continue
            val line = AnchorUtil.relocateLine(document, note.lineStart, note.textHash)
            if (line < 0 || line >= document.lineCount) continue

            val highlighter = editor.markupModel.addLineHighlighter(
                null, line, HighlighterLayer.ADDITIONAL_SYNTAX
            )
            highlighter.gutterIconRenderer = NoteGutterIconRenderer(project, note.id)
        }
    }
}
