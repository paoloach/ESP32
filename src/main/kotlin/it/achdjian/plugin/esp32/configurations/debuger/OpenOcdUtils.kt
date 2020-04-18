package it.achdjian.plugin.esp32.configurations.debuger

import java.io.File

fun findOcdScripts(openOcdLocation: File): File {
    val binFolder = openOcdLocation.parentFile
    val homeFolder = binFolder?.parentFile ?: openOcdLocation
    val ocdScripts = File(homeFolder, "share/openocd/scripts")
    if (!ocdScripts.exists()) {
        return File(homeFolder, "scripts")
    }
    return ocdScripts
}