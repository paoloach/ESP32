package it.achdjian.plugin.esp32.generator

import com.intellij.execution.RunManagerEx
import com.intellij.execution.impl.RunManagerImpl
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ex.ToolWindowManagerEx
import com.jetbrains.cidr.cpp.cmake.CMakeSettings
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeAbstractCProjectGenerator
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.CMakeProjectSettings
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import com.jetbrains.cidr.cpp.execution.CMakeAppRunConfigurationType
import it.achdjian.plugin.esp32.configurations.flash.ESP32FlashConfigurationType
import it.achdjian.plugin.esp32.configurations.flash.ESP32FlashRunConfiguration
import it.achdjian.plugin.esp32.configurator.ESP32WizardPanel
import it.achdjian.plugin.esp32.configurator.MAIN_DIR
import it.achdjian.plugin.esp32.configurator.MissingConfig
import it.achdjian.plugin.esp32.configurator.WizardData
import it.achdjian.plugin.esp32.sdk_config.createSdkConfigFile
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import it.achdjian.plugin.esp32.ui.getResourceAsString
import java.util.concurrent.TimeoutException
import javax.swing.JComponent

class CProjectGenerator : CMakeAbstractCProjectGenerator() {
    companion object {
        val esp32Project: Key<Boolean> = Key.create("ESP32")
        private val LOG = Logger.getInstance(CProjectGenerator::class.java)
        private val allowedCmakeTarget = listOf("flash", "app","apidoc","partition_table")
    }


    private val wizardData = WizardData()

    override fun getName(): String = "ESP32 C project"

     override fun createSourceFiles(projectName: String, path: VirtualFile): Array<VirtualFile> {
         return if (ESP32SettingState.validSDKPath()) {
             createSdkConfigFile(wizardData.entries, path)
             val files = mutableListOf<VirtualFile>()
             createMainFile(path, files)
             files.toTypedArray()
         } else {
             arrayOf()
         }
    }

    override fun getSettingsPanel(): JComponent {
        return if (ESP32SettingState.validSDKPath()) {
            ESP32WizardPanel(createSettingsPanel(), wizardData.entries)
        } else {
            MissingConfig(createSettingsPanel())
        }

    }

    override fun getCMakeFileContent(projectName: String): String {
        return if (ESP32SettingState.validSDKPath()) {
            var cmakelists = getResourceAsString("templates/CMakeLists.txt")
            cmakelists = cmakelists
                .replace("__{project_name}__", projectName)
                .replace("__{SDK_PATH}__", ESP32SettingState.sdkPath)
            cmakelists
        } else {
            ""
        }
    }

    override fun generateProject(
        project: Project,
        path: VirtualFile,
        cmakeSetting: CMakeProjectSettings,
        module: Module
    ) {
        if (ESP32SettingState.validSDKPath()) {
            super.generateProject(project, path, cmakeSetting, module)
            generateCrossCompilerConfiguration(project)
            generateFlashConfiguration(project)
            project.putUserData(esp32Project, true)

            val toolWindowManagerEx = ToolWindowManagerEx.getInstanceEx(project)
            val toolWindow = toolWindowManagerEx.getToolWindow("ESP32 Serial Monitor")
            toolWindow?.let {
                toolWindow.setAvailable(true) {
//                    project.getComponent(SerialMonitorView::class.java)?.initToolWindow(toolWindow)
                }
            }

            removeCmakeConfiguration(project)
        }
    }

    private fun removeCmakeConfiguration(project: Project){
        val cMakeWorkspace = CMakeWorkspace.getInstance(project)
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                cMakeWorkspace.waitForReloadsToFinish(1000000)
                val runManager = RunManagerEx.getInstanceEx(project) as RunManagerImpl
                val toRemove = runManager
                    .allSettings
                    .filter { it.type is CMakeAppRunConfigurationType}
                    .filter {  !allowedCmakeTarget.contains(it.name)}

                runManager.removeConfigurations(toRemove)

            } catch (e: TimeoutException)   {
                LOG.warn("workspace building too long")
            }
        }
    }
    private fun generateCrossCompilerConfiguration(project: Project) {
        val cMakeWorkspace = CMakeWorkspace.getInstance(project)
        val settings = cMakeWorkspace.settings
        val env = mapOf("PATH" to "${ESP32SettingState.crosscompilerPath}:/usr/bin:/sbin:/bin:/opt/bin")
        val releaseProfile = CMakeSettings.Profile(
            "Release",
            "Release",
            "",
            "-DCMAKE_TOOLCHAIN_FILE=CrossCompiler.cmake",
            true,
            env,
            null,
            null
        )

        settings.profiles = listOf(releaseProfile)
    }

    private fun generateFlashConfiguration(project: Project) {
        val runManager = RunManagerEx.getInstanceEx(project) as RunManagerImpl
        ESP32FlashConfigurationType.factoryESP32?.let {
            val newConfig = RunnerAndConfigurationSettingsImpl(
                runManager,
                ESP32FlashRunConfiguration(project, it, "Flash"),
                false
            )

            runManager.addConfiguration(newConfig)
            runManager.selectedConfiguration = newConfig

            project.save()
        }

    }
}

fun createMainFile(
    path: VirtualFile,
    files: MutableList<VirtualFile>
) {

    ApplicationManager.getApplication().runWriteAction {
        val mainDir = path.findChild(MAIN_DIR) ?: path.createChildDirectory(null, MAIN_DIR)
        val cMakeListsFile = mainDir.findOrCreateChildData(null, "CMakeLists.txt")
        val cMakeListsData = getResourceAsString("templates/main/CMakeLists.txt")

        val helloWordFile = mainDir.findOrCreateChildData(null, "hello_world_main.c")


        val helloWordData = getResourceAsString("templates/main/hello_world_main.c")
        cMakeListsFile.setBinaryContent(cMakeListsData.toByteArray())
        helloWordFile.setBinaryContent(helloWordData.toByteArray())
        files.add(helloWordFile)
    }
}


