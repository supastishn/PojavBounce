/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.gui.screens;

import net.ccbluex.liquidbounce.features.feature.altmanager.AltManagerScreenButton;
import net.ccbluex.liquidbounce.integration.ui.IntegrationMenuScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {

    protected MixinTitleScreen(Component title) {
        super(title);
    }

    /**
     * Add the LiquidBounce button to the title screen after init.
     * Position: Top-right corner, below the Alts button.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Position in top-right corner, below the Alts button
        // Alts button is at: x = width - 60 - 10, y = 10, width = 60, height = 20
        int buttonWidth = 60;
        int buttonHeight = 20;
        int margin = 10;
        int buttonX = this.width - buttonWidth - margin;
        int buttonY = margin + 20 + 4; // Below Alts button with 4px gap

        Button liquidBounceButton = Button.builder(
            Component.literal("LB"),
            button -> liquidbounce$openIntegrationMenu()
        ).bounds(buttonX, buttonY, buttonWidth, buttonHeight).build();

        this.addRenderableWidget(liquidBounceButton);
    }

    /**
     * Opens the Integration Menu screen.
     */
    @Unique
    private void liquidbounce$openIntegrationMenu() {
        Minecraft.getInstance().setScreen(new IntegrationMenuScreen());
    }

    /**
     * Inject rendering of the Alt Manager button at the end of the render method.
     */
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        TitleScreen screen = (TitleScreen)(Object)this;
        AltManagerScreenButton.renderAltManagerButton(guiGraphics, screen, mouseX, mouseY);
    }

    /**
     * Handle mouse clicks on the Alt Manager button.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClick(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            TitleScreen screen = (TitleScreen)(Object)this;
            if (AltManagerScreenButton.INSTANCE.handleButtonClick((int)click.x(), (int)click.y(), screen.width)) {
                cir.setReturnValue(true);
            }
        }
    }
}
