package it.achdjian.plugin.esp32.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ScrollPaneFactory
import it.achdjian.plugin.esp32.configurator.CONFIG_FILE_NAME
import it.achdjian.plugin.esp32.configurator.ESP32WizardPanel
import it.achdjian.plugin.esp32.configurator.WizardData
import it.achdjian.plugin.esp32.entry_type.SdkConfigEntry
import it.achdjian.plugin.esp32.generator.createSdkConfigFile
import java.awt.event.ActionEvent
import java.io.File
import javax.swing.AbstractAction
import javax.swing.BoxLayout
import javax.swing.JPanel

class SaveAction(
    private val menuWizardData: WizardData,
    private val projectPath: VirtualFile,
    private val dialogWrapper: DialogWrapper
) : AbstractAction("Save") {
    override fun actionPerformed(p0: ActionEvent?) {
        createSdkConfigFile(menuWizardData.entries, projectPath)
        dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
    }

}

class Settings : AnAction("ESP32 setting...") {


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

            wizardData
                .entries
                .filter { it is SdkConfigEntry }
                .map { it as SdkConfigEntry }
                .forEach { confEntry ->
                    configEntries[confEntry.text]?.let { value ->
                        confEntry.set(value)

                    }
                }


            val contentPanel = JPanel()
            contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
            val settingPanel = ESP32WizardPanel(contentPanel, wizardData.entries)

            val scrollPane = ScrollPaneFactory.createScrollPane(settingPanel, false)

            val dialog = DialogBuilder(project)
                .centerPanel(scrollPane)
                .title("Settings")

            dialog.addAction(SaveAction(wizardData, getProjectPath(project), dialog.dialogWrapper))
            dialog.addCancelAction()
            dialog.show()
        }
    }

    private fun getProjectPath(project: Project): VirtualFile {
        val modules = ModuleManager.getInstance(project).modules
        if (modules.isEmpty())
            throw RuntimeException("Project has no modules")
        val moduleFile = modules[0].moduleFile
        return moduleFile!!.parent.parent
    }

    private fun configParsing(project: Project): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val parent = getProjectPath(project)

        val config = parent.findChild(CONFIG_FILE_NAME)

        config?.let { file ->
            File(file.path)
                .readLines()
                .filter { !it.startsWith("#") }
                .forEach {
                    val configPair = it.split("=")
                    if (configPair.size == 2) {
                        val key = configPair[0]
                        val value = configPair[1]
                        result[key] = value
                    }
                }
        } ?: Messages.showErrorDialog(project, "Unable to find $CONFIG_FILE_NAME file", "ESP32 plugin")

        return result
    }
}