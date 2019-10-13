package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBComboBoxLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import com.jetbrains.cidr.execution.CidrRunConfigurationEditorUtil
import it.achdjian.plugin.esp32.actions.configParsing
import javax.swing.JComboBox

class FlashSettingEditor : SettingsEditor<FlashRunConfiguration>() {
    private val target = ComboBox<String>()
    private val image = ComboBox<String>()
    private val espToolPy = JBTextField("/dev/ttyUSB0")
    private val espToolBaudrate = JBTextField("115200B")


    override fun resetEditorFrom(runConfiguration: FlashRunConfiguration) {
        val state = runConfiguration.state
        state?.let { st ->
            val config = configParsing(runConfiguration.project)
            config["ESPTOOLPY_PORT"]?.let {
                st.port = it
                espToolPy.text = it}
            config["ESPTOOLPY_BAUD"]?.let{
                st.baud = it
                espToolBaudrate.text=it
            }
        }
    }

    override fun applyEditorTo(runConfiguration: FlashRunConfiguration) {

        val state = runConfiguration.state
        state?.let {st->
            image.text.let { st.image = it }
            espToolPy.text ?.let {st.port = it}
            espToolBaudrate.text ?.let{st.baud=it}
        }


    }

    override fun createEditor() = panel{
        row("Serial port"){
            image()
            espToolPy()
            espToolBaudrate()
        }
    }
}