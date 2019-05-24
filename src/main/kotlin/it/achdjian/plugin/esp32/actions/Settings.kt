package it.achdjian.plugin.esp32.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import it.achdjian.plugin.esp32.configurator.CONFIG_FILE_NAME
import java.io.File

class Settings : AnAction("ESP32 setting...") {

    companion object {
        private val LOG = Logger.getInstance(Settings::class.java)
    }
    /**
     * Implement this method to provide your action handler.
     *
     * @param e Carries information on the invocation place
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = AnAction.getEventProject(event)
        project?.let { loadSetting(project) }
    }

    private fun loadSetting(project: Project) {
        val config = configParsing(project)
    }

    fun configParsing(project: Project): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val modules = ModuleManager.getInstance(project).getModules()
        if (modules.isEmpty())
            return result
        val moduleFile = modules[0].moduleFile?:return result
        val parent = moduleFile.parent.parent



        val config = parent.findChild(CONFIG_FILE_NAME)

        config?.let {

        }
        return result
    }
}