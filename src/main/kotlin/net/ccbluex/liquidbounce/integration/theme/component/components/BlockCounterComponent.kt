package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class BlockCounterComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeComponent(name, enabled, alignment, tweaks) {
    override fun render(context: DrawContext) {
        val player = mc.player ?: return
        val blockCounts = player.inventory.main.toList().groupingBy { it.item.registryName?.path }.eachCount()
        var y = 10
        val x = 150
        for ((block, count) in blockCounts.take(10)) {
            context.drawText(mc.textRenderer, "${block ?: "unknown"}: $count", x, y, 0xFFFFFFFF.toInt(), true)
            y += mc.textRenderer.fontHeight
        }
    }
}
