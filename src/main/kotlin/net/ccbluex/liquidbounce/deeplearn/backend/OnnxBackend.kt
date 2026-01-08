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
import net.ccbluex.liquidbounce.deeplearn.utils.NativeLibExtractor
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

/**
 * Backend manager for ONNX Runtime (mobile / Android).
 * Responsible for extracting native libraries and creating OnnxModel instances.
 */
object OnnxBackend : BackendManager {

    override val name: String = "ONNX Runtime"

    @Volatile
    private var initialized: Boolean = false

    override suspend fun init(task: Task?, cacheFolder: File): Boolean {
        logger.info("[OnnxBackend] Initializing ONNX Runtime backend (cacheFolder=${cacheFolder.absolutePath})")

        try {
            // Extract and load native libraries from resources to the engines cache folder
            val extractedAndLoaded = NativeLibExtractor.extractAndLoad(cacheFolder, listOf("libonnxruntime.so"))

            if (!extractedAndLoaded) {
                logger.error("[OnnxBackend] Failed to extract or load native ONNX Runtime libraries")
                return false
            }

            // Try to get environment. This will verify that the native libraries are loadable.
            try {
                // Use OrtEnvironment lazily where needed in OnnxModel; just check we can obtain it here.
                ai.onnxruntime.OrtEnvironment.getEnvironment()
            } catch (e: Throwable) {
                logger.error("[OnnxBackend] OrtEnvironment initialization failed", e)
                logger.error("[OnnxBackend] Diagnostics: ${NativeLibExtractor.collectDiagnostics(cacheFolder)}")
                return false
            }

            initialized = true
            logger.info("[OnnxBackend] ONNX Runtime initialized successfully")
            return true
        } catch (t: Throwable) {
            logger.error("[OnnxBackend] Unexpected error while initializing ONNX backend", t)
            logger.error("[OnnxBackend] Diagnostics: ${NativeLibExtractor.collectDiagnostics(cacheFolder)}")
            initialized = false
            return false
        }
    }

    override fun available(): Boolean = initialized

    override fun createModel(name: String, parent: ChoiceConfigurable<*>?): DeepModel {
        return OnnxModel(name, parent)
    }
}
