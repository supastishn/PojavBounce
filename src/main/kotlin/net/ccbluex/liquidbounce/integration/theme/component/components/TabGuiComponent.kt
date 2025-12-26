package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class TabGuiComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        // Basic tab GUI: show categories and first module in each category
        val categories = ModuleManager.groupBy { it.category }
        var y = 10
        val x = 10
        for ((category, modules) in categories) {
            context.drawText(mc.textRenderer, category.name, x, y, 0xFFFFFF.toInt(), true)
            y += mc.textRenderer.fontHeight + 2
            val firstModule = modules.firstOrNull() ?: continue
            context.drawText(mc.textRenderer, "  ${firstModule.name}", x, y, 0xAAAAAA.toInt(), true)
            y += mc.textRenderer.fontHeight + 6
        }
    }
}
