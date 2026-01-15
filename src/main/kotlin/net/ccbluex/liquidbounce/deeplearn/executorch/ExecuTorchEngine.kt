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
 *
 *
 */
package net.ccbluex.liquidbounce.deeplearn.executorch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.ConfigSystem.rootFolder
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.util.*

/**
 * ExecuTorch runtime engine for on-device PyTorch model inference on Android and mobile platforms.
 *
 * ExecuTorch is the official PyTorch deployment solution for edge devices, replacing PyTorch Mobile.
 * It provides:
 * - Direct PyTorch model export to .pte (ExecuTorch Program) format
 * - Native hardware acceleration (CPU, GPU, NPU)
 * - Smaller model sizes compared to full PyTorch
 * - Optimized inference performance on mobile devices
 *
 * This engine manages:
 * - Native library loading and initialization
 * - Cache folder management for .pte models
 * - Graceful fallback if native libraries are unavailable
 * - Diagnostics for troubleshooting on restrictive platforms (Android)
 */
object ExecuTorchEngine {

    var isInitialized = false
        private set

    /**
     * Detects if running in an Android environment (e.g., PojavLauncher, Termux).
     * Android has different native library requirements and restrictions.
     */
    private val isAndroid: Boolean = detectAndroid()

    /**
     * On Android, use app-private storage to avoid namespace isolation and SELinux restrictions.
     * On desktop, use the standard config folder.
     */
    private val executorchFolder = if (isAndroid) {
        createAndroidExecutorchFolder().apply { mkdirs() }
    } else {
        rootFolder.resolve("executorch").apply { mkdirs() }
    }

    val modelsFolder = executorchFolder.resolve("models").apply {
        mkdirs()
    }

    val cacheFolder = executorchFolder.resolve("cache").apply {
        mkdirs()
    }

    val nativeFolder = executorchFolder.resolve("native").apply {
        mkdirs()
    }

    init {
        // Configure ExecuTorch cache directories
        System.setProperty("EXECUTORCH_CACHE_DIR", cacheFolder.absolutePath)
        System.setProperty("EXECUTORCH_NATIVE_DIR", nativeFolder.absolutePath)

        // Disable tracking of ExecuTorch
        System.setProperty("OPT_OUT_TRACKING", "true")

        if (isAndroid) {
            // Android-specific configuration
            logger.info("[ExecuTorch] Android environment detected, configuring for PojavLauncher")

            // Add native folder to library path for dlopen
            val javaLibPath = System.getProperty("java.library.path", "")
            System.setProperty(
                "java.library.path",
                "$javaLibPath:${nativeFolder.absolutePath}"
            )
        } else {
            logger.info("[ExecuTorch] Desktop environment detected")
        }
    }

    @JvmStatic
    var task: Task? = null

    /**
     * Initializes the ExecuTorch runtime.
     * This attempts to load native libraries and prepare for model execution.
     *
     * On Android platforms, native library loading may fail due to:
     * - Namespace isolation (libraries on external storage not accessible)
     * - GLIBC vs Bionic libc incompatibility
     * - Missing Android-specific native builds
     * In these cases, ExecuTorch features will be gracefully disabled.
     */
    suspend fun init(task: Task) {
        this.task = task

        logger.info("[ExecuTorch] Initializing ExecuTorch runtime...")
        if (isAndroid) {
            logger.info("[ExecuTorch] Running on Android platform - attempting native initialization")
        }

        try {
            // Attempt to load ExecuTorch native library
            val loaded = withContext(Dispatchers.IO) {
                try {
                    // Try to load the ExecuTorch native library
                    System.loadLibrary("executorch")
                    logger.info("[ExecuTorch] Successfully loaded native ExecuTorch library")
                    true
                } catch (t: Throwable) {
                    logger.warn("[ExecuTorch] Failed to load native library: ${t.message}")
                    false
                }
            }

            if (loaded) {
                isInitialized = true
                logger.info("[ExecuTorch] ExecuTorch runtime initialized successfully")
            } else {
                throw ExecuTorchInitializationException("Failed to load ExecuTorch native library")
            }
        } catch (t: Throwable) {
            logger.error("[ExecuTorch] Failed to initialize ExecuTorch engine", t)
            logger.error("[ExecuTorch] Engine initialization failure details:\n${collectDiagnosticInfo()}")

            if (isAndroid) {
                // Graceful degradation on Android
                logger.warn("[ExecuTorch] Android native library support is currently experimental")
                logger.warn("[ExecuTorch] Possible causes:")
                logger.warn("[ExecuTorch]   - Namespace isolation (libs not accessible from external storage)")
                logger.warn("[ExecuTorch]   - GLIBC vs Bionic incompatibility")
                logger.warn("[ExecuTorch]   - Missing Android-specific ExecuTorch natives")
                logger.warn("[ExecuTorch] ExecuTorch features will be disabled on this platform")
                logger.warn("[ExecuTorch] Desktop platforms are fully supported")

                isInitialized = false
                this.task = null
                return  // Don't throw - graceful degradation on Android
            } else {
                // Rethrow on desktop platforms - this is a critical error
                this.task = null
                throw t
            }
        }

        this.task = null
    }

    /**
     * Collects diagnostic information useful for troubleshooting ExecuTorch initialization failures.
     * Includes system properties, environment info, and folder contents.
     */
    @Suppress("CognitiveComplexMethod") // Diagnostic function with multiple checks
    private fun collectDiagnosticInfo(): String = buildString {
        appendLine("System Properties:")
        appendLine("  os.arch: ${System.getProperty("os.arch")}")
        appendLine("  os.name: ${System.getProperty("os.name")}")
        appendLine("  java.vendor: ${System.getProperty("java.vendor")}")
        appendLine("  java.vm.name: ${System.getProperty("java.vm.name")}")
        appendLine("  java.runtime.name: ${System.getProperty("java.runtime.name")}")
        appendLine("  java.library.path: ${System.getProperty("java.library.path")}")
        appendLine("  EXECUTORCH_CACHE_DIR: ${System.getProperty("EXECUTORCH_CACHE_DIR")}")
        appendLine("  EXECUTORCH_NATIVE_DIR: ${System.getProperty("EXECUTORCH_NATIVE_DIR")}")
        appendLine("  isAndroid (detected): $isAndroid")

        appendLine("\nCache Folder Contents:")

        try {
            appendLine("  executorchFolder (${executorchFolder.absolutePath}):")
            val dlFiles = executorchFolder.listFiles()
            if (dlFiles != null && dlFiles.isNotEmpty()) {
                dlFiles.forEach { file ->
                    val fileInfo = if (file.isDirectory) "dir" else "file, ${file.length()} bytes"
                    appendLine("    - ${file.name} ($fileInfo)")
                }
            } else {
                appendLine("    (empty or not accessible)")
            }
        } catch (e: Exception) {
            appendLine("    Error listing executorchFolder: ${e.message}")
        }

        try {
            appendLine("  nativeFolder (${nativeFolder.absolutePath}):")
            val nativeFiles = nativeFolder.listFiles()
            if (nativeFiles != null && nativeFiles.isNotEmpty()) {
                nativeFiles.forEach { file ->
                    val fileInfo = if (file.isDirectory) "dir" else "file, ${file.length()} bytes"
                    appendLine("    - ${file.name} ($fileInfo)")
                }
            } else {
                appendLine("    (empty or not accessible)")
            }
        } catch (e: Exception) {
            appendLine("    Error listing nativeFolder: ${e.message}")
        }
    }

    /**
     * Detects if the current environment is Android-based.
     * Checks multiple indicators including:
     * - Java vendor/VM name (Dalvik, Android Runtime)
     * - Presence of Android system files
     * - Runtime name containing "Android"
     */
    private fun detectAndroid(): Boolean {
        return System.getProperty("java.vendor")?.contains("Android", ignoreCase = true) == true ||
               System.getProperty("java.vm.name")?.contains("Dalvik", ignoreCase = true) == true ||
               System.getProperty("java.runtime.name")?.contains("Android", ignoreCase = true) == true ||
               File("/system/build.prop").exists()
    }

    /**
     * Choose an Android cache folder that is executable (external storage is usually mounted noexec).
     * Prefers temp/cache directories under app-private storage to allow dlopen of native libraries.
     */
    private fun createAndroidExecutorchFolder(): File {
        val candidates = listOfNotNull(
            System.getenv("LIQUIDBOUNCE_EXECUTORCH_DIR")?.let(::File),
            System.getProperty("java.io.tmpdir")?.let(::File),
            System.getenv("TMPDIR")?.let(::File),
            File("/data/data/com.termux/files/usr/tmp").takeIf { it.exists() },
            System.getProperty("user.home")?.let(::File),
            File("/data/local/tmp")
        )

        val usableBase = candidates.firstOrNull { dir ->
            try {
                dir.mkdirs()
                dir.isDirectory && dir.canWrite() && !isNoExecPath(dir)
            } catch (_: Exception) {
                false
            }
        } ?: File("/data/local/tmp")

        logger.info("[ExecuTorch] Using Android cache directory: ${usableBase.absolutePath}")
        return File(usableBase, "LiquidBounce/executorch")
    }

    /**
     * Checks whether a directory resides on a noexec mount by inspecting /proc/self/mounts.
     */
    private fun isNoExecPath(path: File): Boolean {
        return try {
            val absPath = path.absolutePath
            File("/proc/self/mounts").useLines { lines ->
                lines.mapNotNull { line ->
                    val parts = line.split(" ")
                    if (parts.size < 4) return@mapNotNull null
                    val mountPoint = parts[1]
                    val options = parts[3]
                    if (absPath.startsWith(mountPoint)) mountPoint to options else null
                }.maxByOrNull { it.first.length }?.second?.contains("noexec") == true
            }
        } catch (_: Exception) {
            false
        }
    }

}

/**
 * Exception thrown when ExecuTorch engine initialization fails.
 */
class ExecuTorchInitializationException(message: String, cause: Throwable? = null) : Exception(message, cause)

