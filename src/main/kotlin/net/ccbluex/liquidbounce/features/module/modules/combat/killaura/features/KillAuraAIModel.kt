/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchEngine
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchModel
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.util.*

/**
 * KillAura AI Model feature - scans for and uses .pte (ExecuTorch) models for combat prediction.
 *
 * This feature scans both:
 * - Bundled models from JAR resources (/resources/liquidbounce/models/executorch/*.pte)
 * - User models from the ExecuTorch models folder
 *
 * Models can be used to predict optimal attack timing, rotation adjustments, etc.
 */
object KillAuraAIModel : ToggleableConfigurable(ModuleKillAura, "AIModel", false) {

    /**
     * Bundled .pte models that are included in the JAR.
     * These are always available regardless of user configuration.
     */
    private val bundledModels = arrayOf(
        "19kc8kp",
        "21kc11kp",
        "android-pte-final"
    )

    /**
     * Resource path for bundled ExecuTorch models.
     */
    private const val MODELS_RESOURCE_PATH = "/resources/liquidbounce/models/executorch"

    /**
     * Cache of discovered .pte model names (without extension).
     */
    private var discoveredModels: List<String> = emptyList()

    /**
     * Currently loaded ExecuTorch model.
     */
    private var loadedModel: ExecuTorchModel? = null

    /**
     * Currently selected model name.
     */
    private var selectedModelName: String = ""

    /**
     * Model selection - dynamically populated from discovered models.
     */
    private val modelSelection by text("Model", "").onChanged { newModel ->
        if (newModel.isNotEmpty() && newModel != selectedModelName) {
            selectedModelName = newModel
            loadSelectedModel()
        }
    }

    /**
     * Auto-scan on enable - automatically scan for models when feature is enabled.
     */
    private val autoScan by boolean("AutoScan", true)

    /**
     * Use for attack timing - use AI model predictions to adjust attack timing.
     */
    private val useForTiming by boolean("UseForTiming", true)

    /**
     * Confidence threshold - minimum confidence required to use model prediction.
     */
    private val confidenceThreshold by float("ConfidenceThreshold", 0.7f, 0.0f..1.0f)

    init {
        // Scan for models on initialization
        scanForModels()
    }

    override fun enable() {
        if (autoScan) {
            scanForModels()
        }
        if (selectedModelName.isNotEmpty()) {
            loadSelectedModel()
        }
    }

    override fun disable() {
        unloadModel()
    }

    /**
     * Scans for available .pte model files.
     *
     * Checks both:
     * 1. Bundled models in JAR resources
     * 2. User models in ExecuTorch models folder
     *
     * @return List of available model names (without .pte extension)
     */
    fun scanForModels(): List<String> {
        val models = mutableListOf<String>()

        // Add bundled models
        models.addAll(bundledModels)
        logger.info("[KillAura-AI] Found ${bundledModels.size} bundled .pte models")

        // Scan ExecuTorch models folder for user models
        if (DeepLearningEngine.isExecuTorchAvailable) {
            val userModels = scanModelsFolder()
            models.addAll(userModels)
            logger.info("[KillAura-AI] Found ${userModels.size} user .pte models in models folder")
        }

        // Remove duplicates and sort
        discoveredModels = models.distinct().sorted()
        logger.info("[KillAura-AI] Total available .pte models: ${discoveredModels.size}")

        for (model in discoveredModels) {
            logger.debug("[KillAura-AI]   - $model")
        }

        return discoveredModels
    }

    /**
     * Scans the ExecuTorch models folder for .pte files.
     *
     * @return List of model names found in the folder
     */
    private fun scanModelsFolder(): List<String> {
        val modelsFolder = ExecuTorchEngine.modelsFolder
        if (!modelsFolder.exists() || !modelsFolder.isDirectory) {
            logger.debug("[KillAura-AI] Models folder does not exist: ${modelsFolder.absolutePath}")
            return emptyList()
        }

        val models = mutableListOf<String>()

        // Scan for .pte files directly in models folder
        modelsFolder.listFiles { file -> file.isFile && file.extension.equals("pte", ignoreCase = true) }
            ?.forEach { file ->
                models.add(file.nameWithoutExtension)
            }

        // Scan for .pte files in subdirectories (model folders)
        modelsFolder.listFiles { file -> file.isDirectory }
            ?.forEach { dir ->
                dir.listFiles { file -> file.isFile && file.extension.equals("pte", ignoreCase = true) }
                    ?.forEach { file ->
                        models.add(file.nameWithoutExtension)
                    }
            }

        return models
    }

    /**
     * Gets the list of all discovered .pte models.
     *
     * @return List of model names (without extension)
     */
    fun getAvailableModels(): List<String> {
        if (discoveredModels.isEmpty()) {
            scanForModels()
        }
        return discoveredModels
    }

    /**
     * Checks if a specific model exists (bundled or user).
     *
     * @param modelName Model name without extension
     * @return True if model exists
     */
    fun modelExists(modelName: String): Boolean {
        val lowerName = modelName.lowercase(Locale.ENGLISH)

        // Check bundled models
        if (bundledModels.any { it.lowercase(Locale.ENGLISH) == lowerName }) {
            return true
        }

        // Check user models folder
        val modelsFolder = ExecuTorchEngine.modelsFolder

        // Check direct .pte file
        if (File(modelsFolder, "$modelName.pte").exists()) {
            return true
        }

        // Check subdirectory
        val subDir = File(modelsFolder, modelName)
        if (subDir.exists() && subDir.isDirectory) {
            return File(subDir, "$modelName.pte").exists()
        }

        return false
    }

    /**
     * Gets the file path for a model (if it's a user model).
     *
     * @param modelName Model name
     * @return File path or null if bundled/not found
     */
    fun getModelPath(modelName: String): File? {
        val modelsFolder = ExecuTorchEngine.modelsFolder

        // Check direct .pte file
        val directFile = File(modelsFolder, "$modelName.pte")
        if (directFile.exists()) {
            return directFile
        }

        // Check subdirectory
        val subDir = File(modelsFolder, modelName)
        if (subDir.exists() && subDir.isDirectory) {
            val subFile = File(subDir, "$modelName.pte")
            if (subFile.exists()) {
                return subFile
            }
        }

        return null
    }

    /**
     * Loads the currently selected model.
     */
    private fun loadSelectedModel() {
        if (selectedModelName.isEmpty()) {
            logger.warn("[KillAura-AI] No model selected")
            return
        }

        if (!DeepLearningEngine.isExecuTorchAvailable) {
            logger.warn("[KillAura-AI] ExecuTorch is not available, cannot load model")
            return
        }

        try {
            unloadModel()

            loadedModel = ExecuTorchModel(selectedModelName, parent = ModuleKillAura.rotations).apply {
                load(selectedModelName)
            }
            logger.info("[KillAura-AI] Successfully loaded model: $selectedModelName")
        } catch (e: Exception) {
            logger.error("[KillAura-AI] Failed to load model '$selectedModelName': ${e.message}")
            loadedModel = null
        }
    }

    /**
     * Unloads the current model.
     */
    private fun unloadModel() {
        loadedModel?.close()
        loadedModel = null
    }

    /**
     * Performs prediction using the loaded model.
     *
     * @param input Input features for prediction
     * @return Prediction output or null if model not loaded/prediction failed
     */
    fun predict(input: FloatArray): FloatArray? {
        if (!running) return null
        val model = loadedModel ?: return null

        return try {
            model.predict(input)
        } catch (e: Exception) {
            logger.error("[KillAura-AI] Prediction failed: ${e.message}")
            null
        }
    }

    /**
     * Checks if a model is currently loaded and ready for inference.
     *
     * @return True if model is loaded
     */
    fun isModelLoaded(): Boolean = loadedModel != null

    /**
     * Gets the name of the currently loaded model.
     *
     * @return Model name or empty string if no model loaded
     */
    fun getLoadedModelName(): String = selectedModelName

    /**
     * Whether to use model predictions for attack timing.
     */
    fun shouldUseForTiming(): Boolean = running && useForTiming && isModelLoaded()

    /**
     * Gets the confidence threshold for predictions.
     */
    fun getConfidenceThreshold(): Float = confidenceThreshold

    /**
     * Reloads the models list and current model.
     */
    fun reload() {
        scanForModels()
        if (selectedModelName.isNotEmpty()) {
            loadSelectedModel()
        }
    }

}
