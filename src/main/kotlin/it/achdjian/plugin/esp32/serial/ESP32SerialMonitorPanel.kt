package it.achdjian.plugin.esp32.serial

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Consumer
import it.achdjian.plugin.esp32.serial.console.ESP32AnsiConsoleView
import java.awt.BorderLayout
import javax.swing.JPanel


class ESP32SerialMonitorPanel(val project: Project) : JPanel(), Disposable {
    private val myESP32SerialService: ESP32SerialService
    private val dataListener: Consumer<ByteArray>

    private val consoleView = ESP32AnsiConsoleView(project, true)


    init {
        val consoleComponent = consoleView.component
        layout = BorderLayout()
        val toolbarActions = DefaultActionGroup()
        val toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, toolbarActions, false)
        toolbarActions.addAll(*consoleView.createConsoleActions())
        toolbar.setTargetComponent(this)
        add(toolbar.component, BorderLayout.WEST)

        add(consoleComponent, BorderLayout.CENTER)


        dataListener = Consumer {
            consoleView.print(String(it), ConsoleViewContentType.NORMAL_OUTPUT)
        }
        myESP32SerialService = ServiceManager.getService(project, ESP32SerialService::class.java)
        myESP32SerialService.addDataListener(dataListener)
    }


    override fun dispose() {
        Disposer.dispose(consoleView)
        myESP32SerialService.removeDataListener(dataListener)
    }


}