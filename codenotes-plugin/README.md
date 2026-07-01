# Code Notes — IntelliJ IDEA Plugin (MVP)

A knowledge base attached directly to your source code: file/line-anchored
notes, TODOs, gutter icons, a searchable tool window, and a bilingual
(English / 简体中文) UI — all stored locally per-project.

## What's actually implemented (real, working code — not a mockup)

- **Add Code Note...** — right-click any line or selection in the editor.
- Note types: Comment, Bug, Question, Optimization, Review, Warning,
  Important, Architecture, Temporary, Permanent, TODO, Decision.
- Rich-ish fields per note: title, summary, Markdown description, tags,
  and (for TODO) priority / status / due date.
- **Line + content-hash anchoring**: each note remembers the text of its
  anchored line(s). When the file is reopened, the plugin searches ±200
  lines around the stored position for a hash match, so notes stay roughly
  correct even if code shifted above them.
- **Gutter icons** on every annotated line, with a hover tooltip and
  click-to-edit.
- **Code Notes tool window** (right-hand side): searchable table of every
  note in the project, double-click to jump straight to the code,
  right-click to edit/delete.
- **Persistence**: `<project>/.idea/codeNotes.xml` — plain XML, diff-able,
  no external database required.
- **Settings → Tools → Code Notes**: switch UI language (Follow IDE /
  English / 简体中文) independent of the IDE's own locale.
- **i18n architecture**: zero hard-coded UI strings; everything routes
  through `CodeNotesBundle`. Adding Japanese/Korean/French/etc. later is
  just dropping a new `messages/CodeNotesBundle_xx.properties` file.

## Compatibility

`since-build="231"` (2023.1) with **no** `until-build` set, so the built
plugin is not pinned to a specific IDE version — it keeps working on newer
IntelliJ Platform releases without a rebuild. Works in IntelliJ IDEA
Community and Ultimate, and any other IntelliJ-Platform IDE (WebStorm,
PyCharm Pro, GoLand, Rider, etc.) since it only depends on
`com.intellij.modules.platform`.

## Build it

You need a local JDK 17+ and internet access to JetBrains'/Gradle's
artifact servers (this project was authored in a sandboxed environment
without that access, so the zip you have is source, not a compiled jar —
one build step turns it into an installable plugin):

```bash
cd code-notes
# generate the Gradle wrapper once if it's not already present:
gradle wrapper --gradle-version 8.5
./gradlew buildPlugin
```

The installable plugin zip will be at
`build/distributions/code-notes-1.0.0.zip`.

Install it via **Settings → Plugins → ⚙️ → Install Plugin from Disk...**

For quick manual testing without installing anywhere, run:

```bash
./gradlew runIde
```

which launches a sandboxed IDE instance with the plugin already loaded.

## Project layout

```
src/main/kotlin/com/codenotes/plugin/
  model/        NoteEntity, NoteType, NoteScope, Todo enums
  state/        NoteStorageService (project-level PersistentStateComponent)
  settings/     CodeNotesSettingsState (app-level) + CodeNotesConfigurable UI
  actions/      AddNoteAction (editor right-click)
  ui/           NoteEditorDialog (add/edit form)
  toolwindow/   CodeNotesToolWindowFactory, CodeNotesPanel, NotesTableModel
  gutter/       NoteGutterEditorListener, NoteGutterIconRenderer
  util/         AnchorUtil (hashing/relocation), CodeNotesBundle (i18n)
src/main/resources/
  META-INF/plugin.xml
  messages/CodeNotesBundle*.properties
```

The layers are deliberately separated (storage vs. UI vs. anchoring vs.
actions) so each of the roadmap items below can be added by extending one
layer without rewriting the others.

## Roadmap (not yet implemented — by design, to ship something real first)

**Phase 2 — Professional**
- Full rich editor: Markdown live preview, Mermaid/PlantUML rendering,
  LaTeX, code-block syntax highlighting, drag-and-drop image/file
  attachments, paste-screenshot support.
- PSI-element anchoring (not just line+hash) so notes track a method/class
  even across refactors and renames.
- TODO Kanban board and timeline view.
- Full-text/fuzzy/regex search engine with a dedicated search window.
- Note version history, diff, and rollback.

**Phase 3 — Enterprise**
- Knowledge graph view (interactive graph of classes/methods/notes/TODOs).
- SQLite-backed storage option for 100k+ notes with lazy loading/indexing.
- Git-aware anchoring (blame-based), team sync/merge conflict resolution.
- Encryption / password-protected notes, sensitive-data masking.
- Code review mode (threaded replies, resolve/reopen).

**Phase 4 — Marketplace release**
- AI Assistant panel (pluggable provider) for summary/JavaDoc/KDoc
  generation, bug detection, refactor & unit-test suggestions.
- JavaDoc/KDoc import/export/sync.
- Cloud/remote server sync with auto-backup and conflict resolution.
- Dashboard (coverage stats, heatmap, most-viewed/most-active files).
- Additional languages (Japanese, Korean, French, German, Spanish, ...).

Each of these plugs into the existing `NoteStorageService` /
`NoteEditorDialog` / `CodeNotesBundle` seams rather than requiring a
rewrite.
