package net.ccbluex.liquidbounce.additions

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen

// Compatibility properties for Minecraft 1.21 Screen API
val Screen.client: Minecraft?
    inline get() = this.minecraft

// Note: width and height are already properties on Screen in 1.21
// Note: title is already a property on Screen in 1.21
