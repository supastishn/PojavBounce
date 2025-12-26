// TODO: Fix for Minecraft 1.21 - API changes required
package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class EffectsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {
    override fun render(context: GuiGraphics) {
        val player = mc.player ?: return
        val effects = player.statusEffects
        var y = 10
        val x = mc.window.scaledWidth - 150
        for (effect in effects) {
            context.drawText(mc.textRenderer, effect.type.name.string, x, y, 0xFFFFFF.toInt(), true)
            y += mc.textRenderer.fontHeight
        }
    }
}
