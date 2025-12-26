package net.ccbluex.liquidbounce.integration.theme.component

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.integration.theme.component.components.*
import net.ccbluex.liquidbounce.utils.render.Alignment

// Register native components with the theme component registry

fun registerNativeComponents() {
    // Watermark
    NativeComponentRegistry.register("Watermark") { name, enabled, alignment, tweaks, values ->
        WatermarkComponent(name, enabled, alignment, tweaks)
    }

    // ArrayList
    NativeComponentRegistry.register("ArrayList") { name, enabled, alignment, tweaks, values ->
        ArrayListComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Text") { name, enabled, alignment, tweaks, values ->
        // values contain JSON configuration; pass them into TextComponent
        TextComponent(name, enabled, alignment, tweaks, values)
    }

    NativeComponentRegistry.register("Keystrokes") { name, enabled, alignment, tweaks, values ->
        KeystrokesComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Notifications") { name, enabled, alignment, tweaks, values ->
        NotificationsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("TargetHUD") { name, enabled, alignment, tweaks, values ->
        TargetHudComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Image") { name, enabled, alignment, tweaks, values ->
        ImageComponent(name, enabled, alignment, tweaks, values)
    }

    NativeComponentRegistry.register("Scoreboard") { name, enabled, alignment, tweaks, values ->
        ScoreboardComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("ArmorItems") { name, enabled, alignment, tweaks, values ->
        ArmorItemsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Effects") { name, enabled, alignment, tweaks, values ->
        EffectsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Keybinds") { name, enabled, alignment, tweaks, values ->
        KeybindsComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Inventory") { name, enabled, alignment, tweaks, values ->
        InventoryComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("BlockCounter") { name, enabled, alignment, tweaks, values ->
        BlockCounterComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Hotbar") { name, enabled, alignment, tweaks, values ->
        HotbarComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("CraftingInventory") { name, enabled, alignment, tweaks, values ->
        CraftingInventoryComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("TabGUI") { name, enabled, alignment, tweaks, values ->
        TabGuiComponent(name, enabled, alignment, tweaks)
    }

    NativeComponentRegistry.register("Taco") { name, enabled, alignment, tweaks, values ->
        TacoComponent(name, enabled, alignment, tweaks)
    }

}

// Ensure registration happens on load - use private val to execute at module load time
private val _initialization = registerNativeComponents()
