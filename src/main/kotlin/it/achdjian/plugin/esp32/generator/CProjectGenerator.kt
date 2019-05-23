package it.achdjian.plugin.esp32.generator

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.cidr.cpp.cmake.CMakeSettings
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.CMakeAbstractCProjectGenerator
import com.jetbrains.cidr.cpp.cmake.projectWizard.generators.settings.CMakeProjectSettings
import com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace
import it.achdjian.plugin.esp32.configurator.*
import it.achdjian.plugin.esp32.entry_type.ConfigurationEntry
import it.achdjian.plugin.esp32.setting.ESP32SettingState
import it.achdjian.plugin.esp32.ui.getResourceAsString
import javax.swing.JComponent

class CProjectGenerator : CMakeAbstractCProjectGenerator() {
    val wizardData = WizardData()
//    var settingPanel = ESP32WizardPanel(createSettingsPanel(), wizardData.entries)

    override fun getName(): String = "ESP32 C project"

    override fun createSourceFiles(projectName: String, path: VirtualFile): Array<VirtualFile> {
        createSdkConfigFile(wizardData.entries,path)
        val main = createMainFile(path)
        return arrayOf(main)
    }

    override fun getSettingsPanel(): JComponent? {
        val settingPanel = ESP32WizardPanel(createSettingsPanel(), wizardData.entries)
        return settingPanel
    }

    override fun getCMakeFileContent(projectName: String): String {
        var cmakelists = getResourceAsString("templates/CMakeLists.txt")
        cmakelists = cmakelists
            .replace("__{project_name}__", projectName)
            .replace("__{SDK_PATH}__", ESP32SettingState.sdkPath)
        return cmakelists
    }

    override fun generateProject(project: Project, path: VirtualFile, cmakeSetting: CMakeProjectSettings, module: Module) {
        super.generateProject(project, path, cmakeSetting, module)
        val cMakeWorkspace = CMakeWorkspace.getInstance(project)
        val settings = cMakeWorkspace.settings
        val env = mapOf("PATH" to "${ESP32SettingState.crosscompilerPath}:/usr/bin:/sbin:/bin:/opt/bin")
        var releaseProfile = CMakeSettings.Profile("Release", "Release", "", "-DCMAKE_TOOLCHAIN_FILE=CrossCompiler.cmake", true, env, null, null)

        settings.profiles = listOf(releaseProfile)
    }
}

fun createMainFile(path:VirtualFile):VirtualFile{
    val mainDir = path.findChild(MAIN_DIR)?: path.createChildDirectory(null, MAIN_DIR)
    val cMakeListsFile = mainDir.findOrCreateChildData(null, "CMakeLists.txt")
    var cMakeListsData = getResourceAsString("templates/main/CMakeLists.txt")

    val helloWordFile = mainDir.findOrCreateChildData(null, "hello_world_main.c")

    var helloWordData = getResourceAsString("templates/main/hello_world_main.c")
    ApplicationManager.getApplication().runWriteAction {
        cMakeListsFile.setBinaryContent(cMakeListsData.toByteArray())
        helloWordFile.setBinaryContent(helloWordData.toByteArray())
    }

    return helloWordFile
}

fun createConfigFolders(path: VirtualFile): VirtualFile {
    val main = path.findChild(MAIN_DIR)?: path.createChildDirectory(null, MAIN_DIR)
    val build = main.findChild(BUILD_DIR)?:main.createChildDirectory(null, BUILD_DIR)
   return build.findChild(CONFIG_DIR)?:build.createChildDirectory(null, CONFIG_DIR)

}


fun createSdkConfigFile(entries: List<ConfigurationEntry>, path: VirtualFile) {
    val sdkConfig = path.findOrCreateChildData(null, CONFIG_FILE_NAME)
    val configurations = mutableListOf<Pair<String,String>>()
    entries.forEach { it.addConfiguration(configurations) }

    val data = configurations.joinToString(separator = "\n") { "CONFIG_${it.first}=${it.second}" }

    ApplicationManager.getApplication().runWriteAction {
        sdkConfig.setBinaryContent(data.toByteArray())
    }
}