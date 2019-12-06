package it.achdjian.plugin.esp32.setting

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.installFileCompletionAndBrowseDialog
import com.intellij.ui.layout.LCFlags
import com.intellij.ui.layout.panel
import com.jetbrains.cidr.ui.ActionItemsComboBox
import it.achdjian.plugin.esp32.actions.Settings
import it.achdjian.plugin.esp32.availableBaudRate
import it.achdjian.plugin.esp32.serial.SerialPortList
import java.awt.Color
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.border.Border
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun DocumentEvent.getText(): String = document.getText(0, document.length)

class SDKPathDocumentListener(var path: String) : DocumentListener {
    var component: JComponent?
    get() = _component
    set(value) {_component=value
    _border = value?.border
    changeBorder()
    }

    private var _component: JComponent? = null
    private var _border: Border? = null


    override fun insertUpdate(p: DocumentEvent) {
        path = p.getText()
        changeBorder()

    }

    override fun removeUpdate(p: DocumentEvent) {
        path = p.getText()
        changeBorder()
    }

    override fun changedUpdate(p: DocumentEvent) {
        path = p.getText()
        changeBorder()
    }

    private fun changeBorder() {
        _component?.let {
            if (!validSDKPath(path)) {
                it.border = BorderFactory.createLineBorder(Color.RED)
            } else {
                it.border =_border
            }
        }
    }

}


class GccPathDocumentListener(var path: String) : DocumentListener {
    override fun insertUpdate(p: DocumentEvent) {
        path = p.getText()
    }

    override fun removeUpdate(p: DocumentEvent) {
        path = p.getText()
    }

    override fun changedUpdate(p: DocumentEvent) {
        path = p.getText()
    }

}


class ESP32Setting : Configurable {
    val LOG = Logger.getInstance(ESP32Setting::class.java)
    private val espToolBaudrate = ActionItemsComboBox<Int>()
    private val espToolPy = ActionItemsComboBox<String>()
    private var esp32Sdk: SDKPathDocumentListener = SDKPathDocumentListener(ESP32SettingState.sdkPath)
    private var crosscompiler: GccPathDocumentListener = GccPathDocumentListener(ESP32SettingState.crosscompilerPath)

    init {
        availableBaudRate.forEach { espToolBaudrate.addItem(it) }
    }

    /**
     * Indicates whether the Swing form was modified or not.
     * This method is called very often, so it should not take a long time.
     *
     * @return `true` if the settings were modified, `false` otherwise
     */
    override fun isModified(): Boolean {
        return esp32Sdk.path != ESP32SettingState.sdkPath
                || crosscompiler.path != ESP32SettingState.crosscompilerPath
                || espToolPy.selectedItem != ESP32SettingState.serialPortName
                || espToolBaudrate.selectedItem != ESP32SettingState.serialPortBaud
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
        ESP32SettingState.sdkPath = esp32Sdk.path
        ESP32SettingState.crosscompilerPath = crosscompiler.path
        espToolPy.selectedItem?.let { ESP32SettingState.serialPortName = it as String }
        espToolBaudrate.selectedItem?.let { ESP32SettingState.serialPortBaud = it as Int }
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
                editor.document.addDocumentListener(esp32Sdk)
                esp32Sdk.component = component.childComponent
                installFileCompletionAndBrowseDialog(
                    null,
                    component,
                    editor,
                    "ESP32 sdk path",
                    FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
                ) {
                    esp32Sdk.path = it.path
                    esp32Sdk.path
                }
                component()
            }
            row("crosscompile path: ") {
                val component = TextFieldWithHistoryWithBrowseButton()
                val editor = component.childComponent.textEditor
                editor.text = ESP32SettingState.crosscompilerPath
                editor.document.addDocumentListener(crosscompiler)
                installFileCompletionAndBrowseDialog(
                    null,
                    component,
                    editor,
                    "ESP32 crosscompilerPath path",
                    FileChooserDescriptorFactory.createSingleFileDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
                ) {
                    crosscompiler.path = it.path
                    crosscompiler.path
                }
                component()
            }
            row("Default serial port: "){
                espToolPy.isEditable = true
                espToolPy.removeAll()
                val portList = SerialPortList.getPortNames()
                portList.forEach { espToolPy.addItem(it) }
                espToolPy.addItem(ESP32SettingState.serialPortName)
                espToolPy.selectedItem = ESP32SettingState.serialPortName
                espToolPy()
            }
            row("Default serial flashing baud rate"){
                espToolBaudrate.selectedItem = ESP32SettingState.serialPortBaud
                espToolBaudrate()
            }
        }
}