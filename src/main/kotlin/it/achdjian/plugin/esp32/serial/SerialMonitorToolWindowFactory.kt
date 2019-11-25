package it.achdjian.plugin.esp32.serial

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import it.achdjian.plugin.esp32.actions.configParsing

class SerialMonitorToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        project.getComponent(SerialMonitorView::class.java)?.initToolWindow(toolWindow)
    }

    override fun shouldBeAvailable(project: Project): Boolean {
        val config = configParsing(project, false)
        if (config.isEmpty())
            return false
        return config["IDF_TARGET"]?.let { it == "esp32" } ?: false
    }
}