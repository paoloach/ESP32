package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import it.achdjian.plugin.esp32.DEFAULT_BAUD

@State(
    name = "ESP32",
    storages = [Storage(
        value = "ESP32-support.xml",
        roamingType = RoamingType.PER_OS
    )]
)
class ESP32DebugConfigurationState: PersistentStateComponent<ESP32DebugConfigurationState> {
    var openOcdLocation = ""
    var configurationName:String?=null
    var port = "ttyUSB0"
    var baud = DEFAULT_BAUD


    override fun getState(): ESP32DebugConfigurationState {
        return this
    }

    override fun loadState(state: ESP32DebugConfigurationState) {
        configurationName = state.configurationName
        openOcdLocation = state.openOcdLocation
        port = state.port
        baud = state.baud
    }

}