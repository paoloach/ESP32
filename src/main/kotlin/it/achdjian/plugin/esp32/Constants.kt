package it.achdjian.plugin.esp32

import it.achdjian.plugin.esp32.configurations.flash.FlashSettingEditor

val configurationName = "ESP32 flash rom"

const val DEFAULT_BAUD = 115200

val availableBaudRate = listOf(
    300,
    600,
    1200,
    2400,
    4800,
    9600,
    19200,
    38400,
    57600,
    DEFAULT_BAUD,
    230400,
    460800,
    576000,
    921600,
    1000000,
    2000000
)