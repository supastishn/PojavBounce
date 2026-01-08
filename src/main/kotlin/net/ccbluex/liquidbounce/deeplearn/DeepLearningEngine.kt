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
package net.ccbluex.liquidbounce.deeplearn

import ai.djl.engine.Engine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.ConfigSystem.rootFolder
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.util.*

object DeepLearningEngine {

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
    private val deepLearningFolder = if (isAndroid) {
        // Try to use app-private directory on Android
        val appDataDir = System.getProperty("user.home")
            ?: System.getProperty("java.io.tmpdir")
            ?: "/data/local/tmp"
        File(appDataDir, "LiquidBounce/deeplearning").apply { mkdirs() }
    } else {
        rootFolder.resolve("deeplearning").apply { mkdirs() }
    }

    val djlCacheFolder = deepLearningFolder.resolve("djl").apply {
        mkdirs()
    }

    val enginesCacheFolder = deepLearningFolder.resolve("engines").apply {
        mkdirs()
    }

    val modelsFolder = deepLearningFolder.resolve("models").apply {
        mkdirs()
    }

    init {
        System.setProperty("DJL_CACHE_DIR", djlCacheFolder.absolutePath)
        System.setProperty("ENGINE_CACHE_DIR", enginesCacheFolder.absolutePath)

        // Disable tracking of DJL
        System.setProperty("OPT_OUT_TRACKING", "true")

        if (isAndroid) {
            // Android-specific configuration
            logger.info("[DeepLearning] Android environment detected, using Android-optimized settings")

            // Override OS name to request Android natives
            System.setProperty("os.name", "android")

            // Set the default engine to PyTorch with Android flavor
            System.setProperty("DJL_DEFAULT_ENGINE", "PyTorch")
            System.setProperty("PYTORCH_FLAVOR", "cpu-android")

            // Android-specific library path hints
            val javaLibPath = System.getProperty("java.library.path", "")
            System.setProperty(
                "java.library.path",
                "$javaLibPath:${enginesCacheFolder.absolutePath}"
            )
        } else {
            // Desktop configuration
            System.setProperty("DJL_DEFAULT_ENGINE", "PyTorch")
            System.setProperty("PYTORCH_FLAVOR", "cpu")
        }

        ModelHolster
    }

    @JvmStatic
    var task: Task? = null

    /**
     * DJL will automatically download engine libraries, as soon we call [Engine.getInstance()],
     * for the platform we are running on.
     *
     * This should be done here,
     * as we want to make sure that the libraries are downloaded
     * before we try to load any models.
     *
     * On Android platforms, native library loading may fail due to:
     * - Namespace isolation (libraries on external storage not accessible)
     * - GLIBC vs Bionic libc incompatibility
     * - Missing Android-specific native builds
     * In these cases, DJL features will be gracefully disabled.
     */
    suspend fun init(task: Task) {
        this.task = task

        logger.info("[DeepLearning] Initializing engine...")
        if (isAndroid) {
            logger.info("[DeepLearning] Running on Android platform - attempting native initialization")
        }

        try {
            val engine = withContext(Dispatchers.IO) {
                Engine.getInstance()
            }
            val name = engine.engineName
            val version = engine.version
            val deviceType = engine.defaultDevice().deviceType.uppercase(Locale.ENGLISH)
            logger.info("[DeepLearning] Using engine $name $version on $deviceType.")

            isInitialized = true
        } catch (t: Throwable) {
            logger.error("[DeepLearning] Failed to initialize DJL engine", t)
            logger.error("[DeepLearning] Engine initialization failure details:\n${collectDiagnosticInfo()}")

            if (isAndroid) {
                // Graceful degradation on Android
                logger.warn("[DeepLearning] Android native library support is currently experimental")
                logger.warn("[DeepLearning] Possible causes:")
                logger.warn("[DeepLearning]   - Namespace isolation (libs not accessible from external storage)")
                logger.warn("[DeepLearning]   - GLIBC vs Bionic incompatibility")
                logger.warn("[DeepLearning]   - Missing Android-specific PyTorch natives")
                logger.warn("[DeepLearning] Deep learning features will be disabled on this platform")
                logger.warn("[DeepLearning] Desktop platforms are fully supported")

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
     * Collects diagnostic information useful for troubleshooting DJL engine initialization failures.
     * Includes system properties, environment info, and cache folder contents.
     */
    private fun collectDiagnosticInfo(): String = buildString {
        appendLine("System Properties:")
        appendLine("  os.arch: ${System.getProperty("os.arch")}")
        appendLine("  os.name: ${System.getProperty("os.name")}")
        appendLine("  java.vendor: ${System.getProperty("java.vendor")}")
        appendLine("  java.vm.name: ${System.getProperty("java.vm.name")}")
        appendLine("  java.runtime.name: ${System.getProperty("java.runtime.name")}")
        appendLine("  java.library.path: ${System.getProperty("java.library.path")}")
        appendLine("  DJL_CACHE_DIR: ${System.getProperty("DJL_CACHE_DIR")}")
        appendLine("  ENGINE_CACHE_DIR: ${System.getProperty("ENGINE_CACHE_DIR")}")
        appendLine("  DJL_DEFAULT_ENGINE: ${System.getProperty("DJL_DEFAULT_ENGINE")}")
        appendLine("  PYTORCH_FLAVOR: ${System.getProperty("PYTORCH_FLAVOR")}")
        appendLine("  isAndroid (detected): $isAndroid")

        appendLine("\nCache Folder Contents:")

        try {
            appendLine("  deepLearningFolder (${deepLearningFolder.absolutePath}):")
            val dlFiles = deepLearningFolder.listFiles()
            if (dlFiles != null && dlFiles.isNotEmpty()) {
                dlFiles.forEach { file ->
                    appendLine("    - ${file.name} (${if (file.isDirectory) "dir" else "file, ${file.length()} bytes"})")
                }
            } else {
                appendLine("    (empty or not accessible)")
            }
        } catch (e: Exception) {
            appendLine("    Error listing deepLearningFolder: ${e.message}")
        }

        try {
            appendLine("  djlCacheFolder (${djlCacheFolder.absolutePath}):")
            val djlFiles = djlCacheFolder.listFiles()
            if (djlFiles != null && djlFiles.isNotEmpty()) {
                djlFiles.forEach { file ->
                    appendLine("    - ${file.name} (${if (file.isDirectory) "dir" else "file, ${file.length()} bytes"})")
                }
            } else {
                appendLine("    (empty or not accessible)")
            }
        } catch (e: Exception) {
            appendLine("    Error listing djlCacheFolder: ${e.message}")
        }

        try {
            appendLine("  enginesCacheFolder (${enginesCacheFolder.absolutePath}):")
            val engineFiles = enginesCacheFolder.listFiles()
            if (engineFiles != null && engineFiles.isNotEmpty()) {
                engineFiles.forEach { file ->
                    appendLine("    - ${file.name} (${if (file.isDirectory) "dir" else "file, ${file.length()} bytes"})")
                }
            } else {
                appendLine("    (empty or not accessible)")
            }
        } catch (e: Exception) {
            appendLine("    Error listing enginesCacheFolder: ${e.message}")
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

}
