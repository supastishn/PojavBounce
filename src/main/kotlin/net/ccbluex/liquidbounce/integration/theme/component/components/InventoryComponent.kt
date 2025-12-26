package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class InventoryComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeComponent(name, enabled, alignment, tweaks) {
    override fun render(context: DrawContext) {
        val player = mc.player ?: return
        val x = 10
        var y = 100
        for (i in 0 until 9) {
            val stack = player.inventory.getStack(i)
            context.drawItem(stack, x + i * 18, y)
        }
    }
}
