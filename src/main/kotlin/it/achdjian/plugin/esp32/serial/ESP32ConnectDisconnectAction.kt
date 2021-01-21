package it.achdjian.plugin.esp32.serial

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.CONFIGURATION_NAME
import it.achdjian.plugin.esp32.ICON_SERIAL
import it.achdjian.plugin.esp32.configurations.flash.ESP32FlashRunConfiguration
import it.achdjian.plugin.esp32.setting.ESP32SettingState

class ESP32ConnectDisconnectAction(val customPortData: ESP32SerialPortData) : ToggleAction("Connect", "Connect to ESP32", ICON_SERIAL), DumbAware {

    override fun isSelected(event: AnActionEvent): Boolean =
        event.project?.let {
            ServiceManager.getService(it, ESP32SerialService::class.java).isConnected()
        } ?: false

    override fun setSelected(event: AnActionEvent, doConnect: Boolean) {
        event.project?.let {project->
            var serialPortData = ESP32SerialPortData(customPortData.portName, customPortData.baud)   //.getFlashConfiguration(project) ?: getSerialPort(project)
            if (serialPortData.portName == NO_PORT){
                getFlashConfiguration(project)
                    ?.let { serialPortData = it }
                    ?: run{serialPortData = ESP32SerialPortData( ESP32SettingState.serialPortName, ESP32SettingState.serialPortBaud)}
            }
            val serialService = ServiceManager.getService(project, ESP32SerialService::class.java)
            //val serialPortData  = getSerialPort(project )
            serialPortData.let {
                try {
                    if (doConnect) {
                        serialService.connect(it.portName, it.baud)
                    } else {
                        serialService.close()
                    }
                } catch (sme: ESP32SerialMonitorException) {
                    sme.message?.let { message ->
                        ESP32NotificationsService.createErrorNotification(message).notify(project)
                    }
                }
            } ?: ESP32NotificationsService.createErrorNotification("Unable to get the serial port to use").notify(project)
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)

        val presentation = event.presentation
        presentation.isEnabled = false
        event.project?.let { project ->
            presentation.isEnabled = true
            val serialService = ServiceManager.getService(project, ESP32SerialService::class.java)
            if (serialService.isConnected()) {
                presentation.text = "Disconnect"
            } else {
                presentation.text = "Connected"
            }
        }
    }

    private fun getFlashConfiguration(project: Project): ESP32SerialPortData? {
        val conf = RunManagerEx.getInstanceEx(project).allSettings.firstOrNull{it.name ==CONFIGURATION_NAME} ?: return null
        val flashConfigurationState = (conf.configuration as ESP32FlashRunConfiguration).flashConfigurationState
        return ESP32SerialPortData(flashConfigurationState.port, flashConfigurationState.baud)
    }
}