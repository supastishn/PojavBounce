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
package net.ccbluex.liquidbounce.features.module.modules.render.gui

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.client.mc
import java.io.File

/**
 * Data class representing the state of a ClickGUI panel
 */
data class ClickGuiPanelState(
    val category: String,
    val x: Int,
    val y: Int,
    val expanded: Boolean
)

/**
 * Configuration data for all ClickGUI panels
 */
data class ClickGuiPanelConfig(
    val panels: Map<String, ClickGuiPanelState> = emptyMap()
)

/**
 * Manager for saving and loading ClickGUI panel configurations
 */
object ClickGuiConfigManager {
    private val configFile = File(mc.runDirectory, "config/liquidbounce_clickgui_panels.json")
    private val gson = Gson()
    
    private var currentConfig = ClickGuiPanelConfig()
    
    /**
     * Load panel configurations from file
     */
    fun loadConfig(): ClickGuiPanelConfig {
        return try {
            if (configFile.exists()) {
                val jsonText = configFile.readText()
                currentConfig = gson.fromJson(jsonText, ClickGuiPanelConfig::class.java) ?: ClickGuiPanelConfig()
                currentConfig
            } else {
                // Return default config if file doesn't exist
                ClickGuiPanelConfig()
            }
        } catch (e: JsonSyntaxException) {
            println("Error parsing ClickGUI panel config: ${e.message}")
            ClickGuiPanelConfig()
        } catch (e: Exception) {
            println("Error loading ClickGUI panel config: ${e.message}")
            ClickGuiPanelConfig()
        }
    }
    
    /**
     * Save panel configurations to file
     */
    fun saveConfig(config: ClickGuiPanelConfig) {
        try {
            // Ensure parent directory exists
            configFile.parentFile?.mkdirs()
            
            val jsonText = gson.toJson(config)
            configFile.writeText(jsonText)
            currentConfig = config
        } catch (e: Exception) {
            println("Error saving ClickGUI panel config: ${e.message}")
        }
    }
    
    /**
     * Get the state for a specific panel category
     */
    fun getPanelState(category: Category): ClickGuiPanelState? {
        return currentConfig.panels[category.name]
    }
    
    /**
     * Save the state for a specific panel category
     */
    fun savePanelState(category: Category, x: Int, y: Int, expanded: Boolean) {
        val newState = ClickGuiPanelState(category.name, x, y, expanded)
        val updatedPanels = currentConfig.panels.toMutableMap()
        updatedPanels[category.name] = newState
        
        val newConfig = ClickGuiPanelConfig(updatedPanels)
        saveConfig(newConfig)
    }
    
    /**
     * Get default position for a panel if no saved state exists
     */
    fun getDefaultPosition(categoryIndex: Int): Pair<Int, Int> {
        val spacing = GuiConfig.headerHeight + 10
        return Pair(20, 20 + categoryIndex * spacing)
    }
    
    /**
     * Initialize the config manager - should be called once at startup
     */
    fun initialize() {
        currentConfig = loadConfig()
    }
}
