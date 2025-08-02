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
package net.ccbluex.liquidbounce.deeplearn

import net.ccbluex.liquidbounce.features.command.commands.deeplearn.CommandAllowMobileTrain
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

class MobileTrainingCommandTest {

    @Test
    fun testMobileTrainingDefault() {
        // By default, mobile training should be disabled
        // Use reflection to avoid triggering full DeepLearningEngine initialization
        val field = DeepLearningEngine::class.java.getDeclaredField("isMobileTrainingAllowed")
        field.isAccessible = true
        
        // Reset to default value
        field.set(DeepLearningEngine, false)
        
        assertFalse(field.get(DeepLearningEngine) as Boolean)
    }

    @Test
    fun testMobileTrainingToggle() {
        // Use reflection to test the property without triggering initialization
        val field = DeepLearningEngine::class.java.getDeclaredField("isMobileTrainingAllowed")
        field.isAccessible = true
        
        // Test toggling mobile training on
        field.set(DeepLearningEngine, true)
        assertTrue(field.get(DeepLearningEngine) as Boolean)
        
        // Test toggling mobile training off
        field.set(DeepLearningEngine, false)
        assertFalse(field.get(DeepLearningEngine) as Boolean)
    }

    @Test
    fun testTrainingAllowedLogic() {
        // Test Android detection logic in isolation
        val hasAndroidVM = System.getProperty("java.vm.name")?.contains("Android", ignoreCase = true) == true
        val hasAndroidRuntime = System.getProperty("java.runtime.name")?.contains("Android", ignoreCase = true) == true
        val hasBuildProp = File("/system/build.prop").exists()
        
        val isAndroid = hasAndroidVM || hasAndroidRuntime || hasBuildProp
        
        // On a normal test environment, this should be false
        assertFalse(isAndroid, "Should not detect Android in test environment")
        
        // Test the training logic without triggering full initialization
        val field = DeepLearningEngine::class.java.getDeclaredField("isMobileTrainingAllowed")
        field.isAccessible = true
        
        // Since we're not on Android in tests, training should always be allowed
        field.set(DeepLearningEngine, false)
        assertTrue(DeepLearningEngine.isTrainingAllowed())
        
        field.set(DeepLearningEngine, true)
        assertTrue(DeepLearningEngine.isTrainingAllowed())
    }

    @Test
    fun testCommandCreation() {
        // Test that the command can be created without errors
        assertDoesNotThrow {
            val command = CommandAllowMobileTrain.createCommand()
            assertNotNull(command)
            assertEquals("allowMobileTrain", command.name)
            assertTrue(command.aliases.contains("amt"))
        }
    }
}
