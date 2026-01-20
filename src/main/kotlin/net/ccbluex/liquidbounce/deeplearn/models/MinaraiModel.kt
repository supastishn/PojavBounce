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
package net.ccbluex.liquidbounce.deeplearn.models

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchModel
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchTrainer
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator
import net.ccbluex.liquidbounce.utils.client.logger

/**
 * Minarai model wrapper that automatically selects the appropriate backend:
 * - ExecuTorch on Android (mobile-optimized)
 * - DJL on PC (full-featured)
 * - ExecuTorch fallback if DJL fails on PC
 */
class MinaraiModel(
    name: String,
    parent: ChoiceConfigurable<*>
) : ModelWrapper<FloatArray, FloatArray>(
    name,
    TRANSLATOR,
    2, // X, Y
    parent
) {

    // ExecuTorch model for Android or fallback
    private var execuTorchModel: ExecuTorchModel? = null

    @Suppress("SwallowedException")
    override fun predict(input: FloatArray): FloatArray {
        // Use ExecuTorch when it's available and DJL is not (Android or PC fallback case)
        if (DeepLearningEngine.isExecuTorchAvailable && !DeepLearningEngine.isInitialized) {
            if (execuTorchModel == null) {
                execuTorchModel = ExecuTorchModel(
                    name,
                    TRANSLATOR,
                    outputs,
                    parent
                )
                // Load the model if not already loaded
                try {
                    execuTorchModel?.load(name)
                } catch (e: Exception) {
                    // ExecuTorch model loading failed (expected if JNI not implemented)
                    // Fall through to DJL if available
                    logger.debug(
                        "ExecuTorch model loading failed, falling back to DJL",
                        e
                    )
                    execuTorchModel = null
                }
            }
            execuTorchModel?.let {
                return it.predict(input)
            }
        }

        // Use DJL on PC or as fallback
        return super.predict(input)
    }

    override fun train(features: Array<FloatArray>, labels: Array<FloatArray>) {
        // Route training based on available backend
        if (DeepLearningEngine.isExecuTorchAvailable && !DeepLearningEngine.isInitialized) {
            // Android: ExecuTorch training requires specially exported models
            // Current models are inference-only, so we need to inform the user
            logger.warn("[MinaraiModel] Training on Android is not yet supported")
            throw UnsupportedOperationException(
                "Model training is not yet available on Android. " +
                "The bundled models are inference-only. " +
                "\n\nWorkaround: " +
                "\n1. Use '.models create $name' on PC to train a model" +
                "\n2. Copy the trained model folder to your device" +
                "\n3. Use the pre-trained models (19KC8KP or 21KC11KP) for now" +
                "\n\nLocation: LiquidBounce/deeplearning/models/"
            )
        } else {
            // PC: Use DJL training (parent implementation)
            logger.info("[MinaraiModel] Using DJL training on PC for model '$name'")
            super.train(features, labels)
        }
    }

    /**
     * Trains the model using ExecuTorch's on-device training API.
     * This enables model training on Android where DJL is not available.
     */
    private fun trainWithExecuTorch(features: Array<FloatArray>, labels: Array<FloatArray>) {
        logger.info("[MinaraiModel] Starting ExecuTorch training for model '$name'")

        if (features.isEmpty() || labels.isEmpty()) {
            throw IllegalArgumentException("Features and labels must not be empty")
        }

        if (features.size != labels.size) {
            throw IllegalArgumentException(
                "Features (${features.size}) and labels (${labels.size}) must have the same size"
            )
        }

        try {
            // Normalize data for better training stability
            val (normalizedFeatures, normalizedLabels) = ExecuTorchTrainer.normalizeData(features, labels)

            // Train using ExecuTorch
            ExecuTorchTrainer.train(name, normalizedFeatures, normalizedLabels)

            logger.info("[MinaraiModel] ExecuTorch training completed successfully for model '$name'")
        } catch (e: Exception) {
            logger.error("[MinaraiModel] ExecuTorch training failed for model '$name': ${e.message}", e)
            throw RuntimeException(
                "ExecuTorch training failed: ${e.message}\n" +
                "Make sure the model is exported with training support enabled (to_executorch(training=true))",
                e
            )
        }
    }

    override fun load(name: String) {
        // On Android (ExecuTorch only), skip DJL model loading
        // The ExecuTorch model will be loaded lazily on first predict() call
        if (DeepLearningEngine.isExecuTorchAvailable && !DeepLearningEngine.isInitialized) {
            logger.info(
                "[MinaraiModel] Skipping DJL load for '$name' on Android - ExecuTorch will load on demand"
            )
            return
        }

        // On PC, use standard DJL loading
        super.load(name)
    }

    override fun close() {
        execuTorchModel?.close()
        execuTorchModel = null
        super.close()
    }

    companion object {
        // Shared translator instance to avoid duplication
        private val TRANSLATOR = FloatArrayInAndOutTranslator()
    }
}
