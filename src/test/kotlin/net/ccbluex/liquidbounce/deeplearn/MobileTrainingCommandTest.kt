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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MobileTrainingCommandTest {

    @BeforeEach
    fun setup() {
        // Reset mobile training setting before each test
        DeepLearningEngine.isMobileTrainingAllowed = false
    }

    @Test
    fun testMobileTrainingDefault() {
        // By default, mobile training should be disabled
        assertFalse(DeepLearningEngine.isMobileTrainingAllowed)
    }

    @Test
    fun testMobileTrainingToggle() {
        // Test toggling mobile training on
        DeepLearningEngine.isMobileTrainingAllowed = true
        assertTrue(DeepLearningEngine.isMobileTrainingAllowed)
        
        // Test toggling mobile training off
        DeepLearningEngine.isMobileTrainingAllowed = false
        assertFalse(DeepLearningEngine.isMobileTrainingAllowed)
    }

    @Test
    fun testTrainingAllowedLogic() {
        // On non-Android systems, training should always be allowed
        if (!DeepLearningEngine.runningOnAndroid) {
            assertTrue(DeepLearningEngine.isTrainingAllowed())
            
            DeepLearningEngine.isMobileTrainingAllowed = false
            assertTrue(DeepLearningEngine.isTrainingAllowed())
            
            DeepLearningEngine.isMobileTrainingAllowed = true
            assertTrue(DeepLearningEngine.isTrainingAllowed())
        } else {
            // On Android systems, training depends on the flag
            DeepLearningEngine.isMobileTrainingAllowed = false
            assertFalse(DeepLearningEngine.isTrainingAllowed())
            
            DeepLearningEngine.isMobileTrainingAllowed = true
            assertTrue(DeepLearningEngine.isTrainingAllowed())
        }
    }

    @Test
    fun testCommandCreation() {
        // Test that the command can be created without errors
        val command = CommandAllowMobileTrain.createCommand()
        assertNotNull(command)
        assertEquals("allowMobileTrain", command.name)
        assertTrue(command.aliases.contains("amt"))
    }
}
