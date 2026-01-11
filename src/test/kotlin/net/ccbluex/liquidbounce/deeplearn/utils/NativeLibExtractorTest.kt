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
package net.ccbluex.liquidbounce.deeplearn.utils

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import kotlin.test.assertTrue

class NativeLibExtractorTest {

    private lateinit var tempDir: File

    @BeforeEach
    fun setUp() {
        tempDir = createTempDir("native-lib-test")
    }

    @AfterEach
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `test extract native libraries with empty list`() {
        val extracted = NativeLibExtractor.extractNativeLibraries(tempDir, emptyList())
        assertTrue(extracted.isEmpty())
    }

    @Test
    fun `test extract non-existent libraries returns empty list`() {
        val extracted = NativeLibExtractor.extractNativeLibraries(
            tempDir,
            listOf("libDefinitelyDoesNotExist.so")
        )
        assertTrue(extracted.isEmpty())
    }

    @Test
    fun `test load empty library list succeeds`() {
        val loaded = NativeLibExtractor.loadNativeLibraries(emptyList())
        assertTrue(loaded)
    }

    @Test
    fun `test extract and load integration with empty list`() {
        val success = NativeLibExtractor.extractAndLoad(tempDir, emptyList())
        assertTrue(success)
    }

    @Test
    fun `test abi detection returns valid android abi`() {
        val abi = NativeLibExtractor::class.java.getDeclaredMethod("detectAndroidAbi").apply {
            isAccessible = true
        }.invoke(NativeLibExtractor) as String

        assertTrue(abi.isNotEmpty())
        assertTrue(abi.matches(Regex("^(arm64-v8a|armeabi-v7a|x86_64|x86)$")))
    }

    @Test
    fun `test diagnostics collection includes expected information`() {
        val diagnostics = NativeLibExtractor.collectDiagnostics(tempDir)

        assertTrue(diagnostics.contains("Native Library Diagnostics"))
        assertTrue(diagnostics.contains("Detected ABI"))
        assertTrue(diagnostics.contains("os.arch"))
        assertTrue(diagnostics.contains("os.name"))
        assertTrue(diagnostics.contains("java.library.path"))
    }

    @Test
    fun `test diagnostics for non-existent directory`() {
        val nonExistentDir = File(tempDir, "does-not-exist")
        val diagnostics = NativeLibExtractor.collectDiagnostics(nonExistentDir)

        assertTrue(diagnostics.contains("Native Library Diagnostics"))
        assertTrue(diagnostics.contains("does-not-exist"))
    }

    @Test
    fun `test multiple library names are handled correctly`() {
        val libraryNames = listOf("libTest1.so", "libTest2.so", "libTest3.so")
        val extracted = NativeLibExtractor.extractNativeLibraries(tempDir, libraryNames)

        // Should return empty since these libraries don't exist
        assertTrue(extracted.isEmpty())
        assertEquals(0, extracted.size)
    }

    @Test
    fun `test target directory is created if it doesn't exist`() {
        val subDir = File(tempDir, "nested/deep/directory")
        assertFalse(subDir.exists())

        val extracted = NativeLibExtractor.extractNativeLibraries(subDir, listOf("libTest.so"))
        assertTrue(extracted.isEmpty())

        // Directory should not be created if no libraries are extracted
        assertFalse(subDir.exists())
    }

    @Test
    fun `test extract and load with non-existent libraries fails gracefully`() {
        val success = NativeLibExtractor.extractAndLoad(
            tempDir,
            listOf("libNonExistent.so", "libAlsoFake.so")
        )

        // Should fail because no libraries were extracted
        assertFalse(success)
    }

    @Test
    fun `test extraction from jar via classloader resources`() {
        val prevOsArch = System.getProperty("os.arch")
        System.setProperty("os.arch", "aarch64")
        try {
            val tempJar = File.createTempFile("onnxruntime-test", ".jar")
            java.util.jar.JarOutputStream(java.io.FileOutputStream(tempJar)).use { jos ->
                jos.putNextEntry(java.util.jar.JarEntry("jni/arm64-v8a/libonnxruntime.so"))
                jos.write(byteArrayOf(1, 2, 3))
                jos.closeEntry()
            }

            val urlClassLoader = java.net.URLClassLoader(arrayOf(tempJar.toURI().toURL()), Thread.currentThread().contextClassLoader)
            val prevCL = Thread.currentThread().contextClassLoader
            Thread.currentThread().contextClassLoader = urlClassLoader
            try {
                val extracted = NativeLibExtractor.extractNativeLibraries(tempDir, listOf("libonnxruntime.so"))
                assertTrue(extracted.isNotEmpty())
                assertTrue(extracted[0].exists())
                assertEquals(3, extracted[0].length())
            } finally {
                Thread.currentThread().contextClassLoader = prevCL
            }
        } finally {
            if (prevOsArch == null) System.clearProperty("os.arch") else System.setProperty("os.arch", prevOsArch)
        }
    }

    @Test
    fun `test find aar via java class path`() {
        val prevOsArch = System.getProperty("os.arch")
        val prevClassPath = System.getProperty("java.class.path")
        System.setProperty("os.arch", "aarch64")
        try {
            val tempJar = File.createTempFile("onnx-cp-test", ".jar")
            java.util.jar.JarOutputStream(java.io.FileOutputStream(tempJar)).use { jos ->
                jos.putNextEntry(java.util.jar.JarEntry("jni/arm64-v8a/libonnxruntime.so"))
                jos.write(byteArrayOf(5, 6, 7))
                jos.closeEntry()
            }

            System.setProperty("java.class.path", tempJar.absolutePath)

            val extracted = NativeLibExtractor.extractNativeLibraries(tempDir, listOf("libonnxruntime.so"))
            assertTrue(extracted.isNotEmpty())
            assertTrue(extracted[0].exists())
            assertEquals(3, extracted[0].length())
        } finally {
            if (prevClassPath == null) System.clearProperty("java.class.path") else System.setProperty("java.class.path", prevClassPath)
            if (prevOsArch == null) System.clearProperty("os.arch") else System.setProperty("os.arch", prevOsArch)
        }
    }

    @Test
    fun `test diagnostics include classloader info`() {
        val diagnostics = NativeLibExtractor.collectDiagnostics(tempDir)
        assertTrue(diagnostics.contains("ClassLoader diagnostics"))
        assertTrue(diagnostics.contains("contextClassLoader"))
    }

    @Test
    fun `test extract from embedded onnxruntime with linux-aarch64 layout`() {
        val prevOsArch = System.getProperty("os.arch")
        System.setProperty("os.arch", "aarch64")
        try {
            // Create embedded onnxruntime JAR with ai/onnxruntime/native/linux-aarch64/libonnxruntime.so
            val embeddedJar = File.createTempFile("onnx-embed", ".jar")
            java.util.jar.JarOutputStream(java.io.FileOutputStream(embeddedJar)).use { jos ->
                jos.putNextEntry(java.util.jar.JarEntry("ai/onnxruntime/native/linux-aarch64/libonnxruntime.so"))
                jos.write(byteArrayOf(9, 8, 7))
                jos.closeEntry()
            }

            // Create main JAR containing the embedded JAR under META-INF/jars/
            val mainJar = File.createTempFile("main-jar", ".jar")
            java.util.jar.JarOutputStream(java.io.FileOutputStream(mainJar)).use { jos ->
                val entry = java.util.jar.JarEntry("META-INF/jars/onnxruntime-embedded.jar")
                jos.putNextEntry(entry)
                embeddedJar.inputStream().use { it.copyTo(jos) }
                jos.closeEntry()
            }

            // Call extractFromAar directly with the jar:file URL pointing at the embedded JAR
            val method = NativeLibExtractor::class.java.getDeclaredMethod("extractFromAar", String::class.java, String::class.java, String::class.java, File::class.java)
            method.isAccessible = true
            val aarPath = "jar:file:${mainJar.absolutePath}!/META-INF/jars/onnxruntime-embedded.jar"
            val extracted = method.invoke(NativeLibExtractor, aarPath, "libonnxruntime.so", "arm64-v8a", tempDir) as java.io.File?

            assertNotNull(extracted)
            assertTrue(extracted!!.exists())
            assertEquals(3, extracted.length())
        } finally {
            if (prevOsArch == null) System.clearProperty("os.arch") else System.setProperty("os.arch", prevOsArch)
        }
    }

}