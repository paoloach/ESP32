package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.RunConfigurationSingletonPolicy
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType

class Factory(val esp32Conf: ESP32DebugConfigurationType) : ConfigurationFactory(esp32Conf){
    override fun createTemplateConfiguration(project: Project): RunConfiguration  = ESP32DebugRunConfiguration(project, esp32Conf.factory, "" )
    override fun getSingletonPolicy(): RunConfigurationSingletonPolicy = RunConfigurationSingletonPolicy.SINGLE_INSTANCE_ONLY
    override fun getId(): String= DEBUG_CONFIGURATION_NAME
}

class ESP32DebugConfigurationType : CMakeRunConfigurationType(
    DEBUG_CONFIGURATION_ID,
    DEBUG_CONFIGURATION_FACTORY_ID,
    DEBUG_CONFIGURATION_NAME,
    DEBUG_CONFIGURATION_DESCRIPTION,
    DEBUG_ICON
) {
    val factory = Factory(this)


    override fun createEditor(project: Project): SettingsEditor<out CMakeAppRunConfiguration> = ESP32DebugSettingEditor(project, getHelper(project))

    override fun createRunConfiguration(project: Project, configurationFactory: ConfigurationFactory): CMakeAppRunConfiguration =
        ESP32DebugRunConfiguration(project, this.factory, "")
}