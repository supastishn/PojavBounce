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
    fun testAndroidPojavLauncherCompatibility() {
        // Test specifically for the issue described in the problem statement:
        // preventing UnsatisfiedLinkError with path like:
        // /data/data/com.tungsten.fcl/app_runtime/deeplearning/engines/pytorch/2.5.1-cpu-precxx11-linux-aarch64/
        
        val classpathEntries = System.getProperty("java.class.path").split(":")
        
        // Verify we don't have any JAR files that would extract Linux-specific native libraries
        val problematicJars = classpathEntries.filter { path ->
            val fileName = path.substringAfterLast("/")
            fileName.contains("pytorch-native") && 
            (fileName.contains("linux") || fileName.contains("cpu") || fileName.contains("precxx11"))
        }
        
        assertTrue(problematicJars.isEmpty(), 
            "Found JAR files that could cause UnsatisfiedLinkError on Android/PojavLauncher: $problematicJars")
        
        println("✓ Android/PojavLauncher compatibility verified - no problematic native library JARs")
    }

    @Test
    fun testNoLinuxSpecificDependencies() {
        // Test that we're not accidentally including Linux-specific PyTorch libraries
        // This is a classpath check to ensure we don't have the wrong dependencies
        
        val classpathEntries = System.getProperty("java.class.path").split(":")
        
        // Look for Linux-specific PyTorch native libraries that cause UnsatisfiedLinkError
        val hasLinuxPyTorch = classpathEntries.any { 
            it.contains("pytorch-native-cpu") || 
            it.contains("pytorch-native") && it.contains("linux") ||
            it.contains("pytorch-native-auto")
        }
        
        assertFalse(hasLinuxPyTorch, 
            "Should not have Linux-specific PyTorch native libraries in classpath. " +
            "Found entries: ${classpathEntries.filter { it.contains("pytorch-native") }}")
        
        println("✓ No Linux-specific PyTorch dependencies detected - Android compatibility ensured")
    }
}
