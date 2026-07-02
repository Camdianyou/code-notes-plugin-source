package com.codenotes.plugin.anchor

import com.codenotes.plugin.model.SymbolAnchor
import com.codenotes.plugin.util.AnchorUtil
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.PsiRecursiveElementWalkingVisitor

object SymbolAnchorService {

    fun createAnchor(project: Project, editor: Editor, file: VirtualFile): SymbolAnchor? =
        ReadAction.compute<SymbolAnchor?, RuntimeException> {
            val psiFile = PsiManager.getInstance(project).findFile(file) ?: return@compute null
            val offset = editor.caretModel.offset.coerceIn(0, maxOf(editor.document.textLength - 1, 0))
            val element = psiFile.findElementAt(offset) ?: return@compute null
            val symbol = generateSequence(element) { it.parent }
                .filterIsInstance<PsiNamedElement>()
                .firstOrNull { symbolKind(it).isNotBlank() && !it.name.isNullOrBlank() }
                ?: return@compute null

            val line = editor.document.getLineNumber(symbol.textRange.startOffset)
            val lineText = editor.document.getText(
                TextRange(editor.document.getLineStartOffset(line), editor.document.getLineEndOffset(line))
            )
            SymbolAnchor().apply {
                language = psiFile.language.id
                symbolKind = symbolKind(symbol)
                qualifiedName = qualifiedName(symbol)
                signature = signature(symbol)
                filePath = AnchorUtil.relativePath(project, file)
                fallbackLine = line
                fallbackHash = AnchorUtil.hashOf(lineText)
            }
        }

    fun resolve(project: Project, anchor: SymbolAnchor): Int? =
        ReadAction.compute<Int?, RuntimeException> {
            val basePath = project.basePath ?: return@compute null
            val vFile = com.intellij.openapi.vfs.LocalFileSystem.getInstance()
                .findFileByPath("$basePath/${anchor.filePath}") ?: return@compute null
            val psiFile = PsiManager.getInstance(project).findFile(vFile) ?: return@compute null
            var resolvedLine: Int? = null
            psiFile.accept(object : PsiRecursiveElementWalkingVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (resolvedLine != null) return
                    if (element is PsiNamedElement &&
                        symbolKind(element) == anchor.symbolKind &&
                        qualifiedName(element) == anchor.qualifiedName
                    ) {
                        val document = com.intellij.psi.PsiDocumentManager.getInstance(project).getDocument(psiFile)
                        resolvedLine = document?.getLineNumber(element.textRange.startOffset)
                        return
                    }
                    super.visitElement(element)
                }
            })
            resolvedLine
        }

    private fun symbolKind(element: PsiNamedElement): String {
        val simpleName = element.javaClass.simpleName
        return when {
            simpleName.contains("Class", ignoreCase = true) -> "CLASS"
            simpleName.contains("Method", ignoreCase = true) -> "METHOD"
            simpleName.contains("Function", ignoreCase = true) -> "METHOD"
            simpleName.contains("Field", ignoreCase = true) -> "FIELD"
            simpleName.contains("Property", ignoreCase = true) -> "FIELD"
            else -> ""
        }
    }

    private fun qualifiedName(element: PsiNamedElement): String {
        val reflected = listOf("getQualifiedName", "getFqName", "getFqNameString")
            .firstNotNullOfOrNull { methodName ->
                runCatching {
                    val value = element.javaClass.methods.firstOrNull { it.name == methodName && it.parameterCount == 0 }
                        ?.invoke(element)
                    value?.toString()?.takeIf { it.isNotBlank() }
                }.getOrNull()
            }
        if (reflected != null) return reflected

        return generateSequence(element as PsiElement?) { it.parent }
            .filterIsInstance<PsiNamedElement>()
            .mapNotNull { it.name?.takeIf { name -> name.isNotBlank() } }
            .toList()
            .asReversed()
            .joinToString(".")
    }

    private fun signature(element: PsiNamedElement): String {
        val header = element.text.substringBefore('{').substringBefore('=').lineSequence().firstOrNull().orEmpty()
        return "${symbolKind(element)}:${qualifiedName(element)}:${header.trim()}"
    }
}
