package it.achdjian.plugin.esp32.serial

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import it.achdjian.plugin.esp32.serial.console.ESP32AnsiConsoleView
import java.awt.BorderLayout
import javax.swing.JPanel


class ESP32SerialMonitorPanel(val project: Project) : JPanel(), Disposable {
    private val myESP32SerialService: ESP32SerialService
    private val consoleView = ESP32AnsiConsoleView(project, true)


    init {
        layout = BorderLayout()
        add(consoleView.component, BorderLayout.CENTER)

        val toolbarActions = DefaultActionGroup()
        toolbarActions.addAll(*consoleView.createConsoleActions())

        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false)
        toolbar.setTargetComponent(this)
        add(toolbar.component, BorderLayout.WEST)


        myESP32SerialService = ServiceManager.getService(project, ESP32SerialService::class.java)
        myESP32SerialService.addDataListener(::consumeData)
    }

    private fun consumeData(data:ByteArray){
        consoleView.print(String(data), ConsoleViewContentType.NORMAL_OUTPUT)
    }

    override fun dispose() {
        Disposer.dispose(consoleView)
        myESP32SerialService.removeDataListener(::consumeData)
    }


}