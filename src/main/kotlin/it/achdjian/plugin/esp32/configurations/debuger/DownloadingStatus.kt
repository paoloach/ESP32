package it.achdjian.plugin.esp32.configurations.debuger

enum class DownloadingStatus {
    FLASH_NO_DEVICE_FOUND,
    FLASH_JTAG_ERROR,
    FLASH_GENERIC_ERROR,
    FLASH_PROGRAM_START,
    FLASH_PROGRAM_SUCCESS,
    FLASH_VERIFY_SUCCESS,
    FLASH_DOWNLOAD_SUCCESS,
    FLASH_WARNING,
    FLASH_ERROR
}