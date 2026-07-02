package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.settings.CodeNotesSettingsState
import com.codenotes.plugin.util.LocalizedEnumLabels
import com.intellij.openapi.project.Project
import org.apache.poi.ss.usermodel.Cell
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
                add("走查范围：${review.scope.ifBlank { "未填写" }}")
                add("问题概览：共 ${issues.size} 个问题，待跟进 ${issues.count { !it.isClosed() }} 个。")
                if (review.conclusion.isNotBlank()) add("走查结论：${review.conclusion}")
                if (review.notes.isNotBlank()) add("补充说明：${review.notes}")
            }
            sheet.writeMergedLines(11, 6, 17, contentLines)

            val followUpIssues = issues.filter { !it.isClosed() }
            sheet.writeMergedLines(18, 5, 23, followUpIssues.mapIndexed { index, issue -> issueLine(index + 1, issue) })

            val otherLines = buildList {
                val closedIssues = issues.filter { it.isClosed() }
                if (closedIssues.isNotEmpty()) {
                    add("已完成或归档问题：${closedIssues.joinToString("；") { it.title.ifBlank { it.id } }}")
                }
                val detachedIssues = issues.filter { it.filePath.isBlank() && it.symbolQualifiedName.isBlank() }
                if (detachedIssues.isNotEmpty()) {
                    add("未关联代码的问题：${detachedIssues.joinToString("；") { it.title.ifBlank { it.id } }}")
                }
                add("报告由 Code Notes 插件根据代码走查记录导出。")
            }
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

    private fun issueLine(index: Int, issue: CodeReviewIssueEntity): String {
        val location = when {
            issue.symbolQualifiedName.isNotBlank() -> issue.symbolQualifiedName
            issue.filePath.isNotBlank() && issue.lineStart >= 0 -> "${issue.filePath}:${issue.lineStart + 1}"
            issue.filePath.isNotBlank() -> issue.filePath
            else -> "未关联代码"
        }
        val owner = issue.owner.ifBlank { "未指定" }
        val suggestion = issue.suggestion.ifBlank { "未填写" }
        val type = LocalizedEnumLabels.noteType(issue.issueType)
        val severity = LocalizedEnumLabels.priority(issue.severity)
        val status = LocalizedEnumLabels.status(issue.status)
        return "$index. [$severity/$status/$type] ${issue.title} - $location；责任人：$owner；建议：$suggestion；描述：${issue.description}"
    }

    private fun org.apache.poi.ss.usermodel.Sheet.setMergedCellValue(rowNumber: Int, columnNumber: Int, value: String) {
        val row = getRow(rowNumber - 1) ?: createRow(rowNumber - 1)
        val cell = row.getCell(columnNumber - 1) ?: row.createCell(columnNumber - 1)
        cell.setCellValue(value)
    }

    private fun org.apache.poi.ss.usermodel.Sheet.writeMergedLines(
        startRowNumber: Int,
        templateCapacity: Int,
        nextSectionRowNumber: Int,
        lines: List<String>
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
        lines.forEachIndexed { offset, line ->
            val rowIndex = startRowNumber - 1 + offset
            val row = getRow(rowIndex) ?: createRow(rowIndex)
            val cell: Cell = row.getCell(0) ?: row.createCell(0)
            cell.setCellValue(line)
        }
        (lines.size until templateCapacity).forEach { offset ->
            val row = getRow(startRowNumber - 1 + offset) ?: return@forEach
            row.getCell(0)?.setCellValue("")
        }
    }
}

