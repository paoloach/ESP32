package it.achdjian.plugin.esp32.configurator

import it.achdjian.plugin.esp32.setting.ESP32SettingState
import java.io.File

class SourceList {
    operator fun get(envVariable: String): List<File> {
        return sourcesList[envVariable] ?.let { it } ?: addNewEnvVariable(envVariable)
    }



    private val idfPath = File(ESP32SettingState.sdkPath)
    private val componentDir = File(idfPath, "components")
    private val componentKConfig = componentDir.walk().filter { file -> file.name == "Kconfig" }.toList()
    private val componentKConfigProjbuild = componentDir.walk().filter { file -> file.name == "Kconfig.projbuild" }.toList()
    private val sourcesList = mapOf(
        "COMPONENT_KCONFIGS" to componentKConfig.toList(),
        "COMPONENT_KCONFIGS_PROJBUILD" to componentKConfigProjbuild.toList()
    )


    private fun addNewEnvVariable(envVariable: String): List<File> {
        envVariable.replace("\$IDF_PATH",ESP32SettingState.sdkPath )
        val file=File(envVariable);
        return if (file.exists()){
            if (file.isDirectory){
                componentDir.walk().toList()
            } else {
                listOf(file)
            }
        } else {
            listOf()
        }

    }

}