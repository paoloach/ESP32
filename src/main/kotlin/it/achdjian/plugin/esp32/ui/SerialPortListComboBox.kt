package it.achdjian.plugin.esp32.ui

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.cidr.ui.ActionItemsComboBox
import it.achdjian.plugin.esp32.serial.ESP32SerialPortList
import it.achdjian.plugin.esp32.setting.ESP32SettingState

class SerialPortListComboBox(val selectAction:(selected: String)->Unit): ActionItemsComboBox<String>() {
    companion object {
        val LOG = Logger.getInstance(SerialPortListComboBox::class.java)
    }
    init {
        val serialPortList = ESP32SerialPortList()
        isEditable = true
        try {
            val portList = serialPortList.getPortNames()
            portList.forEach { addItem(it) }
        } catch (e: Exception){
            LOG.error("Unable to create the serial port list", e)
            addItem("Not available")
        }
        addItem(ESP32SettingState.serialPortName)
        selectedItem = ESP32SettingState.serialPortName
        addActionListener { selectedItem?.let { selectAction(it as String)}}
    }

    fun isModified() = selectedItem != ESP32SettingState.serialPortName
}