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
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.mc
import java.io.File

/**
 * Data class representing accordion state for a module
 */
data class AccordionState(
    val moduleName: String,
    val expandedSections: Map<String, Boolean> = emptyMap()
)

/**
 * Configuration data for all accordion states
 */
data class AccordionConfig(
    val moduleStates: Map<String, AccordionState> = emptyMap()
)

/**
 * Manager for saving and loading accordion expand/collapse states per module
 */
object AccordionStateManager {
    private val configFile = File(mc.runDirectory, "config/liquidbounce_accordion_states.json")
    private val gson = Gson()
    
    private var currentConfig = AccordionConfig()
    
    /**
     * Load accordion configurations from file
     */
    fun loadConfig(): AccordionConfig {
        return try {
            if (configFile.exists()) {
                val jsonText = configFile.readText()
                currentConfig = gson.fromJson(jsonText, AccordionConfig::class.java) ?: AccordionConfig()
                currentConfig
            } else {
                // Return default config if file doesn't exist
                AccordionConfig()
            }
        } catch (e: Exception) {
            println("Error loading accordion config: ${e.message}")
            AccordionConfig()
        }
    }
    
    /**
     * Save accordion configurations to file
     */
    fun saveConfig(config: AccordionConfig) {
        try {
            // Ensure parent directory exists
            configFile.parentFile?.mkdirs()
            
            val jsonText = gson.toJson(config)
            configFile.writeText(jsonText)
            currentConfig = config
        } catch (e: Exception) {
            println("Error saving accordion config: ${e.message}")
        }
    }
    
    /**
     * Get the accordion state for a specific module
     */
    fun getModuleState(module: ClientModule): AccordionState {
        return currentConfig.moduleStates[module.name] ?: AccordionState(module.name)
    }
    
    /**
     * Save the accordion state for a specific module
     */
    fun saveModuleState(module: ClientModule, expandedSections: Map<String, Boolean>) {
        val newState = AccordionState(module.name, expandedSections)
        val updatedStates = currentConfig.moduleStates.toMutableMap()
        updatedStates[module.name] = newState
        
        val newConfig = AccordionConfig(updatedStates)
        saveConfig(newConfig)
    }
    
    /**
     * Get the expanded state for a specific section in a module
     */
    fun isSectionExpanded(module: ClientModule, sectionName: String, defaultExpanded: Boolean = true): Boolean {
        val moduleState = getModuleState(module)
        return moduleState.expandedSections[sectionName] ?: defaultExpanded
    }
    
    /**
     * Set the expanded state for a specific section in a module
     */
    fun setSectionExpanded(module: ClientModule, sectionName: String, expanded: Boolean) {
        val moduleState = getModuleState(module)
        val updatedSections = moduleState.expandedSections.toMutableMap()
        updatedSections[sectionName] = expanded
        
        saveModuleState(module, updatedSections)
    }
    
    /**
     * Initialize the accordion state manager
     */
    fun initialize() {
        loadConfig()
    }
}
