package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.configurationName

class ESP32DebugConfigurationFactory(configurationTypeESP32: ESP32DebugConfigurationType): ConfigurationFactory(configurationTypeESP32)  {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        ESP32DebugRunConfiguration(project, this, configurationName)
    override fun getName() = configurationName
}

