package it.achdjian.plugin.esp32.setting

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage


object ESP32SettingState {
    var sdkPath: String
        get() = PropertiesComponent.getInstance().getValue("ESP32_SDKPATH", "")
        set(path) =  PropertiesComponent.getInstance().setValue("ESP32_SDKPATH", path)

    var crosscompilerPath: String
        get() = PropertiesComponent.getInstance().getValue("ESP32_CROSSCOMPILER_PATH", "")
        set(path) =  PropertiesComponent.getInstance().setValue("ESP32_CROSSCOMPILER_PATH", path)

}
