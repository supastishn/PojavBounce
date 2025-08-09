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
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.BooleanSettingWidget
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Unit tests for ClickGuiPanelWidgetFactory to ensure type safety
 */
class ClickGuiPanelWidgetFactoryTest {
    
    private lateinit var mockModule: ClientModule
    private lateinit var originalOut: PrintStream
    private lateinit var testOut: ByteArrayOutputStream
    
    @BeforeEach
    fun setup() {
        // Create a mock module (minimal implementation for testing)
        mockModule = object : ClientModule("TestModule", Category.CLIENT) {}
        
        // Capture stdout to test error messages
        originalOut = System.out
        testOut = ByteArrayOutputStream()
        System.setOut(PrintStream(testOut))
    }
    
    fun tearDown() {
        System.setOut(originalOut)
    }

    @Test
    fun `test createBooleanWidget with valid Boolean value`() {
        tearDown() // Don't capture output for successful cases
        
        // Create a valid Boolean value
        val booleanValue = Value("testBoolean", defaultValue = true, valueType = ValueType.BOOLEAN)
        
        // Test the factory method through reflection to access private method
        val factoryClass = ClickGuiPanelWidgetFactory::class.java
        val method = factoryClass.getDeclaredMethod(
            "createBooleanWidget", 
            Value::class.java, 
            Int::class.java, 
            Int::class.java, 
            Int::class.java, 
            ClientModule::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(ClickGuiPanelWidgetFactory, booleanValue, 0, 0, 100, mockModule)
        
        assertNotNull(result, "Boolean widget should be created successfully")
        assertTrue(result is BooleanSettingWidget, "Result should be BooleanSettingWidget")
    }

    @Test
    fun `test createBooleanWidget with ArrayList value should not crash`() {
        // Create a value that contains ArrayList instead of Boolean (simulates the bug)
        val arrayListValue = object : Value<ArrayList<Value<*>>>(
            "testArrayList", 
            defaultValue = arrayListOf(), 
            valueType = ValueType.BOOLEAN // Incorrectly marked as BOOLEAN
        ) {}
        
        // Test the factory method
        val factoryClass = ClickGuiPanelWidgetFactory::class.java
        val method = factoryClass.getDeclaredMethod(
            "createBooleanWidget", 
            Value::class.java, 
            Int::class.java, 
            Int::class.java, 
            Int::class.java, 
            ClientModule::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(ClickGuiPanelWidgetFactory, arrayListValue, 0, 0, 100, mockModule)
        
        // Should return null instead of crashing
        assertNull(result, "Widget creation should return null for incompatible types")
        
        // Check that warning was printed
        val output = testOut.toString()
        assertTrue(output.contains("Warning: Expected Boolean value but got ArrayList"), 
                   "Should print warning about type mismatch")
    }

    @Test
    fun `test createBooleanWidget with String value should not crash`() {
        // Create a value that contains String instead of Boolean
        val stringValue = object : Value<String>(
            "testString", 
            defaultValue = "not a boolean", 
            valueType = ValueType.BOOLEAN // Incorrectly marked as BOOLEAN
        ) {}
        
        val factoryClass = ClickGuiPanelWidgetFactory::class.java
        val method = factoryClass.getDeclaredMethod(
            "createBooleanWidget", 
            Value::class.java, 
            Int::class.java, 
            Int::class.java, 
            Int::class.java, 
            ClientModule::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(ClickGuiPanelWidgetFactory, stringValue, 0, 0, 100, mockModule)
        
        assertNull(result, "Widget creation should return null for String type")
        
        val output = testOut.toString()
        assertTrue(output.contains("Warning: Expected Boolean value but got String"), 
                   "Should print warning about String type mismatch")
    }

    @Test
    fun `test createWidgetForValue handles null return from createBooleanWidget`() {
        tearDown() // Use normal output
        
        // Create an incompatible value 
        val incompatibleValue = object : Value<Int>(
            "testInt", 
            defaultValue = 42, 
            valueType = ValueType.BOOLEAN // Incorrectly marked as BOOLEAN
        ) {}
        
        // Test through the public interface
        val factoryClass = ClickGuiPanelWidgetFactory::class.java
        val method = factoryClass.getDeclaredMethod(
            "createWidgetForValue", 
            Value::class.java, 
            Int::class.java, 
            Int::class.java, 
            Int::class.java, 
            ClientModule::class.java,
            Map::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(
            ClickGuiPanelWidgetFactory, 
            incompatibleValue, 
            0, 
            0, 
            100, 
            mockModule, 
            emptyMap<String, Boolean>()
        )
        
        // Should handle null gracefully
        assertNull(result, "createWidgetForValue should return null when createBooleanWidget returns null")
    }

    @Test
    fun `test toggleable value type with Boolean works correctly`() {
        tearDown() // Don't capture output for successful cases
        
        val toggleableValue = Value("testToggleable", defaultValue = false, valueType = ValueType.TOGGLEABLE)
        
        val factoryClass = ClickGuiPanelWidgetFactory::class.java
        val method = factoryClass.getDeclaredMethod(
            "createBooleanWidget", 
            Value::class.java, 
            Int::class.java, 
            Int::class.java, 
            Int::class.java, 
            ClientModule::class.java
        )
        method.isAccessible = true
        
        val result = method.invoke(ClickGuiPanelWidgetFactory, toggleableValue, 0, 0, 100, mockModule)
        
        assertNotNull(result, "Toggleable widget should be created successfully")
        assertTrue(result is BooleanSettingWidget, "Result should be BooleanSettingWidget")
    }
}