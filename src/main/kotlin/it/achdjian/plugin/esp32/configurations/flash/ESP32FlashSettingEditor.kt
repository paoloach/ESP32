package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.ui.ActionItemsComboBox
import it.achdjian.plugin.esp32.DEFAULT_BAUD
import it.achdjian.plugin.esp32.actions.configParsing
import it.achdjian.plugin.esp32.serial.ESP32SerialPortData
import it.achdjian.plugin.esp32.serial.ESP32SerialPortList
import it.achdjian.plugin.esp32.ui.BaudRateComboBox
import it.achdjian.plugin.esp32.ui.GridLayout2
import javax.swing.JComponent
import javax.swing.JLabel

class ESP32FlashSettingEditor(private val project: Project) : SettingsEditor<ESP32FlashRunConfiguration>() {
    val serialPortData = ESP32SerialPortData("ttyUsb0", DEFAULT_BAUD)

    private val configuration = ActionItemsComboBox<String>()
    private val espToolPy = ActionItemsComboBox<String>()
    private val espToolBaudRate =  BaudRateComboBox{ serialPortData.baud = it }

    init {
        espToolPy.isEditable = true

    }

    override fun resetEditorFrom(runConfigurationESP32: ESP32FlashRunConfiguration) {
        val targets = CMakeWorkspace.getInstance(project).modelTargets
        configuration.removeAll()
        espToolPy.removeAll()
        targets.find { it.name == "flash" }
            ?.let { it.buildConfigurations.forEach { conf -> configuration.addItem(conf.profileName) } }

        val state = runConfigurationESP32.flashConfigurationState
        state.configurationName?.let { configuration.selectedItem = state.configurationName }


        val portList = ESP32SerialPortList().getPortNames()
        if (portList.isEmpty()){
            espToolPy.addItem("No valid port");
        } else {
            portList.forEach { espToolPy.addItem(it) }
        }
        val config = configParsing(runConfigurationESP32.project)
        val port = if (state.port.isBlank()) {
            config["ESPTOOLPY_PORT"]?.let { it }
        } else {
            state.port
        }
        if (portList.contains(port)){
            espToolPy.selectedItem = port
        }
        espToolPy.selectedItem = port

        val baud = state.baud
    }


    override fun applyEditorTo(runConfigurationESP32: ESP32FlashRunConfiguration) {

        val state = runConfigurationESP32.flashConfigurationState
        configuration.selectedItem?.let { state.configurationName = it as String }
        espToolPy.selectedItem?.let { state.port = it as String }
        state.baud = serialPortData.baud
    }

    override fun createEditor():JComponent {
        val panel = DialogPanel()
        panel.layout = GridLayout2(3,2)
        panel.add(JLabel("Configuration"))
        panel.add(configuration)
        panel.add(JLabel("Serial port"))
        panel.add(espToolPy)
        panel.add(JLabel("Serial Baud Rate"))
        panel.add(espToolBaudRate)
        return panel
    }
}