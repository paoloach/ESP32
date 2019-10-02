package it.achdjian.plugin.esp32.configurator

import com.intellij.ui.components.JBTextArea
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import java.awt.BorderLayout
import java.io.File
import javax.swing.JPanel

class MissingConfig(clionPanel: JPanel) : JPanel(BorderLayout()) {
    init {
        add(clionPanel, BorderLayout.PAGE_START)
        val warning = JBTextArea()
        warning.isEditable=false
        if (ESP32SettingState.sdkPath.isEmpty()){
            warning.append("Please before to proceed set a valid ESP32 espressif SDK path in the settings\n")
            warning.append("Open File->setting->Build,Execution->ESP32 config")
        } else {
            val idfPath = File(ESP32SettingState.sdkPath)
            if (!idfPath.exists()) {
                warning.append("The ESP32 SDK path\n${ESP32SettingState.sdkPath}\npath doesn't exist\n")
                warning.append("Please set the right one")
            }  else {
                val kConfig = File(idfPath, "Kconfig")
                if (!kConfig.exists()){
                    warning.append("The path ${ESP32SettingState.sdkPath} is NOT valid\n")
                    warning.append("It must contain a Kconfig file\n")
                    warning.append("Please change the setting for ESP32 plugin\n")
                }
            }
        }
        add(warning, BorderLayout.CENTER)
    }
}