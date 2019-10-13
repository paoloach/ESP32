package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.ConfigurationTypeBase

class FlashConfigurationType : ConfigurationTypeBase(
    FLASH_CONFIGURATION_ID,
    FLASH_CONFIGURATION_NAME,
    FLASH_CONFIGURATION_DESCRIPTION,
    ICON_FLASH
) {
    init {
        addFactory(FlashConfigurationFactory(this))
    }
}