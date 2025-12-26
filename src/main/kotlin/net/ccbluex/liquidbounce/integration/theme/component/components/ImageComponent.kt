package net.ccbluex.liquidbounce.integration.theme.component.components

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class ImageComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>,
    val values: Array<JsonObject>
) : NativeComponent(name, enabled, alignment, tweaks) {

    override fun render(context: DrawContext) {
        // Simple static image rendering for theme images. We'll parse the 'value' key for image path
        val imageUrl = values.find { it["name"]?.asString == "Image" }?.get("value")?.asString ?: return
        // TODO: Load the image from the theme's resource. For now, render a placeholder box.
        val x = 10
        val y = 80
        val width = 80
        val height = 80
        context.fill(x, y, x + width, y + height, 0xFF444444.toInt())
        context.drawText(mc.textRenderer, "Image", x + 4, y + 4, 0xFFFFFFFF.toInt(), true)
    }
}
