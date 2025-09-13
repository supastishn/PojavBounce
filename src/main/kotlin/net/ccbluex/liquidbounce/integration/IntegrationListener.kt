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
 *
 */
package net.ccbluex.liquidbounce.integration

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
<<<<<<< HEAD
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
=======
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings
import net.ccbluex.liquidbounce.integration.backend.browser.IntegrationBrowserSettings
>>>>>>> upstream/nextgen
import net.ccbluex.liquidbounce.integration.task.TaskProgressScreen
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import org.lwjgl.glfw.GLFW
<<<<<<< HEAD
=======
import kotlin.math.min
>>>>>>> upstream/nextgen

object IntegrationListener : EventListener {

    /**
     * This tab is always open and initialized. We keep this tab open to make it possible to draw on the screen,
     * even when no specific tab is open.
     * It also reduces the time required to open a new tab and allows for smooth transitions between tabs.
     *
     * The client tab will be initialized when the browser is ready.
     */
    lateinit var browser: Browser
        private set
<<<<<<< HEAD
    lateinit var browserSettings: BrowserSettings
=======
    lateinit var browserSettings: IntegrationBrowserSettings
>>>>>>> upstream/nextgen
        private set

    var momentaryVirtualScreen: VirtualScreen? = null
        private set

<<<<<<< HEAD
    var runningTheme = ThemeManager.activeTheme
=======
    var theme: Theme? = null
>>>>>>> upstream/nextgen
        private set

    /**
     * Acknowledgement is used to detect desyncs between the integration browser and the client.
     * It is reset when the client opens a new screen and confirmed when the integration browser
     * opens the same screen.
     *
     * If the acknowledgement is not confirmed after 500ms, the integration browser will be reloaded.
     */
    val acknowledgement = Acknowledgement()

    private val standardCursor = GLFW.glfwCreateStandardCursor(GLFW.GLFW_ARROW_CURSOR)

    data class VirtualScreen(val type: VirtualScreenType, val openSince: Chronometer = Chronometer())

    class Acknowledgement(
        val since: Chronometer = Chronometer(),
        var confirmed: Boolean = false
    ) {

        @Suppress("unused")
        val isDesynced
            get() = !confirmed && since.hasElapsed(1000)

        fun confirm() {
            confirmed = true
        }

        fun reset() {
            since.reset()
            confirmed = false
        }

    }

    internal val parent: Screen
        get() = mc.currentScreen ?: TitleScreen()

    private var browserIsReady = false

    @Suppress("unused")
    val handleBrowserReady = handler<BrowserReadyEvent>(priority = FIRST_PRIORITY) {
        logger.info("Browser is ready.")

        // Fires up the client tab
<<<<<<< HEAD
        browserSettings = BrowserSettings(0, ::restart)
=======
        browserSettings = IntegrationBrowserSettings(0, ::restart)
>>>>>>> upstream/nextgen
        browser = ThemeManager.openInputAwareImmediate(settings = browserSettings)
        browserIsReady = true
    }

    @Suppress("unused")
    fun virtualOpen(name: String) {
        val type = VirtualScreenType.byName(name) ?: return
        virtualOpen(type = type)
    }

<<<<<<< HEAD
    fun virtualOpen(theme: Theme = ThemeManager.activeTheme, type: VirtualScreenType) {
=======
    fun virtualOpen(theme: Theme = ThemeManager.theme, type: VirtualScreenType) {
>>>>>>> upstream/nextgen
        // Check if the virtual screen is already open
        if (momentaryVirtualScreen?.type == type) {
            return
        }

<<<<<<< HEAD
        if (runningTheme != theme) {
            runningTheme = theme
=======
        if (this.theme != theme) {
            this.theme = theme
>>>>>>> upstream/nextgen
            ThemeManager.updateImmediate(browser, type)
        }

        val virtualScreen = VirtualScreen(type).apply { momentaryVirtualScreen = this }
        acknowledgement.reset()
        EventManager.callEvent(
            VirtualScreenEvent(
<<<<<<< HEAD
                virtualScreen.type.routeName,
                VirtualScreenEvent.Action.OPEN
=======
                virtualScreen.type,
                action = VirtualScreenEvent.Action.OPEN
>>>>>>> upstream/nextgen
            )
        )
    }

    fun virtualClose() {
        val virtualScreen = momentaryVirtualScreen ?: return

        momentaryVirtualScreen = null
        acknowledgement.reset()
        EventManager.callEvent(
            VirtualScreenEvent(
<<<<<<< HEAD
                virtualScreen.type.routeName,
                VirtualScreenEvent.Action.CLOSE
=======
                virtualScreen.type,
                action = VirtualScreenEvent.Action.CLOSE
>>>>>>> upstream/nextgen
            )
        )
    }

    fun restart() {
        if (!browserIsReady || !BrowserBackendManager.browserBackend.isInitialized) {
            return
        }

        try {
            browser.close()
            browser = ThemeManager.openInputAwareImmediate(settings = browserSettings)
        } catch (e: Exception) {
            logger.error("Failed to restart browser backend for screen integration.", e)
        }

        try {
<<<<<<< HEAD
            // ModuleClickGui.reload(true) - no longer needed with native GUI
            logger.info("ClickGUI browser integration restarted -> native GUI (no-op)")
=======
            ModuleClickGui.reload(true)
>>>>>>> upstream/nextgen
        } catch (e: Exception) {
            logger.error("Failed to restart ClickGUI browser integration.", e)
        }

        try {
<<<<<<< HEAD
            // ModuleHud.reopen() - method no longer exists in this fork
=======
            ModuleHud.reopen()
>>>>>>> upstream/nextgen
        } catch (e: Exception) {
            logger.error("Failed to restart HUD browser integration.", e)
        }
    }

    fun update() {
        if (!browserIsReady || !BrowserBackendManager.browserBackend.isInitialized) {
            return
        }

        logger.info(
            "Reloading integration browser ${browser.javaClass.simpleName} " +
<<<<<<< HEAD
                "to ${ThemeManager.route()}"
=======
                "to ${ThemeManager.getScreenLocation()}"
>>>>>>> upstream/nextgen
        )
        ThemeManager.updateImmediate(browser, momentaryVirtualScreen?.type)
    }

    fun restoreOriginalScreen() {
        if (mc.currentScreen is VirtualDisplayScreen) {
            mc.setScreen((mc.currentScreen as VirtualDisplayScreen).originalScreen)
        }
    }

    /**
     * Handle opening new screens
     */
    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> { event ->
        // Set to default GLFW cursor
        GLFW.glfwSetCursor(mc.window.handle, standardCursor)

        if (handleCurrentScreen(event.screen)) {
            event.cancelEvent()
        }
    }

    @Suppress("unused")
    private val screenRefresher = handler<GameTickEvent> {
        if (browserIsReady && mc.currentScreen !is TaskProgressScreen) {
            handleCurrentScreen(mc.currentScreen)
        }
    }

    /**
     * Refresh integration browser when we change worlds, this can also mean we disconnect from a server
     * and go back to the main menu.
     */
    @Suppress("unused")
    private val worldChangeEvent = handler<WorldChangeEvent> {
        update()
    }

    @Suppress("unused")
<<<<<<< HEAD
=======
    private val fpsLimitHandler = handler<FpsLimitEvent> { event ->
        if (!browserIsReady || !browserSettings.syncGameFps || !isClientScreen(mc.currentScreen)) {
            return@handler
        }

        event.fps = min(event.fps, browserSettings.currentFps)
    }

    @Suppress("unused")
>>>>>>> upstream/nextgen
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val keyCode = event.keyCode
        val modifier = event.mods

        if (inGame) {
            return@handler
        }

        // F12 to toggle GPU acceleration
        if (event.action == GLFW.GLFW_PRESS && keyCode == GLFW.GLFW_KEY_F12) {
            if (!BrowserBackendManager.browserBackend.isAccelerationSupported) {
                logger.warn("GPU acceleration is not supported by the current browser backend.")
                return@handler
            }

            val accelerated = GlobalBrowserSettings.accelerated ?: return@handler
            accelerated.set(!accelerated.get())
            logger.info("GPU acceleration is now ${if (accelerated.get()) "enabled" else "disabled"}.")
        }
    }

    private fun handleCurrentScreen(screen: Screen?): Boolean {
        return when {
            screen !is VirtualDisplayScreen && HideAppearance.isHidingNow -> {
                virtualClose()

                false
            }
            !browserIsReady || screen is VirtualDisplayScreen -> false
            else -> {
                // Are we currently playing the game?
                if (mc.world != null && screen == null) {
                    virtualClose()

                    return false
                }

                handleCurrentMinecraftScreen(screen ?: TitleScreen())
            }
        }
    }

    /**
     * @return should cancel the minecraft screen
     */
    private fun handleCurrentMinecraftScreen(virtScreen: Screen): Boolean {
        val virtualScreenType = VirtualScreenType.recognize(virtScreen)

        if (virtualScreenType == null) {
            virtualClose()

            return false
        }

        val name = virtualScreenType.routeName
        val route = runCatching {
<<<<<<< HEAD
            ThemeManager.route(virtualScreenType, false)
=======
            ThemeManager.getScreenLocation(virtualScreenType, false)
>>>>>>> upstream/nextgen
        }.getOrNull()

        if (route == null) {
            virtualClose()
            return false
        }

        val theme = route.theme

        return when {
<<<<<<< HEAD
            false -> { // Stubbed - doesSupport functionality not needed for native GUI
=======
            theme.isScreenSupported(name) -> {
>>>>>>> upstream/nextgen
                mc.setScreen(VirtualDisplayScreen(virtualScreenType, theme, originalScreen = virtScreen))

                true
            }
<<<<<<< HEAD
            false -> { // Stubbed - doesOverlay functionality not needed for native GUI
=======
            theme.isOverlaySupported(name) -> {
>>>>>>> upstream/nextgen
                virtualOpen(theme, virtualScreenType)

                false
            }
            else -> {
                virtualClose()

                false
            }
        }
    }

<<<<<<< HEAD
=======
    /**
     * Checks if the given screen is an active client screen.
     */
    @JvmStatic
    fun isClientScreen(screen: Screen?) = screen is VirtualDisplayScreen || screen is ModuleClickGui.ClickScreen ||
        screen is BrowserScreen

>>>>>>> upstream/nextgen
}
