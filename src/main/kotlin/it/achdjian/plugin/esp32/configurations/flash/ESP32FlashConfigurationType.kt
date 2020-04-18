package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeBase
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.CONFIGURATION_NAME
import it.achdjian.plugin.esp32.FLASH_CONFIGURATION_DESCRIPTION
import it.achdjian.plugin.esp32.FLASH_CONFIGURATION_ID
import it.achdjian.plugin.esp32.FLASH_CONFIGURATION_NAME

class ESP32FlashConfigurationFactory(configurationTypeESP32: ESP32FlashConfigurationType): ConfigurationFactory(configurationTypeESP32)  {
    override fun createTemplateConfiguration(project: Project): RunConfiguration = ESP32FlashRunConfiguration(project, this, CONFIGURATION_NAME)
    override fun getName() = CONFIGURATION_NAME
    override fun getId(): String= name
}


class ESP32FlashConfigurationType : ConfigurationTypeBase(
    FLASH_CONFIGURATION_ID,
    FLASH_CONFIGURATION_NAME,
    FLASH_CONFIGURATION_DESCRIPTION,
    ICON_FLASH
) {
    companion object {
        var factoryESP32: ESP32FlashConfigurationFactory?=null
    }

    init {
        if (factoryESP32 == null)
            factoryESP32 = ESP32FlashConfigurationFactory(this)
        factoryESP32?.let { addFactory(it)}
    }
}