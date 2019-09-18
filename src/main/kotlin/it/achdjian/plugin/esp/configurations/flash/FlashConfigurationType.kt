package it.achdjian.plugin.esp.configurations.flash

import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.configurations.SimpleConfigurationType
import com.intellij.openapi.project.Project

class FlashConfigurationType : SimpleConfigurationType(FLASH_CONFIGURATION_ID, FLASH_CONFIGURATION_NAME, FLASH_CONFIGURATION_DESCRIPTION) {
    override fun createTemplateConfiguration(p0: Project): RunConfiguration {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}