package com.codenotes.plugin.gutter

import com.codenotes.plugin.anchor.SymbolAnchorService
import com.codenotes.plugin.events.NoteChangeBus
import com.codenotes.plugin.events.NoteChangeEvent
import com.codenotes.plugin.events.NoteChangeListener
import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.SymbolAnchor
import com.codenotes.plugin.repository.NoteRepository
import com.codenotes.plugin.util.AnchorUtil
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.editor.markup.HighlighterLayer
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

        project.messageBus.connect(project).subscribe(NoteChangeBus.TOPIC, object : NoteChangeListener {
            override fun notesChanged(event: NoteChangeEvent) {
                if (event.filePath.isBlank() || event.filePath == relativePath) {
                    repaint(editor, project, relativePath)
                }
            }
        })

        repaint(editor, project, relativePath)
    }

    private fun repaint(editor: com.intellij.openapi.editor.Editor, project: Project, relativePath: String) {
        editor.markupModel.allHighlighters
            .filter { it.gutterIconRenderer is NoteGutterIconRenderer }
            .forEach { editor.markupModel.removeHighlighter(it) }

        val notes = NoteRepository.getInstance(project).notesForFile(relativePath)
        if (notes.isEmpty()) return

        val document = editor.document
        for (note in notes) {
            if (note.lineStart < 0) continue
            val line = if (note.anchorType == NoteAnchor.SYMBOL.name) {
                SymbolAnchorService.resolve(project, SymbolAnchor().apply {
                    language = note.symbolLanguage
                    symbolKind = note.symbolKind
                    qualifiedName = note.symbolQualifiedName
                    signature = note.symbolSignature
                    filePath = note.filePath
                    fallbackLine = note.fallbackLine
                    fallbackHash = note.fallbackTextHash
                }) ?: AnchorUtil.relocateLineRange(document, note.lineStart, note.lineEnd, note.textHash)
            } else {
                AnchorUtil.relocateLineRange(document, note.lineStart, note.lineEnd, note.textHash)
            }
            if (line < 0 || line >= document.lineCount) continue

            val highlighter = editor.markupModel.addLineHighlighter(
                null, line, HighlighterLayer.ADDITIONAL_SYNTAX
            )
            highlighter.gutterIconRenderer = NoteGutterIconRenderer(project, note.id)
        }
    }
}
