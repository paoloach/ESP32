package it.achdjian.plugin.esp32.configurations.flash

import com.intellij.execution.configurations.RunConfigurationOptions
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.actions.configParsing
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import org.jdom.Element


class FlashConfigurationState(project: Project? = null) : RunConfigurationOptions() {
    var configurationName:String?=null
    var port = "ttyUSB0"
    var baud = FlashSettingEditor.DEFAULT_BAUD

    init {
//        project?.let {
//            val config = configParsing(it)
//            port =  config["ESPTOOLPY_PORT"]?.let { it } ?: "ttyUSB0"
//            baud = config["ESPTOOLPY_BAUD"]?.toIntOrNull() ?: FlashSettingEditor.DEFAULT_BAUD
//        }

    }

    companion object {
        private val LOG = Logger.getInstance(ESP32SettingState::class.java)
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
        baud = element.getAttributeValue(ATTR_NAME_BAUD) ?.let { it.toIntOrNull() } ?: FlashSettingEditor.DEFAULT_BAUD
    }

}