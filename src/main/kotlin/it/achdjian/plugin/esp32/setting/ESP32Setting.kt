package it.achdjian.plugin.esp32.setting

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.installFileCompletionAndBrowseDialog
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import javax.swing.JComponent

class ESP32Setting : Configurable {

    private var esp32Sdk: String
    private var crosscompiler: String

    init {
        esp32Sdk = ESP32SettingState.sdkPath
        crosscompiler = ESP32SettingState.crosscompilerPath
    }

    /**
     * Indicates whether the Swing form was modified or not.
     * This method is called very often, so it should not take a long time.
     *
     * @return `true` if the settings were modified, `false` otherwise
     */
    override fun isModified(): Boolean {
        return true
        //return esp32Sdk != state.sdkPath || gcc != state.gccPath || cxx != state.cxxPath
    }

    /**
     * Returns the visible name of the configurable component.
     * Note, that this method must return the display name
     * that is equal to the display name declared in XML
     * to avoid unexpected errors.
     *
     * @return the visible name of the configurable component
     */
    override fun getDisplayName(): String = "ESP32 config"

    /**
     * Stores the settings from the Swing form to the configurable component.
     * This method is called on EDT upon user's request.
     *
     */
    override fun apply() {
        ESP32SettingState.sdkPath = esp32Sdk
        ESP32SettingState.crosscompilerPath = crosscompiler
    }

    /**
     * Creates new Swing form that enables user to configure the settings.
     * Usually this method is called on the EDT, so it should not take a long time.
     *
     * Also this place is designed to allocate resources (subscriptions/listeners etc.)
     * @see .disposeUIResources
     *
     *
     * @return new Swing form to show, or `null` if it cannot be created
     */
    override fun createComponent(): JComponent =
        panel(LCFlags.fillX, title = "Tools path") {
            row("ESP32 espressif SDK path") {
                val component = TextFieldWithHistoryWithBrowseButton()
                val editor = component.childComponent.textEditor
                editor.text = ESP32SettingState.sdkPath
                installFileCompletionAndBrowseDialog(
                    null,
                    component,
                    editor,
                    "ESP32 sdk path",
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
                ) {
                    esp32Sdk = it.path
                    esp32Sdk
                }
                component()
            }
            row("crosscompile path: ") {
                val component = TextFieldWithHistoryWithBrowseButton()
                val editor = component.childComponent.textEditor
                editor.text = ESP32SettingState.crosscompilerPath
                installFileCompletionAndBrowseDialog(
                    null,
                    component,
                    editor,
                    "ESP32 crosscompilerPath path",
                    FileChooserDescriptorFactory.createSingleFileDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
                ) {
                    crosscompiler = it.path
                    crosscompiler
                }
                component()
            }
        }
}