package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.settings.CodeNotesSettingsState
import com.codenotes.plugin.util.LocalizedEnumLabels
import com.intellij.openapi.project.Project
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.ss.util.CellRangeAddress
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

object CodeReviewExportService {
    private const val TEMPLATE_RESOURCE = "/templates/code-review-meeting-template.xlsx"

    fun export(project: Project, review: CodeReviewEntity, issues: List<CodeReviewIssueEntity>, target: File) {
        templateStream().use { stream ->
            exportWithTemplate(stream, review, issues, target)
        }
    }

    fun exportWithTemplate(
        template: InputStream,
        review: CodeReviewEntity,
        issues: List<CodeReviewIssueEntity>,
        target: File
    ) {
        issues.forEach { CodeReviewDefaults.normalizeIssueDefaults(it) }
        WorkbookFactory.create(template).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            sheet.setMergedCellValue(2, 2, review.meetingName)
            sheet.setMergedCellValue(3, 2, review.meetingDate)
            sheet.setMergedCellValue(3, 4, review.location)
            sheet.setMergedCellValue(4, 2, review.startTime)
            sheet.setMergedCellValue(4, 4, review.endTime)
            sheet.setMergedCellValue(5, 2, review.host)
            sheet.setMergedCellValue(5, 4, review.recorder)
            sheet.setMergedCellValue(6, 2, review.attendees)
            sheet.setMergedCellValue(7, 2, review.topic)
            sheet.setMergedCellValue(8, 2, review.sendTo)
            sheet.setMergedCellValue(9, 2, review.copyTo)

            val contentLines = buildList {
                add("\u4E00\u3001\u8D70\u67E5\u8303\u56F4")
                val scopeLines = review.scopeLines()
                if (scopeLines.isEmpty()) {
                    addIndented("1. \u672A\u586B\u5199")
                } else {
                    scopeLines.forEachIndexed { index, scope ->
                        addIndented("${index + 1}. $scope")
                    }
                }
                addIndented("\u95EE\u9898\u6982\u89C8\uFF1A\u5171 ${issues.size} \u4E2A\u95EE\u9898\uFF0C\u5F85\u8DDF\u8FDB ${issues.count { !it.isClosed() }} \u4E2A\u3002")
            }.map { ExportLine(it) }
            sheet.writeMergedLines(11, 6, 17, contentLines)

            val followUpIssues = issues.filter { !it.isClosed() }
            sheet.writeMergedLines(
                18,
                5,
                23,
                followUpIssues.mapIndexed { index, issue -> issueLine(index + 1, issue) }
            )

            val otherLines = buildList {
                review.notesLines().forEachIndexed { index, note ->
                    addIndented("\u5176\u4ED6\u6CE8\u610F\u4E8B\u9879 ${index + 1}\uFF1A$note")
                }
                val closedIssues = issues.filter { it.isClosed() }
                if (closedIssues.isNotEmpty()) {
                    addIndented("\u5DF2\u5B8C\u6210\u6216\u5F52\u6863\u95EE\u9898\uFF1A${closedIssues.joinToString("\uFF1B") { it.title.ifBlank { it.id } }}")
                }
                val detachedIssues = issues.filter { it.filePath.isBlank() && it.symbolQualifiedName.isBlank() }
                if (detachedIssues.isNotEmpty()) {
                    addIndented("\u672A\u5173\u8054\u4EE3\u7801\u7684\u95EE\u9898\uFF1A${detachedIssues.joinToString("\uFF1B") { it.title.ifBlank { it.id } }}")
                }
            }.map { ExportLine(it) }
            sheet.writeMergedLines(24, 5, 29, otherLines)

            target.parentFile?.mkdirs()
            target.outputStream().use { workbook.write(it) }
        }
    }

    private fun templateStream(): InputStream {
        val overridePath = CodeNotesSettingsState.getInstance().codeReviewTemplatePath
        if (overridePath.isNotBlank()) {
            val file = File(overridePath)
            if (file.isFile) return FileInputStream(file)
        }
        return requireNotNull(CodeReviewExportService::class.java.getResourceAsStream(TEMPLATE_RESOURCE)) {
            "Missing bundled code review template: $TEMPLATE_RESOURCE"
        }
    }

    private fun CodeReviewIssueEntity.isClosed(): Boolean {
        val statusCode = LocalizedEnumLabels.statusCode(status)
        return statusCode == TodoStatus.DONE || statusCode == TodoStatus.ARCHIVED
    }

    private fun CodeReviewEntity.notesLines(): List<String> =
        notes.splitCleanLines()

    private fun CodeReviewEntity.scopeLines(): List<String> =
        scope.splitCleanLines()

    private fun String.splitCleanLines(): List<String> =
        this.split(Regex("\\r?\\n"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun issueLine(index: Int, issue: CodeReviewIssueEntity): ExportLine {
        val severity = severityOf(issue.severity)
        val severityLabel = LocalizedEnumLabels.priority(severity)
        val title = issue.title.ifBlank { issue.id }
        val owner = issue.owner.ifBlank { "\u672A\u6307\u5B9A" }
        val suggestion = issue.suggestion.ifBlank { "\u672A\u586B\u5199" }
        val description = issue.description.ifBlank { "\u672A\u586B\u5199" }
        val text = indent("$index. [$severityLabel] $title - ${compactLocation(issue)}\uFF1B\u8D23\u4EFB\u4EBA\uFF1A$owner\uFF1B\u5EFA\u8BAE\uFF1A$suggestion\uFF1B\u63CF\u8FF0\uFF1A$description")
        return ExportLine(text, severity)
    }

    private fun severityOf(value: String): TodoPriority =
        LocalizedEnumLabels.priorityCode(value) ?: TodoPriority.MEDIUM

    private fun compactLocation(issue: CodeReviewIssueEntity): String {
        if (issue.symbolQualifiedName.isNotBlank()) {
            return issue.symbolQualifiedName
                .split('.')
                .filter { it.isNotBlank() }
                .takeLast(2)
                .joinToString(".")
                .ifBlank { issue.symbolQualifiedName }
        }
        if (issue.filePath.isNotBlank()) {
            val fileName = issue.filePath.replace('\\', '/').substringAfterLast('/')
            val lineSuffix = if (issue.lineStart >= 0) ":${issue.lineStart + 1}" else ""
            return "$fileName$lineSuffix"
        }
        return "\u672A\u5173\u8054\u4EE3\u7801"
    }

    private fun Sheet.setMergedCellValue(rowNumber: Int, columnNumber: Int, value: String) {
        val row = getRow(rowNumber - 1) ?: createRow(rowNumber - 1)
        val cell = row.getCell(columnNumber - 1) ?: row.createCell(columnNumber - 1)
        cell.setCellValue(value)
        cell.cellStyle = workbook.leftAlignedStyle(cell.cellStyle)
    }

    private fun Sheet.writeMergedLines(
        startRowNumber: Int,
        templateCapacity: Int,
        nextSectionRowNumber: Int,
        lines: List<ExportLine>
    ) {
        val extra = (lines.size - templateCapacity).coerceAtLeast(0)
        if (extra > 0) {
            shiftRows(nextSectionRowNumber - 1, lastRowNum, extra, true, false)
            repeat(extra) { offset ->
                val rowIndex = nextSectionRowNumber - 1 + offset
                val sourceRow = getRow(rowIndex - 1)
                val targetRow = getRow(rowIndex) ?: createRow(rowIndex)
                if (sourceRow != null) {
                    targetRow.height = sourceRow.height
                    for (columnIndex in 0..3) {
                        val sourceCell = sourceRow.getCell(columnIndex)
                        val targetCell = targetRow.getCell(columnIndex) ?: targetRow.createCell(columnIndex)
                        if (sourceCell != null) targetCell.cellStyle = sourceCell.cellStyle
                    }
                }
                addMergedRegion(CellRangeAddress(rowIndex, rowIndex, 0, 3))
            }
        }

        val severityStyles = mutableMapOf<TodoPriority, CellStyle>()
        var leftAlignedStyle: CellStyle? = null
        lines.forEachIndexed { offset, line ->
            val rowIndex = startRowNumber - 1 + offset
            val row = getRow(rowIndex) ?: createRow(rowIndex)
            val cell: Cell = row.getCell(0) ?: row.createCell(0)
            cell.setCellValue(line.text)
            cell.cellStyle = line.severity?.let { severity ->
                severityStyles.getOrPut(severity) { workbook.leftAlignedStyle(cell.cellStyle, severity) }
            } ?: run {
                leftAlignedStyle ?: workbook.leftAlignedStyle(cell.cellStyle).also { leftAlignedStyle = it }
            }
        }
        (lines.size until templateCapacity).forEach { offset ->
            val row = getRow(startRowNumber - 1 + offset) ?: return@forEach
            row.getCell(0)?.setCellValue("")
        }
    }

    private fun Workbook.leftAlignedStyle(baseStyle: CellStyle, severity: TodoPriority? = null): CellStyle {
        val style = createCellStyle()
        style.cloneStyleFrom(baseStyle)
        style.alignment = HorizontalAlignment.LEFT
        if (severity != null) {
            val font = createFont()
            font.color = when (severity) {
                TodoPriority.LOW -> IndexedColors.GREEN.index
                TodoPriority.MEDIUM -> IndexedColors.DARK_YELLOW.index
                TodoPriority.HIGH,
                TodoPriority.CRITICAL -> IndexedColors.RED.index
            }
            style.setFont(font)
        }
        return style
    }

    private fun MutableList<String>.addIndented(text: String) {
        add(indent(text))
    }

    private fun indent(text: String): String = "  $text"

    private data class ExportLine(
        val text: String,
        val severity: TodoPriority? = null
    )
}
