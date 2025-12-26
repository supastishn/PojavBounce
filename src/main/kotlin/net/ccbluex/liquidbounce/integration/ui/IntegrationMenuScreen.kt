package net.ccbluex.liquidbounce.integration.ui

import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.ThemeManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.text.LiteralText
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.screen.TitleScreen

class IntegrationMenuScreen : Screen(LiteralText("Integration Menu")) {

    private val buttons = mutableListOf<ButtonWidget>()

    override fun init() {
        super.init()
        // Build buttons for available routes
        var y = 40
        for (type in VirtualScreenType.values()) {
            val name = type.routeName.replaceFirstChar { it.uppercase() }
            val route = ThemeManager.getScreenLocation(type, markAsStatic = true)
            if (route.url != "") {
                val button = Button.builder(LiteralText(name), this::onOpen).dimensions(20, y, width - 40, 20).build()
                buttons.add(button)
                this.addButton(button)
                y += 24
            }
        }
    }

    private fun onOpen(button: ButtonWidget) {
        val label = button.message.string
        val type = VirtualScreenType.values().firstOrNull { it.routeName.equals(label, ignoreCase = true) }
            ?: return
        // Open native screen if available, else open the theme browser
        if (type == VirtualScreenType.CLICK_GUI) {
            mc.setScreen(net.ccbluex.liquidbounce.integration.ui.clickgui.NativeClickGuiScreen())
        } else if (type == VirtualScreenType.ALT_MANAGER) {
            mc.setScreen(net.ccbluex.liquidbounce.integration.ui.altmanager.NativeAltManagerScreen(this))
        } else if (type == VirtualScreenType.PROXY_MANAGER) {
            mc.setScreen(net.ccbluex.liquidbounce.integration.ui.proxymanager.NativeProxyManagerScreen(this))
        } else {
            // Fallback: open virtual display screen that would otherwise open the browser
            mc.setScreen(net.ccbluex.liquidbounce.integration.VirtualDisplayScreen(type))
        }
    }

    override fun render(context: GuiGraphics, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context)
        super.render(context, mouseX, mouseY, delta)
    }
}
