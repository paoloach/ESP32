package it.achdjian.plugin.esp32.ui

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.ui.CommonProgramParametersPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vcs.changes.RefreshablePanel
import com.intellij.ui.AbstractTitledSeparatorWithIcon
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.HorizontalLayout
import com.jetbrains.cidr.cpp.CPPBundle
import com.jetbrains.cidr.cpp.execution.gdbserver.DownloadType
import com.jetbrains.cidr.cpp.execution.gdbserver.GdbServerRunConfiguration
import com.jetbrains.cidr.cpp.execution.gdbserver.RadioButtonPanel
import javax.swing.JComponent
import javax.swing.JPanel

class TitleSeparator(val parent: DebugerParameterPanel) : AbstractTitledSeparatorWithIcon(
    AllIcons.General.ArrowRight,
    AllIcons.General.ArrowDown,
    CPPBundle.message("gdbserver.options.advanced")
) {
    override fun createPanel(): RefreshablePanel {
        return object : RefreshablePanel {
            override fun refresh() {}
            override fun getPanel(): JPanel {
                return JPanel()
            }
        }
    }

    override fun initOnImpl() {}
    override fun onImpl() {
        parent.onAdvancedPanelChanged(true)
    }

    override fun offImpl() {
        parent.onAdvancedPanelChanged(false)
    }
}


class DebugerParameterPanel : CommonProgramParametersPanel() {
    private lateinit var myDownloadGroup: RadioButtonPanel<DownloadType>
    protected lateinit var myDownloadComponent: LabeledComponent<RadioButtonPanel<DownloadType>>
    protected lateinit var myGdbServerFileField: TextFieldWithBrowseButton
    protected lateinit var myGdbRemoteField: JBTextField
    protected lateinit var myDelayField: JBIntSpinner
    protected lateinit var myGdbServerComponent: LabeledComponent<TextFieldWithBrowseButton>
    protected lateinit var myGdbRemoteComponent: LabeledComponent<JBTextField>
    protected lateinit var myDelayComponent: LabeledComponent<*>
    protected lateinit var myOptionsSeparator: AbstractTitledSeparatorWithIcon

    init {

    }

    override fun initComponents() {
        myDownloadGroup = RadioButtonPanel<DownloadType>(DownloadType.values())
        myDownloadComponent =
            LabeledComponent.create(myDownloadGroup, CPPBundle.message("gdbserver.executable.download"), "West")
        myGdbServerFileField = TextFieldWithBrowseButton()
        myGdbRemoteField = JBTextField()
        myDelayField = JBIntSpinner(0, 0, 1000000, 500)
        myGdbServerComponent =
            LabeledComponent.create(myGdbServerFileField, CPPBundle.message("gdbserver.title"), "West")
        val label = JPanel(HorizontalLayout(0))
        label.add(myDelayField, "LEFT")
        label.add(JBLabel(CPPBundle.message("gdbserver.startup.delay.label.ms")), "LEFT")
        myDelayComponent = LabeledComponent.create(label, CPPBundle.message("gdbserver.startup.delay.label"), "West")
        myOptionsSeparator = TitleSeparator(this)
        myGdbRemoteComponent = LabeledComponent.create(myGdbRemoteField, CPPBundle.message("gdbRemote.command"), "West")

        (myGdbServerFileField.textField as JBTextField).emptyText.setText("/usr/bin/gdbserver")
        myGdbServerFileField.addBrowseFolderListener(
            CPPBundle.message("gdbserver.select.executable"),
            null as String?,
            null as Project?,
            FileChooserDescriptorFactory.createSingleFileNoJarsDescriptor()
        )
        myGdbRemoteField.emptyText.text = "gdb remote"
        myDelayField.toolTipText = CPPBundle.message("gdbserver.startup.delay.text")

        super.initComponents()
        myProgramParametersComponent.label.text = CPPBundle.message("gdbserver.arguments")
        myOptionsSeparator.off()
    }

    fun onAdvancedPanelChanged(visible: Boolean) {
        myWorkingDirectoryComponent.isVisible = visible
        myEnvVariablesComponent.isVisible = visible
        myDelayComponent.isVisible = visible
    }

    override fun addComponents() {
        this.add(myDownloadComponent)
        addCommonComponents()
        this.add(myDelayComponent)
    }

    protected fun addCommonComponents() {
        this.add(myGdbRemoteComponent)
        this.add(myGdbServerComponent)
        this.add(myProgramParametersComponent)
        this.add(myOptionsSeparator)
        this.add(myWorkingDirectoryComponent)
        this.add(myEnvVariablesComponent)
    }

    override fun setAnchor(anchor: JComponent?) {
        super.setAnchor(anchor)
        myGdbServerComponent.anchor = anchor
        myGdbRemoteComponent.anchor = anchor
        myDelayComponent.anchor = anchor
        myProgramParametersComponent.anchor = anchor
        myDownloadComponent.anchor = anchor
        myWorkingDirectoryComponent.anchor = anchor
        myEnvVariablesComponent.anchor = anchor
    }

    override fun applyTo(configuration: CommonProgramRunConfigurationParameters) {
        super.applyTo(configuration)
        val gdbConfiguration = configuration as GdbServerRunConfiguration
        gdbConfiguration.serverExecutable = myGdbServerFileField.text
        gdbConfiguration.setGdbRemoteString(myGdbRemoteField.text)
        gdbConfiguration.warmUpMs = myDelayField.number
        gdbConfiguration.downloadType = myDownloadGroup.selectedValue
    }

    override fun reset(configuration: CommonProgramRunConfigurationParameters) {
        super.reset(configuration)
        val serverConfiguration = configuration as GdbServerRunConfiguration
        myGdbServerFileField.setText(serverConfiguration.serverExecutable)
        myGdbRemoteField.text = serverConfiguration.gdbRemoteString
        myDelayField.value = serverConfiguration.warmUpMs
        myDownloadGroup.setSelectedValue(serverConfiguration.downloadType)
    }
}