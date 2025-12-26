package net.ccbluex.liquidbounce.additions

import net.minecraft.client.gui.GuiGraphics

// Compatibility properties for Minecraft 1.21 GuiGraphics
val GuiGraphics.scaledWidth: Int
    inline get() = this.guiWidth()

val GuiGraphics.scaledHeight: Int
    inline get() = this.guiHeight()
