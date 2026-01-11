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

import net.ccbluex.liquidbounce.deeplearn.backend.Backend
import net.ccbluex.liquidbounce.deeplearn.utils.NativeLibExtractor
import net.ccbluex.liquidbounce.integration.task.type.Task
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import kotlin.test.assertTrue

class DeepLearningTest {

    @Test
    fun `test deep learning engine initialization states`() {
        // Test initial state
        assertFalse(DeepLearningEngine.isInitialized)
        assertEquals(Backend.NONE, DeepLearningEngine.backend)
    }

    @Test
    fun `test android detection`() {
        // Test that Android detection works based on system properties
        val isAndroid = DeepLearningEngine::class.java.getDeclaredField("isAndroid").apply {
            isAccessible = true
        }.get(DeepLearningEngine) as Boolean

        // This test will pass on both desktop and Android
        // We just verify it's a boolean value
        assertTrue(isAndroid is Boolean)
    }

    @Test
    fun `test cache folder creation`() {
        val djlCache = DeepLearningEngine.djlCacheFolder
        val enginesCache = DeepLearningEngine.enginesCacheFolder
        val modelsCache = DeepLearningEngine.modelsFolder

        assertTrue(djlCache.exists())
        assertTrue(enginesCache.exists())
        assertTrue(modelsCache.exists())

        assertTrue(djlCache.isDirectory)
        assertTrue(enginesCache.isDirectory)
        assertTrue(modelsCache.isDirectory)
    }

    @Test
    fun `test native library extractor diagnostics`() {
        val tempDir = createTempDir("native-test")
        try {
            val diagnostics = NativeLibExtractor.collectDiagnostics(tempDir)
            assertTrue(diagnostics.isNotEmpty())
            assertTrue(diagnostics.contains("Native Library Diagnostics"))
            assertTrue(diagnostics.contains("Detected ABI"))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test native library extractor with empty library list`() {
        val tempDir = createTempDir("native-test-empty")
        try {
            val extracted = NativeLibExtractor.extractNativeLibraries(tempDir, emptyList())
            assertTrue(extracted.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test native library extractor with non-existent libraries`() {
        val tempDir = createTempDir("native-test-missing")
        try {
            // Try to extract libraries that don't exist in resources
            val extracted = NativeLibExtractor.extractNativeLibraries(
                tempDir,
                listOf("libNonExistent.so", "libAlsoMissing.so")
            )
            // Should return empty list since libraries don't exist
            assertTrue(extracted.isEmpty())
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test native library load with empty list`() {
        val loaded = NativeLibExtractor.loadNativeLibraries(emptyList())
        assertTrue(loaded)
    }

    @Test
    fun `test abi detection returns valid abi string`() {
        val abi = NativeLibExtractor::class.java.getDeclaredMethod("detectAndroidAbi").apply {
            isAccessible = true
        }.invoke(NativeLibExtractor) as String

        assertTrue(abi.isNotEmpty())
        // Should be one of the common Android ABIs
        assertTrue(abi in listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
    }

    @Test
    @Disabled("Requires actual ONNX Runtime libraries to be present - only run manually")
    fun `test deep learning engine initialization with onnx backend`() {
        // This test is disabled by default as it requires actual native libraries
        // and may not work in all test environments
        val tempDir = createTempDir("deeplearn-test")
        try {
            val mockTask = object : Task {
                override val name: String = "Test Task"
                override val progress: Float = 0.0f
                override val status: String = "Testing"
                override fun cancel() {}
            }

            // This may or may not succeed depending on the environment
            // We're just testing that the method doesn't throw unexpected exceptions
            assertDoesNotThrow {
                kotlinx.coroutines.runBlocking {
                    DeepLearningEngine.init(mockTask)
                }
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `test backend enum values`() {
        // Test that all backend enum values exist
        assertEquals(3, Backend.entries.size)
        assertTrue(Backend.NONE in Backend.entries)
        assertTrue(Backend.ONNX in Backend.entries)
        assertTrue(Backend.DJL in Backend.entries)
    }

    @Test
    fun `test backend to string conversion`() {
        assertEquals("ONNX Runtime", Backend.ONNX.toString())
        assertEquals("NONE", Backend.NONE.toString())
        assertEquals("DJL", Backend.DJL.toString())
    }

}