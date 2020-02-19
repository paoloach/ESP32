package it.achdjian.plugin.esp32.configurator

import it.achdjian.plugin.esp32.entry_type.ConfigurationEntry
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.BoxLayout
import javax.swing.JPanel


class ESP32WizardPanel(clionPanel: JPanel, entriesMenu: List<ConfigurationEntry>, autoResize: Boolean = false) :
    JPanel(BorderLayout()),
    ComponentListener {

    var realHeight = 0
    override fun componentMoved(p0: ComponentEvent?) {
    }

    override fun componentResized(p0: ComponentEvent?) {
        realHeight = internalPanel.components.map { it.preferredSize.height }.sum()
        internalPanel.size = Dimension(internalPanel.width, realHeight)
        internalPanel.preferredSize = Dimension(internalPanel.width, realHeight)
    }

    override fun componentHidden(p0: ComponentEvent?) {
    }

    override fun componentShown(p0: ComponentEvent?) {
    }


    val internalPanel = JPanel()

    init {
        add(clionPanel, BorderLayout.PAGE_START)
        internalPanel.layout = BoxLayout(internalPanel, BoxLayout.Y_AXIS)

        entriesMenu.forEach {
            configuratorViewFactory(it)?.let { view ->
                if (autoResize)
                    view.addComponentListener(this)
                internalPanel.add(view)
            }
        }
        add(internalPanel, BorderLayout.CENTER)

    }
}