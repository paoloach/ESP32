package it.achdjian.plugin.esp32.serial

import com.intellij.execution.RunManagerEx
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import it.achdjian.plugin.esp32.ICON_SERIAL
import it.achdjian.plugin.esp32.configurationName
import it.achdjian.plugin.esp32.configurations.flash.FlashRunConfiguration

class ESP32ConnectDisconnectAction : ToggleAction("Connect", "Connect to ESP32", ICON_SERIAL), DumbAware {

    override fun isSelected(event: AnActionEvent): Boolean =
        event.project?.let {
            ServiceManager.getService(it, ESP32SerialService::class.java).isConnected()
        } ?: false

    override fun setSelected(event: AnActionEvent, doConnect: Boolean) {
        event.project?.let {project->
            val serialPortData = getFlashConfiguration(project) ?: getSerialPort(project)
            val serialService = ServiceManager.getService(project, ESP32SerialService::class.java)
            //val serialPortData  = getSerialPort(project )
            serialPortData?.let {
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
        val conf = RunManagerEx.getInstanceEx(project).allSettings.firstOrNull{it.name ==configurationName} ?: return null
        val flashConfigurationState = (conf.configuration as FlashRunConfiguration).flashConfigurationState
        return ESP32SerialPortData(flashConfigurationState.port, flashConfigurationState.baud)
    }
}