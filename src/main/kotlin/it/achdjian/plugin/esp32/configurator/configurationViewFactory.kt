package it.achdjian.plugin.esp32.configurator

import com.intellij.openapi.ui.ComboBox
import it.achdjian.plugin.espparser.AndOper
import it.achdjian.plugin.esp32.entry_type.*
import it.achdjian.plugin.esp32.entry_type.ConfigElements.configElements
import it.achdjian.plugin.esp32.ui.ButtonTitledBorder
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import java.lang.RuntimeException
import java.text.NumberFormat
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun configuratorViewFactory(configurationEntry: ConfigurationEntry): Component? {
    if (configurationEntry.text.isEmpty()) {
        return null
    }
    val jLabel = JLabel(configurationEntry.text)
    jLabel.toolTipText = configurationEntry.description
    when (configurationEntry) {
        is BoolConfigEntry -> {
            val panel = JPanel()
            panel.layout = GridLayout(1, 2)
            panel.add(jLabel)
            val checkBox = JCheckBox()
            checkBox.toolTipText = configurationEntry.description
            checkBox.addItemListener(configurationEntry)
            checkBox.isSelected = configurationEntry.value
            panel.add(checkBox)
            panel.isVisible = configurationEntry.enabled
            configurationEntry.addListenerToDepending() { panel.isVisible = it }
            return panel
        }
        is SubMenuConfigEntry -> {
            return configElements[configurationEntry.name]?.let {
                if (it is BoolConfigEntry){
                    val panel=JPanel()
                    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

                    val boolPanel = JPanel()
                    boolPanel.layout = GridLayout(1, 2)
                    boolPanel.add(jLabel)
                    val checkBox = JCheckBox()
                    checkBox.toolTipText = it.description
                    checkBox.addItemListener(it)
                    checkBox.isSelected = it.value
                    boolPanel.add(checkBox)
                    boolPanel.isVisible = configurationEntry.enabled
                    configurationEntry.addListenerToDepending() { boolPanel.isVisible = it }

                    val menuPanel = menuPanel(configurationEntry, configurationEntry )
                    panel.add(boolPanel)
                    panel.add(menuPanel)
                    panel
                } else {
                    throw RuntimeException("")
                }

            } ?:     throw RuntimeException("Wrong SubMenuConfigEntry: missing configurationEntry ${configurationEntry.name}")
        }
        is SubMenuEntry -> {
            val panel = menuPanel(configurationEntry,configurationEntry)
            return panel
        }
        is ChoiceConfigEntry -> {
            val panel = JPanel()
            panel.layout = GridLayout(1, 2)
            panel.add(jLabel)
            val comboBox = ComboBox<String>()
            comboBox.toolTipText = configurationEntry.description
            configurationEntry.choices.forEach {
                comboBox.addItem(it.text)
                it.dependsOn = AndOper(configurationEntry.dependsOn, it.dependsOn)
            }
            comboBox.addActionListener {
                configurationEntry.default?.value = false
                configurationEntry.choices.firstOrNull { c -> c.text == comboBox.selectedItem }?.let { c ->
                    c.value = true
                    configurationEntry.choiced = c
                }
            }

            comboBox.selectedItem = configurationEntry.default?.text
            panel.add(comboBox)
            panel.isVisible = configurationEntry.enabled
            configurationEntry.addListenerToDepending { panel.isVisible = it }
            return panel
        }
        is IntConfigEntry -> {
            val panel = JPanel()
            panel.layout = GridLayout(1, 2)
            panel.add(jLabel)
            val textField = IntegerTextField(configurationEntry)
            panel.add(textField)
            panel.isVisible = configurationEntry.enabled
            configurationEntry.addListenerToDepending { panel.isVisible = it }
            return panel
        }
        is StringConfigEntry -> {
            val jTextField = JTextField()
            val panel = JPanel()
            panel.layout = GridLayout(1, 2)
            panel.add(jLabel)

            jTextField.text = configurationEntry.value
            jTextField.addFocusListener(StringFocusListener(configurationEntry, jTextField))
            jTextField.addActionListener {
                configurationEntry.value = jTextField.text
            }
            panel.add(jTextField)
            panel.isVisible = configurationEntry.enabled
            configurationEntry.addListenerToDepending { panel.isVisible = it }
            return panel

        }
    }

    return null
}

private fun menuPanel(configurationEntry: ConfigurationEntry, menuEntry: MenuEntry): JPanel {
    val panel = JPanel()
    panel.layout = BorderLayout()
    val internalPanel = JPanel()
    panel.add(internalPanel, BorderLayout.CENTER)
    internalPanel.layout = BoxLayout(internalPanel, BoxLayout.Y_AXIS)
    menuEntry.subMenu.forEach { configuratorViewFactory(it)?.let { view -> internalPanel.add(view) } }
    internalPanel.isVisible = false
    panel.border = ButtonTitledBorder(configurationEntry.text, panel) {
        internalPanel.setVisible(!it)
    }

    panel.isVisible = configurationEntry.enabled
    configurationEntry.addListenerToDepending { panel.isVisible = it }
    return panel
}


class IntInputVerifier(private val min: Long, private val max: Long) : InputVerifier() {
    companion object {
        val ERROR_BACKGROUND = Color(255, 215, 215)
    }

    override fun verify(component: JComponent?): Boolean {
        if (component is JFormattedTextField) {
            try {
                val value = component.text.toInt()
                if (value < min || value > max) {
                    JOptionPane.showMessageDialog(
                        null,
                        "The value must be between $min and $max",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                    component.background = ERROR_BACKGROUND
                    return false
                }
                return true
            } catch (e: Exception) {
                component.background = ERROR_BACKGROUND
                return false
            }
        }
        return true
    }

}


class IntegerTextField(private val intConfigEntry: IntConfigEntry) : JFormattedTextField(NumberFormat.getInstance()),
    DocumentListener {
    companion object {
        val NORMAL_BACKGROUND = Color(255, 255, 255)
    }

    init {
        background = NORMAL_BACKGROUND
        text = intConfigEntry.value.toString()
        document.addDocumentListener(this)
        inputVerifier = IntInputVerifier(intConfigEntry.min, intConfigEntry.max)
    }

    override fun processKeyBinding(ks: KeyStroke, e: KeyEvent, condition: Int, pressed: Boolean): Boolean {
        background = NORMAL_BACKGROUND
        return super.processKeyBinding(ks, e, condition, pressed)
    }

    override fun changedUpdate(documentEvent: DocumentEvent) {
        val length = documentEvent.document.length
        val str = documentEvent.document.getText(0, length)
        try {
            intConfigEntry.value = str.toInt()
            background = NORMAL_BACKGROUND
        } catch (e: Exception) {
            background = IntInputVerifier.ERROR_BACKGROUND
        }

    }

    override fun insertUpdate(documentEvent: DocumentEvent) {
        val length = documentEvent.document.length
        val str = documentEvent.document.getText(0, length)
        try {
            intConfigEntry.value = str.toInt()
            background = NORMAL_BACKGROUND
        } catch (e: Exception) {
            background = IntInputVerifier.ERROR_BACKGROUND
        }
    }

    override fun removeUpdate(documentEvent: DocumentEvent) {
        val length = documentEvent.document.length
        val str = documentEvent.document.getText(0, length)
        try {
            intConfigEntry.value = str.toInt()
            background = NORMAL_BACKGROUND
        } catch (e: Exception) {
            background = IntInputVerifier.ERROR_BACKGROUND
        }
    }
}

class StringFocusListener(
    private val stringConfigEntry: StringConfigEntry,
    private val jTextField: JTextField
) : FocusListener {
    override fun focusLost(p0: FocusEvent?) {
        stringConfigEntry.value = jTextField.text
    }

    override fun focusGained(p0: FocusEvent?) {
    }

}