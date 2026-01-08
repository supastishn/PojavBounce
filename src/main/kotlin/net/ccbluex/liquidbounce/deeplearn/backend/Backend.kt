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
package net.ccbluex.liquidbounce.deeplearn.backend

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.integration.task.type.Task
import java.io.File
import java.io.InputStream
import java.nio.file.Path

/**
 * Enum representing available deep learning backends.
 */
enum class Backend {
    /**
     * DJL (Deep Java Library) with PyTorch engine.
     * Supports training and inference. Desktop only.
     */
    DJL,

    /**
     * ONNX Runtime Mobile.
     * Supports inference only. Optimized for Android/mobile platforms.
     */
    ONNX,

    /**
     * No backend available (deep learning disabled).
     */
    NONE
}

/**
 * Interface for deep learning model implementations.
 * Abstracts DJL and ONNX Runtime backends.
 */
interface DeepModel {
    /**
     * Model name
     */
    val name: String

    /**
     * Predict output from input array.
     * @param input FloatArray of length 6 (angles, velocity, distance)
     * @return FloatArray of length 2 (yaw/pitch delta)
     */
    fun predict(input: FloatArray): FloatArray

    /**
     * Load model from input stream.
     * @param stream Model data stream
     */
    fun load(stream: InputStream)

    /**
     * Load model from file path.
     * @param path Model file path
     */
    fun load(path: Path)

    /**
     * Load model by name (from resources or models folder).
     * @param name Model name
     */
    fun load(name: String)

    /**
     * Save model to file path.
     * @param path Destination path
     */
    fun save(path: Path)

    /**
     * Save model by name to models folder.
     * @param name Model name
     */
    fun save(name: String)

    /**
     * Train the model with features and labels.
     * Note: Training is only supported on DJL backend (desktop).
     * @param features Training input data
     * @param labels Training output labels
     * @throws UnsupportedOperationException if backend doesn't support training
     */
    fun train(features: Array<FloatArray>, labels: Array<FloatArray>)

    /**
     * Delete model from storage.
     */
    fun delete()

    /**
     * Close and release model resources.
     */
    fun close()
}

/**
 * Interface for backend managers.
 * Handles backend initialization and model creation.
 */
interface BackendManager {
    /**
     * Backend name for logging
     */
    val name: String

    /**
     * Initialize the backend.
     * @param task Task for progress tracking
     * @param cacheFolder Folder for caching native libraries/models
     * @return true if initialization succeeded
     */
    suspend fun init(task: Task?, cacheFolder: File): Boolean

    /**
     * Check if backend is available/initialized.
     * @return true if backend can be used
     */
    fun available(): Boolean

    /**
     * Create a model instance for this backend.
     * @param name Model name
     * @param parent Parent configurable (for UI integration)
     * @return DeepModel implementation for this backend
     */
    fun createModel(name: String, parent: ChoiceConfigurable<*>?): DeepModel
}
