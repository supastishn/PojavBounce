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
 */
package net.ccbluex.liquidbounce.deeplearn.models

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtException
import ai.onnxruntime.OrtSession
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.io.InputStream
import java.nio.FloatBuffer
import java.nio.file.Path

/**
 * ONNX model implementation using ai.onnxruntime.
 * This implementation supports inference only.
 */
class OnnxModel(
    private val modelName: String,
    val parent: ChoiceConfigurable<*>?
) : net.ccbluex.liquidbounce.deeplearn.backend.DeepModel {

    override val name: String = modelName

    @Volatile
    private var session: OrtSession? = null

    private val env: OrtEnvironment
        get() = OrtEnvironment.getEnvironment()

    override fun predict(input: FloatArray): FloatArray {
        val sess = session ?: throw IllegalStateException("ONNX model is not loaded")

        // Create tensor of shape [1, inputLength]
        val inputShape = longArrayOf(1, input.size.toLong())
        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(input), inputShape)

        return try {
            sess.run(mapOf("input" to tensor)).use { results ->
                val value = results[0].value
                // Commonly ONNX outputs are float[][] or float[] depending on model; handle both
                when (value) {
                    is Array<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val arr = value as Array<FloatArray>
                        arr[0]
                    }
                    is FloatArray -> value
                    else -> throw IllegalStateException("Unexpected ONNX output type: ${value?.javaClass}")
                }
            }
        } catch (e: ai.onnxruntime.OrtException) {
            logger.error("[OnnxModel] Failed to run inference for model '$name'", e)
            throw e
        } finally {
            try { tensor.close() } catch (ignored: Exception) {}
        }
    }

    override fun load(stream: InputStream) {
        // Write to temp file and create session from file
        val tmp = File.createTempFile("onnx_model_", ".onnx")
        stream.use { input -> tmp.outputStream().use { output -> input.copyTo(output) } }
        tmp.deleteOnExit()
        load(tmp.toPath())
    }

    override fun load(path: Path) {
        try {
            session?.close()
        } catch (ignored: Exception) {
        }

        try {
            session = env.createSession(path.toString())
            logger.info("[OnnxModel] Loaded ONNX model from $path")
        } catch (e: Throwable) {
            logger.error("[OnnxModel] Failed to create OrtSession from $path", e)
            throw e
        }
    }

    override fun load(name: String) {
        val folder = modelsFolder.resolve(name)
        val modelFile = folder.resolve("model.onnx")

        if (modelFile.exists()) {
            load(modelFile.toPath())
            return
        }

        val lowercaseName = name.lowercase()
        val resourcePath = "/resources/liquidbounce/models/${lowercaseName}.onnx"
        val stream = javaClass.getResourceAsStream(resourcePath)
            ?: throw IllegalArgumentException("Model resource not found: $resourcePath and $modelFile")

        stream.use { load(it) }
    }

    override fun save(path: Path) {
        throw UnsupportedOperationException("Saving models is not supported on ONNX backend")
    }

    override fun save(name: String) {
        throw UnsupportedOperationException("Saving models is not supported on ONNX backend")
    }

    override fun train(features: Array<FloatArray>, labels: Array<FloatArray>) {
        throw UnsupportedOperationException("Training not supported on ONNX backend")
    }

    override fun delete() {
        // Close session if open
        session = null

        val folder = modelsFolder.resolve(name)
        if (folder.exists()) {
            folder.deleteRecursively()
        }
    }

    override fun close() {
        // Release session
        session = null
    }
}
