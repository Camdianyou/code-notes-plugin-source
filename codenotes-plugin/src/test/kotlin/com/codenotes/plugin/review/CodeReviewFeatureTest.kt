package com.codenotes.plugin.review

import com.codenotes.plugin.model.CodeReviewEntity
import com.codenotes.plugin.model.CodeReviewIssueEntity
import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.model.TodoPriority
import com.codenotes.plugin.model.TodoStatus
import com.codenotes.plugin.state.NoteStorageState
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
    fun `creates code review issue snapshot from note`() {
        val note = NoteEntity().apply {
            id = "note-1"
            title = "空指针风险"
            summary = "需要判空"
            description = "调用前没有检查 null"
            filePath = "src/Foo.kt"
            lineStart = 9
            lineEnd = 10
            symbolQualifiedName = "com.example.Foo.bar"
            type = NoteType.BUG.name
            priority = TodoPriority.HIGH.name
            status = TodoStatus.TODO.name
            dueDate = "2026-07-03"
        }

        val issue = CodeReviewIssueFactory.fromNote("review-1", note)

        assertEquals("review-1", issue.reviewId)
        assertEquals("note-1", issue.noteId)
        assertEquals("空指针风险", issue.title)
        assertEquals("调用前没有检查 null", issue.description)
        assertEquals("src/Foo.kt", issue.filePath)
        assertEquals(9, issue.lineStart)
        assertEquals("com.example.Foo.bar", issue.symbolQualifiedName)
        assertEquals(NoteType.BUG.name, issue.issueType)
        assertEquals(TodoPriority.HIGH.name, issue.severity)
        assertEquals(TodoStatus.TODO.name, issue.status)
        assertEquals("2026-07-03", issue.dueDate)
    }

    @Test
    fun `validator reports missing review and issue fields`() {
        val review = CodeReviewEntity()
        val issue = CodeReviewIssueEntity().apply {
            id = "issue-1"
            filePath = "src/Foo.kt"
        }

        val result = CodeReviewExportValidator.validate(review, listOf(issue))

        assertFalse(result.isValid)
        assertTrue("会议名称" in result.missingReviewFields)
        assertTrue("记录人" in result.missingReviewFields)
        assertTrue("标题" in result.missingIssueFields.getValue("issue-1"))
        assertTrue("行号或符号" in result.missingIssueFields.getValue("issue-1"))
    }

    @Test
    fun `exports code review report into xlsx template`() {
        val review = CodeReviewEntity().apply {
            meetingName = "支付模块代码走查"
            meetingDate = "2026-07-02"
            location = "线上"
            startTime = "10:00"
            endTime = "11:00"
            host = "张三"
            recorder = "李四"
            attendees = "张三、李四"
            topic = "支付模块走查"
            scope = "支付下单与回调"
            conclusion = "需要修复高优问题"
        }
        val issues = listOf(
            CodeReviewIssueEntity().apply {
                title = "金额精度风险"
                description = "金额使用 Double 计算"
                filePath = "src/Pay.kt"
                lineStart = 11
                issueType = NoteType.BUG.name
                severity = TodoPriority.HIGH.name
                status = TodoStatus.TODO.name
                owner = "王五"
                suggestion = "改用 BigDecimal"
            }
        )
        val template = requireNotNull(javaClass.getResourceAsStream("/templates/code-review-meeting-template.xlsx"))
        val target = File.createTempFile("code-review", ".xlsx")

        CodeReviewExportService.exportWithTemplate(template, review, issues, target)

        WorkbookFactory.create(target).use { workbook ->
            val sheet = workbook.getSheetAt(0)
            assertEquals("支付模块代码走查", sheet.getRow(1).getCell(1).stringCellValue)
            assertEquals("2026-07-02", sheet.getRow(2).getCell(1).stringCellValue)
            assertTrue(sheet.getRow(10).getCell(0).stringCellValue.contains("支付下单与回调"))
            assertTrue(sheet.getRow(17).getCell(0).stringCellValue.contains("金额精度风险"))
        }
    }
}

