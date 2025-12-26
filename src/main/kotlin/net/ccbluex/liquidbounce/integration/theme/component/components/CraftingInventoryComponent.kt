package net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.additions.*

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class CraftingInventoryComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {
    override fun render(context: GuiGraphics) {
        val player = mc.player ?: return
        // Render a small 3x3 grid of crafting area on screen
        val x = mc.window.scaledWidth / 2 - 40
        val y = mc.window.scaledHeight / 2 - 40
        // This is a simplified representation; in-depth rendering would reuse vanilla GUI code
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                val stack = net.minecraft.item.ItemStack.EMPTY
                context.fill(x + col * 20, y + row * 20, x + col * 20 + 18, y + row * 20 + 18, 0xAA000000.toInt())
            }
        }
    }
}
