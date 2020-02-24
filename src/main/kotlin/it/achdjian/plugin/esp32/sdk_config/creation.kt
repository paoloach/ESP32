package it.achdjian.plugin.esp32.sdk_config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.VirtualFile
import it.achdjian.plugin.esp32.configurator.CONFIG_FILE_NAME
import it.achdjian.plugin.esp32.entry_type.ConfigurationEntry

fun createSdkConfigFile(entries: List<ConfigurationEntry>, path: VirtualFile) {
    val configurations = mutableListOf<Pair<String, String>>()
    entries.forEach {
        it.addConfiguration(configurations)
    }

    val data = configurations.joinToString(separator = "\n") { "CONFIG_${it.first}=${it.second}" }

    ApplicationManager.getApplication().runWriteAction {
        val sdkConfig = path.findOrCreateChildData(null, CONFIG_FILE_NAME)
        sdkConfig.setBinaryContent(data.toByteArray())
    }
}