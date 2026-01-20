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

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.integration.theme.component.components.ArrayListComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.ArmorItemsComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.BlockCounterComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.CraftingInventoryComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.EffectsComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.HotbarComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.ImageComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.InventoryComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.KeybindsComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.KeystrokesComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.NotificationsComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.ScoreboardComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.TabGuiComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.TacoComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.TargetHudComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.TextComponent
import net.ccbluex.liquidbounce.integration.theme.component.components.WatermarkComponent

// Register native components with the theme component registry

fun registerNativeComponents() {
    // Watermark
    NativeComponentRegistry.register("Watermark") { name, enabled, alignment, tweaks, _ ->
        WatermarkComponent(name, enabled, alignment, tweaks)
    }

    // ArrayList
    NativeComponentRegistry.register("ArrayList") { name, enabled, alignment, tweaks, _ ->
        ArrayListComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Text") { name, enabled, alignment, tweaks, values ->
        // values contain JSON configuration; pass them into TextComponent
        TextComponent(name, enabled, alignment, tweaks, values)
    }

    NativeComponentRegistry.register("Keystrokes") { name, enabled, alignment, tweaks, _ ->
        KeystrokesComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Notifications") { name, enabled, alignment, tweaks, _ ->
        NotificationsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("TargetHUD") { name, enabled, alignment, tweaks, _ ->
        TargetHudComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Image") { name, enabled, alignment, tweaks, values ->
        ImageComponent(name, enabled, alignment, tweaks, values)
    }

    NativeComponentRegistry.register("Scoreboard") { name, enabled, alignment, tweaks, _ ->
        ScoreboardComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("ArmorItems") { name, enabled, alignment, tweaks, _ ->
        ArmorItemsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Effects") { name, enabled, alignment, tweaks, _ ->
        EffectsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Keybinds") { name, enabled, alignment, tweaks, _ ->
        KeybindsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Inventory") { name, enabled, alignment, tweaks, _ ->
        InventoryComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("BlockCounter") { name, enabled, alignment, tweaks, _ ->
        BlockCounterComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Hotbar") { name, enabled, alignment, tweaks, _ ->
        HotbarComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("CraftingInventory") { name, enabled, alignment, tweaks, _ ->
        CraftingInventoryComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("TabGUI") { name, enabled, alignment, tweaks, _ ->
        TabGuiComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Taco") { name, enabled, alignment, tweaks, _ ->
        TacoComponent(name, enabled, alignment, tweaks)
    }

}
