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
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchEngine
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.util.*

internal const val ANDROID_PYTORCH_FLAVOR = "cpu"
internal val ANDROID_OS_NAME_OVERRIDE: String? = null

object DeepLearningEngine {

    var isInitialized = false
        private set

    /**
     * Flag indicating if ExecuTorch (PyTorch Mobile) backend is available.
     * This is independent of DJL initialization and allows fallback.
     */
    var isExecuTorchAvailable = false
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
        createAndroidDeepLearningFolder().apply { mkdirs() }
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
            logger.info("[DeepLearning] Android environment detected, using desktop DJL settings for PojavLauncher")
            ANDROID_OS_NAME_OVERRIDE?.let { System.setProperty("os.name", it) }

            // Set the default engine to PyTorch with Android flavor
            System.setProperty("DJL_DEFAULT_ENGINE", "PyTorch")
            System.setProperty("PYTORCH_FLAVOR", ANDROID_PYTORCH_FLAVOR)

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
     * Initializes the deep learning engine with platform-specific backend selection.
     *
     * Platform strategy:
     * - Android: Use ExecuTorch only (DJL is skipped due to native library issues)
     * - PC: Use DJL primarily, fallback to ExecuTorch if DJL fails
     *
     * ExecuTorch is the PyTorch Mobile solution optimized for on-device inference.
     * DJL is a full-featured framework better suited for desktop platforms.
     */
    suspend fun init(task: Task) {
        this.task = task

        logger.info("[DeepLearning] Initializing engine...")
        
        if (isAndroid) {
            initAndroid(task)
        } else {
            initPC(task)
        }

        this.task = null
    }

    /**
     * Initializes ExecuTorch backend for Android platform.
     * DJL is not initialized on Android due to native library compatibility issues.
     */
    private suspend fun initAndroid(task: Task) {
        logger.info("[DeepLearning] Running on Android platform - using ExecuTorch backend")
        logger.info("[DeepLearning] DJL initialization skipped on Android (use ExecuTorch instead)")
        
        // Initialize ExecuTorch for Android
        try {
            ExecuTorchEngine.init(task)
            isExecuTorchAvailable = ExecuTorchEngine.isInitialized
            if (isExecuTorchAvailable) {
                logger.info("[DeepLearning] ExecuTorch backend initialized successfully on Android")
            } else {
                logger.warn("[DeepLearning] ExecuTorch backend initialization failed on Android")
            }
        } catch (t: Throwable) {
            logger.error("[DeepLearning] Failed to initialize ExecuTorch on Android", t)
            isExecuTorchAvailable = false
        }
        
        // DJL is not initialized on Android
        isInitialized = false
    }

    /**
     * Initializes DJL backend for PC platform, with ExecuTorch fallback.
     * Tries to initialize DJL first, and falls back to ExecuTorch if that fails.
     */
    private suspend fun initPC(task: Task) {
        logger.info("[DeepLearning] Running on PC platform - using DJL backend")
        
        try {
            initDJL()
        } catch (t: Throwable) {
            logger.error("[DeepLearning] Failed to initialize DJL engine on PC", t)
            logger.error("[DeepLearning] DJL initialization failure details:\n${collectDiagnosticInfo()}")
            logger.warn("[DeepLearning] Attempting to fallback to ExecuTorch...")
            
            isInitialized = false
            initExecuTorchFallback(task, t)
            return
        }
        
        // If DJL succeeded, also try to initialize ExecuTorch for additional backend option
        tryInitExecuTorchAdditional(task)
    }

    /**
     * Initializes DJL engine.
     */
    private suspend fun initDJL() {
        val engine = withContext(Dispatchers.IO) {
            Engine.getInstance()
        }
        val name = engine.engineName
        val version = engine.version
        val deviceType = engine.defaultDevice().deviceType.uppercase(Locale.ENGLISH)
        logger.info("[DeepLearning] Using DJL engine $name $version on $deviceType.")

        isInitialized = true
    }

    /**
     * Initializes ExecuTorch as fallback when DJL initialization fails on PC.
     */
    private suspend fun initExecuTorchFallback(task: Task, originalError: Throwable) {
        try {
            ExecuTorchEngine.init(task)
            isExecuTorchAvailable = ExecuTorchEngine.isInitialized
            if (isExecuTorchAvailable) {
                logger.info("[DeepLearning] ExecuTorch backend initialized successfully (fallback mode)")
            } else {
                logger.error("[DeepLearning] ExecuTorch fallback also failed")
                this.task = null
                throw originalError
            }
        } catch (execuTorchError: Throwable) {
            logger.error("[DeepLearning] ExecuTorch fallback initialization failed", execuTorchError)
            isExecuTorchAvailable = false
            this.task = null
            throw originalError  // Rethrow original DJL error
        }
    }

    /**
     * Tries to initialize ExecuTorch as an additional backend when DJL is already initialized.
     */
    private suspend fun tryInitExecuTorchAdditional(task: Task) {
        logger.info("[DeepLearning] Initializing ExecuTorch as additional backend...")
        try {
            ExecuTorchEngine.init(task)
            isExecuTorchAvailable = ExecuTorchEngine.isInitialized
            if (isExecuTorchAvailable) {
                logger.info("[DeepLearning] ExecuTorch backend also available")
            }
        } catch (t: Throwable) {
            logger.warn("[DeepLearning] ExecuTorch backend initialization failed (DJL is still available)", t)
            isExecuTorchAvailable = false
        }
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

    /**
     * Choose an Android cache folder that is executable (external storage is usually mounted noexec).
     * Prefers temp/cache directories under app-private storage to allow dlopen of native libraries.
     */
    private fun createAndroidDeepLearningFolder(): File {
        val candidates = listOfNotNull(
            System.getenv("LIQUIDBOUNCE_DL_DIR")?.let(::File),
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

        logger.info("[DeepLearning] Using Android cache directory: ${usableBase.absolutePath}")
        return File(usableBase, "LiquidBounce/deeplearning")
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
