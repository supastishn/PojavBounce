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
import java.util.*

object DeepLearningEngine {

    var isInitialized = false
        private set

    private val deepLearningFolder = rootFolder.resolve("deeplearning").apply {
        mkdirs()
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

        // Set the default engine to PyTorch
        System.setProperty("DJL_DEFAULT_ENGINE", "PyTorch")
        // Enforce CPU pytorch flavor (CUDA often conflicts with NVIDIA CUDA and is too large for our use case)
        System.setProperty("PYTORCH_FLAVOR", "cpu")

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
     */
    suspend fun init(task: Task) {
        this.task = task

        logger.info("[DeepLearning] Initializing engine...")

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
            this.task = null
            throw t
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
        appendLine("  java.library.path: ${System.getProperty("java.library.path")}")
        appendLine("  DJL_CACHE_DIR: ${System.getProperty("DJL_CACHE_DIR")}")
        appendLine("  ENGINE_CACHE_DIR: ${System.getProperty("ENGINE_CACHE_DIR")}")
        appendLine("  DJL_DEFAULT_ENGINE: ${System.getProperty("DJL_DEFAULT_ENGINE")}")
        appendLine("  PYTORCH_FLAVOR: ${System.getProperty("PYTORCH_FLAVOR")}")

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

}
