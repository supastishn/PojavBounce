/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.integration.ui.hud.NotificationManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics

class NotificationsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    fun render(context: GuiGraphics) {
        val notifications = NotificationManager.getNotifications()
        val margin = 10
        var y = margin
        val width = 200

        for (n in notifications) {
            context.fill(margin, y, margin + width, y + mc.font.lineHeight + 6, 0xAA000000.toInt())
            context.drawString(mc.font, n.title, margin + 4, y + 2, 0xFFFFFFFF.toInt())
            context.drawString(mc.font, n.message, margin + 4, y + 2 + mc.font.lineHeight, 0xFFAAAAAA.toInt())
            y += mc.font.lineHeight + 8
        }
    }
}
