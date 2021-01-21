package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.WriteExternalException
import org.jdom.Element

class ESP32FlashRunConfiguration(project: Project, factory: ConfigurationFactory, name:String) : RunConfigurationBase<ESP32FlashConfigurationState>(project, factory, name) {
    val flashConfigurationState = ESP32FlashConfigurationState()

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        ESP32FlashSettingEditor(project)

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        return ESP32FlashLauncher(executionEnvironment, flashConfigurationState)
    }

    @Throws(WriteExternalException::class)
    override fun writeExternal(element: Element) {
        flashConfigurationState.toElement(element)
    }

    @Throws(InvalidDataException::class)
     override fun readExternal(element: Element) {
         flashConfigurationState.fromElement(element)
    }

    override  fun clone(): RunConfiguration {
        val cloned = super.clone() as ESP32FlashRunConfiguration
        cloned.flashConfigurationState.configurationName = flashConfigurationState.configurationName
        cloned.flashConfigurationState.port = flashConfigurationState.port
        cloned.flashConfigurationState.baud = flashConfigurationState.baud
        return cloned
    }
}