package net.ccbluex.liquidbounce.integration.theme.component.components
import net.ccbluex.liquidbounce.additions.*

import net.ccbluex.liquidbounce.integration.theme.component.HudComponent
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.ccbluex.liquidbounce.utils.client.logger

class WatermarkComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        try {
            val watermarkText = "LiquidBounce ${net.ccbluex.liquidbounce.LiquidBounce.clientVersion}"
            val textWidth = mc.textRenderer.getWidth(watermarkText)

            val margin = 4
            context.fill(
                margin - 2,
                margin,
                margin + textWidth + 2,
                margin + mc.textRenderer.fontHeight + 2,
                java.awt.Color(0, 0, 0, 120).rgb
            )

            context.drawText(
                mc.textRenderer,
                watermarkText,
                margin,
                margin + 1,
                java.awt.Color.HSBtoRGB(((System.currentTimeMillis() % 3600L) / 3600.0f), 0.8f, 1f),
                true
            )
        } catch (e: Exception) {
            logger.error("Failed to render watermark component", e)
        }
    }
}
