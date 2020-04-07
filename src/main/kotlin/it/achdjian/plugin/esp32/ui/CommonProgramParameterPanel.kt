package it.achdjian.plugin.esp32.ui

import com.intellij.execution.CommonProgramRunConfigurationParameters
import com.intellij.execution.ExecutionBundle
import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.util.ProgramParametersConfigurator
import com.intellij.execution.util.ProgramParametersUtil
import com.intellij.ide.macro.EditorMacro
import com.intellij.ide.macro.Macro
import com.intellij.ide.macro.MacrosDialog
import com.intellij.openapi.application.PathMacros
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.TextComponentAccessor
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.registry.Registry
import com.intellij.ui.RawCommandLineEditor
import com.intellij.ui.TextAccessor
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.util.PathUtil
import com.intellij.util.ui.UIUtil
import org.jetbrains.jps.model.serialization.PathMacroUtil
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import java.util.function.Predicate
import javax.swing.JComponent
import javax.swing.JPanel

open class CommonProgramParameterPanel : JPanel() {
    protected var myProgramParametersComponent = LabeledComponent.create(RawCommandLineEditor(), ExecutionBundle.message("run.configuration.program.parameters"))
    protected var myWorkingDirectoryField =TextFieldWithBrowseButton()
    protected var myWorkingDirectoryComponent = LabeledComponent.create(myWorkingDirectoryField, ExecutionBundle.message("run.configuration.working.directory.label"))
    protected var myEnvVariablesComponent = EnvironmentVariablesComponent()
    protected var myAnchor: JComponent? = null

    private var myModuleContext: Module? = null
    private var myHasModuleMacro = false

    protected val project : Project?
        get() = myModuleContext ?.let {  it.project}

    init {
        layout = VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, 5, true, false)
        initComponents()
        updateUI()
        setupAnchor()
    }

    protected fun initComponents() {

        // for backward compatibility: com.microsoft.tooling.msservices.intellij.azure:3.0.11
        myWorkingDirectoryField.addBrowseFolderListener(
            ExecutionBundle.message("select.working.directory.message"), null,
            project,
            FileChooserDescriptorFactory.createSingleFolderDescriptor(),
            TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT
        )
        myEnvVariablesComponent = EnvironmentVariablesComponent()
        myEnvVariablesComponent.labelLocation = BorderLayout.WEST
        myProgramParametersComponent.labelLocation = BorderLayout.WEST
        myWorkingDirectoryComponent.labelLocation = BorderLayout.WEST
        addComponents()
        if (isMacroSupportEnabled()) {
            initMacroSupport()
        }
        preferredSize = Dimension(10, 10)
        copyDialogCaption(myProgramParametersComponent)
    }

    protected fun setupAnchor() {
        myAnchor = UIUtil.mergeComponentsWithAnchor(myProgramParametersComponent, myWorkingDirectoryComponent, myEnvVariablesComponent)
    }

    protected fun addComponents() {
        add(myProgramParametersComponent)
        add(myWorkingDirectoryComponent)
        add(myEnvVariablesComponent)
    }

    /**
     * Macro support for run configuration fields is opt-in.
     * Run configurations that can handle macros (basically any using [ProgramParametersConfigurator] or [ProgramParametersUtil])
     * are encouraged to enable "add macro" inline button for program parameters and working directory fields by overriding this method,
     * and optionally overriding [.initMacroSupport] to enable macros for other fields.
     */
    protected fun isMacroSupportEnabled(): Boolean {
        return false
    }

    protected fun initMacroSupport() {
        addMacroSupport(
            myProgramParametersComponent.component.editorField,
            MacrosDialog.Filters.ALL,
            getPathMacros()
        )
        addMacroSupport(
            myWorkingDirectoryField.textField as ExtendableTextField,
            MacrosDialog.Filters.DIRECTORY_PATH,
            getPathMacros()
        )
    }

    fun addMacroSupport(textField: ExtendableTextField) {
        doAddMacroSupport(textField, MacrosDialog.Filters.ALL, null)
    }

    protected fun addMacroSupport(
        textField: ExtendableTextField,
        macroFilter: Predicate<in Macro?>,
        userMacros: Map<String?, String?>?
    ) {
        val commonMacroFilter = getCommonMacroFilter()
        doAddMacroSupport(
            textField,
            Predicate { t: Macro ->
                commonMacroFilter.test(
                    t
                ) && macroFilter.test(t)
            }, userMacros
        )
    }

    protected fun getCommonMacroFilter(): Predicate<in Macro> {
        return MacrosDialog.Filters.ALL
    }

    private fun doAddMacroSupport(
        textField: ExtendableTextField,
        macroFilter: Predicate<in Macro>,
        userMacros: Map<String?, String?>?
    ) {
        if (Registry.`is`("allow.macros.for.run.configurations")) {
            MacrosDialog.addTextFieldExtension(
                textField,
                macroFilter.and { macro -> macro !is EditorMacro },
                userMacros
            )
        }
    }

    protected fun getPathMacros(): Map<String?, String?> {
        val macros =
            HashMap(
                PathMacros.getInstance().userMacros
            )
        if (myModuleContext != null || myHasModuleMacro) {
            macros[PathMacroUtil.MODULE_DIR_MACRO_NAME] =
                PathMacros.getInstance().getValue(PathMacroUtil.MODULE_DIR_MACRO_NAME)
            macros[ProgramParametersConfigurator.MODULE_WORKING_DIR] =
                PathMacros.getInstance().getValue(PathMacroUtil.MODULE_WORKING_DIR_NAME)
        }
        return macros
    }

    protected fun copyDialogCaption(component: LabeledComponent<RawCommandLineEditor>) {
        val rawCommandLineEditor = component.component
        rawCommandLineEditor.dialogCaption = component.rawText
        component.label.labelFor = rawCommandLineEditor.textField
    }

    fun setProgramParametersLabel(textWithMnemonic: String?) {
        myProgramParametersComponent.text = textWithMnemonic
        copyDialogCaption(myProgramParametersComponent)
    }

    fun setProgramParameters(params: String?) {
        myProgramParametersComponent.component.text = params
    }

    fun getWorkingDirectoryAccessor(): TextAccessor? {
        return myWorkingDirectoryField
    }

    fun setWorkingDirectory(dir: String?) {
        myWorkingDirectoryField.setText(dir)
    }

    fun setModuleContext(moduleContext: Module) {
        myModuleContext = moduleContext
    }

    fun setHasModuleMacro() {
        myHasModuleMacro = true
    }

    fun getProgramParametersComponent(): LabeledComponent<RawCommandLineEditor>? {
        return myProgramParametersComponent
    }


    var anchor: JComponent ?
        get() = myAnchor
        set(anchor) {
            myAnchor=anchor
            myProgramParametersComponent.anchor = anchor
            myWorkingDirectoryComponent.anchor = anchor
            myEnvVariablesComponent.anchor = anchor
        }


    fun applyTo(configuration: CommonProgramRunConfigurationParameters) {
        configuration.programParameters = fromTextField(myProgramParametersComponent.component, configuration)
        configuration.workingDirectory = fromTextField(myWorkingDirectoryField, configuration)
        configuration.envs = myEnvVariablesComponent.envs
        configuration.isPassParentEnvs = myEnvVariablesComponent.isPassParentEnvs
    }

    protected fun fromTextField(
        textAccessor: TextAccessor,
        configuration: CommonProgramRunConfigurationParameters
    ): String? {
        return textAccessor.text
    }

    fun reset(configuration: CommonProgramRunConfigurationParameters) {
        setProgramParameters(configuration.programParameters)
        setWorkingDirectory(PathUtil.toSystemDependentName(configuration.workingDirectory))
        myEnvVariablesComponent!!.envs = configuration.envs
        myEnvVariablesComponent!!.isPassParentEnvs = configuration.isPassParentEnvs
    }
}