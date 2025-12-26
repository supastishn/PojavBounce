package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class KeystrokesComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        val margin = 10
        val keySize = 12
        val spacing = 4
        val xPos = margin
        var yPos = mc.window.scaledHeight - margin - keySize

        // W key
        drawKey(context, xPos, yPos - (keySize + spacing), "W", mc.options.forwardKey.isPressed)

        // A S D arranged below
        drawKey(context, xPos - (keySize + spacing), yPos, "A", mc.options.leftKey.isPressed)
        drawKey(context, xPos, yPos, "S", mc.options.backKey.isPressed)
        drawKey(context, xPos + (keySize + spacing), yPos, "D", mc.options.rightKey.isPressed)

        // Space (jump)
        drawKey(context, xPos + (2 * (keySize + spacing)), yPos, "J", mc.options.jumpKey.isPressed)
    }

    private fun drawKey(context: DrawContext, x: Int, y: Int, key: String, pressed: Boolean) {
        val size = 18
        val background = if (pressed) 0xFF00FF00.toInt() else 0xFF000000.toInt()
        context.fill(x, y, x + size, y + size, background)
        context.drawText(mc.textRenderer, key, x + 4, y + 4, 0xFFFFFFFF.toInt(), true)
    }
}
