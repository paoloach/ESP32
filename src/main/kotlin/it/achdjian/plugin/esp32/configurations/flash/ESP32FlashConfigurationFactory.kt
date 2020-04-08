package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.configurationName

class ESP32FlashConfigurationFactory(configurationTypeESP32: ESP32FlashConfigurationType): ConfigurationFactory(configurationTypeESP32)  {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        ESP32FlashRunConfiguration(project, this, configurationName)
    override fun getName() = configurationName

    override fun getId(): String= name
}

