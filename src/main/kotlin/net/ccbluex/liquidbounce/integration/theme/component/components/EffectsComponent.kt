package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class EffectsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeComponent(name, enabled, alignment, tweaks) {
    override fun render(context: DrawContext) {
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
