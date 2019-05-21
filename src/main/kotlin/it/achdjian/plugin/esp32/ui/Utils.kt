package it.achdjian.plugin.esp32.ui

import com.intellij.openapi.util.io.FileUtil
import it.achdjian.plugin.esp32.setting.ESP32Setting

fun getResourceAsString(resourceName: String): String {
    val resource = ESP32Setting::class.java.classLoader.getResourceAsStream(resourceName)
    return if (resource != null) {
        FileUtil.loadTextAndClose(resource)
    } else {
        ""
    }

}

fun getResourceAsBytes(resourceName: String): ByteArray {
    val resource = ESP32Setting::class.java.classLoader.getResourceAsStream(resourceName)
    return if (resource != null) {
        FileUtil.loadBytes(resource)
    } else {
        ByteArray(0)
    }

}