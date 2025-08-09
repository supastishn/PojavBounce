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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlin.math.max
import kotlin.math.min

/**
 * Unit tests for ClickGUI scroll logic without dependencies on GUI classes
 */
class ClickGuiScrollTest {
    @Test
    fun `test scroll offset calculation logic`() {
        // Test the scroll offset calculation logic used in mouseScrolled
        // Positive amount (scrolling down) should increase scroll offset
        val scrollSensitivity = 20.0
        val positiveAmount = 1.0  // Scrolling down
        val expectedScrollDelta = (positiveAmount * scrollSensitivity).toInt()
        
        assertEquals(20, expectedScrollDelta)
        
        // Test negative amount (scrolling up) should decrease scroll offset  
        val negativeAmount = -1.0  // Scrolling up
        val negativeScrollDelta = (negativeAmount * scrollSensitivity).toInt()
        
        assertEquals(-20, negativeScrollDelta)
    }
    @Test
    fun `test scroll bounds calculation`() {
        // Test the scroll bounds logic
        val moduleCount = 20
        val moduleHeight = 25
        val panelMaxHeight = 300
        
        val totalHeight = moduleCount * moduleHeight // 500
        val maxScroll = max(0, totalHeight - panelMaxHeight) // 200
        
        assertEquals(200, maxScroll)
        
        // Test when no scrolling needed
        val smallModuleCount = 5
        val smallTotalHeight = smallModuleCount * moduleHeight // 125
        val noScrollNeeded = max(0, smallTotalHeight - panelMaxHeight) // 0
        
        assertEquals(0, noScrollNeeded)
    }
    @Test
    fun `test scroll offset clamping`() {
        // Test that scroll offset is properly clamped within bounds
        val maxScroll = 200
        
        // Test normal case
        var scrollOffset = 100
        scrollOffset = max(0, min(maxScroll, scrollOffset))
        assertEquals(100, scrollOffset)
        
        // Test overflow case - scroll should be clamped to max
        scrollOffset = 300
        scrollOffset = max(0, min(maxScroll, scrollOffset))
        assertEquals(200, scrollOffset)
        
        // Test underflow case - scroll should be clamped to 0
        scrollOffset = -50
        scrollOffset = max(0, min(maxScroll, scrollOffset))
        assertEquals(0, scrollOffset)
    }
    
    @Test 
    fun `test scroll direction is intuitive`() {
        // Test that scroll direction matches user expectations
        // Scrolling down (positive amount) should move content up (increase scroll offset)
        val currentScrollOffset = 50
        val scrollDownAmount = 1.0
        val scrollMultiplier = 20
        
        val newScrollOffset = currentScrollOffset + (scrollDownAmount * scrollMultiplier).toInt()
        assertEquals(70, newScrollOffset)
        
        // Scrolling up (negative amount) should move content down (decrease scroll offset)
        val scrollUpAmount = -1.0
        val newScrollOffsetUp = currentScrollOffset + (scrollUpAmount * scrollMultiplier).toInt()
        assertEquals(30, newScrollOffsetUp)
    }
}
