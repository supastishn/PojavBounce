/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2025 CCBlueX
 *  *
 *  * LiquidBounce is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * LiquidBounce is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.ccbluex.liquidbounce.integration.ui.altmanager.NativeAltManagerScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends MixinScreen {

    @Unique
    private Button altManagerButton;

    @Inject(method = "init", at = @At("TAIL"))
    private void injectAltManagerButton(final CallbackInfo callback) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        // Add Alt Manager button to the right side of the screen
        int x = this.width - 110;
        int y = 10;
        altManagerButton = Button.builder(Component.literal("Alt Manager"), 
                button -> this.minecraft.setScreen(new NativeAltManagerScreen((TitleScreen) (Object) this)))
            .bounds(x, y, 100, 20)
            .build();
        addRenderableWidget(altManagerButton);
    }

}
