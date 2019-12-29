package it.achdjian.plugin.esp32.serial

import com.intellij.openapi.Disposable
import com.intellij.util.Consumer
import jssc.SerialPort

interface ESP32SerialService : Disposable {
    fun isConnected(): Boolean
    fun connect(portName: String, baudRate: Int)
    fun close()
    fun write(bytes: ByteArray)
    fun addDataListener(listener: Consumer<ByteArray>)
    fun removeDataListener(listener: Consumer<ByteArray>)
    fun addPortStateListener(listener: Consumer<Boolean>)
    fun removePortStateListener(listener: Consumer<Boolean>)
    fun notifyStateListeners(isConnected: Boolean)
    var port: SerialPort?
    var baudRate: Int
}