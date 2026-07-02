package com.codenotes.plugin.settings

import com.intellij.openapi.components.*

class SettingsBean {
    var languageOverride: String = "zh-CN" // "" = follow IDE, else "en" / "zh-CN"
    var codeReviewTemplatePath: String = ""
}

@Service(Service.Level.APP)
@State(name = "CodeNotesSettings", storages = [Storage("codeNotesSettings.xml")])
class CodeNotesSettingsState : PersistentStateComponent<SettingsBean> {

    private var bean = SettingsBean()

    var languageOverride: String
        get() = bean.languageOverride
        set(value) { bean.languageOverride = value }

    var codeReviewTemplatePath: String
        get() = bean.codeReviewTemplatePath
        set(value) { bean.codeReviewTemplatePath = value }

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
