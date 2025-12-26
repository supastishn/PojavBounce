package net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.additions.*

import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class ArrayListComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    private val MARGIN = 4
    private val MODULE_SPACING = 2

    override fun render(context: GuiGraphics) {
        val enabledModules = ModuleManager
            .filter { it.enabled && !it.hidden }
            .sortedByDescending { mc.textRenderer.getWidth(getModuleDisplayText(it)) }

        var yOffset = MARGIN
        val screenWidth = mc.window.scaledWidth

        for (module in enabledModules) {
            val displayText = getModuleDisplayText(module)
            val textWidth = mc.textRenderer.getWidth(displayText)
            val xPos = screenWidth - textWidth - MARGIN

            // Background
            context.fill(
                xPos - 2,
                yOffset,
                screenWidth - MARGIN + 2,
                yOffset + mc.textRenderer.fontHeight + 2,
                java.awt.Color(0, 0, 0, 120).rgb
            )

            // Module text
            context.drawText(
                mc.textRenderer,
                displayText,
                xPos,
                yOffset + 1,
                java.awt.Color.HSBtoRGB(((System.currentTimeMillis() % 3600L) / 3600.0f), 0.8f, 1f),
                true
            )

            yOffset += mc.textRenderer.fontHeight + MODULE_SPACING
        }
    }

    private fun getModuleDisplayText(module: net.ccbluex.liquidbounce.features.module.ClientModule): String {
        return if (module.tag != null) {
            "${module.name} §7${module.tag}"
        } else {
            module.name
        }
    }
}
