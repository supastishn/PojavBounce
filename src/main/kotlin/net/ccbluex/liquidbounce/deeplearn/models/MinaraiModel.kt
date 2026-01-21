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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchEngine
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchModel
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.Closeable

/**
 * Minarai model wrapper that automatically selects the appropriate backend:
 * - ExecuTorch on Android (mobile-optimized)
 * - DJL on PC (full-featured)
 * - ExecuTorch fallback if DJL fails on PC
 *
 * This class uses composition to delegate to either a DJL-based TwoDimensionalRegressionModel
 * or an ExecuTorch-based model depending on platform availability.
 */
class MinaraiModel(
    name: String,
    override val parent: ChoiceConfigurable<*>
) : Choice(name), Closeable {

    // DJL model for PC (wrapped in a lazy delegate)
    private val djlModel: TwoDimensionalRegressionModel? by lazy {
        if (DeepLearningEngine.isInitialized) {
            TwoDimensionalRegressionModel(name, parent)
        } else {
            null
        }
    }

    // ExecuTorch model for Android
    private var execuTorchModel: ExecuTorchModel? = null

    // Indicates if ExecuTorch should be used
    private val useExecuTorch: Boolean
        get() = DeepLearningEngine.isAndroid ||
                (DeepLearningEngine.isExecuTorchAvailable &&
                 ExecuTorchEngine.isInitialized &&
                 !DeepLearningEngine.isInitialized)

    @Suppress("SwallowedException")
    fun predict(input: FloatArray): FloatArray {
        // Use ExecuTorch when it's available and DJL is not (Android or PC fallback case)
        if (useExecuTorch) {
            logger.debug("[MinaraiModel] Using ExecuTorch for prediction")
            val model = execuTorchModel
            if (model != null) {
                return model.predict(input)
            }
            logger.warn("[MinaraiModel] ExecuTorch model not loaded")
            return floatArrayOf(0f, 0f)
        }

        // Use DJL on PC
        val model = djlModel
        if (model != null) {
            return model.predict(input)
        }

        logger.warn("[MinaraiModel] No ML backend available for prediction")
        return floatArrayOf(0f, 0f)
    }

    fun train(features: Array<FloatArray>, labels: Array<FloatArray>) {
        // Route training based on available backend
        if (useExecuTorch) {
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
        }

        // PC: Use DJL training
        val model = djlModel
        if (model != null) {
            logger.info("[MinaraiModel] Using DJL training on PC for model '$name'")
            model.train(features, labels)
        } else {
            throw IllegalStateException("No ML backend available for training")
        }
    }

    fun load(modelName: String = name) {
        // On Android, use ExecuTorch
        if (useExecuTorch) {
            logger.info("[MinaraiModel] Loading ExecuTorch model '$modelName' for Android")
            try {
                execuTorchModel = ExecuTorchModel(modelName, parent = parent).apply {
                    load(modelName)
                }
                logger.info("[MinaraiModel] Successfully loaded ExecuTorch model '$modelName'")
            } catch (e: Exception) {
                logger.error("[MinaraiModel] Failed to load ExecuTorch model '$modelName': ${e.message}")
                execuTorchModel = null
            }
            return
        }

        // On PC, use standard DJL loading
        djlModel?.load(modelName)
    }

    fun save(modelName: String = name) {
        if (useExecuTorch) {
            execuTorchModel?.save(modelName)
        } else {
            djlModel?.save(modelName)
        }
    }

    fun delete() {
        if (useExecuTorch) {
            execuTorchModel?.delete()
        } else {
            djlModel?.delete()
        }
    }

    override fun close() {
        execuTorchModel?.close()
        execuTorchModel = null
        djlModel?.close()
    }

    companion object {
        // Shared translator instance to avoid duplication
        @Suppress("unused")
        private val TRANSLATOR = FloatArrayInAndOutTranslator()
    }
}
