package it.achdjian.plugin.esp32.serial

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory

class SerialMonitorView(val project: Project) : ProjectComponent {

    private var serialMonitorPanel: SerialMonitorPanel?=null
    fun initToolWindow(toolWindow: ToolWindow) {

        val serialMonitorPanel = SerialMonitorPanel(project)
        this.serialMonitorPanel = serialMonitorPanel

        val panel = SimpleToolWindowPanel(false, true)
        val content = ContentFactory.SERVICE.getInstance().createContent(panel, project.name, false)

        panel.setContent(serialMonitorPanel)

        val toolbar = createToolbar()
        toolbar.setTargetComponent(panel)
        panel.toolbar = toolbar.component

        toolWindow.contentManager.addContent(content)
    }

    private fun createToolbar(): ActionToolbar {
        val group = DefaultActionGroup()

        group.add(ConnectDisconnectAction())
        group.add(FlashAction(project))

        return ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, false)
    }

    override fun disposeComponent() {
        serialMonitorPanel?.dispose()
    }

    override fun projectClosed() = disposeComponent()
}