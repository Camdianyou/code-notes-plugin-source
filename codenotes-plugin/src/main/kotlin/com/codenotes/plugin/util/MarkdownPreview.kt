package com.codenotes.plugin.util

object MarkdownPreview {
    fun toHtml(markdown: String): String {
        val body = markdown
            .lineSequence()
            .joinToString("\n") { line -> renderLine(line) }
        return """
            <html>
              <body style="font-family: sans-serif; font-size: 12px;">
                $body
              </body>
            </html>
        """.trimIndent()
    }

    private fun renderLine(line: String): String {
        val escaped = escape(line)
            .replace(Regex("\\*\\*(.+?)\\*\\*"), "<b>$1</b>")
            .replace(Regex("`(.+?)`"), "<code>$1</code>")
        return when {
            escaped.startsWith("### ") -> "<h3>${escaped.removePrefix("### ")}</h3>"
            escaped.startsWith("## ") -> "<h2>${escaped.removePrefix("## ")}</h2>"
            escaped.startsWith("# ") -> "<h1>${escaped.removePrefix("# ")}</h1>"
            escaped.isBlank() -> "<br/>"
            else -> "<p>$escaped</p>"
        }
    }

    private fun escape(text: String): String =
        text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
}
