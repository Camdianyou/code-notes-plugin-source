package com.codenotes.plugin.settings

import com.intellij.openapi.components.*

class SettingsBean {
    var languageOverride: String = "" // "" = follow IDE, else "en" / "zh-CN"
}

@Service(Service.Level.APP)
@State(name = "CodeNotesSettings", storages = [Storage("codeNotesSettings.xml")])
class CodeNotesSettingsState : PersistentStateComponent<SettingsBean> {

    private var bean = SettingsBean()

    var languageOverride: String
        get() = bean.languageOverride
        set(value) { bean.languageOverride = value }

    override fun getState(): SettingsBean = bean

    override fun loadState(state: SettingsBean) {
        bean = state
    }

    companion object {
        fun getInstance(): CodeNotesSettingsState =
            com.intellij.openapi.application.ApplicationManager.getApplication()
                .getService(CodeNotesSettingsState::class.java)
    }
}
