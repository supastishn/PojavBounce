package net.ccbluex.liquidbounce.additions

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Renderable
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.client.gui.components.AbstractWidget

// Compatibility properties for Minecraft 1.21 Screen API
val Screen.client: Minecraft?
    inline get() = this.minecraft

// Compatibility method for addDrawableChild -> addRenderableWidget
fun <T> Screen.addDrawableChild(widget: T): T where T : Renderable, T : GuiEventListener {
    return this.addRenderableWidget(widget)
}

// Note: width and height are already properties on Screen in 1.21
// Note: title is already a property on Screen in 1.21
