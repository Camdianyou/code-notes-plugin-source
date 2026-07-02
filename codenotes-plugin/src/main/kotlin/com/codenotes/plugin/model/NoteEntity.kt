package com.codenotes.plugin.model

import java.util.UUID

/**
 * A single note, persisted as a plain bean (public var, no-arg constructor)
 * so IntelliJ's XmlSerializer can (de)serialize it inside PersistentStateComponent
 * without any custom converters.
 *
 * Anchoring strategy: we store the project-relative file path, the 1-based
 * line range the note applies to, and a hash of the anchored text. On file
 * open we try to re-locate the exact text near the stored line range (it may
 * have shifted a few lines due to edits above it); if the hash can't be
 * found within a small search window we fall back to the stored line range
 * and show a "may be out of date" indicator. This is best-effort, matching
 * the PSI/text-hash resilience approach described in the spec without
 * requiring a full incremental PSI-anchor index (a Phase 2 hardening item).
 */
class NoteEntity {
    var id: String = UUID.randomUUID().toString()

    // Anchor
    var filePath: String = ""          // project-relative path, "" for project/module-level notes
    var elementFqName: String = ""     // e.g. fully-qualified class/method name, if known
    var lineStart: Int = -1            // 0-based, -1 if not line-anchored
    var lineEnd: Int = -1
    var textHash: String = ""          // hash of the anchored text snippet, for drift detection
    var scope: String = NoteScope.FILE.name // see NoteScope
    var anchorType: String = NoteAnchor.LINE.name

    // Symbol anchor, used when notes are pinned to Java/Kotlin symbols.
    var symbolLanguage: String = ""
    var symbolKind: String = ""
    var symbolQualifiedName: String = ""
    var symbolSignature: String = ""
    var fallbackLine: Int = -1
    var fallbackTextHash: String = ""

    // Content
    var type: String = NoteType.COMMENT.name
    var title: String = ""
    var summary: String = ""
    var description: String = ""       // markdown body
    var tags: String = ""              // comma separated
    var relatedElements: String = ""   // comma separated FQNs
    var folderId: String = ""
    var favorite: Boolean = false
    var orderIndex: Int = 0
    var attachments: MutableList<AttachmentEntity> = mutableListOf()

    // TODO-specific (only meaningful when type == TODO)
    var priority: String = TodoPriority.MEDIUM.name
    var status: String = TodoStatus.TODO.name
    var dueDate: String = ""           // ISO date, "" if none

    // Metadata
    var author: String = ""
    var createdAt: Long = System.currentTimeMillis()
    var updatedAt: Long = System.currentTimeMillis()

    override fun toString(): String = "NoteEntity($id, $filePath:$lineStart-$lineEnd, $title)"
}

enum class NoteScope { PROJECT, MODULE, PACKAGE, FOLDER, FILE, CLASS, METHOD, FIELD, SELECTION, LINE }

enum class NoteAnchor { LINE, SELECTION, SYMBOL, FILE, PROJECT }

class SymbolAnchor {
    var language: String = ""
    var symbolKind: String = ""
    var qualifiedName: String = ""
    var signature: String = ""
    var filePath: String = ""
    var fallbackLine: Int = -1
    var fallbackHash: String = ""
}

class AttachmentEntity {
    var id: String = UUID.randomUUID().toString()
    var fileName: String = ""
    var relativePath: String = ""
    var contentType: String = ""
    var sizeBytes: Long = 0
    var createdAt: Long = System.currentTimeMillis()
}

class NoteFolder {
    var id: String = UUID.randomUUID().toString()
    var name: String = ""
    var parentId: String = ""
    var orderIndex: Int = 0
}
