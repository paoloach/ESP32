package it.achdjian.plugin.esp32



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

val CONFIGURATION_NAME = "ESP32 flash rom"
const val FLASH_CONFIGURATION_ID="ESP32.FLASH"
const val FLASH_CONFIGURATION_NAME="ESP32 flash"
const val FLASH_CONFIGURATION_DESCRIPTION="Flash an ESP32 image"
const val DEBUG_CONFIGURATION_ID="ESP32.debugger"
const val DEBUG_CONFIGURATION_FACTORY_ID="ESP32.debugger"
const val DEBUG_CONFIGURATION_NAME="ESP32.debugger"
const val DEBUG_CONFIGURATION_DESCRIPTION="Debug an ESP32 image"
const val ESP32_TOOLCHAIN="ESP32"