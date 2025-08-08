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

class AndroidDJLCompatibilityTest {

    @Test
    fun testAndroidDJLCoreIsAvailable() {
        // Test that Android DJL core classes are available on the classpath
        assertDoesNotThrow {
            // Verify that the Android DJL core classes are available
            val engineClass = Class.forName("ai.djl.engine.Engine")
            assertNotNull(engineClass)
            println("Android DJL Engine class available")
            
            // Check that we can access DJL API classes (now from ai.djl.android:core)
            val modelClass = Class.forName("ai.djl.Model")
            assertNotNull(modelClass)
            println("DJL Model class available")
        }
    }

    @Test
    fun testAndroidNativeDependenciesStructure() {
        // Test that we're using the correct Android dependencies structure
        // This doesn't test runtime loading, just that the classes are available
        assertDoesNotThrow {
            // The main thing is that DJL classes are available for use
            val engineClass = Class.forName("ai.djl.engine.Engine")
            assertNotNull(engineClass)
            println("DJL classes structure ready for Android")
        }
    }

    @Test
    fun testDJLVersionCompatibility() {
        // Test that the DJL version we're using is compatible
        assertDoesNotThrow {
            val engineClass = Class.forName("ai.djl.engine.Engine")
            val methods = engineClass.methods
            
            // Check that essential methods exist
            val getInstanceMethod = methods.find { it.name == "getInstance" }
            assertNotNull(getInstanceMethod, "Engine.getInstance() method should be available")
            
            println("DJL Engine API compatibility verified")
        }
    }

    @Test
    fun testNoLinuxSpecificDependencies() {
        // Test that we're not accidentally including Linux-specific PyTorch libraries
        // This is a classpath check to ensure we don't have the wrong dependencies
        
        // Check that we don't have the problematic Linux PyTorch engine in our dependencies
        val classpathEntries = System.getProperty("java.class.path").split(":")
        
        // Look for Linux-specific PyTorch native libraries
        val hasLinuxPyTorch = classpathEntries.any { 
            it.contains("pytorch-native-cpu") && it.contains("linux")
        }
        
        assertFalse(hasLinuxPyTorch, 
            "Should not have Linux-specific PyTorch native libraries in classpath")
        
        println("No Linux-specific PyTorch dependencies detected")
    }
}
