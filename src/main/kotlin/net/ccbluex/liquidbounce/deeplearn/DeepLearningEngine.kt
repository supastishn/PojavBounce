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
     * Detect if we're running on Android (likely via PojavLauncher)
     */
    private val isAndroid: Boolean = try {
        System.getProperty("java.vm.name")?.contains("Android", ignoreCase = true) == true ||
        System.getProperty("java.runtime.name")?.contains("Android", ignoreCase = true) == true ||
        File("/system/build.prop").exists()
    } catch (_: Exception) {
        false
    }

    /**
     * Detect if we're running under FCL (Fold Craft Launcher)
     */
    private val isFCL: Boolean = try {
        isAndroid && File("/data/data/com.tungsten.fcl").exists()
    } catch (_: Exception) {
        false
    }

    /**
     * Get the FCL runtime directory path if available
     */
    private val fclRuntimePath: File? = try {
        if (isFCL) {
            File("/data/data/com.tungsten.fcl/app_runtime").takeIf { it.exists() || it.mkdirs() }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    }

    /**
     * Controls whether training is allowed on mobile devices
     */
    var isMobileTrainingAllowed: Boolean = false

    /**
     * Check if we're currently running on Android
     */
    val runningOnAndroid: Boolean get() = isAndroid

    /**
     * Check if we're running under FCL (Fold Craft Launcher)
     */
    val runningOnFCL: Boolean get() = isFCL

    /**
     * Check if training is allowed in the current environment
     */
    fun isTrainingAllowed(): Boolean {
        return if (isAndroid) isMobileTrainingAllowed else true
    }

    /**
     * Check if DJL can be initialized based on platform and available paths
     */
    fun canInitializeDJL(): Boolean {
        return if (isAndroid) {
            // On Android, require FCL runtime path to be available
            fclRuntimePath != null
        } else {
            true
        }
    }

    private val deepLearningFolder = when {
        fclRuntimePath != null -> {
            // On FCL, use the runtime directory for better compatibility and storage access
            fclRuntimePath.resolve("deeplearning").apply { mkdirs() }
        }
        isAndroid -> {
            // On Android without FCL, use external files directory
            val externalDir = System.getProperty("user.home") ?: "/storage/emulated/0"
            File(externalDir, "Android/data/deeplearning").apply { mkdirs() }
        }
        else -> {
            rootFolder.resolve("deeplearning").apply { mkdirs() }
        }
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
        // Set Android-friendly cache directories
        System.setProperty("DJL_CACHE_DIR", djlCacheFolder.absolutePath)
        System.setProperty("ENGINE_CACHE_DIR", enginesCacheFolder.absolutePath)

        // Disable tracking of DJL
        System.setProperty("OPT_OUT_TRACKING", "true")
        
        // For mobile/Android compatibility, prefer inference-only mode
        if (isAndroid) {
            System.setProperty("DJL_DEFAULT_ENGINE", "PyTorch")
            // Limit to CPU for better mobile compatibility
            System.setProperty("ai.djl.pytorch.graph_optimizer", "false")
            
            if (isFCL) {
                logger.info("[DeepLearning] FCL (Fold Craft Launcher) environment detected, using runtime directory for DJL cache")
            } else {
                logger.info("[DeepLearning] Android environment detected without FCL, using external storage for DJL cache")
            }
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
     */
    suspend fun init(task: Task) {
        this.task = task

        // Check if DJL can be initialized on this platform
        if (!canInitializeDJL()) {
            logger.warn("[DeepLearning] Cannot initialize DJL: Platform requirements not met (Android requires FCL runtime directory)")
            this.task = null
            return
        }

        logger.info("[DeepLearning] Initializing engine (Android: $isAndroid, FCL: $isFCL)...")
        val engine = withContext(Dispatchers.IO) {
            Engine.getInstance()
        }
        val name = engine.engineName
        val version = engine.version
        val deviceType = engine.defaultDevice().deviceType.uppercase(Locale.ENGLISH)
        logger.info("[DeepLearning] Using engine $name $version on $deviceType.")

        isInitialized = true
        this.task = null
    }

}
