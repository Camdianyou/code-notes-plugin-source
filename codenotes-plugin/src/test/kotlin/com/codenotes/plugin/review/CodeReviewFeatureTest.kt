package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.state.NoteStorageState
import com.codenotes.plugin.util.LocalizedEnumLabels
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CodeReviewFeatureTest {

    @Test
    fun `old storage state starts with empty code review collections`() {
        val state = NoteStorageState()

        assertTrue(state.codeReviews.isEmpty())
        assertTrue(state.codeReviewIssues.isEmpty())
    }

    @Test
    fun `localized enum labels convert both directions`() {
        assertEquals("\u7F3A\u9677", LocalizedEnumLabels.noteType(NoteType.BUG))
        assertEquals(NoteType.BUG, LocalizedEnumLabels.noteTypeCode("\u7F3A\u9677"))
        assertEquals(NoteType.BUG, LocalizedEnumLabels.noteTypeCode("BUG"))
        assertEquals("\u9AD8", LocalizedEnumLabels.priority(TodoPriority.HIGH))
        assertEquals(TodoPriority.HIGH, LocalizedEnumLabels.priorityCode("\u9AD8"))
        assertEquals("\u5F85\u529E", LocalizedEnumLabels.status(TodoStatus.TODO))
        assertEquals(TodoStatus.TODO, LocalizedEnumLabels.statusCode("\u5F85\u529E"))
    }

    @Test
    fun `creates code review issue snapshot from note with stable enum codes`() {
        val note = NoteEntity().apply {
            id = "note-1"
            title = "\u7A7A\u6307\u9488\u98CE\u9669"
            summary = "\u9700\u8981\u5224\u7A7A"
            description = "\u8C03\u7528\u524D\u6CA1\u6709\u68C0\u67E5 null"
            filePath = "src/Foo.kt"
            lineStart = 9
            lineEnd = 10
            symbolQualifiedName = "com.example.Foo.bar"
            type = "\u7F3A\u9677"
            priority = "\u9AD8"
            status = "\u5F85\u529E"
            dueDate = "2026-07-03"
        }

        val issue = CodeReviewIssueFactory.fromNote("review-1", note)

        assertEquals("review-1", issue.reviewId)
        assertEquals("note-1", issue.noteId)
        assertEquals("\u7A7A\u6307\u9488\u98CE\u9669", issue.title)
        assertEquals("\u8C03\u7528\u524D\u6CA1\u6709\u68C0\u67E5 null", issue.description)
        assertEquals("src/Foo.kt", issue.filePath)
        assertEquals(9, issue.lineStart)
        assertEquals("com.example.Foo.bar", issue.symbolQualifiedName)
        assertEquals(NoteType.BUG.name, issue.issueType)
        assertEquals(TodoPriority.HIGH.name, issue.severity)
        assertEquals(TodoStatus.TODO.name, issue.status)
        assertEquals("2026-07-03", issue.dueDate)
    }

    @Test
    fun `validator only reports fields that need user input`() {
        val review = CodeReviewEntity()
        val issue = CodeReviewIssueEntity().apply {
            id = "issue-1"
            filePath = "src/Foo.kt"
        }

        val result = CodeReviewExportValidator.validate(review, listOf(issue))

        assertFalse(result.isValid)
        assertTrue("\u4F1A\u8BAE\u540D\u79F0" in result.missingReviewFields)
        assertTrue("\u8BB0\u5F55\u4EBA" in result.missingReviewFields)
        assertTrue("\u6807\u9898" in result.missingIssueFields.getValue("issue-1"))
        assertTrue("\u95EE\u9898\u63CF\u8FF0" in result.missingIssueFields.getValue("issue-1"))
        assertTrue("\u884C\u53F7\u6216\u7B26\u53F7" in result.missingIssueFields.getValue("issue-1"))
        assertFalse("\u95EE\u9898\u7C7B\u578B" in result.missingIssueFields.getValue("issue-1"))
        assertFalse("\u4E25\u91CD\u7A0B\u5EA6" in result.missingIssueFields.getValue("issue-1"))
        assertFalse("\u72B6\u6001" in result.missingIssueFields.getValue("issue-1"))
    }

    @Test
    fun `exports compact follow up issues with short symbols and severity colors`() {
        val review = CodeReviewEntity().apply {
            meetingName = "\u652F\u4ED8\u6A21\u5757\u4EE3\u7801\u8D70\u67E5"
            meetingDate = "2026-07-02"
            location = "\u7EBF\u4E0A"
            startTime = "10:00"
            endTime = "11:00"
            host = "\u5F20\u4E09"
            recorder = "\u674E\u56DB"
            attendees = "\u5F20\u4E09\u3001\u674E\u56DB"
            topic = "\u652F\u4ED8\u6A21\u5757\u8D70\u67E5"
            scope = "\u652F\u4ED8\u4E0B\u5355\u6D41\u7A0B\n\n\u56DE\u8C03\u9A8C\u7B7E\u903B\u8F91"
            conclusion = "\u9700\u8981\u4FEE\u590D\u9AD8\u4F18\u95EE\u9898"
            notes = "\u6CE8\u610F\u4E00\uFF1A\u5148\u5408\u5E76\u9AD8\u98CE\u9669\u4FEE\u590D\n\n\u6CE8\u610F\u4E8C\uFF1A\u56DE\u5F52\u8986\u76D6\u8FB9\u754C\u573A\u666F"
        }
        val issues = listOf(
            CodeReviewIssueEntity().apply {
                title = "\u91D1\u989D\u7CBE\u5EA6\u98CE\u9669"
                description = "\u91D1\u989D\u4F7F\u7528 Double \u8BA1\u7B97"
                filePath = "src/main/kotlin/com/example/payment/PayService.kt"
                lineStart = 11
                symbolQualifiedName = "com.example.payment.PayService.createOrder"
                issueType = NoteType.BUG.name
                severity = TodoPriority.HIGH.name
                status = TodoStatus.TODO.name
                owner = "\u738B\u4E94"
                suggestion = "\u6539\u7528 BigDecimal"
            },
            CodeReviewIssueEntity().apply {
                title = "\u65E5\u5FD7\u7F3A\u5931"
                description = "\u5931\u8D25\u5206\u652F\u6CA1\u6709\u5173\u952E\u65E5\u5FD7"
                filePath = "src/main/kotlin/com/example/payment/RefundService.kt"
                lineStart = 21
                severity = TodoPriority.MEDIUM.name
            },
            CodeReviewIssueEntity().apply {
                title = "\u547D\u540D\u53EF\u4F18\u5316"
                description = "\u5C40\u90E8\u53D8\u91CF\u547D\u540D\u4E0D\u591F\u6E05\u6670"
                filePath = "src/Name.kt"
                lineStart = 2
                severity = TodoPriority.LOW.name
            },
            CodeReviewIssueEntity().apply {
                title = "\u7A7A\u6307\u9488\u98CE\u9669"
                description = "\u8C03\u7528\u524D\u672A\u5224\u7A7A"
                symbolQualifiedName = "com.example.UserService.load"
                severity = TodoPriority.CRITICAL.name
            }
        )
        val template = requireNotNull(javaClass.getResourceAsStream("/templates/code-review-meeting-template.xlsx"))
        val target = File.createTempFile("code-review", ".xlsx")

        CodeReviewExportService.exportWithTemplate(template, review, issues, target)

        WorkbookFactory.create(target).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            val meetingNameCell = sheet.getRow(1).getCell(1)
            val meetingDateCell = sheet.getRow(2).getCell(1)
            val locationCell = sheet.getRow(2).getCell(3)
            assertEquals("\u652F\u4ED8\u6A21\u5757\u4EE3\u7801\u8D70\u67E5", meetingNameCell.stringCellValue)
            assertEquals("2026-07-02", meetingDateCell.stringCellValue)
            assertEquals(HorizontalAlignment.LEFT, meetingNameCell.cellStyle.alignment)
            assertEquals(HorizontalAlignment.LEFT, meetingDateCell.cellStyle.alignment)
            assertEquals(HorizontalAlignment.LEFT, locationCell.cellStyle.alignment)
            val scopeTitleCell = sheet.getRow(10).getCell(0)
            val firstScopeCell = sheet.getRow(11).getCell(0)
            assertEquals("\u4E00\u3001\u8D70\u67E5\u8303\u56F4\uFF1A", scopeTitleCell.stringCellValue)
            assertEquals("  1. \u652F\u4ED8\u4E0B\u5355\u6D41\u7A0B", firstScopeCell.stringCellValue)
            assertEquals("  2. \u56DE\u8C03\u9A8C\u7B7E\u903B\u8F91", sheet.getRow(12).getCell(0).stringCellValue)
            assertEquals(HorizontalAlignment.LEFT, firstScopeCell.cellStyle.alignment)
            assertEquals(BorderStyle.THIN, scopeTitleCell.cellStyle.borderBottom)
            assertFalse((10..15).any { rowIndex ->
                sheet.getRow(rowIndex)?.getCell(0)?.stringCellValue?.contains("\u95EE\u9898\u6982\u89C8") == true
            })
            assertFalse(sheet.getRow(13).getCell(0).stringCellValue.contains("\u8D70\u67E5\u7ED3\u8BBA"))

            val highCell = sheet.getRow(17).getCell(0)
            val highLine = highCell.stringCellValue
            assertTrue(highLine.startsWith("  1. "))
            assertTrue(highLine.contains("[\u9AD8]"))
            assertTrue(highLine.contains("\u91D1\u989D\u7CBE\u5EA6\u98CE\u9669"))
            assertTrue(highLine.contains("PayService.createOrder"))
            assertFalse(highLine.contains("com.example.payment"))
            assertFalse(highLine.contains("src/main/kotlin"))
            assertFalse(highLine.contains("\u5F85\u529E"))
            assertFalse(highLine.contains("\u7F3A\u9677"))
            assertFalse(highLine.contains("TODO"))
            assertFalse(highLine.contains("BUG"))
            assertEquals(HorizontalAlignment.LEFT, highCell.cellStyle.alignment)

            val mediumCell = sheet.getRow(18).getCell(0)
            assertTrue(mediumCell.stringCellValue.startsWith("  2. "))
            assertTrue(mediumCell.stringCellValue.contains("[\u4E2D]"))
            assertTrue(mediumCell.stringCellValue.contains("RefundService.kt:22"))
            assertFalse(mediumCell.stringCellValue.contains("src/main/kotlin"))

            val lowCell = sheet.getRow(19).getCell(0)
            assertTrue(lowCell.stringCellValue.contains("[\u4F4E]"))

            val criticalCell = sheet.getRow(20).getCell(0)
            assertTrue(criticalCell.stringCellValue.contains("[\u7D27\u6025]"))
            assertTrue(criticalCell.stringCellValue.contains("UserService.load"))

            val followUpHeadingCell = sheet.getRow(16).getCell(0)
            assertEquals("\u4E8C\u3001\u5F85\u8DDF\u8FDB\u4E8B\u9879\uFF1A", followUpHeadingCell.stringCellValue)
            assertEquals(BorderStyle.THIN, followUpHeadingCell.cellStyle.borderBottom)

            val otherHeadingCell = sheet.getRow(22).getCell(0)
            assertEquals("\u4E09\u3001\u5176\u4ED6\u6CE8\u610F\u4E8B\u9879\uFF1A", otherHeadingCell.stringCellValue)
            assertEquals(BorderStyle.THIN, otherHeadingCell.cellStyle.borderBottom)

            val noteLineOne = sheet.getRow(23).getCell(0).stringCellValue
            val noteLineTwo = sheet.getRow(24).getCell(0).stringCellValue
            assertTrue(noteLineOne.startsWith("  "))
            assertTrue(noteLineTwo.startsWith("  "))
            assertTrue(noteLineOne.contains("\u5176\u4ED6\u6CE8\u610F\u4E8B\u9879 1\uFF1A\u6CE8\u610F\u4E00"))
            assertTrue(noteLineTwo.contains("\u5176\u4ED6\u6CE8\u610F\u4E8B\u9879 2\uFF1A\u6CE8\u610F\u4E8C"))
            assertFalse(noteLineOne.contains("\u8865\u5145\u8BF4\u660E"))
            assertFalse(noteLineTwo.contains("\u62A5\u544A\u7531 Code Notes"))

            assertEquals(IndexedColors.RED.index, workbook.getFontAt(highCell.cellStyle.fontIndexAsInt).color)
            assertEquals(IndexedColors.DARK_YELLOW.index, workbook.getFontAt(mediumCell.cellStyle.fontIndexAsInt).color)
            assertEquals(IndexedColors.GREEN.index, workbook.getFontAt(lowCell.cellStyle.fontIndexAsInt).color)
            assertEquals(IndexedColors.RED.index, workbook.getFontAt(criticalCell.cellStyle.fontIndexAsInt).color)
        }
    }
}
