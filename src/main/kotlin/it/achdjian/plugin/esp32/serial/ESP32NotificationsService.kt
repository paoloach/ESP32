package it.achdjian.plugin.esp32.serial

import com.intellij.notification.*

object ESP32NotificationsService {
    private var notificationGroup =  NotificationGroupManager.getInstance().getNotificationGroup("Serial Monitor Notification")


    fun createNotification(content: String, type: NotificationType): Notification {
        return notificationGroup.createNotification(content, type)
    }

    fun createErrorNotification(content: String): Notification {
        return notificationGroup.createNotification(content, NotificationType.ERROR)
    }
}