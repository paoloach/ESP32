package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.DEFAULT_BAUD
import org.jdom.Element


class ESP32FlashConfigurationState(project: Project? = null) : RunConfigurationOptions() {
    var configurationName:String?=null
    var port = "ttyUSB0"
    var baud = DEFAULT_BAUD

    companion object {
        private val LOG = Logger.getInstance(ESP32FlashConfigurationState::class.java)
        const val ATTR_NAME_CONF_NAME="configurationName"
        const val ATTR_NAME_PORT="port"
        const val ATTR_NAME_BAUD="baud"

    }


    fun toElement(element: Element){
        configurationName?.let { element.setAttribute(ATTR_NAME_CONF_NAME, it) }
        element.setAttribute(ATTR_NAME_PORT,port )
        element.setAttribute(ATTR_NAME_BAUD, baud.toString())
    }

    fun fromElement(element: Element) {
        configurationName = element.getAttributeValue(ATTR_NAME_CONF_NAME)
        port = element.getAttributeValue(ATTR_NAME_PORT)
        baud = element.getAttributeValue(ATTR_NAME_BAUD) ?.let { it.toIntOrNull() } ?: DEFAULT_BAUD
    }

}