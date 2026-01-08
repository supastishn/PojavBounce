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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.deeplearn.models.DjlModel
import net.ccbluex.liquidbounce.integration.task.type.Task
import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File

/**
 * Backend manager for DJL (PyTorch). Used on desktop for training + inference.
 */
object DjlBackend : BackendManager {

    override val name: String = "DJL (PyTorch)"

    @Volatile
    private var initialized: Boolean = false

    override suspend fun init(task: Task?, cacheFolder: File): Boolean {
        logger.info("[DjlBackend] Initializing DJL engine")

        return try {
            val engine = withContext(Dispatchers.IO) {
                ai.djl.engine.Engine.getInstance()
            }

            logger.info("[DjlBackend] Using engine ${engine.engineName} ${engine.version}")
            initialized = true
            true
        } catch (t: Throwable) {
            logger.error("[DjlBackend] Failed to initialize DJL engine", t)
            initialized = false
            false
        }
    }

    override fun available(): Boolean = initialized

    override fun createModel(name: String, parent: ChoiceConfigurable<*>?): DeepModel {
        // For now we only support the Minarai model type (FloatArray in/out, 2 outputs).
        return DjlModel(name, net.ccbluex.liquidbounce.deeplearn.translators.FloatArrayInAndOutTranslator(), 2, parent!!)
    }
}
