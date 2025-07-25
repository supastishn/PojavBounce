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
package net.ccbluex.liquidbounce.render.engine

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.common.GlobalFramebuffer
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud.isBlurEffectActive
import net.ccbluex.liquidbounce.features.module.modules.render.gui.ClickGuiScreen
import net.ccbluex.liquidbounce.features.module.modules.render.gui.ModuleSettingsScreen
import net.ccbluex.liquidbounce.render.shader.BlitShader
import net.ccbluex.liquidbounce.render.shader.UniformProvider
import net.ccbluex.liquidbounce.render.ui.ItemImageAtlas
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.io.resourceToString
import net.minecraft.client.gl.SimpleFramebuffer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.render.RenderPhase
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL20
import kotlin.math.sin

object BlurEffectRenderer : MinecraftShortcuts {

    private object BlurShader : BlitShader(
        resourceToString("/resources/liquidbounce/shaders/sobel.vert"),
        resourceToString("/resources/liquidbounce/shaders/blur/ui_blur.frag"),
        arrayOf(
            UniformProvider("texture0") { pointer ->
                GlStateManager._activeTexture(GL13.GL_TEXTURE0)
                GlStateManager._bindTexture(tmpFramebuffer.colorAttachment)
                GL20.glUniform1i(pointer, 0)
            },
            UniformProvider("overlay") { pointer ->
                val active = GlStateManager._getActiveTexture()
                GlStateManager._activeTexture(GL13.GL_TEXTURE9)
                GlStateManager._bindTexture(overlayFramebuffer.colorAttachment)
                GL20.glUniform1i(pointer, 9)
                GlStateManager._activeTexture(active)
            },
            UniformProvider("radius") { pointer -> GL20.glUniform1f(pointer, getBlurRadius()) }
        ))

    private var isDrawingHudFramebuffer = false

    private val overlayFramebuffer by lazy {
        val fb = SimpleFramebuffer(
            mc.window.framebufferWidth,
            mc.window.framebufferHeight,
            true
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    private val tmpFramebuffer by lazy {
        val fb = SimpleFramebuffer(
            mc.window.framebufferWidth,
            mc.window.framebufferHeight,
            true
        )

        fb.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        fb
    }

    @JvmStatic
    val outlineTarget = RenderPhase.Target("overlay_target", {
        if (isDrawingHudFramebuffer) {
            overlayFramebuffer.beginWrite(true)
        }
    }, {})

    private val lastTimeScreenOpened = Chronometer()
    private var wasScreenOpen = false

    private fun easeFunction(x: Float): Float {
        return sin((x * Math.PI) / 2.0).toFloat()
    }

    private fun getBlurRadiusFactor(): Float {
        val isScreenOpen = mc.currentScreen != null && mc.currentScreen !is ChatScreen
        
        // Check if it's a ClickGUI-related screen to reduce blur interference
        val isClickGuiScreen = mc.currentScreen is ClickGuiScreen || mc.currentScreen is ModuleSettingsScreen

        if (isScreenOpen && !wasScreenOpen) {
            lastTimeScreenOpened.reset()
        }

        wasScreenOpen = isScreenOpen

        return if (isScreenOpen) {
            val normalFactor = easeFunction((lastTimeScreenOpened.elapsed.toFloat() / 500.0F + 0.1F).coerceIn(0.0F..1.0F))
            // Reduce blur intensity for ClickGUI screens to prevent interference
            if (isClickGuiScreen) {
                normalFactor * 0.3f // Reduce blur by 70% for ClickGUI screens
            } else {
                normalFactor
            }
        } else {
            1.0F
        }
    }

    private fun getBlurRadius(): Float {
        return (this.getBlurRadiusFactor() * 20.0F).coerceIn(2.0F..20.0F) // Lowered min radius to help with reduced blur
    }

    fun startOverlayDrawing(context: DrawContext, tickDelta: Float) {
        ItemImageAtlas.updateAtlas(context)

        // Reduce blur intensity during ClickGUI screen transitions to prevent visual artifacts
        val shouldReduceBlur = mc.currentScreen is ClickGuiScreen || mc.currentScreen is ModuleSettingsScreen
        val effectiveBlurActive = isBlurEffectActive && (!shouldReduceBlur || getBlurRadiusFactor() > 0.5f)

        if (effectiveBlurActive) {
            this.isDrawingHudFramebuffer = true

            this.overlayFramebuffer.clear()
            this.overlayFramebuffer.beginWrite(true)
            GlobalFramebuffer.push(overlayFramebuffer)
        }

        callEvent(OverlayRenderEvent(context, tickDelta))
    }

    fun endOverlayDrawing() {
        if (!this.isDrawingHudFramebuffer) {
            return
        }

        this.isDrawingHudFramebuffer = false

        GlobalFramebuffer.pop()
        this.overlayFramebuffer.endWrite()

        // Remember the previous projection matrix because the draw method changes it AND NEVER FUCKING CHANGES IT
        // BACK IN ORDER TO INTRODUCE HARD TO FUCKING FIND BUGS. Thanks Mojang :+1:
        val projectionMatrix = RenderSystem.getProjectionMatrix()
        val vertexSorting = RenderSystem.getProjectionType()

        RenderSystem.disableBlend()
//        RenderSystem.disableDepthTest()
//        RenderSystem.resetTextureMatrix()

        // Draw Minecraft's framebuffer to the temporary one to avoid feedback loop
        this.tmpFramebuffer.clear()
        this.tmpFramebuffer.beginWrite(false)

        mc.framebuffer.drawInternal(mc.window.framebufferWidth, mc.window.framebufferHeight)

        mc.framebuffer.beginWrite(false)

        BlurShader.blit()

        RenderSystem.enableBlend()
        RenderSystem.blendFunc(GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)

        this.overlayFramebuffer.drawInternal(mc.window.framebufferWidth, mc.window.framebufferHeight)

        RenderSystem.setProjectionMatrix(projectionMatrix, vertexSorting)
        RenderSystem.defaultBlendFunc()
    }

    fun setupDimensions(width: Int, height: Int) {
        this.overlayFramebuffer.resize(width, height)
        this.tmpFramebuffer.resize(width, height)
    }

}
