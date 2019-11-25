package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.ui.layout.panel
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.ui.ActionItemsComboBox
import it.achdjian.plugin.esp32.actions.configParsing
import org.jetbrains.annotations.NotNull
import java.io.File

class FlashSettingEditor(private val project: @NotNull Project) : SettingsEditor<FlashRunConfiguration>() {
    companion object {
        const val DEFAULT_BAUD = 115200
        val availableBaudRate = listOf(
            300,
            600,
            1200,
            2400,
            4800,
            9600,
            19200,
            38400,
            57600,
            DEFAULT_BAUD,
            230400,
            460800,
            576000,
            921600,
            1000000,
            2000000
        )
    }

    private val configuration = ActionItemsComboBox<String>()
    private val espToolPy = ActionItemsComboBox<String>()
    private val espToolBaudrate = ActionItemsComboBox<Int>()

    init {
        espToolPy.isEditable = true

    }

    override fun resetEditorFrom(runConfiguration: FlashRunConfiguration) {
        val targets = CMakeWorkspace.getInstance(project).modelTargets
        configuration.removeAll()
        espToolPy.removeAll()
        targets.find { it.name == "flash" }
            ?.let { it.buildConfigurations.forEach {conf-> configuration.addItem(conf.profileName) } }

        val state = runConfiguration.flashConfigurationState
        state.configurationName?.let { configuration.selectedItem = state.configurationName }

        val portList = createPortList()
        portList.forEach { espToolPy.addItem(it) }
        val config = configParsing(runConfiguration.project)
        val port = if (state.port.isNullOrBlank()) {
            config["ESPTOOLPY_PORT"]?.let { it }
        } else {
            state.port
        }
        if (!port.isNullOrBlank() && !portList.contains(port)) {
            espToolPy.addItem(port)
        }
        espToolPy.selectedItem = port

        availableBaudRate.forEach { espToolBaudrate.addItem(it) }
        val baud = state.baud
        espToolBaudrate.selectedItem = baud
    }


    private fun selectBaud(baud: Int?){
        if (!availableBaudRate.contains(baud)) {
            espToolBaudrate.addItem(baud)
        }
        espToolBaudrate.selectedItem = baud
    }

    override fun applyEditorTo(runConfiguration: FlashRunConfiguration) {

        val state = runConfiguration.flashConfigurationState
        configuration.selectedItem?.let { state.configurationName = it as String }
        espToolPy.selectedItem?.let { state.port = it as String }
        espToolBaudrate.selectedItem?.let { state.baud = it as Int }
    }

    override fun createEditor() = panel {
        row("Configuration") {
            configuration()
        }
        row("Serial port") {
            espToolPy()
        }
        row("Serial port baud rate") {
            espToolBaudrate()
        }
    }

    private fun createPortList() =
        File("/dev").walk().filter {
            it.name.startsWith("ttyS") || it.name.startsWith("ttyUSB")
        }.map { it.name }

}