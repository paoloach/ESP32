package it.achdjian.plugin.esp32.ui

import com.jetbrains.cidr.ui.ActionItemsComboBox
import it.achdjian.plugin.esp32.serial.ESP32SerialPortList
import it.achdjian.plugin.esp32.setting.ESP32SettingState

class SerialPortListComboBox(val selectAction:(selected: String)->Unit): ActionItemsComboBox<String>() {
    init {
        isEditable = true
        val portList = ESP32SerialPortList.getPortNames()
        portList.forEach { addItem(it) }

        addItem(ESP32SettingState.serialPortName)
        selectedItem = ESP32SettingState.serialPortName
        addActionListener { selectedItem?.let { selectAction(it as String)}}
    }

    fun isModified() = selectedItem != ESP32SettingState.serialPortName
}