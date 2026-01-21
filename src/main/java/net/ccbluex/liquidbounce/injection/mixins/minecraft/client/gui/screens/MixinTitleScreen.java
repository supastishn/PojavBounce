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
     * Dynamically positions below all existing buttons to avoid overlap.
     */
    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        // Find the lowest button Y position among existing widgets
        int lowestY = 0;
        for (var widget : this.children()) {
            if (widget instanceof Button button) {
                int buttonBottom = button.getY() + button.getHeight();
                if (buttonBottom > lowestY) {
                    lowestY = buttonBottom;
                }
            }
        }

        // Add LiquidBounce button below all existing buttons with 4px gap
        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2 - buttonWidth / 2;
        int buttonY = lowestY + 4;

        Button liquidBounceButton = Button.builder(
            Component.literal("LiquidBounce"),
            button -> liquidbounce$openIntegrationMenu()
        ).bounds(centerX, buttonY, buttonWidth, buttonHeight).build();

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
