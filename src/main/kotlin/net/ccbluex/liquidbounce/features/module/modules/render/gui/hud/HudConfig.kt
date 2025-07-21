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
package net.ccbluex.liquidbounce.features.module.modules.render.gui.hud

import net.ccbluex.liquidbounce.utils.client.mc
import java.io.File
import java.util.Properties

/**
 * Simple configuration manager for HUD element positions
 */
object HudConfig {
    private val configFile = File(mc.runDirectory, "config/liquidbounce_hud.properties")
    private val properties = Properties()
    
    init {
        loadConfig()
    }
    
    private fun loadConfig() {
        try {
            if (configFile.exists()) {
                configFile.inputStream().use { properties.load(it) }
            }
        } catch (e: Exception) {
            println("Error loading HUD config: ${e.message}")
        }
    }
    
    fun saveConfig() {
        try {
            configFile.parentFile?.mkdirs()
            configFile.outputStream().use { 
                properties.store(it, "LiquidBounce HUD Element Positions") 
            }
        } catch (e: Exception) {
            println("Error saving HUD config: ${e.message}")
        }
    }
    
    fun getElementPosition(elementName: String): Pair<Int, Int>? {
        return try {
            val x = properties.getProperty("$elementName.x")?.toIntOrNull()
            val y = properties.getProperty("$elementName.y")?.toIntOrNull()
            if (x != null && y != null) Pair(x, y) else null
        } catch (e: Exception) {
            println("Error getting position for element $elementName: ${e.message}")
            null
        }
    }
    
    fun setElementPosition(elementName: String, x: Int, y: Int) {
        try {
            properties.setProperty("$elementName.x", x.toString())
            properties.setProperty("$elementName.y", y.toString())
        } catch (e: Exception) {
            println("Error setting position for element $elementName: ${e.message}")
        }
    }
    
    fun getElementEnabled(elementName: String): Boolean {
        return try {
            properties.getProperty("$elementName.enabled", "true").toBoolean()
        } catch (e: Exception) {
            println("Error getting enabled state for element $elementName: ${e.message}")
            true
        }
    }
    
    fun setElementEnabled(elementName: String, enabled: Boolean) {
        try {
            properties.setProperty("$elementName.enabled", enabled.toString())
        } catch (e: Exception) {
            println("Error setting enabled state for element $elementName: ${e.message}")
        }
    }
}