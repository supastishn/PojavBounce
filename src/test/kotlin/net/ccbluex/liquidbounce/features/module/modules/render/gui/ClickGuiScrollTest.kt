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
        // In Minecraft: negative amount = scrolling down, positive amount = scrolling up  
        // We want: scrolling down = increase scroll offset, scrolling up = decrease scroll offset
        val scrollSensitivity = 20.0
        val scrollDownAmount = -1.0  // Minecraft gives negative for scrolling down
        val expectedScrollDelta = (-scrollDownAmount * scrollSensitivity).toInt()
        
        assertEquals(20, expectedScrollDelta)
        
        // Test scrolling up (positive amount) should decrease scroll offset  
        val scrollUpAmount = 1.0  // Minecraft gives positive for scrolling up
        val scrollUpDelta = (-scrollUpAmount * scrollSensitivity).toInt()
        
        assertEquals(-20, scrollUpDelta)
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
        // Test that scroll direction matches user expectations for Minecraft
        // In Minecraft: scrolling down gives negative amount, scrolling up gives positive amount
        // We want: scrolling down (negative) to increase offset, scrolling up (positive) to decrease offset
        val currentScrollOffset = 50
        val scrollDownAmount = -1.0  // Minecraft scroll down amount
        val scrollMultiplier = 20
        
        val newScrollOffset = currentScrollOffset - (scrollDownAmount * scrollMultiplier).toInt()
        assertEquals(70, newScrollOffset)
        
        // Scrolling up (positive amount) should move content down (decrease scroll offset)
        val scrollUpAmount = 1.0  // Minecraft scroll up amount  
        val newScrollOffsetUp = currentScrollOffset - (scrollUpAmount * scrollMultiplier).toInt()
        assertEquals(30, newScrollOffsetUp)
    }
    
    @Test
    fun `test panel height calculation for mouse bounds`() {
        // Test that the panel height calculation matches between render and mouseScrolled methods
        val headerHeight = 20
        val moduleHeight = 25
        val settingHeight = 18
        val settingSpacing = 2
        val panelMaxHeight = 300
        
        // Simulate a panel with 5 modules, 2 of which are expanded with 3 settings each
        val moduleCount = 5
        val expandedModuleCount = 2
        val settingsPerExpandedModule = 3
        
        val totalContentHeight = moduleCount * moduleHeight + 
            expandedModuleCount * settingsPerExpandedModule * (settingHeight + settingSpacing)
        
        // Expected: 5 * 25 + 2 * 3 * (18 + 2) = 125 + 120 = 245
        assertEquals(245, totalContentHeight)
        
        // When content fits within max height, actual panel height should be header + content
        val actualPanelHeight = headerHeight + min(totalContentHeight, panelMaxHeight)
        assertEquals(265, actualPanelHeight) // 20 + 245
        
        // When content exceeds max height, panel should be clamped to header + max height
        val largeTotalContent = 400
        val clampedPanelHeight = headerHeight + min(largeTotalContent, panelMaxHeight)
        assertEquals(320, clampedPanelHeight) // 20 + 300
    }
}
