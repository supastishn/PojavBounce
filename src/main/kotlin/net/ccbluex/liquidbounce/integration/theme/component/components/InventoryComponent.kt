package net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.additions.*

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class InventoryComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {
    override fun render(context: GuiGraphics) {
        val player = mc.player ?: return
        val x = 10
        var y = 100
        for (i in 0 until 9) {
            val stack = player.inventory.getStack(i)
            context.drawItem(stack, x + i * 18, y)
        }
    }
}
