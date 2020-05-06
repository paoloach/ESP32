package it.achdjian.plugin.esp32.configurator

import it.achdjian.plugin.esp32.setting.ESP32SettingState
import java.io.File

class SourceList(val idfPath:String  = ESP32SettingState.sdkPath) {
    operator fun get(envVariable: String): List<File> {
        return sourcesList[envVariable] ?.let { it } ?: addNewEnvVariable(envVariable)
    }



    private val componentDir = File(File(idfPath), "components")
    private val componentKConfig = componentDir.walk().filter { file -> file.name == "Kconfig" }.toList()
    private val componentKConfigProjbuild = componentDir.walk().filter { file -> file.name == "Kconfig.projbuild" }.toList()
    private val sourcesList = mapOf(
        "COMPONENT_KCONFIGS" to componentKConfig.toList(),
        "COMPONENT_KCONFIGS_SOURCE_FILE" to componentKConfig.toList(),
        "COMPONENT_KCONFIGS_PROJBUILD" to componentKConfigProjbuild.toList(),
        "COMPONENT_KCONFIGS_PROJBUILD_SOURCE_FILE" to componentKConfigProjbuild.toList()
    )


    private fun addNewEnvVariable(envVariable: String): List<File> {
        val path = envVariable.replace("IDF_PATH",idfPath )
        val file=File(path)
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