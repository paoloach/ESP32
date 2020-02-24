package it.achdjian.plugin.esp32.configurator

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.ui.TextFieldWithHistoryWithBrowseButton
import com.intellij.ui.components.installFileCompletionAndBrowseDialog
import it.achdjian.plugin.esp32.entry_type.*
import it.achdjian.plugin.esp32.entry_type.ESP32ConfigElements.esp32configElements
import it.achdjian.plugin.esp32.ui.ButtonTitledBorder
import it.achdjian.plugin.espparser.AndOper
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener


private fun createBoolPanel(jLabel: JLabel, configurationEntry: BoolConfigEntry): JPanel {
    val panel = JPanel()
    panel.layout = GridLayout(1, 2)
    panel.add(jLabel)
    val checkBox = JCheckBox()
    checkBox.toolTipText = configurationEntry.description
    checkBox.addItemListener(configurationEntry)
    checkBox.isSelected = configurationEntry.value
    panel.add(checkBox)
    panel.isVisible = configurationEntry.enabled
    configurationEntry.addListenerToDepending { panel.isVisible = it }
    return panel
}


fun configuratorViewFactory(configurationEntry: ConfigurationEntry): Component? {
    if (configurationEntry.text.isEmpty()) {
        return null
    }
    val jLabel = JLabel(configurationEntry.text)
    jLabel.toolTipText = configurationEntry.description
    when (configurationEntry) {
        is BoolConfigEntry -> {
            val panel = createBoolPanel(jLabel, configurationEntry)
            panel.name = "Boolconfig ${configurationEntry.text}"
            return panel
        }
        is SubMenuConfigEntry -> {
            return esp32configElements[configurationEntry.name]?.let {
                if (it is BoolConfigEntry){
                    val panel=JPanel()
                    panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

                    val boolPanel= createBoolPanel(jLabel, it)

                    val menuPanel = menuPanel(configurationEntry, configurationEntry )
                    panel.add(boolPanel)
                    panel.add(menuPanel)
                    panel.name = "SubMenuConfig ${configurationEntry.text}"
                    panel
                } else {
                    throw RuntimeException("")
                }

            } ?:     throw RuntimeException("Wrong SubMenuConfigEntry: missing configurationEntry ${configurationEntry.name}")
        }
        is SubMenuEntry -> {
            val panel = menuPanel(configurationEntry,configurationEntry)
            panel.name = "submenu ${configurationEntry.text}"
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
            panel.name = "choice  ${configurationEntry.text}"
            return panel
        }
        is HexConfigEntry -> {
            val panel = JPanel()
            panel.layout = GridLayout(1, 2)
            jLabel.text = jLabel.text + " (hex value)"
            panel.add(jLabel)
            val textField = IntegerTextField(configurationEntry,16)
            panel.add(textField)
            panel.isVisible = configurationEntry.enabled
            configurationEntry.addListenerToDepending { panel.isVisible = it }
            panel.name = configurationEntry.text
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
            panel.name = configurationEntry.text
            return panel
        }
        is StringConfigEntry -> {
            val panel = JPanel()
            panel.layout = GridLayout(1, 2)
            panel.add(jLabel)
            if (configurationEntry.configEntry=="SDK_PYTHON"){
                val component = TextFieldWithHistoryWithBrowseButton()
                val editor = component.childComponent.textEditor
                editor.text = configurationEntry.value

                installFileCompletionAndBrowseDialog(
                    null,
                    component,
                    editor,
                    configurationEntry.text,
                    FileChooserDescriptorFactory.createSingleFileDescriptor(),
                    TextComponentAccessor.TEXT_FIELD_WITH_HISTORY_WHOLE_TEXT
                ) {
                    configurationEntry.value = it.path
                    configurationEntry.value
                }
                panel.add(component)
            } else {
                val jTextField = JTextField()

                jTextField.text = configurationEntry.value
                jTextField.addFocusListener(StringFocusListener(configurationEntry, jTextField))
                jTextField.addActionListener {
                    configurationEntry.value = jTextField.text
                }
                panel.add(jTextField)
            }
            panel.isVisible = configurationEntry.enabled
            configurationEntry.addListenerToDepending { panel.isVisible = it }
            panel.name = configurationEntry.text
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
        internalPanel.isVisible = !it
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


class IntegerTextField(private val intConfigEntry: IntConfigEntry, val radix: Int=10) : JTextField(),
    DocumentListener {
    companion object {
        val NORMAL_BACKGROUND = Color(255, 255, 255)
    }

    init {
        background = NORMAL_BACKGROUND
        text = intConfigEntry.value.toString(16)
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
            intConfigEntry.value = str.toInt(radix)
            background = NORMAL_BACKGROUND
        } catch (e: Exception) {
            background = IntInputVerifier.ERROR_BACKGROUND
        }

    }

    override fun insertUpdate(documentEvent: DocumentEvent) {
        val length = documentEvent.document.length
        val str = documentEvent.document.getText(0, length)
        try {
            intConfigEntry.value = str.toInt(radix)
            background = NORMAL_BACKGROUND
        } catch (e: Exception) {
            background = IntInputVerifier.ERROR_BACKGROUND
        }
    }

    override fun removeUpdate(documentEvent: DocumentEvent) {
        val length = documentEvent.document.length
        val str = documentEvent.document.getText(0, length)
        try {
            intConfigEntry.value = str.toInt(radix)
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