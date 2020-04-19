package it.achdjian.plugin.esp32.help

import com.intellij.openapi.help.WebHelpProvider

class ESP32HelpProvider :  WebHelpProvider(){
    override fun getHelpPageUrl(topicId: String): String? {
       return when(topicId) {
           "it.achdjian.plugin.ESP32.setup" -> "https://github.com/paoloach/ESP32/blob/Debugger/README.md#setup"
           else -> "https://github.com/paoloach/ESP32/blob/Debugger/README.md"
       }
    }
}