package it.achdjian.plugin.esp32

import com.intellij.openapi.util.IconLoader
import com.intellij.ui.IconManager
import it.achdjian.plugin.esp32.setting.ESP32Setting
import javax.swing.Icon

val ICON_SERIAL = IconLoader.findIcon("/images/iconConnect.png")
val ICON_SERIAL_FLASH= IconLoader.findIcon("/images/iconflash.png")
val CUSTOM_GDB_RUN_CONFIGURATION: Icon = IconManager.getInstance().getIcon("/images/icon-gdb-settings.svg",ESP32Setting::class.java )
val ICON_RESET: Icon = IconManager.getInstance().getIcon("/images/iconReset.svg",ESP32Setting::class.java )
