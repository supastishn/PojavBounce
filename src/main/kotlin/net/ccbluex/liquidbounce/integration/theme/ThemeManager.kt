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
package net.ccbluex.liquidbounce.integration.theme

import com.google.gson.JsonArray
import com.google.gson.annotations.SerializedName
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.decode
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.backend.browser.BrowserSettings
import net.ccbluex.liquidbounce.integration.backend.input.InputAcceptor
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.integration.theme.component.ComponentOverlay
import net.ccbluex.liquidbounce.integration.theme.component.ComponentType
import net.ccbluex.liquidbounce.render.shader.CanvasShader
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.extractZip
import net.ccbluex.liquidbounce.utils.io.resource
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.util.Identifier
import java.io.Closeable
import java.io.File

object ThemeManager : Configurable("theme") {

    internal val themesFolder = File(ConfigSystem.rootFolder, "themes")
    internal val defaultTheme = Theme.defaults()

    var shaderEnabled by boolean("Shader", false)
        .onChange { enabled ->
            if (enabled) {
                RenderSystem.recordRenderCall {
                    activeTheme.compileShader()
                    defaultTheme.compileShader()
                }
            }

            return@onChange enabled
        }

    var activeTheme = defaultTheme
        set(value) {
            try {
                if (!value.exists) {
                    logger.warn(
                        "Unable to set theme to ${value.name}, theme does not exist. Using default theme instead."
                    )
                    if (field != defaultTheme) {
                        field.close()
                        field = defaultTheme
                    }
                    return
                }

                if (field != defaultTheme) {
                    field.close()
                }

                field = value

                // Update components
                ComponentOverlay.insertDefaultComponents()

                // Update integration browser
                IntegrationListener.update()
                // ModuleHud.reopen() - method no longer exists in this fork
                // Note: ModuleClickGui.reload() no longer needed with native GUI
            } catch (e: Exception) {
                logger.error("Failed to set active theme to ${value.name}, falling back to default theme", e)
                if (field != defaultTheme) {
                    runCatching { field.close() }
                    field = defaultTheme
                }
                
                // Still try to update components with default theme
                try {
                    ComponentOverlay.insertDefaultComponents()
                    IntegrationListener.update()
                    // ModuleHud.reopen() - method no longer exists in this fork
                } catch (updateException: Exception) {
                    logger.error("Failed to update components after theme fallback", updateException)
                }
            }
        }

    private val takesInputHandler = InputAcceptor { mc.currentScreen != null && mc.currentScreen !is ChatScreen }

    init {
        ConfigSystem.root(this)
    }

    /**
     * Open [Browser] with the given [VirtualScreenType] and mark as static if [markAsStatic] is true.
     * This tab will be locked to 60 FPS since it is not input aware.
     */
    fun openImmediate(
        virtualScreenType: VirtualScreenType? = null,
        markAsStatic: Boolean = false,
        settings: BrowserSettings
    ): Browser =
        BrowserBackendManager.browserBackend.createBrowser(
            route(virtualScreenType, markAsStatic).url,
            settings = settings
        )

    /**
     * Open [Browser] with the given [VirtualScreenType] and mark as static if [markAsStatic] is true.
     * This tab will be locked to the highest refresh rate since it is input aware.
     */
    fun openInputAwareImmediate(
        virtualScreenType: VirtualScreenType? = null,
        markAsStatic: Boolean = false,
        settings: BrowserSettings,
        priority: Short = 10,
        inputAcceptor: InputAcceptor = takesInputHandler
    ): Browser = BrowserBackendManager.browserBackend.createBrowser(
        route(virtualScreenType, markAsStatic).url,
        settings = settings,
        priority = priority,
        inputAcceptor = inputAcceptor
    )

    fun updateImmediate(
        browser: Browser?,
        virtualScreenType: VirtualScreenType? = null,
        markAsStatic: Boolean = false
    ) {
        browser?.url = route(virtualScreenType, markAsStatic).url
    }

    fun route(virtualScreenType: VirtualScreenType? = null, markAsStatic: Boolean = false): Route {
        val theme = try {
            if (virtualScreenType == null || activeTheme.isSupported(virtualScreenType.routeName)) {
                activeTheme
            } else if (defaultTheme.isSupported(virtualScreenType.routeName)) {
                defaultTheme
            } else {
                logger.warn("No theme supports the route ${virtualScreenType.routeName}, using default theme")
                defaultTheme
            }
        } catch (e: Exception) {
            logger.error(
                "Error while determining theme for route ${virtualScreenType?.routeName}, " +
                    "falling back to default theme",
                e
            )
            defaultTheme
        }

        return Route(
            theme,
            theme.getUrl(virtualScreenType?.routeName, markAsStatic)
        )
    }

    fun initializeBackground() {
        runCatching {
            // Load background image of active theme and fallback to default theme if not available
            if (!activeTheme.loadBackgroundImage()) {
                defaultTheme.loadBackgroundImage()
            }
        }.onFailure {
            logger.error("Failed to load background images", it)
        }

        runCatching {
            // Compile shader of active theme and fallback to default theme if not available
            if (shaderEnabled && !activeTheme.compileShader()) {
                defaultTheme.compileShader()
            }
        }.onFailure {
            logger.error("Failed to compile shaders", it)
        }
    }

    fun drawBackground(context: DrawContext, width: Int, height: Int, mousePos: Vec2i, delta: Float): Boolean {
        if (shaderEnabled) {
            val shader = activeTheme.themeBackgroundShader ?: defaultTheme.themeBackgroundShader

            if (shader != null) {
                return shader.draw(context, width, height, mousePos.x, mousePos.y, delta)
            }
        }

        val image = activeTheme.themeBackgroundTexture ?: defaultTheme.themeBackgroundTexture
        if (image != null) {
            return image.draw(context, width, height, mousePos.x, mousePos.y, delta)
        }

        return false
    }

    fun chooseTheme(name: String) {
        try {
            // For now, just set to defaultTheme since proper loading is complex
            // This maintains the native GUI approach by avoiding theme loading
            activeTheme = defaultTheme
        } catch (e: Exception) {
            logger.error("Failed to load theme '$name', falling back to default theme", e)
            if (activeTheme != defaultTheme) {
                activeTheme = defaultTheme
            }
        }
    }

    fun themes() = runCatching {
        themesFolder.listFiles()?.filter { it.isDirectory }?.mapNotNull { it.name } ?: emptyList()
    }.onFailure {
        logger.error("Failed to list available themes", it)
    }.getOrElse { emptyList() }

    data class Route(val theme: Theme, val url: String)

}

