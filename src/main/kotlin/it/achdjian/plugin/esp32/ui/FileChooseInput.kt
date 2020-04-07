package it.achdjian.plugin.esp32.ui

import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.InvalidDataException
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.valueEditors.TextFieldValueEditor
import it.achdjian.plugin.esp32.configurations.debuger.ESP32DebugSettingEditor
import it.achdjian.plugin.esp32.configurations.debuger.findScripts
import java.awt.event.ActionEvent
import java.io.File
import java.io.IOException
import java.util.function.Supplier

abstract class FileChooseInput(valueName:String, defaultLocation: File,  fileChooserDescriptor: FileChooserDescriptor) : TextFieldWithBrowseButton(JBTextField()) {

    val BOARD_FOLDER = "board"
    val editor  = FileTextFieldValueEditor(this, valueName, defaultLocation)
    private val fileDescriptor = fileChooserDescriptor.withFileFilter { virtualFile: VirtualFile? ->  validateFile( VfsUtilCore.virtualToIoFile( virtualFile!! )) }

    open val defaultLocation =ESP32DebugSettingEditor.USER_HOME
    val valueName =editor.valueName

    init {
        this.installPathCompletion(fileDescriptor)
        addActionListener { e: ActionEvent? ->
            var file: File? = null
            val text = this.textField.text
            if (text != null && !text.isEmpty()) {
                file = try {
                    parseTextToFile(text)
                } catch (var5: InvalidDataException) {
                    File(text)
                }
            }
            if (file == null) {
                file = defaultLocation
            }
            val chosenFile =
                FileChooser.chooseFile(
                    fileDescriptor,
                    null as Project?,
                    VfsUtil.findFileByIoFile(file, true)
                )
            if (chosenFile != null) {
                this.textField.text = fileToTextValue(VfsUtilCore.virtualToIoFile(chosenFile))
            }
        }
    }



    protected open fun fileToTextValue(file: File): String {
        return try {
            file.canonicalPath
        } catch (var3: IOException) {
            file.absolutePath
        }
    }

    abstract fun validateFile(var1: File): Boolean



    open fun parseTextToFile(text: String?): File {
        val file = text?.let { File(it) } ?: editor.defaultValue
        return if (!validateFile(file)) {
            throw InvalidDataException("is invalid")
        } else {
            file
        }
    }
}


class FileTextFieldValueEditor(val fileChooseInput: FileChooseInput, valueName: String, defaultLocation: File) : TextFieldValueEditor<File?>(fileChooseInput.textField, valueName, defaultLocation) {
    override fun parseValue(text: String?) = fileChooseInput.parseTextToFile(text)

    override fun valueToString(value: File) = value.path

    override fun isValid(file: File) = fileChooseInput.validateFile(file)
}

class BoardCfg(valueName: String, defaultLocation: File, val openOcdLocation: Supplier<String>) :    FileChooseInput(valueName, defaultLocation, FileChooserDescriptorFactory.createSingleLocalFileDescriptor()) {

    override val defaultLocation: File
        get() {
            val ocdScripts = findOcdScripts()
            val ocdBoards = File(ocdScripts, "board")
            return if (!ocdBoards.exists()) {
                ocdBoards
            } else {
                super.defaultLocation
            }
        }

    override fun parseTextToFile(text: String?): File {
        var file: File
        if (text == null) {
            file = editor.defaultValue
        } else {
            file = File(text)
            if (!file.exists()) {
                file = File(findOcdScripts(), text)
            }
        }
        return if (file.exists() && validateFile(file)) {
            file
        } else {
            throw InvalidDataException("is invalid")
        }
    }

    override fun validateFile(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    override fun fileToTextValue(file: File): String {
        val ocdScripts = findOcdScripts()
        if (FileUtil.isAncestor(ocdScripts, file, true)) {
            val relativePath = FileUtil.getRelativePath(ocdScripts, file)
            if (relativePath != null) {
                return relativePath
            }
        }
        return super.fileToTextValue(file)
    }

    private fun findOcdScripts()= findScripts(File(openOcdLocation.get()))
}