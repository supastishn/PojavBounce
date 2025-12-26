package net.ccbluex.liquidbounce.integration.ui.hud

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import java.util.concurrent.CopyOnWriteArrayList

object NotificationManager : EventListener {

    private const val MAX_NOTIFICATIONS = 8
    private val notifications = CopyOnWriteArrayList<Notification>()

    data class Notification(val title: String, val message: String, val severity: NotificationEvent.Severity)

    private val notificationHandler = handler<NotificationEvent>(
        priority = EventPriorityConvention.READ_FINAL_STATE
    ) { event ->
        notifications.add(0, Notification(event.title, event.message, event.severity))
        while (notifications.size > MAX_NOTIFICATIONS) {
            notifications.removeAt(notifications.size - 1)
        }
    }

    fun getNotifications(): List<Notification> = notifications
}
