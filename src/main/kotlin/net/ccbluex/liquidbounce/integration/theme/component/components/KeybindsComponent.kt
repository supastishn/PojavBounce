package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class KeybindsComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        val keyboundModules = ModuleManager.filter { it.key != null && it.key != 0 }
        var y = 10
        val x = 10
        for (module in keyboundModules.take(10)) {
            context.drawText(mc.textRenderer, "${module.name} - ${module.key}", x, y, 0xFFFFFFFF.toInt(), true)
            y += mc.textRenderer.fontHeight
        }
    }
}
