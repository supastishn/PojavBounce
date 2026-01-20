package net.ccbluex.liquidbounce.additions

import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

// Compatibility for Button API changes in Minecraft 1.21
// In 1.21, Button.getMessage() returns the button text
val Button.message: Component
    inline get() = this.getMessage()
