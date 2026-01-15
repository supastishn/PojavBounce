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
    FloatArrayInAndOutTranslator(),
    2, // X, Y
    parent
) {
    
    // ExecuTorch model for Android or fallback
    private var execuTorchModel: ExecuTorchModel? = null
    
    override fun predict(input: FloatArray): FloatArray {
        // On Android or when DJL is not available, use ExecuTorch
        if (DeepLearningEngine.isExecuTorchAvailable && !DeepLearningEngine.isInitialized) {
            if (execuTorchModel == null) {
                execuTorchModel = ExecuTorchModel(name, FloatArrayInAndOutTranslator(), outputs, parent)
                // Load the model if not already loaded
                try {
                    execuTorchModel?.load(name)
                } catch (e: Exception) {
                    // Fall through to DJL if ExecuTorch model loading fails
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
    
    override fun close() {
        execuTorchModel?.close()
        execuTorchModel = null
        super.close()
    }
}
