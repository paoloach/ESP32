package it.achdjian.plugin.esp32.serial

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogPanel
import it.achdjian.plugin.esp32.ui.BaudRateComboBox
import it.achdjian.plugin.esp32.ui.GridLayout2
import it.achdjian.plugin.esp32.ui.SerialPortListComboBox
import javax.swing.JComponent
import javax.swing.JLabel

fun createSerialSetupDialog(project: Project,serialPort: ESP32SerialPortData): DialogBuilder {
    return DialogBuilder(project).centerPanel(createCenterPanel(serialPort))
}

private fun createCenterPanel(serialPort: ESP32SerialPortData): JComponent {
        val mainPanel = DialogPanel()
    mainPanel.layout = GridLayout2(2, 2)
    mainPanel.name = "Serial Monitor Setup"

    mainPanel.add(JLabel("Serial Port: "))
    mainPanel.add(SerialPortListComboBox{serialPort.portName=it})
    mainPanel.add(JLabel("Serial Flashing Baud Rate"))
    mainPanel.add(BaudRateComboBox{serialPort.baud=it})

    return mainPanel
}

