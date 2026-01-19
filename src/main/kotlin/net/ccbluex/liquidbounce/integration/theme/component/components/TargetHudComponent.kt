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
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.phys.EntityHitResult

// TODO: Fix for Minecraft 1.21 - API changes required
class TargetHudComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<HudComponentTweak> = emptyArray()
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    fun render(context: GuiGraphics) {
        val hit = mc.hitResult
        val entity = if (hit is EntityHitResult) hit.entity else null

        if (entity is net.minecraft.world.entity.LivingEntity) {
            val displayName = entity.name.string
            val healthString = "Health: ${entity.health}"

            // Render at top-left
            val x = 10
            val y = 30
            context.fill(x, y, x + 140, y + 30, 0xAA000000.toInt())
            context.drawString(mc.font, displayName, x + 4, y + 4, 0xFFFFFFFF.toInt())
            context.drawString(mc.font, healthString, x + 4, y + 14, 0xFFAAAAAA.toInt())
        }
    }
}
