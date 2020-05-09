package it.achdjian.plugin.esp32.serial

import it.achdjian.plugin.esp32.DEFAULT_BAUD

const val NO_PORT = "No Serial Port"

data class ESP32SerialPortData(var portName:String=NO_PORT, var baud:Int  = DEFAULT_BAUD)