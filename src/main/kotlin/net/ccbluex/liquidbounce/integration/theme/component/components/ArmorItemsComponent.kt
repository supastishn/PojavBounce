package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class ArmorItemsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {
    override fun render(context: GuiGraphics) {
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
