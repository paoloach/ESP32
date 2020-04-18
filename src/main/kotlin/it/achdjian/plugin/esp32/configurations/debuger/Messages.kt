package it.achdjian.plugin.esp32.configurations.debuger

import com.intellij.CommonBundle
import com.intellij.execution.impl.EditConfigurationsDialog
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.help.HelpManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.cidr.cpp.toolchains.CPPToolchainsConfigurable


fun showErrorMessage(project: Project?, title: String, message: String) = showMessage(project, NotificationType.ERROR, title, message, false)

fun showErrorMessage(project: Project?, e: ConfigurationException) =  showErrorMessage(project, e.title, e.message!!)

fun showErrorMessage(project: Project, title: String, message: String, enableRunConfiguration: Boolean) =
    showMessage(project, NotificationType.ERROR, title, message, enableRunConfiguration)

fun showSuccessfulDownloadNotification(project: Project) =
    showSuccessMessage(project, "OpenOCD", "firmware downloaded")

fun showSuccessMessage(project: Project?, title: String, message: String)
        = showMessage(project, NotificationType.INFORMATION, title, message, false)

fun openHelp() {
    HelpManager.getInstance().invokeHelp("Embedded_Development")
}

fun showFailedDownloadNotification(p: Project?) {
    val project = p ?: ProjectManager.getInstance().defaultProject
    showErrorMessage(project, "OpenOCD", "MCU communication failure detailed", true)
}


fun showMessage(project: Project?, notificationType: NotificationType, title: String, message: String, enableRunConfiguration: Boolean) {
    val notification = Notification("Embedded Development", title, message, notificationType)
    notification.addAction(DumbAwareAction.create(CommonBundle.message("action.help")) { openHelp() })
    if (notificationType != NotificationType.INFORMATION) {
        notification.addAction(
            DumbAwareAction.create("Show settings")
             { ShowSettingsUtil.getInstance().showSettingsDialog(project, ESP32DebugSettingsConfigurable::class.java) }
        )
        notification.addAction(
            DumbAwareAction.create("Show toolchains")
             { ShowSettingsUtil.getInstance().showSettingsDialog(project, CPPToolchainsConfigurable::class.java)
            }
        )
        if (enableRunConfiguration && project != null) {
            notification.addAction(
                DumbAwareAction.create("Edit configuration")
                 { EditConfigurationsDialog(project).show() }
            )
        }
    }
    Notifications.Bus.notify(notification, project)
}