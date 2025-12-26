package net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.additions.*

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class TacoComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {
    override fun render(context: GuiGraphics) {
        val x = mc.window.scaledWidth - 50
        val y = mc.window.scaledHeight - 50
        context.fill(x, y, x + 40, y + 20, 0xFFAA4400.toInt())
        context.drawText(mc.textRenderer, "🌮", x + 10, y + 2, 0xFFFFFFFF.toInt(), true)
    }
}
