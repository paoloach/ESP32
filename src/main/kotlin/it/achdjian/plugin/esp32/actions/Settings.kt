package it.achdjian.plugin.esp32.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import it.achdjian.plugin.esp32.configurations.debuger.ESP32DebugConfiguration
import it.achdjian.plugin.esp32.configurator.CONFIG_FILE_NAME
import it.achdjian.plugin.esp32.configurator.ESP32WizardPanel
import it.achdjian.plugin.esp32.configurator.WizardData
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import java.io.File
import javax.swing.BoxLayout
import javax.swing.JPanel


fun getProjectPath(project: Project): VirtualFile? {
    return project.basePath?.let {
        StandardFileSystems.local().findFileByPath(it)
    }
}

fun configParsing(project: Project, showDialog: Boolean=true): Map<String, String> {
    val result = mutableMapOf<String, String>()
    val config = getProjectPath(project)?.findChild(CONFIG_FILE_NAME)

    config?.let { file ->
        File(file.path)
            .readLines()
            .filter { !it.startsWith("#") }
            .forEach {
                val configPair = it.split("=")
                if (configPair.size == 2) {
                    var key = configPair[0]
                    if (key.startsWith("CONFIG_"))
                        key = key.substring(7)
                    var value = configPair[1].trim()
                    if (value.startsWith("\"")){
                       value = value.substring(1)
                    }
                    if (value.endsWith("\"")){
                        value = value.substring(0, value.length-1)
                    }
                    result[key] = value
                }
            }
    } ?: if (showDialog) Messages.showErrorDialog(project, "Unable to find $CONFIG_FILE_NAME file", "ESP32 Plugin")

    return result
}

class Settings : AnAction("ESP32 Setting..."), ComponentListener {
    companion object {
        private val LOG = Logger.getInstance(Settings::class.java)
    }


    private var settingPanel:ESP32WizardPanel? = null


    override fun componentMoved(p0: ComponentEvent?) {
    }

    override fun componentResized(p0: ComponentEvent?) {
        p0?.let { event ->
            if (event.source is JPanel) {

                settingPanel?.let {
                    it.size = Dimension(it.internalPanel.width, it.realHeight)
                    it.preferredSize = Dimension(it.internalPanel.width, it.realHeight)
                }
            }
        }
    }

    override fun componentHidden(p0: ComponentEvent?) {
    }

    override fun componentShown(p0: ComponentEvent?) {
    }


    /**
     * Implement this method to provide your action handler.
     *
     * @param event Carries information on the invocation place
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = getEventProject(event)
        project?.let { loadSetting(project) }
    }

    private fun loadSetting(project: Project) {
        val configEntries = configParsing(project)
        if (configEntries.isEmpty()) {
            Messages.showErrorDialog(project, "Unable to find $CONFIG_FILE_NAME file", "ESP32")
        } else {
            val wizardData = WizardData()

            wizardData.entries.forEach { availableEntry ->
                configEntries.forEach { configSdkEntry ->
                    availableEntry.set(configSdkEntry.key, configSdkEntry.value)
                }
            }


            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)

            ESP32WizardPanel(contentPanel, wizardData.entries, true).let {
                settingPanel=it
                it.internalPanel.addComponentListener(this)
                it.preferredSize = Dimension(500, 500)
                it.size = Dimension(500, 500)
            }

            val scrollPane = ScrollPaneFactory.createScrollPane(settingPanel, false)

            val dialog = DialogBuilder(project)
                .centerPanel(scrollPane)
                .title("ESP32 Settings")

            getProjectPath(project)?.let {
                dialog.addAction(SaveAction(wizardData, it, dialog.dialogWrapper))
                dialog.addCancelAction()
                dialog.show()
            }

        }
    }




}