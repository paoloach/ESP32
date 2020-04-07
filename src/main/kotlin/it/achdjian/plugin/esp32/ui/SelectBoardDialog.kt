package it.achdjian.plugin.esp32.ui

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Pair
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import it.achdjian.plugin.esp32.configurations.debuger.ESP32DebugConfigurationState
import it.achdjian.plugin.esp32.configurations.debuger.findScripts
import it.achdjian.plugin.esp32.configurations.debuger.require
import sun.nio.cs.US_ASCII
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.AbstractAction
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.event.ListSelectionEvent

@Throws(ConfigurationException::class)
fun findBoardsConfigFiles(ESP32Settings: ESP32DebugConfigurationState): Array<File> ? {
    val ocdScripts: File = require(findScripts(File(ESP32Settings.openOcdLocation)))
    return require(File(ocdScripts, "board")).listFiles()
}

private fun calcConfigFileScore(keywordWeight: List<Pair<String, Int>>, ocdBoard: File): Pair<File, Int> {
    return if (ocdBoard.length() <100000L ){
        val text =  ocdBoard.readText(US_ASCII()).toUpperCase()
        val weight = keywordWeight.filter { text.contains(it.first) }.map { it.second }.sum()
        Pair.pair(ocdBoard, weight)
    } else {
        Pair.pair(ocdBoard,0)
    }
}


fun selectBoardByPriority(project: Project, boardName: String, mcuFamilyName: String): String ? {
    try {
        val keywordWeight = mutableListOf<Pair<String, Int>>()
        val board = StringUtil.toUpperCase(Objects.toString(boardName, ""))
        if (!board.isEmpty()) {
            keywordWeight.add(Pair.pair(board, 1000))
        }
        listOf("NUCLEO", "EVAL").filter { board.contains(it) }.firstOrNull()?.let { keywordWeight.add(Pair.pair(it,100)) }
        if (board.contains("DISCOVERY")) {
            keywordWeight.add(Pair.pair("DISCOVERY", 100))
            keywordWeight.add(Pair.pair("DISC", 20))
        } else if (board.contains("DISC")) {
            keywordWeight.add(Pair.pair("DISC", 100))
            keywordWeight.add(Pair.pair("DISCOVERY", 20))
        }
        val mcuFamily = StringUtil.toUpperCase(Objects.toString(mcuFamilyName, ""))
        for (i in mcuFamily.length - 1 downTo 6) {
            keywordWeight.add(Pair.pair(mcuFamily.substring(0, i), i))
        }
        val ESP32settings = ApplicationManager.getApplication().getComponent(ESP32DebugConfigurationState::class.java)
        findBoardsConfigFiles(ESP32settings)?.let{
            val boardByScores = it
                .filter { !it.isDirectory }
                .map { calcConfigFileScore(keywordWeight, it) }
                .sortedWith(kotlin.Comparator { a, b -> when {
                    a.second == b.second -> a.first.compareTo(b.first)
                    else -> a.second.compareTo( b.second)
                }})
                .map { it.first }.toTypedArray()


            val dialog = SelectBoardDialog(project, boardByScores)
            dialog.show()
            if (dialog.exitCode == 0 && dialog.mySelectedFile != null) {
                return dialog.myResult
            }
        }
    } catch (e: ConfigurationException) {
        Messages.showErrorDialog(project, e.message, e.title)
    }
    return null
}


class SelectBoardDialog(val myProject: Project,val myBoardFiles: Array<File>) : DialogWrapper(myProject, false, false) {
    companion object {
        const val CONFIG_FILE_SIZE_LIMIT = 100000
    }

    var myResult: String? = null
    var mySelectedFile: File? = null
    private var myBoardList=JBList<File>(*myBoardFiles)
    private var myCopyAndUseAction = CopyAndUseAction(this)

    init {
        title = "Select board"
        init()
        myBoardList.selectedIndex=0
    }

    override fun createDefaultActions() {
        super.createDefaultActions()
        this.okAction.putValue("Name", "use")
    }

    override fun createActions() = arrayOf(this.okAction, this.myCopyAndUseAction, this.cancelAction)

    override fun createCenterPanel(): JComponent {
        myBoardList = JBList<File>(*myBoardFiles)
        myBoardList.cellRenderer = SimpleListCellRenderer.create("") { obj: File -> obj.name }
        myBoardList.selectionMode = 0
        myBoardList.addListSelectionListener{ e: ListSelectionEvent? ->
            val index: Int = this.myBoardList.getSelectedIndex()
            okAction.isEnabled = index >= 0
            myCopyAndUseAction.isEnabled = index >= 0
            if (index >= 0) {
                val selectedFile = this.myBoardFiles.get(index)
                myResult = "board/" + selectedFile.getName()
                mySelectedFile=selectedFile
            } else {
                mySelectedFile = null
                myResult = null
            }
        }
        myBoardList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (!e.isPopupTrigger && e.clickCount > 1) {
                    clickDefaultButton()
                }
            }
        })
        ListSpeedSearch(myBoardList)
        return JBScrollPane(myBoardList)
    }

    override fun getPreferredFocusedComponent(): JComponent? =myBoardList
}


class WriteSelected(val projectDir: VirtualFile, val selectedFile: File, val overwrite: Boolean) : Computable<File> {
    override fun compute(): File {
        val newConfigFile = projectDir.findOrCreateChildData(this, selectedFile.name)
        if (overwrite) {
            newConfigFile.setBinaryContent(FileUtil.loadFileBytes(selectedFile))
        }
        return VfsUtilCore.virtualToIoFile(newConfigFile)
    }

}

private class CopyAndUseAction(val selectBoard: SelectBoardDialog) :  AbstractAction("CopyAndUsed") {
    override fun actionPerformed(e: ActionEvent) {
        selectBoard.mySelectedFile?.let { selectedFile->
            val projectDir = Objects.requireNonNull(selectBoard.myProject.guessProjectDir()) as VirtualFile
            val overwrite: Boolean
            overwrite = if (projectDir.findChild(selectedFile.name) != null) {
                val yesNoCancelReply: Int = Messages.showYesNoCancelDialog(selectBoard.myProject, "Overwrite file "+ selectedFile.name, "Overwrite", null as Icon?)
                when (yesNoCancelReply) {
                    0 -> true
                    1 -> false
                    else -> return
                }
            } else {
                true
            }
            try {
                selectBoard.mySelectedFile = ApplicationManager.getApplication().runWriteAction( WriteSelected(projectDir, selectedFile, overwrite))
                selectBoard.myResult = selectedFile.absolutePath
                selectBoard.close(0)
            } catch (var5: IOException) {
                Notifications.Bus.notify(
                    Notification(
                        "",
                        var5.javaClass.name,
                        var5.localizedMessage,
                        NotificationType.ERROR
                    )
                )
            }
        }
    }
}