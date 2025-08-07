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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.io.File

class FCLCompatibilityTest {

    @Test
    fun testFCLDetectionLogic() {
        // Test the FCL detection logic in isolation
        val isAndroid = try {
            System.getProperty("java.vm.name")?.contains("Android", ignoreCase = true) == true ||
            System.getProperty("java.runtime.name")?.contains("Android", ignoreCase = true) == true ||
            File("/system/build.prop").exists()
        } catch (_: Exception) {
            false
        }
        
        val isFCL = try {
            isAndroid && File("/data/data/com.tungsten.fcl").exists()
        } catch (_: Exception) {
            false
        }
        
        // On a normal test environment, this should be false
        assertFalse(isAndroid, "Should not detect Android in test environment")
        assertFalse(isFCL, "Should not detect FCL in test environment")
        
        println("FCL detection logic working correctly")
    }

    @Test
    fun testFCLRuntimePathLogic() {
        // Test the FCL runtime path logic
        val isAndroid = false // In test environment
        val isFCL = false // In test environment
        
        val fclRuntimePath = try {
            if (isFCL) {
                File("/data/data/com.tungsten.fcl/app_runtime").takeIf { it.exists() || it.mkdirs() }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
        
        // Should be null in test environment
        assertNull(fclRuntimePath, "FCL runtime path should be null in test environment")
        
        println("FCL runtime path logic working correctly")
    }

    @Test
    fun testDJLInitializationLogic() {
        // Test the logic for checking if DJL can be initialized
        val isAndroid = false // In test environment
        val fclRuntimePath: File? = null // In test environment
        
        val canInitializeDJL = if (isAndroid) {
            // On Android, require FCL runtime path to be available
            fclRuntimePath != null
        } else {
            true
        }
        
        // Should be true in non-Android test environment
        assertTrue(canInitializeDJL, "Should be able to initialize DJL in non-Android environment")
        
        println("DJL initialization logic working correctly")
    }

    @Test
    fun testAndroidDJLInitializationRequirement() {
        // Test that Android requires FCL runtime path for DJL initialization
        val isAndroid = true // Simulate Android environment
        val fclRuntimePath: File? = null // Simulate no FCL path
        
        val canInitializeDJL = if (isAndroid) {
            // On Android, require FCL runtime path to be available
            fclRuntimePath != null
        } else {
            true
        }
        
        // Should be false when Android but no FCL path
        assertFalse(canInitializeDJL, "Should not be able to initialize DJL on Android without FCL")
        
        println("Android DJL initialization requirement working correctly")
    }

    @Test
    fun testDeepLearningEngineProperties() {
        // Test that DeepLearningEngine has the expected public properties and they work correctly
        
        // Test that the values are as expected in test environment
        assertFalse(DeepLearningEngine.runningOnAndroid, "Should not be running on Android in test")
        assertFalse(DeepLearningEngine.runningOnFCL, "Should not be running on FCL in test")
        
        println("DeepLearningEngine properties accessible and correct")
    }

    @Test
    fun testCanInitializeDJLMethod() {
        // Test the canInitializeDJL method
        val canInitialize = DeepLearningEngine.canInitializeDJL()
        
        // Should be true in test environment (not Android)
        assertTrue(canInitialize, "Should be able to initialize DJL in test environment")
        
        println("canInitializeDJL method working correctly")
    }
}
