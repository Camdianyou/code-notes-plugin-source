package com.codenotes.plugin.util

import com.codenotes.plugin.settings.CodeNotesSettingsState
import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey
import java.util.Locale

private const val BUNDLE = "messages.CodeNotesBundle"

/**
 * Central i18n access point. By default this follows IntelliJ's own locale
 * (via DynamicBundle, which already respects the Language and Region Settings
 * plugin when installed). Users can also force English or Chinese from the
 * Code Notes settings page regardless of the IDE's locale.
 *
 * Adding a new language later (Japanese, Korean, French, German, Spanish...)
 * only requires dropping a new messages/CodeNotesBundle_xx.properties file —
 * no code changes needed, since we never hard-code strings anywhere else in
 * the plugin.
 */
object CodeNotesBundle : DynamicBundle(BUNDLE) {

    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
        val override = CodeNotesSettingsState.getInstance().languageOverride
        return if (override.isBlank()) {
            getMessage(key, *params)
        } else {
            val bundle = java.util.ResourceBundle.getBundle(BUNDLE, Locale.forLanguageTag(override))
            try {
                java.text.MessageFormat.format(bundle.getString(key), *params)
            } catch (e: Exception) {
                getMessage(key, *params)
            }
        }
    }
}
