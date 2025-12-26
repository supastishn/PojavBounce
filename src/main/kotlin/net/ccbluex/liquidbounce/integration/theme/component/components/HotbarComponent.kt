package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class HotbarComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeComponent(name, enabled, alignment, tweaks) {

    override fun render(context: DrawContext) {
        val player = mc.player ?: return
        val y = mc.window.scaledHeight - 50
        val x = (mc.window.scaledWidth / 2) - 90

        // Draw 9 hotbar items
        for (i in 0 until 9) {
            val stack = player.inventory.getStack(i)
            context.drawItem(stack, x + i * 20, y)
        }
    }
}
