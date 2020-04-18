package it.achdjian.plugin.esp32.configurations.debuger

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
import com.jetbrains.cidr.cpp.execution.CMakeRunConfigurationType
import com.jetbrains.cidr.cpp.execution.gdbserver.DownloadType
import com.jetbrains.cidr.cpp.execution.gdbserver.RadioButtonPanel
import com.jetbrains.cidr.execution.ExecutableData
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import it.achdjian.plugin.esp32.ui.BoardCfg
import it.achdjian.plugin.esp32.ui.FileChooseInput
import it.achdjian.plugin.esp32.ui.selectBoardByPriority
import java.awt.event.ActionEvent
import java.io.File
import java.nio.file.Paths
import java.util.*
import java.util.function.Supplier
import javax.swing.*


data class Board(val mcu:String?, val board:String?){
    val name: String
        get() = board?.let { board.toUpperCase() } ?: ""
    val mcuFamily: String
        get() = mcu?.let { mcu.toUpperCase() } ?: ""
}

class ESP32DebugSettingEditor(project: Project, configHelper: CMakeBuildConfigurationHelper) :
    CMakeAppRunConfigurationSettingsEditor(project, configHelper) {
    companion object {
        val USER_HOME = File(SystemProperties.getUserHome())
    }

    private lateinit var gdbPort: IntegerField
    private lateinit var telnetPort: IntegerField
    private lateinit var boardConfigFile: FileChooseInput
    private lateinit var openOcdLocation: String
    private lateinit var downloadGroup: RadioButtonPanel<DownloadType>
    private lateinit var resetGroup: RadioButtonPanel<ResetType>

    @Throws(ConfigurationException::class)
    override fun applyEditorTo(cMakeAppRunConfiguration: CMakeAppRunConfiguration) {
        super.applyEditorTo(cMakeAppRunConfiguration)
        val esp32Conf: ESP32DebugConfiguration = cMakeAppRunConfiguration as ESP32DebugConfiguration
        esp32Conf.boardConfigFile = boardConfigFile.text.trim()
        gdbPort.validateContent()
        telnetPort.validateContent()
        esp32Conf.gdbPort = gdbPort.value
        esp32Conf.telnetPort = telnetPort.value
        esp32Conf.downloadType = downloadGroup.selectedValue
        esp32Conf.resetType = resetGroup.selectedValue

        val targets = CMakeRunConfigurationType.getHelper(myProject).targets
        val buildWorkingDir = targets.firstOrNull{ it.name=="app" }?.buildConfigurations?.get(0)?.buildWorkingDir
        buildWorkingDir?.let {
            cMakeAppRunConfiguration.executableData = ExecutableData("$buildWorkingDir/${myProject.name}.bin")
        }
    }

    override fun resetEditorFrom(cMakeAppRunConfiguration: CMakeAppRunConfiguration) {
        super.resetEditorFrom(cMakeAppRunConfiguration)
        val esp32Conf = cMakeAppRunConfiguration as ESP32DebugConfiguration
        openOcdLocation = ESP32SettingState.esp32OpenOcdLocation
        boardConfigFile.text = esp32Conf.boardConfigFile
        gdbPort.text = esp32Conf.gdbPort.toString()
        telnetPort.text = esp32Conf.telnetPort.toString()
        downloadGroup.selectedValue = esp32Conf.downloadType
        resetGroup.selectedValue = esp32Conf.resetType
    }

    override fun createEditorInner(panel: JPanel, gridBag: GridBag) {
        super.createEditorInner(panel, gridBag)
        panel.components.forEach { it.isVisible = false }
        panel.add(JLabel("Board config file"), gridBag.nextLine().next())
        val boardPanel = createBoardSelector()
        panel.add(boardPanel, gridBag.next().coverLine())
        panel.add(Box.createVerticalStrut(12), gridBag.nextLine())
        gdbPort = addPortInput(panel, gridBag, "GDB port", 3333)
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
        boardPanel.add(JButton(BoardSelectAction("Select") {
            selectBoard()?.let { board ->
                boardConfigFile.text = board
            }
        }))
        return boardPanel
    }

    private fun selectBoard(): String? {
        val preferredBoard = findPreferredBoard()
        return selectBoardByPriority(myProject, preferredBoard)
    }

    private fun findPreferredBoard():Board{
        myProject.basePath?.let {
            VfsUtil.findFile(Paths.get(it), true)?.children
        }?.let { projectFile ->
                projectFile.filter { !it.isDirectory }
                    .filter { "ioc".equals(it.extension, ignoreCase = true) }
                    .map {
                        val properties = Properties()
                        properties.load(it.inputStream)
                        properties
                    }
                    .firstOrNull { it.containsKey("board") || it.containsKey("Mcu.Name") }?.let {
                        return Board(it.getProperty("Mcu.name"), it.getProperty("board"))
                    }

            }
        return Board(null, null)
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

private class BoardSelectAction(text: String, val perform: (e: ActionEvent?) -> Unit) : AbstractAction(text) {
    override fun actionPerformed(e: ActionEvent?) {
        perform(e)
    }

}