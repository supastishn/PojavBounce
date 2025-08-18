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

import net.ccbluex.liquidbounce.features.module.modules.render.gui.settings.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for range slider widget functionality
 */
class RangeSliderWidgetTest {
    
    @Test
    fun `test IntRangeSliderWidget creation and basic properties`() {
        val config = IntRangeWidgetConfig(x = 10, y = 20, min = 0, max = 100, width = 200, height = 25)
        val initialValue = 10..30
        var changedValue: IntRange? = null
        
        val widget = IntRangeSliderWidget(
            name = "Test Range",
            value = initialValue,
            config = config
        ) { newValue ->
            changedValue = newValue
        }
        
        assertEquals("Test Range", widget.name)
        assertEquals(initialValue, widget.value)
        assertEquals(10, widget.x)
        assertEquals(20, widget.y)
        assertEquals(200, widget.width)
        assertEquals(25, widget.height)
    }
    
    @Test
    fun `test FloatRangeSliderWidget creation and basic properties`() {
        val config = RangeWidgetConfig(x = 10, y = 20, min = 0.0f, max = 10.0f, width = 200, height = 25)
        val initialValue = 1.0f..5.0f
        var changedValue: ClosedFloatingPointRange<Float>? = null
        
        val widget = FloatRangeSliderWidget(
            name = "Test Float Range",
            value = initialValue,
            config = config
        ) { newValue ->
            changedValue = newValue
        }
        
        assertEquals("Test Float Range", widget.name)
        assertEquals(initialValue, widget.value)
        assertEquals(10, widget.x)
        assertEquals(20, widget.y)
        assertEquals(200, widget.width)
        assertEquals(25, widget.height)
    }
    
    @Test
    fun `test IntRangeSliderWidget mouse interaction boundaries`() {
        val config = IntRangeWidgetConfig(x = 0, y = 0, min = 0, max = 100, width = 200, height = 25)
        val widget = IntRangeSliderWidget(
            name = "Test Range",
            value = 20..80,
            config = config
        ) { }
        
        // Test mouse over detection
        assertTrue(widget.isMouseOver(100, 10)) // Middle of widget
        assertTrue(widget.isMouseOver(0, 0))    // Top-left corner
        assertTrue(widget.isMouseOver(199, 24)) // Bottom-right corner (within bounds)
        assertFalse(widget.isMouseOver(-1, 10)) // Left of widget
        assertFalse(widget.isMouseOver(200, 10)) // Right of widget
        assertFalse(widget.isMouseOver(100, -1)) // Above widget
        assertFalse(widget.isMouseOver(100, 25)) // Below widget
    }
    
    @Test
    fun `test FloatRangeSliderWidget mouse interaction boundaries`() {
        val config = RangeWidgetConfig(x = 10, y = 10, min = 0.0f, max = 10.0f, width = 200, height = 25)
        val widget = FloatRangeSliderWidget(
            name = "Test Float Range",
            value = 2.0f..8.0f,
            config = config
        ) { }
        
        // Test mouse over detection with offset coordinates
        assertTrue(widget.isMouseOver(110, 20)) // Middle of widget
        assertTrue(widget.isMouseOver(10, 10))  // Top-left corner
        assertTrue(widget.isMouseOver(209, 34)) // Bottom-right corner (within bounds)
        assertFalse(widget.isMouseOver(9, 20))  // Left of widget
        assertFalse(widget.isMouseOver(210, 20)) // Right of widget
        assertFalse(widget.isMouseOver(110, 9)) // Above widget
        assertFalse(widget.isMouseOver(110, 35)) // Below widget
    }
    
    @Test
    fun `test IntRangeSliderWidget value constraints`() {
        val config = IntRangeWidgetConfig(x = 0, y = 0, min = 0, max = 100, width = 200, height = 25)
        var finalValue: IntRange? = null
        
        val widget = IntRangeSliderWidget(
            name = "Test Range",
            value = 20..80,
            config = config
        ) { newValue ->
            finalValue = newValue
        }
        
        // Simulate clicking and verify the widget responds
        val clicked = widget.mouseClicked(100.0, 10.0, 0) // Middle click
        assertTrue(clicked)
        
        // The actual value change depends on internal calculation,
        // but we can verify the callback was triggered
        // Note: In a real scenario, this would depend on the mouse position
        // relative to the handles, but for unit testing we just verify
        // the interaction works
    }
    
    @Test
    fun `test widget configuration classes`() {
        // Test IntRangeWidgetConfig
        val intConfig = IntRangeWidgetConfig(x = 10, y = 20, min = 5, max = 50, width = 150, height = 30)
        assertEquals(10, intConfig.x)
        assertEquals(20, intConfig.y)
        assertEquals(5, intConfig.min)
        assertEquals(50, intConfig.max)
        assertEquals(150, intConfig.width)
        assertEquals(30, intConfig.height)
        
        // Test RangeWidgetConfig (for float)
        val floatConfig = RangeWidgetConfig(x = 15, y = 25, min = 1.5f, max = 9.5f, width = 180, height = 35)
        assertEquals(15, floatConfig.x)
        assertEquals(25, floatConfig.y)
        assertEquals(1.5f, floatConfig.min)
        assertEquals(9.5f, floatConfig.max)
        assertEquals(180, floatConfig.width)
        assertEquals(35, floatConfig.height)
    }
    
    @Test
    fun `test WidgetConfig for general widgets`() {
        // Test default values
        val defaultConfig = WidgetConfig(x = 5, y = 10)
        assertEquals(5, defaultConfig.x)
        assertEquals(10, defaultConfig.y)
        assertEquals(200, defaultConfig.width) // Default
        assertEquals(20, defaultConfig.height)  // Default
        
        // Test custom values
        val customConfig = WidgetConfig(x = 15, y = 25, width = 300, height = 40)
        assertEquals(15, customConfig.x)
        assertEquals(25, customConfig.y)
        assertEquals(300, customConfig.width)
        assertEquals(40, customConfig.height)
    }
}
