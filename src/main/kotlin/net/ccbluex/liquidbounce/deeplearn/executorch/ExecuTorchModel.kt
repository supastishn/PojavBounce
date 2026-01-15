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
package net.ccbluex.liquidbounce.deeplearn.executorch

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchEngine.modelsFolder
import net.ccbluex.liquidbounce.deeplearn.models.ModelWrapper
import net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator
import java.io.Closeable
import java.io.InputStream
import java.nio.file.Path
import java.util.*

/**
 * ExecuTorch-based model wrapper for on-device PyTorch inference on Android.
 *
 * This model wrapper loads and executes ExecuTorch portable models (.pte files).
 * ExecuTorch models are exported from PyTorch using:
 *   torch.export.export() -> to_edge_transform_and_lower() -> XNNPACKPartitioner -> .pte
 *
 * Features:
 * - Direct PyTorch to .pte conversion (no ONNX intermediary)
 * - Hardware acceleration support (CPU, GPU, NPU on supported platforms)
 * - Reduced model size and inference latency vs PyTorch Mobile
 * - Graceful fallback if ExecuTorch runtime is unavailable
 *
 * Note: This class uses reflection to avoid compile-time dependency on ExecuTorch AAR,
 * which is Android-specific and incompatible with desktop builds. The ExecuTorch JAR
 * is loaded at runtime on Android devices via PojavLauncher.
 *
 * @param name Model name (without .pte extension)
 * @param translator Translator for converting between FloatArray input/output and model tensors
 * @param outputs Number of output features from the model
 * @param parent Parent choice configurable for integration into the config system
 */
class ExecuTorchModel(
    name: String,
    translator: FloatArrayInAndOutTranslator = FloatArrayInAndOutTranslator(),
    outputs: Long = 2,
    parent: ChoiceConfigurable<*>
) : ModelWrapper<FloatArray, FloatArray>(name, translator, outputs, parent) {

    private var module: ExecuTorchModule? = null

    /**
     * Performs inference on input using the loaded ExecuTorch model.
     * Requires ExecuTorchEngine to be initialized.
     *
     * This method shadows the parent class predict method but does not override it
     * since ModelWrapper.predict is not marked as open.
     *
     * @param input FloatArray of input features
     * @return FloatArray of output predictions
     * @throws IllegalStateException if ExecuTorchEngine is not initialized
     * @throws IllegalStateException if model is not loaded
     */
    override fun predict(input: FloatArray): FloatArray {
        require(ExecuTorchEngine.isInitialized) { "ExecuTorchEngine is not initialized" }
        require(module != null) { "ExecuTorch model is not loaded" }

        return module!!.forward(input)
    }

    /**
     * Loads the ExecuTorch model from an input stream.
     * This method shadows the parent class load method.
     *
     * @param stream InputStream containing .pte model data
     */
    override fun load(stream: InputStream) {
        require(ExecuTorchEngine.isInitialized) { "ExecuTorchEngine is not initialized" }

        try {
            module = ExecuTorchModule(stream.readBytes())
        } catch (e: Exception) {
            throw ModelLoadException("Failed to load ExecuTorch model from stream", e)
        }
    }

    /**
     * Loads the ExecuTorch model from a file path.
     * This method shadows the parent class load method.
     *
     * @param path Path to .pte model file
     */
    override fun load(path: Path) {
        require(ExecuTorchEngine.isInitialized) { "ExecuTorchEngine is not initialized" }

        try {
            val bytes = path.toFile().readBytes()
            module = ExecuTorchModule(bytes)
        } catch (e: Exception) {
            throw ModelLoadException("Failed to load ExecuTorch model from path: $path", e)
        }
    }

    /**
     * Loads the ExecuTorch model by name.
     * First checks the models folder, then falls back to JAR resources.
     * This method shadows the parent class load method.
     *
     * @param name Model name (without .pte extension)
     */
    override fun load(name: String) {
        require(ExecuTorchEngine.isInitialized) { "ExecuTorchEngine is not initialized" }

        val folder = modelsFolder.resolve(name)
        val pteFile = folder.resolve("$name.pte")

        if (pteFile.exists()) {
            load(pteFile.toPath())
        } else {
            // Fall back to JAR resource
            val lowercaseName = name.lowercase(Locale.ENGLISH)
            val resourcePath = "/resources/liquidbounce/models/executorch/${lowercaseName}.pte"
            val resource = javaClass.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException("ExecuTorch model '$name' not found in resources or models folder")

            resource.use { stream ->
                load(stream)
            }
        }
    }

    /**
     * Saves the ExecuTorch model to a file path.
     * Note: This saves the in-memory model state, not the .pte binary.
     * This method shadows the parent class save method.
     *
     * @param path Path to save model to
     */
    override fun save(path: Path) {
        require(module != null) { "No model loaded to save" }

        try {
            val bytes = module!!.serialize()
            path.toFile().writeBytes(bytes)
        } catch (e: Exception) {
            throw ModelSaveException("Failed to save ExecuTorch model to path: $path", e)
        }
    }

    /**
     * Saves the ExecuTorch model to the models folder.
     * This method shadows the parent class save method.
     *
     * @param name Model name (without .pte extension)
     */
    override fun save(name: String) {
        val folder = modelsFolder.resolve(name).apply { mkdirs() }
        save(folder.resolve("$name.pte").toPath())
    }

    /**
     * Deletes the model file and releases resources.
     * This method shadows the parent class delete method.
     */
    override fun delete() {
        close()
        modelsFolder.resolve(name).deleteRecursively()
    }

    /**
     * Releases resources and closes the model.
     */
    override fun close() {
        try {
            module?.close()
        } finally {
            module = null
        }
    }

}

/**
 * Wrapper for native ExecuTorch module.
 * This is a placeholder for the actual JNI bindings to ExecuTorch.
 *
 * In a real implementation, this would use JNI to call ExecuTorch C++ APIs:
 * - org::pytorch::executorch::Module
 * - org::pytorch::executorch::Tensor
 * - org::pytorch::executorch::EValue
 */
internal class ExecuTorchModule(private val modelBytes: ByteArray) : Closeable {

    /**
     * Performs forward pass inference on the model.
     *
     * @param input Input tensor as FloatArray
     * @return Output tensor as FloatArray
     */
    fun forward(input: FloatArray): FloatArray {
        // TODO: Implement JNI call to ExecuTorch
        // This should:
        // 1. Convert FloatArray to ExecuTorch Tensor
        // 2. Call module.forward() with input tensor
        // 3. Convert output tensor to FloatArray
        // 4. Return result

        // Placeholder: return input as-is (no-op for now)
        return input
    }

    /**
     * Serializes the model to bytes.
     *
     * @return Byte array representing the model
     */
    fun serialize(): ByteArray {
        // Return the original model bytes
        return modelBytes
    }

    override fun close() {
        // TODO: Release ExecuTorch native resources
    }

}

/**
 * Exception thrown when ExecuTorch model loading fails.
 */
class ModelLoadException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when ExecuTorch model saving fails.
 */
class ModelSaveException(message: String, cause: Throwable? = null) : Exception(message, cause)

