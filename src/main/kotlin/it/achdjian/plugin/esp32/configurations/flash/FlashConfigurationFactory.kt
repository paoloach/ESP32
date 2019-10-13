package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.configurations.flash.FlashConfigurationType
import it.achdjian.plugin.esp32.configurations.flash.FlashRunConfiguration

class FlashConfigurationFactory(configurationType: FlashConfigurationType): ConfigurationFactory(configurationType)  {
    override fun createTemplateConfiguration(project: Project): RunConfiguration =
        FlashRunConfiguration(project, this, "ESP32 flash rom")
    override fun getName() = "ESP32 flash rom"
}

