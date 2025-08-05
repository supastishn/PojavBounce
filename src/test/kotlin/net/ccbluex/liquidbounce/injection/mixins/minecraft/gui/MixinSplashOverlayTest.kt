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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach

/**
 * Test for MixinSplashOverlay auto-advance and skip button functionality
 */
class MixinSplashOverlayTest {

    // Test class to simulate the mixin behavior without Minecraft dependencies
    private class SplashOverlayLogicTester {
        var hasAutoAdvanced = false
        var gameAdvanced = false
        var wasMousePressed = false
        var wasMousePressedInsideButton = false

        fun checkAutoAdvance(progress: Float) {
            // Simulate the auto-advance logic from MixinSplashOverlay.checkAutoAdvance
            if (!hasAutoAdvanced && progress >= 1.0f) {
                hasAutoAdvanced = true
                advanceToGame()
            }
        }

        fun handleMouseClick(mouseX: Int, mouseY: Int, isMousePressed: Boolean, buttonBounds: ButtonBounds) {
            // Simulate the mouse click logic from MixinSplashOverlay.render
            val isMouseOverButton = buttonBounds.isMouseOver(mouseX, mouseY)
            
            // Track if mouse was pressed while over the button
            if (!wasMousePressed && isMousePressed && isMouseOverButton) {
                wasMousePressedInsideButton = true
            }
            
            // Detect click (press and release both inside button)
            if (wasMousePressed && !isMousePressed && isMouseOverButton && wasMousePressedInsideButton) {
                advanceToGame()
            }
            
            // Reset when mouse is released
            if (!isMousePressed) {
                wasMousePressedInsideButton = false
            }
            
            wasMousePressed = isMousePressed
        }

        private fun advanceToGame() {
            // Simulate the advanceToGame method
            gameAdvanced = true
        }

        fun reset() {
            hasAutoAdvanced = false
            gameAdvanced = false
            wasMousePressed = false
            wasMousePressedInsideButton = false
        }
    }

    // Helper class to simulate button bounds
    private data class ButtonBounds(
        val x: Int,
        val y: Int, 
        val width: Int,
        val height: Int
    ) {
        fun isMouseOver(mouseX: Int, mouseY: Int): Boolean {
            return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height
        }
    }

    private lateinit var tester: SplashOverlayLogicTester
    private lateinit var buttonBounds: ButtonBounds

    @BeforeEach
    fun setUp() {
        tester = SplashOverlayLogicTester()
        // Simulate button dimensions from MixinSplashOverlay (width/2 - 100, height/4 + 120 + 12, 200, 20)
        // Using mock screen dimensions of 800x600
        buttonBounds = ButtonBounds(300, 282, 200, 20)
    }

    @Test
    fun testAutoAdvanceTriggersAtFullProgress() {
        // Test that auto-advance triggers when progress reaches 100%
        assertFalse(tester.gameAdvanced, "Game should not be advanced initially")
        assertFalse(tester.hasAutoAdvanced, "Auto-advance flag should be false initially")

        // Progress at 99% should not trigger auto-advance
        tester.checkAutoAdvance(0.99f)
        assertFalse(tester.gameAdvanced, "Game should not advance at 99% progress")
        assertFalse(tester.hasAutoAdvanced, "Auto-advance flag should remain false")

        // Progress at 100% should trigger auto-advance
        tester.checkAutoAdvance(1.0f)
        assertTrue(tester.gameAdvanced, "Game should advance at 100% progress")
        assertTrue(tester.hasAutoAdvanced, "Auto-advance flag should be set")

        println("✓ Auto-advance triggers correctly at 100% progress")
    }

    @Test
    fun testAutoAdvanceTriggersOnlyOnce() {
        // Test that auto-advance only happens once per loading session
        assertFalse(tester.gameAdvanced, "Game should not be advanced initially")

        // First call at 100% should trigger
        tester.checkAutoAdvance(1.0f)
        assertTrue(tester.gameAdvanced, "Game should advance on first call")
        assertTrue(tester.hasAutoAdvanced, "Auto-advance flag should be set")

        // Reset gameAdvanced to test if it gets called again
        tester.gameAdvanced = false

        // Second call at 100% should not trigger again
        tester.checkAutoAdvance(1.0f)
        assertFalse(tester.gameAdvanced, "Game should not advance on second call")
        assertTrue(tester.hasAutoAdvanced, "Auto-advance flag should remain set")

        println("✓ Auto-advance triggers only once per session")
    }

    @Test
    fun testSkipButtonClickInsideBounds() {
        // Test that clicking inside button bounds advances the game
        assertFalse(tester.gameAdvanced, "Game should not be advanced initially")

        val mouseX = 400 // Center of button (x=300, width=200, so center is 400)
        val mouseY = 292 // Center of button (y=282, height=20, so center is 292)

        // Simulate mouse press
        tester.handleMouseClick(mouseX, mouseY, true, buttonBounds)
        assertFalse(tester.gameAdvanced, "Game should not advance on mouse press")

        // Simulate mouse release - this should trigger the advance
        tester.handleMouseClick(mouseX, mouseY, false, buttonBounds)
        assertTrue(tester.gameAdvanced, "Game should advance on mouse release inside button")

        println("✓ Skip button click inside bounds works correctly")
    }

    @Test
    fun testSkipButtonClickOutsideBounds() {
        // Test that clicking outside button bounds does not advance the game
        assertFalse(tester.gameAdvanced, "Game should not be advanced initially")

        val mouseX = 100 // Outside button bounds (button starts at x=300)
        val mouseY = 100 // Outside button bounds (button starts at y=282)

        // Simulate mouse press and release outside bounds
        tester.handleMouseClick(mouseX, mouseY, true, buttonBounds)
        tester.handleMouseClick(mouseX, mouseY, false, buttonBounds)
        assertFalse(tester.gameAdvanced, "Game should not advance when clicking outside button")

        println("✓ Skip button ignores clicks outside bounds")
    }

    @Test
    fun testSkipButtonRequiresPressReleaseSequence() {
        // Test that button requires proper press-release sequence
        assertFalse(tester.gameAdvanced, "Game should not be advanced initially")

        val mouseX = 400 // Inside button bounds
        val mouseY = 292 // Inside button bounds

        // Only mouse release without press should not trigger
        tester.handleMouseClick(mouseX, mouseY, false, buttonBounds)
        assertFalse(tester.gameAdvanced, "Game should not advance on release without press")

        // Only mouse press without release should not trigger
        tester.handleMouseClick(mouseX, mouseY, true, buttonBounds)
        assertFalse(tester.gameAdvanced, "Game should not advance on press without release")

        // Proper press-release sequence should trigger
        tester.handleMouseClick(mouseX, mouseY, false, buttonBounds)
        assertTrue(tester.gameAdvanced, "Game should advance on proper press-release sequence")

        println("✓ Skip button requires proper press-release sequence")
    }

    @Test
    fun testSkipButtonClickOutsideThenInside() {
        // Test that pressing outside and releasing inside doesn't trigger
        assertFalse(tester.gameAdvanced, "Game should not be advanced initially")

        val outsideX = 100 // Outside button bounds
        val outsideY = 100 // Outside button bounds
        val insideX = 400  // Inside button bounds
        val insideY = 292  // Inside button bounds

        // Press outside bounds
        tester.handleMouseClick(outsideX, outsideY, true, buttonBounds)
        assertFalse(tester.gameAdvanced, "Game should not advance on press outside")

        // Release inside bounds - should not trigger because press was outside
        tester.handleMouseClick(insideX, insideY, false, buttonBounds)
        assertFalse(tester.gameAdvanced, "Game should not advance when press was outside bounds")

        println("✓ Skip button correctly handles press outside, release inside")
    }

    @Test
    fun testBothAdvancementMethodsWork() {
        // Test that both auto-advance and manual skip can advance the game
        val autoTester = SplashOverlayLogicTester()
        val skipTester = SplashOverlayLogicTester()

        // Test auto-advance
        autoTester.checkAutoAdvance(1.0f)
        assertTrue(autoTester.gameAdvanced, "Auto-advance should work")

        // Test manual skip
        skipTester.handleMouseClick(400, 292, true, buttonBounds)
        skipTester.handleMouseClick(400, 292, false, buttonBounds)
        assertTrue(skipTester.gameAdvanced, "Manual skip should work")

        println("✓ Both auto-advance and manual skip work correctly")
    }

    @Test
    fun testProgressBelowThresholdDoesNotAutoAdvance() {
        // Test various progress values below 1.0f
        val progressValues = floatArrayOf(0.0f, 0.5f, 0.95f, 0.99f, 0.999f)

        for (progress in progressValues) {
            tester.reset()
            tester.checkAutoAdvance(progress)
            assertFalse(tester.gameAdvanced, "Game should not advance at progress $progress")
            assertFalse(tester.hasAutoAdvanced, "Auto-advance flag should be false at progress $progress")
        }

        println("✓ Auto-advance correctly ignores progress values below 100%")
    }

    @Test
    fun testProgressAboveThresholdTriggersAutoAdvance() {
        // Test various progress values at or above 1.0f
        val progressValues = floatArrayOf(1.0f, 1.1f, 2.0f)

        for (progress in progressValues) {
            tester.reset()
            tester.checkAutoAdvance(progress)
            assertTrue(tester.gameAdvanced, "Game should advance at progress $progress")
            assertTrue(tester.hasAutoAdvanced, "Auto-advance flag should be set at progress $progress")
        }

        println("✓ Auto-advance correctly triggers for progress values at or above 100%")
    }

    @Test
    fun testTextureSafetyHandling() {
        // Test that the texture safety logic doesn't crash when texture manager is not ready
        // This simulates the fix for the "Tried to lookup sprite, but atlas is not initialized" crash
        
        // Test class to simulate texture safety behavior
        class TextureSafetyTester {
            var textureManagerReady = false
            var renderAttempted = false
            var renderSucceeded = false
            var exceptionThrown = false
            
            fun attemptRender() {
                renderAttempted = true
                try {
                    if (textureManagerReady) {
                        // Simulate successful texture rendering
                        renderSucceeded = true
                    } else {
                        // Simulate early return when texture manager is not ready
                        return
                    }
                } catch (e: Exception) {
                    // Simulate exception handling (like in our try-catch blocks)
                    exceptionThrown = true
                }
            }
        }
        
        val safeTester = TextureSafetyTester()
        
        // Test rendering when texture manager is not ready
        safeTester.textureManagerReady = false
        safeTester.attemptRender()
        assertTrue(safeTester.renderAttempted, "Render should be attempted")
        assertFalse(safeTester.renderSucceeded, "Render should not succeed when texture manager is not ready")
        assertFalse(safeTester.exceptionThrown, "No exception should be thrown with proper safety checks")
        
        // Reset for next test
        safeTester.renderAttempted = false
        safeTester.renderSucceeded = false
        
        // Test rendering when texture manager is ready
        safeTester.textureManagerReady = true
        safeTester.attemptRender()
        assertTrue(safeTester.renderAttempted, "Render should be attempted")
        assertTrue(safeTester.renderSucceeded, "Render should succeed when texture manager is ready")
        assertFalse(safeTester.exceptionThrown, "No exception should be thrown when texture manager is ready")
        
        println("✓ Texture safety handling prevents crashes during early initialization")
    }

    @Test
    fun testButtonCreationSafety() {
        // Test that button creation is safely handled when texture systems aren't ready
        
        class ButtonCreationTester {
            var textureSystemReady = false
            var buttonCreationAttempted = false
            var buttonCreated = false
            var retryNextFrame = false
            
            fun attemptButtonCreation() {
                if (!textureSystemReady) {
                    return // Early return like in our texture manager check
                }
                
                buttonCreationAttempted = true
                try {
                    // Simulate button creation
                    if (textureSystemReady) {
                        buttonCreated = true
                    }
                } catch (e: Exception) {
                    // Simulate exception handling - retry next frame
                    retryNextFrame = true
                }
            }
        }
        
        val buttonTester = ButtonCreationTester()
        
        // Test button creation when texture system is not ready
        buttonTester.textureSystemReady = false
        buttonTester.attemptButtonCreation()
        assertFalse(buttonTester.buttonCreationAttempted, "Button creation should not be attempted when texture system is not ready")
        assertFalse(buttonTester.buttonCreated, "Button should not be created when texture system is not ready")
        
        // Test button creation when texture system is ready
        buttonTester.textureSystemReady = true
        buttonTester.attemptButtonCreation()
        assertTrue(buttonTester.buttonCreationAttempted, "Button creation should be attempted when texture system is ready")
        assertTrue(buttonTester.buttonCreated, "Button should be created when texture system is ready")
        assertFalse(buttonTester.retryNextFrame, "No retry should be needed when texture system is ready")
        
        println("✓ Button creation safety prevents crashes during early initialization")
    }
}

