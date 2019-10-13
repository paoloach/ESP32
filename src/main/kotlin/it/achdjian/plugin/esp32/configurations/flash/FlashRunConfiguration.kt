package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationBase
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class FlashRunConfiguration(project: Project, factory: ConfigurationFactory, name:String) : RunConfigurationBase<FlashConfigurationState>(project, factory, name) {
    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
        FlashSettingEditor()

    override fun getState(executor: Executor, executionEnvironment: ExecutionEnvironment): RunProfileState? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}