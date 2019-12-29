package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.ConfigurationTypeBase

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