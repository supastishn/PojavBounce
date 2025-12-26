package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics

/**
 * Base interface for all HUD components
 */
interface Component {
    val name: String
    val enabled: Boolean
    val alignment: Alignment

    fun render(context: GuiGraphics, partialTicks: Float)
}

/**
 * Represents a tweak/modification to a component
 */
data class ComponentTweak(
    val type: String,
    val value: Any?
)
