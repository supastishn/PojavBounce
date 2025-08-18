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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

/**
 * Tests for accordion state management functionality
 */
class AccordionStateManagerTest {
    
    // Mock module for testing
    private val testModule = object : ClientModule("TestModule", Category.WORLD) {
        init {
            // Add some nested configurables for testing
        }
    }
    
    @BeforeEach
    fun setUp() {
        // Clean up any existing test files
        cleanupTestFiles()
        
        // Initialize the accordion state manager
        AccordionStateManager.initialize()
    }
    
    @Test
    fun `test default section expanded state`() {
        // Test that sections are expanded by default
        assertTrue(AccordionStateManager.isSectionExpanded(testModule, "TestSection", true))
        
        // Test that default value is respected
        assertFalse(AccordionStateManager.isSectionExpanded(testModule, "TestSection", false))
    }
    
    @Test
    fun `test setting and getting section state`() {
        val sectionName = "TestSection"
        
        // Initially should be default (true)
        assertTrue(AccordionStateManager.isSectionExpanded(testModule, sectionName, true))
        
        // Set to collapsed
        AccordionStateManager.setSectionExpanded(testModule, sectionName, false)
        assertFalse(AccordionStateManager.isSectionExpanded(testModule, sectionName, true))
        
        // Set back to expanded
        AccordionStateManager.setSectionExpanded(testModule, sectionName, true)
        assertTrue(AccordionStateManager.isSectionExpanded(testModule, sectionName, false))
    }
    
    @Test
    fun `test module state persistence`() {
        val sectionName = "PersistentSection"
        
        // Set a specific state
        AccordionStateManager.setSectionExpanded(testModule, sectionName, false)
        
        // Get the module state and verify it contains our section
        val moduleState = AccordionStateManager.getModuleState(testModule)
        assertEquals(testModule.name, moduleState.moduleName)
        assertEquals(false, moduleState.expandedSections[sectionName])
    }
    
    @Test
    fun `test different modules have separate states`() {
        val module1 = object : ClientModule("Module1", Category.WORLD) {}
        val module2 = object : ClientModule("Module2", Category.COMBAT) {}
        val sectionName = "SharedSectionName"
        
        // Set different states for the same section name in different modules
        AccordionStateManager.setSectionExpanded(module1, sectionName, true)
        AccordionStateManager.setSectionExpanded(module2, sectionName, false)
        
        // Verify they maintain separate states
        assertTrue(AccordionStateManager.isSectionExpanded(module1, sectionName, false))
        assertFalse(AccordionStateManager.isSectionExpanded(module2, sectionName, true))
    }
    
    @Test
    fun `test state persistence across manager reinitialization`() {
        val sectionName = "ReinitTestSection"
        
        // Set initial state
        AccordionStateManager.setSectionExpanded(testModule, sectionName, false)
        assertFalse(AccordionStateManager.isSectionExpanded(testModule, sectionName, true))
        
        // Reinitialize (simulating restart)
        AccordionStateManager.initialize()
        
        // State should persist
        assertFalse(AccordionStateManager.isSectionExpanded(testModule, sectionName, true))
    }
    
    @Test
    fun `test multiple sections per module`() {
        val section1 = "Section1"
        val section2 = "Section2"
        val section3 = "Section3"
        
        // Set different states for multiple sections
        AccordionStateManager.setSectionExpanded(testModule, section1, true)
        AccordionStateManager.setSectionExpanded(testModule, section2, false)
        AccordionStateManager.setSectionExpanded(testModule, section3, true)
        
        // Verify all states are maintained correctly
        assertTrue(AccordionStateManager.isSectionExpanded(testModule, section1, false))
        assertFalse(AccordionStateManager.isSectionExpanded(testModule, section2, true))
        assertTrue(AccordionStateManager.isSectionExpanded(testModule, section3, false))
        
        // Check the module state contains all sections
        val moduleState = AccordionStateManager.getModuleState(testModule)
        assertEquals(3, moduleState.expandedSections.size)
        assertEquals(true, moduleState.expandedSections[section1])
        assertEquals(false, moduleState.expandedSections[section2])
        assertEquals(true, moduleState.expandedSections[section3])
    }
    
    private fun cleanupTestFiles() {
        try {
            // Try to clean up any test files, but don't fail if they don't exist
            val configDir = File("config")
            if (configDir.exists()) {
                val accordionFile = File(configDir, "liquidbounce_accordion_states.json")
                if (accordionFile.exists()) {
                    accordionFile.delete()
                }
            }
        } catch (e: Exception) {
            // Explicitly ignore cleanup errors in test teardown
            // This is acceptable since it's test cleanup and shouldn't affect test results
            println("Test cleanup warning: ${e.message}")
        }
    }
}
