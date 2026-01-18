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

import net.ccbluex.liquidbounce.authlib.account.CrackedAccount
import net.ccbluex.liquidbounce.authlib.account.MicrosoftAccount
import net.ccbluex.liquidbounce.authlib.account.MinecraftAccount
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.asPlainText
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.util.ARGB

/**
 * Native Alt Manager screen with Inter font support
 * Features:
 * - List of saved accounts
 * - Add cracked account
 * - Login to account
 * - Remove account
 * - Shows current account
 */
class NativeAltManagerScreen(private val parent: Screen?) : Screen("Alt Manager".asPlainText()) {

    private val fontRenderer: FontRenderer get() = FontManager.FONT_RENDERER
    private val fontScale = 0.22f

    private var scrollOffset = 0
    private var selectedIndex = -1
    private var usernameField: EditBox? = null
    private var statusMessage = ""
    private var statusColor = COLOR_WHITE

    companion object {
        private const val ACCOUNT_ROW_HEIGHT = 24
        private const val LIST_TOP = 50
        private const val LIST_PADDING = 20

        private val COLOR_WHITE = Color4b(255, 255, 255, 255)
        private val COLOR_GREEN = Color4b(0, 255, 0, 255)
        private val COLOR_RED = Color4b(255, 102, 102, 255)
        private val COLOR_CYAN = Color4b(0, 255, 255, 255)
        private val COLOR_GRAY = Color4b(170, 170, 170, 255)
        private val COLOR_YELLOW = Color4b(255, 204, 102, 255)
    }

    override fun init() {
        super.init()

        val buttonWidth = 90
        val buttonHeight = 20
        val buttonY = height - 30
        val spacing = 5

        // Username field for adding cracked accounts
        usernameField = EditBox(font, width / 2 - 100, height - 60, 200, 20, "Username".asPlainText())
        usernameField!!.setMaxLength(16)
        usernameField!!.setHint("Enter username...".asPlainText())
        addWidget(usernameField!!)

        // Calculate button positions
        val totalWidth = buttonWidth * 5 + spacing * 4
        var xPos = width / 2 - totalWidth / 2

        // Add cracked account button
        addRenderableWidget(
            Button.builder("Add Cracked".asPlainText()) {
                val username = usernameField?.value ?: ""
                if (username.isNotEmpty()) {
                    AccountManager.newCrackedAccount(username)
                    usernameField?.value = ""
                    statusMessage = "Added account: $username"
                    statusColor = COLOR_GREEN
                } else {
                    statusMessage = "Enter a username first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Random account button
        addRenderableWidget(
            Button.builder("Random".asPlainText()) {
                AccountManager.loginRandomCrackedAccount()
                statusMessage = "Generating random account..."
                statusColor = COLOR_YELLOW
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Login button
        addRenderableWidget(
            Button.builder("Login".asPlainText()) {
                if (selectedIndex >= 0 && selectedIndex < AccountManager.accounts.size) {
                    AccountManager.loginAccount(selectedIndex)
                    statusMessage = "Logging in..."
                    statusColor = COLOR_YELLOW
                } else {
                    statusMessage = "Select an account first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Remove button
        addRenderableWidget(
            Button.builder("Remove".asPlainText()) {
                if (selectedIndex >= 0 && selectedIndex < AccountManager.accounts.size) {
                    val account = AccountManager.accounts[selectedIndex]
                    AccountManager.accounts.remove(account)
                    statusMessage = "Removed account"
                    statusColor = COLOR_GREEN
                    selectedIndex = -1
                } else {
                    statusMessage = "Select an account first!"
                    statusColor = COLOR_RED
                }
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
        xPos += buttonWidth + spacing

        // Close button
        addRenderableWidget(
            Button.builder("Close".asPlainText()) {
                onClose()
            }.bounds(xPos, buttonY, buttonWidth, buttonHeight).build()
        )
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        // Background
        context.fill(0, 0, width, height, ARGB.color(200, 20, 20, 30))

        // Title
        drawText(context, "Alt Manager", width / 2f - getTextWidth("Alt Manager") / 2, 10f, COLOR_CYAN)

        // Current session info
        val currentUser = "Current: ${mc.user.name}"
        drawText(context, currentUser, width / 2f - getTextWidth(currentUser) / 2, 25f, COLOR_GRAY)

        // Account list
        renderAccountList(context, mouseX, mouseY)

        // Status message
        if (statusMessage.isNotEmpty()) {
            drawText(context, statusMessage, width / 2f - getTextWidth(statusMessage) / 2, height - 85f, statusColor)
        }

        // Render username field
        usernameField?.render(context, mouseX, mouseY, delta)

        super.render(context, mouseX, mouseY, delta)
    }

    private fun renderAccountList(context: GuiGraphics, mouseX: Int, mouseY: Int) {
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 100
        val listBottom = LIST_TOP + listHeight

        // List background
        context.fill(listX, LIST_TOP, listX + listWidth, listBottom, ARGB.color(150, 30, 30, 40))

        // Border
        context.fill(listX, LIST_TOP, listX + listWidth, LIST_TOP + 1, ARGB.color(255, 60, 60, 80))
        context.fill(listX, listBottom - 1, listX + listWidth, listBottom, ARGB.color(255, 60, 60, 80))
        context.fill(listX, LIST_TOP, listX + 1, listBottom, ARGB.color(255, 60, 60, 80))
        context.fill(listX + listWidth - 1, LIST_TOP, listX + listWidth, listBottom, ARGB.color(255, 60, 60, 80))

        // Enable scissor for clipping
        context.enableScissor(listX + 1, LIST_TOP + 1, listX + listWidth - 1, listBottom - 1)

        val accounts = AccountManager.accounts
        var yPos = LIST_TOP + 2 - scrollOffset

        for ((index, account) in accounts.withIndex()) {
            if (yPos + ACCOUNT_ROW_HEIGHT > LIST_TOP && yPos < listBottom) {
                renderAccountRow(context, account, index, listX + 2, yPos, listWidth - 4, mouseX, mouseY)
            }
            yPos += ACCOUNT_ROW_HEIGHT
        }

        if (accounts.isEmpty()) {
            val noAccounts = "No accounts added"
            drawText(context, noAccounts, listX + listWidth / 2f - getTextWidth(noAccounts) / 2, LIST_TOP + listHeight / 2f - 5, COLOR_GRAY)
        }

        context.disableScissor()

        // Scroll bar
        if (accounts.size * ACCOUNT_ROW_HEIGHT > listHeight) {
            val maxScroll = accounts.size * ACCOUNT_ROW_HEIGHT - listHeight
            val scrollBarHeight = (listHeight.toFloat() / (accounts.size * ACCOUNT_ROW_HEIGHT) * listHeight).toInt().coerceAtLeast(20)
            val scrollBarY = LIST_TOP + (scrollOffset.toFloat() / maxScroll * (listHeight - scrollBarHeight)).toInt()
            context.fill(listX + listWidth - 5, scrollBarY, listX + listWidth - 2, scrollBarY + scrollBarHeight, ARGB.color(180, 100, 100, 120))
        }
    }

    private fun renderAccountRow(context: GuiGraphics, account: MinecraftAccount, index: Int, x: Int, y: Int, w: Int, mouseX: Int, mouseY: Int) {
        val isHovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + ACCOUNT_ROW_HEIGHT
        val isSelected = index == selectedIndex

        // Row background
        val bgColor = when {
            isSelected -> ARGB.color(200, 70, 70, 120)
            isHovered -> ARGB.color(150, 50, 50, 80)
            else -> ARGB.color(100, 40, 40, 60)
        }
        context.fill(x, y, x + w, y + ACCOUNT_ROW_HEIGHT - 2, bgColor)

        // Account info
        val username = account.profile?.username ?: "Unknown"
        val accountType = when (account) {
            is CrackedAccount -> "Cracked"
            is MicrosoftAccount -> "Microsoft"
            else -> "Other"
        }

        val nameColor = if (isSelected) COLOR_WHITE else if (isHovered) COLOR_WHITE else COLOR_GRAY
        drawText(context, username, x + 8f, y + 3f, nameColor)

        val typeColor = when (account) {
            is MicrosoftAccount -> COLOR_GREEN
            is CrackedAccount -> COLOR_YELLOW
            else -> COLOR_GRAY
        }
        drawText(context, "[$accountType]", x + w - getTextWidth("[$accountType]") - 8, y + 3f, typeColor)
    }

    private fun drawText(context: GuiGraphics, text: String, x: Float, y: Float, color: Color4b, shadow: Boolean = true) {
        val processedText = fontRenderer.process(text.asText(), color)
        with(context) {
            fontRenderer.draw(processedText) {
                this.x = x
                this.y = y
                this.scale = fontScale
                this.shadow = shadow
            }
        }
    }

    private fun getTextWidth(text: String): Float {
        val processedText = fontRenderer.process(text.asText(), COLOR_WHITE)
        return fontRenderer.getStringWidth(processedText, shadow = true) * fontScale
    }

    override fun mouseClicked(click: net.minecraft.client.input.MouseButtonEvent, doubled: Boolean): Boolean {
        val mouseX = click.x.toInt()
        val mouseY = click.y.toInt()
        val listX = LIST_PADDING
        val listWidth = width - LIST_PADDING * 2
        val listHeight = height - LIST_TOP - 100
        val listBottom = LIST_TOP + listHeight

        // Check if clicked in account list
        if (mouseX >= listX && mouseX < listX + listWidth && mouseY >= LIST_TOP && mouseY < listBottom) {
            val accounts = AccountManager.accounts
            var yPos = LIST_TOP + 2 - scrollOffset

            for ((index, _) in accounts.withIndex()) {
                if (mouseY >= yPos && mouseY < yPos + ACCOUNT_ROW_HEIGHT) {
                    selectedIndex = index

                    // Double click to login
                    if (doubled) {
                        AccountManager.loginAccount(index)
                        statusMessage = "Logging in..."
                        statusColor = COLOR_YELLOW
                    }
                    return true
                }
                yPos += ACCOUNT_ROW_HEIGHT
            }
        }

        return super.mouseClicked(click, doubled)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean {
        val listHeight = height - LIST_TOP - 100
        val totalHeight = AccountManager.accounts.size * ACCOUNT_ROW_HEIGHT

        if (totalHeight > listHeight) {
            val maxScroll = totalHeight - listHeight
            scrollOffset = (scrollOffset - (vertical * 20).toInt()).coerceIn(0, maxScroll)
            return true
        }

        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    }

    override fun onClose() {
        mc.setScreen(parent)
    }

    override fun isPauseScreen() = true
}
