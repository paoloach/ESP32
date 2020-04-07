package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.openapi.options.ConfigurationException
import java.io.File

fun findScripts(openOcdLocation: File): File {
    val binFolder = openOcdLocation.parentFile
    var homeFolder: File? = null
    if (binFolder != null) {
        homeFolder = binFolder.parentFile
    }
    if (homeFolder == null) {
        homeFolder = openOcdLocation
    }
    var ocdScripts = File(homeFolder, "share/openocd/scripts")
    if (!ocdScripts.exists()) {
        ocdScripts = File(homeFolder, "scripts")
    }
    return ocdScripts
}

@Throws(ConfigurationException::class)
fun require(fileToCheck: File): File {
    return if (fileToCheck.exists()) {
        fileToCheck
    } else {
        throw ConfigurationException("Invalid location", "OpenOCD")
    }
}
