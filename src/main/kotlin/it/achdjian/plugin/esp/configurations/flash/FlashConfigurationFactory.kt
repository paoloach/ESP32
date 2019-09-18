package it.achdjian.plugin.esp.configurations.flash

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project

class FlashConfigurationFactory(private val configurationType: FlashConfigurationType): ConfigurationFactory(configurationType)  {
    override fun createTemplateConfiguration(p0: Project): RunConfiguration {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

