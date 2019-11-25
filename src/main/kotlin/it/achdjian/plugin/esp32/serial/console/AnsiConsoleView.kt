package it.achdjian.plugin.esp32.serial.console

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project

class AnsiConsoleView(project: Project, viewer: Boolean) :  ConsoleViewImpl(project, viewer) {
    private val ansiEscapeDecoder = AnsiEscapeDecoder()
    override fun print(message: String, contentType: ConsoleViewContentType) {
        ansiEscapeDecoder.escapeText(message, ProcessOutputTypes.STDOUT){ text, attributes->
            super.print(text, ConsoleViewContentType.getConsoleViewType(attributes))
        }
    }
}