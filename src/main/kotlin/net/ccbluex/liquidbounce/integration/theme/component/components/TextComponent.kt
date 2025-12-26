package net.ccbluex.liquidbounce.integration.theme.component.components

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.ccbluex.liquidbounce.utils.render.Alignment

class TextComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>,
    val values: Array<JsonObject>
) : NativeComponent(name, enabled, alignment, tweaks) {

    override fun render(context: DrawContext) {
        val textValue = values.find { it["name"]?.asString == "Text" }?.get("value")?.asString ?: return
        val colorValue = values.find { it["name"]?.asString == "Color" }?.get("value")?.asLong ?: 0xFFFFFFFF

        // Basic drawing, ignoring font and size for now
        val x = 10
        val y = 10
        context.drawText(mc.textRenderer, textValue, x, y, colorValue.toInt(), true)
    }
}
