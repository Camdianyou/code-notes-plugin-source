package com.codenotes.plugin.gutter

import com.codenotes.plugin.anchor.SymbolAnchorService
import com.codenotes.plugin.events.CodeReviewChangeBus
import com.codenotes.plugin.events.CodeReviewChangeEvent
import com.codenotes.plugin.events.CodeReviewChangeListener
import com.codenotes.plugin.events.NoteChangeBus
import com.codenotes.plugin.events.NoteChangeEvent
import com.codenotes.plugin.events.NoteChangeListener
import com.codenotes.plugin.model.NoteAnchor
import com.codenotes.plugin.model.SymbolAnchor
import com.codenotes.plugin.repository.CodeReviewRepository
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
        project.messageBus.connect(project).subscribe(CodeReviewChangeBus.TOPIC, object : CodeReviewChangeListener {
            override fun codeReviewsChanged(event: CodeReviewChangeEvent) {
                repaint(editor, project, relativePath)
            }
        })

        repaint(editor, project, relativePath)
    }

    private fun repaint(editor: com.intellij.openapi.editor.Editor, project: Project, relativePath: String) {
        editor.markupModel.allHighlighters
            .filter { it.gutterIconRenderer is NoteGutterIconRenderer }
            .forEach { editor.markupModel.removeHighlighter(it) }

        val notes = NoteRepository.getInstance(project).notesForFile(relativePath)
        val issues = CodeReviewRepository.getInstance(project).allIssues()
        val markers = CodeAnchorMarker.fromNotesAndIssues(relativePath, notes, issues)
        if (markers.isEmpty()) return

        val document = editor.document
        for (marker in markers) {
            if (marker.lineStart < 0) continue
            val line = if (marker.anchorType == NoteAnchor.SYMBOL.name) {
                SymbolAnchorService.resolve(project, SymbolAnchor().apply {
                    language = marker.symbolLanguage
                    symbolKind = marker.symbolKind
                    qualifiedName = marker.symbolQualifiedName
                    signature = marker.symbolSignature
                    filePath = marker.filePath
                    fallbackLine = marker.fallbackLine
                    fallbackHash = marker.fallbackTextHash
                }) ?: AnchorUtil.relocateLineRange(document, marker.lineStart, marker.lineEnd, marker.textHash)
            } else if (marker.textHash.isNotBlank()) {
                AnchorUtil.relocateLineRange(document, marker.lineStart, marker.lineEnd, marker.textHash)
            } else {
                marker.lineStart
            }
            if (line < 0 || line >= document.lineCount) continue

            val highlighter = editor.markupModel.addLineHighlighter(
                null, line, HighlighterLayer.ADDITIONAL_SYNTAX
            )
            highlighter.gutterIconRenderer = NoteGutterIconRenderer(project, marker)
        }
    }
}
