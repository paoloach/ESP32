package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.ui.components.fields.IntegerField
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.util.SystemProperties
import com.intellij.util.ui.GridBag
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfiguration
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationSettingsEditor
import com.jetbrains.cidr.cpp.execution.CMakeBuildConfigurationHelper
import com.jetbrains.cidr.cpp.execution.gdbserver.DownloadType
import com.jetbrains.cidr.cpp.execution.gdbserver.RadioButtonPanel
import it.achdjian.plugin.esp32.ui.BoardCfg
import it.achdjian.plugin.esp32.ui.FileChooseInput
import it.achdjian.plugin.esp32.ui.selectBoardByPriority
import java.awt.event.ActionEvent
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.function.Supplier
import javax.swing.*

class ESP32DebugSettingEditor( project: Project, configHelper: CMakeBuildConfigurationHelper) : CMakeAppRunConfigurationSettingsEditor(project, configHelper) {
    companion object {
        val USER_HOME = File(SystemProperties.getUserHome())
    }
    private lateinit var gdbPort: IntegerField
    private lateinit var telnetPort: IntegerField
    private lateinit var boardConfigFile: FileChooseInput
    private  lateinit var openOcdLocation: String
    private lateinit var downloadGroup: RadioButtonPanel<DownloadType>
    private lateinit var resetGroup: RadioButtonPanel<ResetType>

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(cMakeAppRunConfiguration: CMakeAppRunConfiguration) {
        super.applyEditorTo(cMakeAppRunConfiguration)
        val esp32Conf: ESP32DebugConfiguration = cMakeAppRunConfiguration as ESP32DebugConfiguration
        val boardConfig = boardConfigFile.text.trim { it <= ' ' }
        esp32Conf.boardConfigFile = boardConfig
        gdbPort.validateContent()
        telnetPort.validateContent()
        esp32Conf.gdbPort = gdbPort.value
        esp32Conf.telnetPort=telnetPort.value
        esp32Conf.downloadType =downloadGroup.selectedValue
        esp32Conf.resetType = resetGroup.selectedValue
    }

    override fun resetEditorFrom(cMakeAppRunConfiguration: CMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration)
        val esp32Conf = cMakeAppRunConfiguration as ESP32DebugConfiguration
        openOcdLocation = ApplicationManager.getApplication().getComponent(ESP32DebugConfigurationState::class.java) .openOcdLocation
        boardConfigFile.text = esp32Conf.boardConfigFile
        gdbPort.text = esp32Conf.gdbPort.toString()
        telnetPort.text = esp32Conf.telnetPort.toString()
        downloadGroup.selectedValue =esp32Conf.downloadType
        resetGroup.selectedValue = esp32Conf.resetType
    }

    override fun createEditorInner(panel: JPanel, gridBag: GridBag) {
        super.createEditorInner(panel, gridBag)
        val var3 = panel.components
        val var4 = var3.size
        for (var5 in 0 until var4) {
            val component = var3[var5]
            (component as? CommonProgramParametersPanel)?.isVisible = false
        }
        panel.add(JLabel("Board config file"), gridBag.nextLine().next())
        val boardPanel = createBoardSelector()
        panel.add(boardPanel, gridBag.next().coverLine())
        panel.add(Box.createVerticalStrut(12), gridBag.nextLine())
        gdbPort = addPortInput(  panel, gridBag, "GDB port", 3333)
        telnetPort = addPortInput(panel, gridBag, "Telnet port", 4444)
        panel.add(Box.createVerticalStrut(12), gridBag.nextLine())
        panel.add(JLabel("Download"), gridBag.nextLine().next())
        downloadGroup = RadioButtonPanel<DownloadType>(DownloadType.values())
        panel.add(downloadGroup, gridBag.next().fillCellHorizontally())
        panel.add(Box.createVerticalStrut(12), gridBag.nextLine())
        panel.add(JLabel("Reset"), gridBag.nextLine().next())
        resetGroup = RadioButtonPanel<ResetType>(ResetType.values())
        panel.add(resetGroup, gridBag.next())
    }

    private fun createBoardSelector(): JPanel {
        val boardPanel: JPanel = HorizontalBox()
        boardConfigFile = BoardCfg("Board config", USER_HOME, Supplier { openOcdLocation })
        boardPanel.add(boardConfigFile)
        boardPanel.add(JButton(BoardSelectAction("Select"){selectBoard()?.let { board->boardConfigFile.text=board }}))
        return boardPanel
    }

    private fun selectBoard(): String? {
            val projectPath = myProject.basePath
            projectPath ?.let {  VfsUtil.findFile(Paths.get(projectPath), true)?.children }
                ?.let { projectFile->
                    projectFile.filter { !it.isDirectory }
                    .filter { "ioc".equals(it.extension, ignoreCase = true) }
                    .map {
                        val properties = Properties()
                        properties.load(it.inputStream)
                        properties
                    }
                    .firstOrNull { it.containsKey("board") || it.containsKey("Mcu.Name") }?.let {
                        val mcuFamily = it.getProperty("Mcu.Name")
                        val board = it.getProperty("board")
                        selectBoardByPriority(myProject, board, mcuFamily)
                        return board
                    }

            }
        return null
    }

    private fun addPortInput(panel: JPanel, gridBag: GridBag, label: String, defaultValue: Int): IntegerField {
        panel.add(JLabel(label), gridBag.nextLine().next())
        val field = IntegerField(label, 1024, 65535)
        field.defaultValue = defaultValue
        field.columns = 5
        panel.add(field, gridBag.next().fillCellNone().anchor(21))
        return field
    }
}

private class BoardSelectAction(text: String, val perform: (e:ActionEvent?) -> Unit): AbstractAction(text){
    override fun actionPerformed(e: ActionEvent?) {
        perform(e)
    }

}