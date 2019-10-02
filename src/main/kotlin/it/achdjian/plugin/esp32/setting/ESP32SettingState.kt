package it.achdjian.plugin.esp32.setting

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import it.achdjian.plugin.esp32.actions.Settings
import java.io.File


fun validSDKPath(sdkPath:String):Boolean {
    val idfPath = File(sdkPath)
    if (idfPath.exists()) {
        val kConfig = File(idfPath, "Kconfig")
        return kConfig.exists()
    } else {
        return false
    }
}

object ESP32SettingState {
    private val LOG = Logger.getInstance(ESP32SettingState::class.java)
    private const val SDK_PATH_KEY="ESP32_SDKPATH"
    private const val COMPILER_PATH_KEY="ESP32_CROSSCOMPILER_PATH"

    var sdkPath: String
        get() = PropertiesComponent.getInstance().getValue(SDK_PATH_KEY, "")
        set(path) =  savePath(path)



    var crosscompilerPath: String
        get() = PropertiesComponent.getInstance().getValue(COMPILER_PATH_KEY, "")
        set(path) =  PropertiesComponent.getInstance().setValue(COMPILER_PATH_KEY, path)

    fun validSDKPath():Boolean  = validSDKPath(sdkPath)

    private fun savePath(path:String){
        LOG.info("save path to $path")
        PropertiesComponent.getInstance().setValue(SDK_PATH_KEY, path)
    }
}
