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
package net.ccbluex.liquidbounce.integration.ui.altmanager

import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.components.Button
import net.minecraft.util.math.ColorHelper

/**
 * Native Alt Manager screen - replaces browser-based alt manager
 */
class NativeAltManagerScreen(private val parent: Screen?) : Screen("Alt Manager".asPlainText()) {

    override fun init() {
        super.init()

        // Add account button
        addDrawableChild(
            ButtonWidget.builder("Add Account".asPlainText()) { button ->
                // TODO: Open add account dialog
            }.dimensions(width / 2 - 100, height - 30, 200, 20).build()
        )

        // Close button
        addDrawableChild(
            ButtonWidget.builder("Close".asPlainText()) { button ->
                close()
            }.dimensions(width / 2 - 100, height - 55, 200, 20).build()
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        // Title
        context.drawCenteredTextWithShadow(
            textRenderer,
            title,
            width / 2,
            20,
            0xFFFFFF
        )

        // Placeholder text
        context.drawCenteredTextWithShadow(
            textRenderer,
            "Alt Manager - Native GUI Implementation".asPlainText(),
            width / 2,
            height / 2 - 10,
            ColorHelper.getArgb(255, 150, 150, 150)
        )

        super.render(context, mouseX, mouseY, delta)
    }

    override fun close() {
        client?.setScreen(parent)
    }

    override fun shouldPause() = true
}
