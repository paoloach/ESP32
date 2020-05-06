package it.achdjian.plugin.esp32.serial

import com.intellij.openapi.Disposable
import jssc.SerialPort

interface ESP32SerialService : Disposable {
    fun isConnected(): Boolean
    fun connect(portName: String, baudRate: Int)
    fun close()
    fun write(bytes: ByteArray)
    fun addDataListener(listener: (data:ByteArray)->Unit)
    fun removeDataListener(listener: (data:ByteArray)->Unit)
    fun addPortStateListener(listener: (status:Boolean)->Unit)
    fun removePortStateListener(listener: (status:Boolean)->Unit)
    fun notifyStateListeners(isConnected: Boolean)
    var port: SerialPort?
    var baudRate: Int
}