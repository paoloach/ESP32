package it.achdjian.plugin.esp32.ui

import com.jetbrains.cidr.ui.ActionItemsComboBox
import it.achdjian.plugin.esp32.AVAILABLE_BAUD_RATE
import it.achdjian.plugin.esp32.DEFAULT_BAUD
import it.achdjian.plugin.esp32.configurations.flash.ESP32FlashRunConfiguration

class BaudRateComboBox(val selectAction:(selected: Int)->Unit) : ActionItemsComboBox<Int>() {
    init {
        AVAILABLE_BAUD_RATE.forEach { addItem(it) }
        selectedItem = DEFAULT_BAUD
        addActionListener { selectedItem?.let { selectAction(it as Int)}}
    }

    fun reset(runConfigurationESP32: ESP32FlashRunConfiguration){
        selectedItem = runConfigurationESP32.state?.baud
    }

}