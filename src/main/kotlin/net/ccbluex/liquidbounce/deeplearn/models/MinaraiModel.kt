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
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator

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
                    net.ccbluex.liquidbounce.utils.client.logger.debug(
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

    override fun load(name: String) {
        // On Android (ExecuTorch only), skip DJL model loading
        // The ExecuTorch model will be loaded lazily on first predict() call
        if (DeepLearningEngine.isExecuTorchAvailable && !DeepLearningEngine.isInitialized) {
            net.ccbluex.liquidbounce.utils.client.logger.info(
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
