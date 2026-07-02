package com.codenotes.plugin.attachments

import com.codenotes.plugin.model.AttachmentEntity
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

object AttachmentService {
    private const val ATTACHMENT_DIR = ".idea/codeNotesAttachments"

    fun addAttachment(project: Project, source: File): AttachmentEntity? {
        val basePath = project.basePath ?: return null
        val targetDir = File(basePath, ATTACHMENT_DIR)
        targetDir.mkdirs()
        val safeName = source.name.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val target = uniqueFile(targetDir, safeName)
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        return AttachmentEntity().apply {
            fileName = source.name
            relativePath = "$ATTACHMENT_DIR/${target.name}".replace('\\', '/')
            contentType = Files.probeContentType(target.toPath()).orEmpty()
            sizeBytes = target.length()
        }
    }

    fun open(project: Project, attachment: AttachmentEntity) {
        val basePath = project.basePath ?: return
        val file = File(basePath, attachment.relativePath)
        if (file.exists()) BrowserUtil.browse(file.toURI())
    }

    fun delete(project: Project, attachment: AttachmentEntity) {
        val basePath = project.basePath ?: return
        File(basePath, attachment.relativePath).delete()
    }

    private fun uniqueFile(dir: File, fileName: String): File {
        val dot = fileName.lastIndexOf('.')
        val stem = if (dot >= 0) fileName.substring(0, dot) else fileName
        val ext = if (dot >= 0) fileName.substring(dot) else ""
        var candidate = File(dir, fileName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$stem-$index$ext")
            index++
        }
        return candidate
    }
}
