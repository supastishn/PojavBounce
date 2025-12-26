// TODO: Fix for Minecraft 1.21 - API changes required
import net.ccbluex.liquidbounce.additions.*
package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.integration.theme.component.HudComponentTweak
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.ccbluex.liquidbounce.utils.render.Alignment

class ScoreboardComponent(
    name: String,
    enabled: Boolean,
    alignment: Alignment,
    tweaks: Array<ComponentTweak>
) : NativeHudComponent(name, enabled, alignment, tweaks) {

    override fun render(context: GuiGraphics) {
        val player = mc.player ?: return
        val objective = player.scoreboard.getObjectiveForSlot(1) ?: return
        val entries = player.scoreboard.getScoreboardEntries(objective).sortedByDescending { player.scoreboard.getScore(it, objective) }

        var y = 10
        val x = mc.window.scaledWidth - 150
        for (entry in entries.take(10)) {
            val score = player.scoreboard.getScore(entry, objective)
            context.drawText(mc.textRenderer, entry, x, y, 0xFFFFFF.toInt(), true)
            context.drawText(mc.textRenderer, score.toString(), x + 120, y, 0xFFFFFF.toInt(), true)
            y += mc.textRenderer.fontHeight
        }
    }
}
