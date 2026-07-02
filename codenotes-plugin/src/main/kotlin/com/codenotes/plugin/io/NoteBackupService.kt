package com.codenotes.plugin.io

import com.codenotes.plugin.state.NoteStorageService
import com.codenotes.plugin.state.NoteStorageState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import java.io.File

object NoteBackupService {
    fun exportTo(project: Project, target: File) {
        val state = NoteStorageService.getInstance(project).state
        val element = XmlSerializer.serialize(state)
        JDOMUtil.write(element, target)
    }

    fun importFrom(source: File): NoteStorageState {
        val element = JDOMUtil.load(source)
        return XmlSerializer.deserialize(element, NoteStorageState::class.java)
    }
}
