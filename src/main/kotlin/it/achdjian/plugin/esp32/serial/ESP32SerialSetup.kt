package it.achdjian.plugin.esp32.serial

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware

class ESP32SerialSetup: ToggleAction("Setup", "Setup serial communication", AllIcons.FileTypes.Config), DumbAware {
    override fun isSelected(p0: AnActionEvent): Boolean {
        return false
    }

    override fun setSelected(p0: AnActionEvent, p1: Boolean) {
    }
}