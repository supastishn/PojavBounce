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
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

/**
 * Test for dynamic module settings discovery
 */
class DynamicModuleSettingsTest {
    
    @Test
    fun testModuleHasConfigurableValues() {
        val module = ModuleAutoClicker
        val values = module.containedValues
        
        // The module should have configurable values
        assertTrue(values.isNotEmpty(), "Module should have configurable values")
        
        println("ModuleAutoClicker has ${values.size} configurable values:")
        values.forEach { value ->
            println("  - ${value.name} (${value.valueType}): ${value.get()}")
        }
    }
    
    @Test
    fun testModuleSettingsIncludeDifferentTypes() {
        val module = ModuleAutoClicker
        val values = module.containedValues
        
        // Check that we have different value types
        val valueTypes = values.map { it.valueType }.toSet()
        
        println("Value types found: $valueTypes")
        
        // We should have boolean values
        assertTrue(valueTypes.contains(ValueType.BOOLEAN), "Should have boolean values")
    }
    
    @Test 
    fun testValueCanBeModified() {
        val module = ModuleAutoClicker
        val values = module.containedValues
        
        // Find a boolean value to test with
        val booleanValue = values.firstOrNull { it.valueType == ValueType.BOOLEAN }
        assertNotNull(booleanValue, "Should have at least one boolean value")
        
        // Test value modification
        val originalValue = booleanValue!!.get() as Boolean
        val newValue = !originalValue
        
        @Suppress("UNCHECKED_CAST")
        val typedValue = booleanValue as net.ccbluex.liquidbounce.config.types.Value<Boolean>
        typedValue.set(newValue)
        
        assertTrue(typedValue.get() == newValue, "Value should be updated")
        
        // Restore original value
        typedValue.set(originalValue)
        assertTrue(typedValue.get() == originalValue, "Value should be restored")
        
        println("Successfully tested value modification for ${booleanValue.name}")
    }
}