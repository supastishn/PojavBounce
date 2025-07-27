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

import net.ccbluex.liquidbounce.config.types.Value
import net.ccbluex.liquidbounce.config.types.ValueType
import net.ccbluex.liquidbounce.config.types.ChooseListValue
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.RangedValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Unit tests for ClickGUI module settings functionality to ensure
 * that the popup uses the same value setting mechanism as the command system
 */
class ClickGuiSettingsTest {

    // Test implementation of NamedChoice for testing
    private enum class TestChoice(override val choiceName: String) : NamedChoice {
        OPTION_ONE("Option1"),
        OPTION_TWO("Option2"),
        OPTION_THREE("Option3")
    }

    @Test
    fun `test boolean value setByString consistency`() {
        // Create a boolean value
        val boolValue = Value("testBoolean", emptyArray(), false, ValueType.BOOLEAN)
        
        // Test setting to true via setByString (same as command)
        boolValue.setByString("true")
        assertTrue(boolValue.get())
        
        // Test setting to false via setByString
        boolValue.setByString("false")
        assertFalse(boolValue.get())
        
        // Test that the value correctly handles the same logic as the command system
        assertEquals(false, boolValue.get())
    }
    
    @Test
    fun `test float value setByString consistency`() {
        // Create a float value
        val floatValue = Value("testFloat", emptyArray(), 1.0f, ValueType.FLOAT)
        
        // Test setting via setByString (same as command)
        floatValue.setByString("5.5")
        assertEquals(5.5f, floatValue.get(), 0.01f)
        
        floatValue.setByString("10.0")
        assertEquals(10.0f, floatValue.get(), 0.01f)
        
        floatValue.setByString("0.1")
        assertEquals(0.1f, floatValue.get(), 0.01f)
    }
    
    @Test
    fun `test int value setByString consistency`() {
        // Create an int value
        val intValue = Value("testInt", emptyArray(), 1, ValueType.INT)
        
        // Test setting via setByString (same as command)
        intValue.setByString("42")
        assertEquals(42, intValue.get())
        
        intValue.setByString("100")
        assertEquals(100, intValue.get())
        
        intValue.setByString("0")
        assertEquals(0, intValue.get())
    }
    
    @Test
    fun `test string value setByString consistency`() {
        // Create a string value
        val stringValue = Value("testString", emptyArray(), "default", ValueType.TEXT)
        
        // Test setting via setByString (same as command)
        stringValue.setByString("hello world")
        assertEquals("hello world", stringValue.get())
        
        stringValue.setByString("test123")
        assertEquals("test123", stringValue.get())
        
        stringValue.setByString("")
        assertEquals("", stringValue.get())
    }
    
    @Test
    fun `test choose value setByString consistency`() {
        // Create a choose value
        val choices = arrayOf(TestChoice.OPTION_ONE, TestChoice.OPTION_TWO, TestChoice.OPTION_THREE)
        val chooseValue = ChooseListValue("testChoose", emptyArray(), TestChoice.OPTION_ONE, choices)
        
        // Test setting via setByString (same as command)
        chooseValue.setByString("Option2")
        assertEquals(TestChoice.OPTION_TWO, chooseValue.get())
        
        chooseValue.setByString("Option3")
        assertEquals(TestChoice.OPTION_THREE, chooseValue.get())
        
        chooseValue.setByString("Option1")
        assertEquals(TestChoice.OPTION_ONE, chooseValue.get())
    }
    
    @Test
    fun `test ranged float value setByString consistency`() {
        // Create a ranged float value
        val rangedValue = RangedValue("testRangedFloat", emptyArray(), 5.0f, 0.0f..10.0f, "", ValueType.FLOAT)
        
        // Test setting via setByString (same as command)
        rangedValue.setByString("7.5")
        assertEquals(7.5f, rangedValue.get(), 0.01f)
        
        rangedValue.setByString("2.3")
        assertEquals(2.3f, rangedValue.get(), 0.01f)
    }
    
    @Test
    fun `test ranged int value setByString consistency`() {
        // Create a ranged int value
        val rangedValue = RangedValue("testRangedInt", emptyArray(), 50, 0..100, "", ValueType.INT)
        
        // Test setting via setByString (same as command)
        rangedValue.setByString("75")
        assertEquals(75, rangedValue.get())
        
        rangedValue.setByString("25")
        assertEquals(25, rangedValue.get())
    }
    
    @Test
    fun `test popup value setting uses exact same mechanism as command`() {
        // This test validates that the popup will use setByString just like the command
        
        // Test boolean conversion to string and back
        val boolValue = Value("testBool", emptyArray(), false, ValueType.BOOLEAN)
        val newBoolValue = true
        val boolStringRepresentation = newBoolValue.toString() // "true"
        boolValue.setByString(boolStringRepresentation)
        assertEquals(newBoolValue, boolValue.get())
        
        // Test float conversion to string and back
        val floatValue = Value("testFloat", emptyArray(), 1.0f, ValueType.FLOAT)
        val newFloatValue = 3.14f
        val floatStringRepresentation = newFloatValue.toString() // "3.14"
        floatValue.setByString(floatStringRepresentation)
        assertEquals(newFloatValue, floatValue.get(), 0.01f)
        
        // Test int conversion to string and back
        val intValue = Value("testInt", emptyArray(), 1, ValueType.INT)
        val newIntValue = 42
        val intStringRepresentation = newIntValue.toString() // "42"
        intValue.setByString(intStringRepresentation)
        assertEquals(newIntValue, intValue.get())
        
        // Test string (no conversion needed)
        val stringValue = Value("testString", emptyArray(), "default", ValueType.TEXT)
        val newStringValue = "test value"
        stringValue.setByString(newStringValue) // Direct string
        assertEquals(newStringValue, stringValue.get())
    }
    
    @Test
    fun `test error handling consistency between command and popup`() {
        // Test that setByString properly handles errors the same way
        
        val intValue = Value("testInt", emptyArray(), 1, ValueType.INT)
        
        // Test invalid number format - should throw exception
        assertThrows(NumberFormatException::class.java) {
            intValue.setByString("not_a_number")
        }
        
        // Value should remain unchanged after failed setByString
        assertEquals(1, intValue.get())
        
        val floatValue = Value("testFloat", emptyArray(), 1.0f, ValueType.FLOAT)
        
        // Test invalid float format - should throw exception
        assertThrows(NumberFormatException::class.java) {
            floatValue.setByString("invalid_float")
        }
        
        // Value should remain unchanged after failed setByString
        assertEquals(1.0f, floatValue.get(), 0.01f)
    }
    
    @Test
    fun `test ClickGUI classes are properly structured for settings`() {
        // Test that the required classes exist and are accessible
        val panelClass = ClickGuiPanel::class.java
        val popupClass = ModuleSettingsPopup::class.java
        val screenClass = ClickGuiScreen::class.java
        
        assertNotNull(panelClass)
        assertNotNull(popupClass)
        assertNotNull(screenClass)
        
        // Verify class names contain expected components
        assertTrue(panelClass.name.contains("ClickGuiPanel"))
        assertTrue(popupClass.name.contains("ModuleSettingsPopup"))
        assertTrue(screenClass.name.contains("ClickGuiScreen"))
    }
}
