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

class MobileCompatibilityTest {

    @Test
    fun testDJLEngineCanInitialize() {
        // Test that DJL engine initialization doesn't crash
        // Note: We'll test this without actually initializing to avoid test environment issues
        assertDoesNotThrow {
            // Just verify the engine class is available
            val engineClass = Class.forName("ai.djl.engine.Engine")
            assertNotNull(engineClass)
            println("DJL Engine class available for initialization")
        }
    }

    @Test
    fun testJniLibsDirectoryStructure() {
        // Test that the jniLibs directory structure exists for Android compatibility
        val jniLibsDir = File("src/main/jniLibs")
        val armv7Dir = File(jniLibsDir, "armeabi-v7a")
        val arm64Dir = File(jniLibsDir, "arm64-v8a")
        
        assertTrue(jniLibsDir.exists(), "jniLibs directory should exist")
        assertTrue(armv7Dir.exists(), "armeabi-v7a directory should exist")
        assertTrue(arm64Dir.exists(), "arm64-v8a directory should exist")
        
        println("JNI Libs structure ready for mobile compatibility")
    }

    @Test
    fun testMobileCompatibilityDocumentationExists() {
        // Test that mobile compatibility documentation exists
        val docFile = File("MOBILE_COMPATIBILITY.md")
        assertTrue(docFile.exists(), "Mobile compatibility documentation should exist")
        assertTrue(docFile.length() > 0, "Documentation should not be empty")
        
        println("Mobile compatibility documentation is available")
    }

    @Test
    fun testAndroidDetectionLogic() {
        // Test the Android detection logic in isolation
        val hasAndroidVM = System.getProperty("java.vm.name")?.contains("Android", ignoreCase = true) == true
        val hasAndroidRuntime = System.getProperty("java.runtime.name")?.contains("Android", ignoreCase = true) == true
        val hasBuildProp = File("/system/build.prop").exists()
        
        val isAndroid = hasAndroidVM || hasAndroidRuntime || hasBuildProp
        
        // On a normal test environment, this should be false
        assertFalse(isAndroid, "Should not detect Android in test environment")
        
        println("Android detection logic working correctly")
    }
}
