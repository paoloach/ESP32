package it.achdjian.plugin.esp32.configurator

import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel

class MissingConfig(clionPanel: JPanel) : JPanel(BorderLayout()) {
    val internalPanel = JPanel()
    init {
        add(clionPanel, BorderLayout.PAGE_START)
        val warning =  JLabel("Please before to proceed set a valid ESP32 espressif SDK path in the settings")
        add(warning, BorderLayout.CENTER)

    }
}