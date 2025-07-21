/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.render.gui.menu

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

/**
 * Alt Manager screen for managing alternative accounts
 */
class AltManagerScreen(private val parent: Screen) : Screen(Text.literal("Alt Manager")) {
    
    override fun init() {
        super.init()
        
        // Back button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) { 
                mc.setScreen(parent)
            }.dimensions(10, height - 30, 60, 20).build()
        )
        
        // Add Account button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Account")) { 
                // TODO: Open add account dialog
            }.dimensions(width / 2 - 100, height / 2, 200, 20).build()
        )
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, 0x80000000.toInt(), 0x80444444.toInt())
        
        // Title
        val title = "Alt Manager"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, (width - titleWidth) / 2, 20, 0xFFFFFF, true)
        
        // Placeholder text
        val placeholder = "Alt Manager functionality will be implemented here"
        val placeholderWidth = textRenderer.getWidth(placeholder)
        context.drawText(textRenderer, placeholder, (width - placeholderWidth) / 2, height / 2 - 40, 0xAAAAAA, false)
        
        super.render(context, mouseX, mouseY, delta)
    }
}

/**
 * LiquidBounce settings screen
 */
class LiquidBounceSettingsScreen(private val parent: Screen) : Screen(Text.literal("LiquidBounce Settings")) {
    
    override fun init() {
        super.init()
        
        // Back button
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Back")) { 
                mc.setScreen(parent)
            }.dimensions(10, height - 30, 60, 20).build()
        )
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        context.fillGradient(0, 0, width, height, 0x80000000.toInt(), 0x80444444.toInt())
        
        // Title
        val title = "LiquidBounce Settings"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, (width - titleWidth) / 2, 20, 0xFFFFFF, true)
        
        // Settings categories
        val categories = listOf(
            "General Settings",
            "GUI Settings", 
            "Performance Settings",
            "Theme Settings"
        )
        
        var yOffset = 60
        for (category in categories) {
            context.drawText(textRenderer, "• $category", 50, yOffset, 0xCCCCCC, false)
            yOffset += 25
        }
        
        // Placeholder text
        val placeholder = "Settings implementation coming soon"
        val placeholderWidth = textRenderer.getWidth(placeholder)
        context.drawText(textRenderer, placeholder, (width - placeholderWidth) / 2, height - 100, 0xAAAAAA, false)
        
        super.render(context, mouseX, mouseY, delta)
    }
}