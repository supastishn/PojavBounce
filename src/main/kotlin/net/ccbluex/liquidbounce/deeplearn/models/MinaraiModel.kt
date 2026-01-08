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
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine

class MinaraiModel(
    name: String,
    override val parent: ChoiceConfigurable<*>
) : Choice(name) {

    private var implementation: net.ccbluex.liquidbounce.deeplearn.backend.DeepModel? = null

    /** Predict using the currently loaded backend implementation */
    fun predict(input: FloatArray): FloatArray {
        val impl = implementation ?: throw IllegalStateException("Model '$name' is not loaded")
        return impl.predict(input)
    }

    /** Load the model using the appropriate backend (ONNX on Android, DJL on desktop) */
    fun load() {
        // Close previous implementation if present
        implementation?.close()

        implementation = when (DeepLearningEngine.backend) {
            net.ccbluex.liquidbounce.deeplearn.backend.Backend.ONNX -> net.ccbluex.liquidbounce.deeplearn.backend.OnnxBackend.createModel(name, parent)
            net.ccbluex.liquidbounce.deeplearn.backend.Backend.DJL -> net.ccbluex.liquidbounce.deeplearn.backend.DjlBackend.createModel(name, parent)
            net.ccbluex.liquidbounce.deeplearn.backend.Backend.NONE -> throw IllegalStateException("No deep learning backend available")
        }

        try {
            implementation!!.load(name)
        } catch (e: Exception) {
            net.ccbluex.liquidbounce.utils.client.logger.error("[MinaraiModel] Failed to load model '$name'", e)
            implementation!!.close()
            implementation = null
            throw e
        }
    }

    fun delete() {
        implementation?.delete() ?: run {
            val folder = DeepLearningEngine.modelsFolder.resolve(name)
            if (folder.exists()) folder.deleteRecursively()
        }
    }

    fun close() {
        implementation?.close()
        implementation = null
    }

    /** Train the model (delegates to backend implementation) */
    fun train(features: Array<FloatArray>, labels: Array<FloatArray>) {
        val impl = implementation ?: throw IllegalStateException("Model '$name' is not loaded")
        impl.train(features, labels)
    }

    /** Save the model (delegates to backend implementation) */
    fun save(name: String) {
        val impl = implementation ?: throw IllegalStateException("Model '$this.name' is not loaded")
        impl.save(name)
    }

    /** Save the model with its current name */
    fun save() {
        save(name)
    }
}