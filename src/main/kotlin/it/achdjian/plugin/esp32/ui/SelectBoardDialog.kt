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
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.ListSpeedSearch
import com.intellij.ui.SimpleListCellRenderer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import it.achdjian.plugin.esp32.configurations.debuger.Board
import it.achdjian.plugin.esp32.configurations.debuger.findScripts
import it.achdjian.plugin.esp32.configurations.debuger.require
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import java.util.*
import javax.swing.AbstractAction
import javax.swing.Icon
import javax.swing.JComponent

@Throws(ConfigurationException::class)
fun findBoardsConfigFiles(): Array<File> ? {
    val ocdScripts: File = require(findScripts(File(ESP32SettingState.esp32OpenOcdLocation)))
    return require(File(ocdScripts, "board")).listFiles()
}

private fun calcConfigFileScore(keywordWeight: List<Pair<String, Int>>, ocdBoard: File): Pair<File, Int> {
    return if (ocdBoard.length() <100000L ){
        val text =  ocdBoard.readText(Charsets.ISO_8859_1).toUpperCase()
        val weight = keywordWeight.filter { text.contains(it.first) }.map { it.second }.sum()
        Pair.pair(ocdBoard, weight)
    } else {
        Pair.pair(ocdBoard,0)
    }
}


fun selectBoardByPriority(project: Project, board: Board): String ? {
    try {
        val keywordWeight = mutableListOf<Pair<String, Int>>()
        if (board.name.isNotEmpty()) {
            keywordWeight.add(Pair.pair(board.name, 1000))
        }
        listOf("ESP32", "WROVER").firstOrNull { board.name.contains(it) }?.let { keywordWeight.add(Pair.pair(it,100)) }
        keywordWeight.add(Pair.pair("ESP32",1000))
        for (i in board.mcuFamily.length - 1 downTo 6) {
            keywordWeight.add(Pair.pair(board.mcuFamily.substring(0, i), i))
        }
        findBoardsConfigFiles()?.let{
            val boardByScores = it
                .filter { !it.isDirectory }
                .map {file-> calcConfigFileScore(keywordWeight, file) }
                .sortedWith { a, b ->
                    when (a.second) {
                        b.second -> FileUtil.compareFiles(a.first, b.first)
                        else -> b.second - a.second
                    }
                }
                .map { it.first }


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


class SelectBoardDialog(val myProject: Project, private val myBoardFiles: List<File>) : DialogWrapper(myProject, false, false) {
    companion object {
        const val CONFIG_FILE_SIZE_LIMIT = 100000
    }

    var myResult: String? = null
    var mySelectedFile: File? = null
    private var myBoardList=JBList(myBoardFiles)
    private var myCopyAndUseAction = CopyAndUseAction(this)

    init {
        title = "Select Board"
        init()
        myBoardList.selectedIndex=0
    }

    override fun createDefaultActions() {
        super.createDefaultActions()
        this.okAction.putValue("Name", "use")
    }

    override fun createActions() = arrayOf(this.okAction, this.myCopyAndUseAction, this.cancelAction)

    override fun createCenterPanel(): JComponent {
        myBoardList = JBList(myBoardFiles)
        myBoardList.cellRenderer = SimpleListCellRenderer.create("") { obj: File -> obj.name }
        myBoardList.selectionMode = 0
        myBoardList.addListSelectionListener{
            val index: Int = this.myBoardList.selectedIndex
            okAction.isEnabled = index >= 0
            myCopyAndUseAction.isEnabled = index >= 0
            if (index >= 0) {
                val selectedFile = this.myBoardFiles[index]
                myResult = "board/" + selectedFile.name
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

    override fun getPreferredFocusedComponent(): JComponent =myBoardList
}


class WriteSelected(private val projectDir: VirtualFile, private val selectedFile: File, private val overwrite: Boolean) : Computable<File> {
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