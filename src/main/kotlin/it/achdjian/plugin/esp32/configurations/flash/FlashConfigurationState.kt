package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.RunConfigurationOptions
import org.jdom.Element


class FlashConfigurationState : RunConfigurationOptions() {
    var configurationName:String?=null
    var port: String?=null
    var baud: Int?=null

    companion object {
        const val ATTR_NAME_CONF_NAME="configurationName"
        const val ATTR_NAME_PORT="port"
        const val ATTR_NAME_BAUD="baud"

    }


    fun toElement(element: Element){
        configurationName?.let { element.setAttribute(ATTR_NAME_CONF_NAME, it) }
        port?.let{element.setAttribute(ATTR_NAME_PORT,it ) }
        baud?.let{ element.setAttribute(ATTR_NAME_BAUD, it.toString())}
    }

    fun fromElement(element: Element) {
        configurationName = element.getAttributeValue(ATTR_NAME_CONF_NAME)
        port = element.getAttributeValue(ATTR_NAME_PORT)
        baud = element.getAttributeValue(ATTR_NAME_BAUD).toIntOrNull()
    }

}