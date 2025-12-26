package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class ArmorItemsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeComponent(name, enabled, alignment, tweaks) {
    override fun render(context: DrawContext) {
        val player = mc.player ?: return
        val armor = player.inventory.armor
        var y = 10
        val x = 10
        for (i in armor.indices) {
            val stack = armor[i]
            context.drawItem(stack, x, y)
            y += 16
        }
    }
}
