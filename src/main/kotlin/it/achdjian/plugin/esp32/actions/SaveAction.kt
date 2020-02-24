package it.achdjian.plugin.esp32.actions

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import it.achdjian.plugin.esp32.configurator.WizardData
import it.achdjian.plugin.esp32.sdk_config.createSdkConfigFile
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

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