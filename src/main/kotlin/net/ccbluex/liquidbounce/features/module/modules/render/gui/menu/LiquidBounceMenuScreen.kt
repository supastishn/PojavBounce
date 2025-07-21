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

import net.ccbluex.liquidbounce.features.module.modules.render.gui.ClickGuiScreen
import net.ccbluex.liquidbounce.features.module.modules.render.gui.hud.HudScreen
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.text.Text

/**
 * Native main menu screen that replaces the Svelte-based implementation
 */
class LiquidBounceMenuScreen : Screen(Text.literal("LiquidBounce")) {
    
    override fun init() {
        super.init()
        
        val buttonWidth = 200
        val buttonHeight = 20
        val spacing = 25
        val startY = height / 2 - 100
        
        // Main menu buttons
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Singleplayer")) { 
                mc.setScreen(SelectWorldScreen(this))
            }.dimensions((width - buttonWidth) / 2, startY, buttonWidth, buttonHeight).build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Multiplayer")) { 
                mc.setScreen(MultiplayerScreen(this))
            }.dimensions((width - buttonWidth) / 2, startY + spacing, buttonWidth, buttonHeight).build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("ClickGUI")) { 
                mc.setScreen(ClickGuiScreen())
            }.dimensions((width - buttonWidth) / 2, startY + spacing * 2, buttonWidth, buttonHeight).build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("HUD Editor")) { 
                mc.setScreen(HudScreen())
            }.dimensions((width - buttonWidth) / 2, startY + spacing * 3, buttonWidth, buttonHeight).build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Alt Manager")) { 
                mc.setScreen(AltManagerScreen(this))
            }.dimensions((width - buttonWidth) / 2, startY + spacing * 4, buttonWidth, buttonHeight).build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Settings")) { 
                mc.setScreen(LiquidBounceSettingsScreen(this))
            }.dimensions((width - buttonWidth) / 2, startY + spacing * 5, buttonWidth, buttonHeight).build()
        )
        
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Quit Game")) { 
                mc.scheduleStop()
            }.dimensions((width - buttonWidth) / 2, startY + spacing * 6, buttonWidth, buttonHeight).build()
        )
    }
    
    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        // Background gradient
        context.fillGradient(0, 0, width, height, 0x80000000.toInt(), 0x80444444.toInt())
        
        // LiquidBounce logo/title
        val title = "LiquidBounce"
        val titleWidth = textRenderer.getWidth(title)
        context.drawText(textRenderer, title, (width - titleWidth) / 2, 50, 0x00AAFF, true)
        
        // Version info
        val version = "Native GUI v1.0"
        val versionWidth = textRenderer.getWidth(version)
        context.drawText(textRenderer, version, (width - versionWidth) / 2, 70, 0xAAAAAA, false)
        
        super.render(context, mouseX, mouseY, delta)
    }
    
    override fun shouldPause(): Boolean {
        return false
    }
}