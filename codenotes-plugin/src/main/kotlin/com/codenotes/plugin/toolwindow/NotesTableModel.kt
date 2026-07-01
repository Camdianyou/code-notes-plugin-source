package com.codenotes.plugin.toolwindow

import com.codenotes.plugin.model.NoteEntity
import com.codenotes.plugin.model.NoteType
import com.codenotes.plugin.util.CodeNotesBundle
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.table.AbstractTableModel

class NotesTableModel : AbstractTableModel() {

    var notes: List<NoteEntity> = emptyList()
        set(value) {
            field = value
            fireTableDataChanged()
        }

    private val columns = arrayOf(
        "panel.column.type",
        "panel.column.title",
        "panel.column.file",
        "panel.column.line",
        "panel.column.updated"
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm")

    override fun getRowCount(): Int = notes.size
    override fun getColumnCount(): Int = columns.size
    override fun getColumnName(column: Int): String = CodeNotesBundle.message(columns[column])

    fun noteAt(row: Int): NoteEntity? = notes.getOrNull(row)

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val note = notes[rowIndex]
        return when (columnIndex) {
            0 -> NoteType.safeValueOf(note.type).icon + " " + note.type
            1 -> note.title.ifBlank { "(untitled)" }
            2 -> note.filePath
            3 -> if (note.lineStart >= 0) (note.lineStart + 1).toString() else ""
            4 -> dateFormat.format(Date(note.updatedAt))
            else -> ""
        }
    }
}
