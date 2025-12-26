package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.ui.hud.NotificationManager
import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class NotificationsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        val notifications = NotificationManager.getNotifications()
        val margin = 10
        var y = margin
        val width = 200

        for (n in notifications) {
            context.fill(margin, y, margin + width, y + mc.textRenderer.fontHeight + 6, 0xAA000000.toInt())
            context.drawText(mc.textRenderer, n.title, margin + 4, y + 2, 0xFFFFFFFF.toInt(), true)
            context.drawText(mc.textRenderer, n.message, margin + 4, y + 2 + mc.textRenderer.fontHeight, 0xFFAAAAAA.toInt(), true)
            y += mc.textRenderer.fontHeight + 8
        }
    }
}
