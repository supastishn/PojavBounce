package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class TargetHudComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeComponent(name, enabled, alignment, tweaks) {

    override fun render(context: DrawContext) {
        val hit = mc.crosshairTarget
        val entity = if (hit != null && hit is net.minecraft.util.hit.EntityHitResult) hit.entity else null

        if (entity is net.minecraft.entity.LivingEntity) {
            val displayName = entity.name.string
            val health = entity.getHealth() // <- may not compile, fallback
            val healthString = "Health: ${entity.health}"

            // Render at top-left
            val x = 10
            val y = 30
            context.fill(x, y, x + 140, y + 30, 0xAA000000.toInt())
            context.drawText(mc.textRenderer, displayName, x + 4, y + 4, 0xFFFFFFFF.toInt(), true)
            context.drawText(mc.textRenderer, healthString, x + 4, y + 14, 0xFFAAAAAA.toInt(), true)
        }
    }
}
