package it.achdjian.plugin.esp32.serial

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import it.achdjian.plugin.esp32.actions.configParsing

class ESP32SerialMonitorToolWindowFactory : ToolWindowFactory, DumbAware {
    private var portData =  ESP32SerialPortData()
    private var serialMonitorPanel: ESP32SerialMonitorPanel?=null

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        initToolWindow(project,toolWindow)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        val config = configParsing(project, false)
        if (config.isEmpty())
            return false
        return config["IDF_TARGET"]?.let { it == "esp32" } ?: false
    }

    private fun initToolWindow(project: Project, toolWindow: ToolWindow) {
//        val conf = RunManagerEx.getInstanceEx(project).allSettings.firstOrNull{it.name == CONFIGURATION_NAME } ?: return null
//        val flashConfigurationState = (conf.configuration as ESP32FlashRunConfiguration).flashConfigurationState
//        return ESP32SerialPortData(flashConfigurationState.port, flashConfigurationState.baud)
        val panel = SimpleToolWindowPanel(false, true)

        val serialMonitorPanel = ESP32SerialMonitorPanel(project)
        this.serialMonitorPanel = serialMonitorPanel
        panel.setContent(serialMonitorPanel)

        val toolbar = createActionToolbar(project, panel)
        panel.toolbar = toolbar.component

        val content = ContentFactory.SERVICE.getInstance().createContent(panel, project.name, false)
        toolWindow.contentManager.addContent(content)
        getSerialPort(project)?.let { portData.copy(portName = it.portName, baud = it.baud)}
    }

    private fun createActionToolbar(project:Project, panel: SimpleToolWindowPanel): ActionToolbar {
        val group = DefaultActionGroup()
        group.add(ESP32ConnectDisconnectAction(portData))
        group.add(ESP32FlashAction(project))
        group.add(  DumbAwareAction.create("Show Settings")
        { createSerialSetupDialog(project,portData).showModal(true)})

        val actionToolbar =  ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, false)
        actionToolbar.setTargetComponent(panel)
        return actionToolbar
    }
}