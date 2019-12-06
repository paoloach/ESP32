package it.achdjian.plugin.esp32.setting

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.Logger
import java.io.File


fun validSDKPath(sdkPath:String):Boolean {
    val idfPath = File(sdkPath)
    return if (idfPath.exists()) {
        val kConfig = File(idfPath, "Kconfig")
        kConfig.exists()
    } else {
        false
    }
}

object ESP32SettingState {
    private val LOG = Logger.getInstance(ESP32SettingState::class.java)
    private const val SDK_PATH_KEY="ESP32_SDKPATH"
    private const val COMPILER_PATH_KEY="ESP32_CROSSCOMPILER_PATH"
    private const val SERIAL_PORT_KEY="ESP32_SERIAL_PORT"
    private const val SERIAL_PORT_BAUD_KEY="ESP32_SERIAL_PORT_BAUD"

    var sdkPath: String
        get() = PropertiesComponent.getInstance().getValue(SDK_PATH_KEY, "")
        set(path) =  savePath(path)



    var crosscompilerPath: String
        get() = PropertiesComponent.getInstance().getValue(COMPILER_PATH_KEY, "")
        set(path) =  PropertiesComponent.getInstance().setValue(COMPILER_PATH_KEY, path)

    var serialPortName: String
        get() = PropertiesComponent.getInstance().getValue(SERIAL_PORT_KEY, "/dev/ttyUSB0")
        set(portName) =  PropertiesComponent.getInstance().setValue(SERIAL_PORT_KEY, portName)

    var serialPortBaud: Int
        get() = PropertiesComponent.getInstance().getInt(SERIAL_PORT_BAUD_KEY, 921600)
        set(baud) =  PropertiesComponent.getInstance().setValue(SERIAL_PORT_BAUD_KEY, baud,0)


    fun validSDKPath():Boolean  = validSDKPath(sdkPath)



    private fun savePath(path:String){
        LOG.info("save path to $path")
        PropertiesComponent.getInstance().setValue(SDK_PATH_KEY, path)
    }
}
