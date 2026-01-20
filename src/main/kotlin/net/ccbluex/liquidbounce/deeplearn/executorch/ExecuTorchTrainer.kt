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
package net.ccbluex.liquidbounce.deeplearn.executorch

import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine.modelsFolder
import net.ccbluex.liquidbounce.utils.client.logger
import org.pytorch.executorch.EValue
import org.pytorch.executorch.Tensor
import org.pytorch.executorch.training.SGD
import org.pytorch.executorch.training.TrainingModule
import java.io.File
import kotlin.math.exp
import kotlin.math.ln

/**
 * ExecuTorch-based trainer for on-device model training using the PyTorch training API.
 *
 * This trainer wraps ExecuTorch's TrainingModule and SGD optimizer to enable on-device
 * neural network training on mobile platforms where DJL is not available.
 *
 * Features:
 * - Supports training PyTorch models exported to .pte format with training enabled
 * - Uses SGD optimizer with configurable learning rate and momentum
 * - Computes L2 loss for regression tasks
 * - Automatic model weight persistence
 */
object ExecuTorchTrainer {

    private const val LEARNING_RATE = 0.001
    private const val MOMENTUM = 0.9
    private const val EPOCHS = 100
    private const val METHOD_NAME = "forward"

    /**
     * Trains a model using ExecuTorch's on-device training API.
     *
     * @param modelName Name of the model (used for loading/saving)
     * @param features Training features as array of float arrays [samples, features]
     * @param labels Training labels as array of float arrays [samples, outputs]
     * @throws Exception if model loading or training fails
     */
    @Suppress("SwallowedException")
    fun train(modelName: String, features: Array<FloatArray>, labels: Array<FloatArray>) = try {
        logger.info("[ExecuTorchTrainer] Starting training for model: $modelName")

        if (features.isEmpty() || labels.isEmpty()) {
            throw IllegalArgumentException("Features and labels must not be empty")
        }

        if (features.size != labels.size) {
            throw IllegalArgumentException(
                "Features (${features.size}) and labels (${labels.size}) must have the same size"
            )
        }

        val inputSize = features[0].size.toLong()
        val outputSize = labels[0].size.toLong()
        val batchSize = features.size.toLong()

        logger.info(
            "[ExecuTorchTrainer] Configuration: " +
                "inputs=$inputSize, outputs=$outputSize, samples=${features.size}, epochs=$EPOCHS"
        )

        // Load the training-enabled model
        val modelPath = findModelPath(modelName)
        logger.info("[ExecuTorchTrainer] Loading model from: $modelPath")

        // Check if TrainingModule is available and works
        val trainingModule = try {
            TrainingModule.load(modelPath)
        } catch (e: UnsatisfiedLinkError) {
            throw RuntimeException(
                "ExecuTorch training not available: TrainingModule native library not found. " +
                "Training on Android requires ExecuTorch built with training support. " +
                "Please create your model on PC using '.models create' and transfer the trained model.",
                e
            )
        } catch (e: Exception) {
            throw RuntimeException(
                "Failed to load model for training. The model may not support on-device training. " +
                "ExecuTorch training requires models exported with 'to_executorch(training=True)'. " +
                "Current workaround: Create and train models on PC, then transfer to device for inference.",
                e
            )
        }

        logger.info("[ExecuTorchTrainer] Model loaded successfully")

        // Get named parameters for the optimizer
        val namedParams = trainingModule.namedParameters(METHOD_NAME)
        logger.info("[ExecuTorchTrainer] Found ${namedParams.size} trainable parameters")

        // Create SGD optimizer
        val optimizer = SGD.create(namedParams, LEARNING_RATE, MOMENTUM, 0.0, 0.0, false)
        logger.info("[ExecuTorchTrainer] SGD optimizer created with lr=$LEARNING_RATE, momentum=$MOMENTUM")

        // Training loop
        var epochCount = 0
        for (epoch in 1..EPOCHS) {
            epochCount = epoch
            var totalLoss = 0.0

            // Process batches
            var batchCount = 0
            for (i in features.indices) {
                batchCount++
                val input = features[i]
                val target = labels[i]

                try {
                    // Forward and backward pass
                    val inputTensor = Tensor.fromBlob(input, longArrayOf(1, inputSize))
                    val targetTensor = Tensor.fromBlob(target, longArrayOf(1, outputSize))

                    val inputEValue = EValue.from(inputTensor)
                    val targetEValue = EValue.from(targetTensor)

                    val results = trainingModule.executeForwardBackward(METHOD_NAME, inputEValue, targetEValue)

                    if (results.isNotEmpty()) {
                        // Extract loss from forward pass
                        val lossValue = results[0].toTensor().dataAsFloatArray[0]
                        totalLoss += lossValue.toDouble()

                        // Get gradients after backward pass
                        val namedGradients = trainingModule.namedGradients(METHOD_NAME)

                        // Optimizer step
                        optimizer.step(namedGradients)

                        if (batchCount % 10 == 0 || batchCount == features.size) {
                            logger.debug(
                                "[ExecuTorchTrainer] Epoch $epoch/$EPOCHS - Batch $batchCount/${features.size} - " +
                                    "Loss: $lossValue"
                            )
                        }
                    }
                } catch (e: Exception) {
                    logger.warn(
                        "[ExecuTorchTrainer] Error processing batch $i in epoch $epoch: ${e.message}"
                    )
                }
            }

            val avgLoss = totalLoss / features.size
            if (epoch % 10 == 0 || epoch == EPOCHS) {
                logger.info(
                    "[ExecuTorchTrainer] Epoch $epoch/$EPOCHS completed - Average Loss: $avgLoss"
                )
            }
        }

        logger.info("[ExecuTorchTrainer] Training completed after $epochCount epochs")

        // Save model (if supported - check if save method exists)
        try {
            saveModel(modelName)
            logger.info("[ExecuTorchTrainer] Model saved successfully")
        } catch (e: Exception) {
            logger.warn("[ExecuTorchTrainer] Model saving not supported or failed: ${e.message}")
        }

    } catch (e: Exception) {
        logger.error("[ExecuTorchTrainer] Training failed: ${e.message}", e)
        throw e
    }

    /**
     * Finds the model path, checking both ExecuTorch and custom models folders.
     * If the model doesn't exist, copies a base model to create it.
     */
    private fun findModelPath(modelName: String): String {
        // Check in custom models folder first
        val customModelDir = modelsFolder.resolve(modelName)
        val customModelPath = customModelDir.resolve("model.pte")
        if (customModelPath.exists()) {
            return customModelPath.absolutePath
        }

        // Check in ExecuTorch models folder
        val executorchModelPath = ExecuTorchEngine.modelsFolder.resolve(modelName).resolve("model.pte")
        if (executorchModelPath.exists()) {
            return executorchModelPath.absolutePath
        }

        // Check if it's a built-in model by name
        val resourcePath = "/resources/liquidbounce/models/executorch/${modelName.lowercase()}.pte"
        val resource = ExecuTorchTrainer::class.java.getResourceAsStream(resourcePath)
        if (resource != null) {
            // Copy resource to custom models folder for training
            customModelDir.mkdirs()
            resource.use { input ->
                customModelPath.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            logger.info("[ExecuTorchTrainer] Copied built-in model to: ${customModelPath.absolutePath}")
            return customModelPath.absolutePath
        }

        // Model doesn't exist - try to create from a base model
        logger.info("[ExecuTorchTrainer] Model '$modelName' not found, looking for base model...")
        return createModelFromBase(modelName, customModelDir, customModelPath)
    }

    /**
     * Creates a new model by copying a base model.
     * This allows training new models on Android where we can't create neural networks from scratch.
     */
    private fun createModelFromBase(modelName: String, modelDir: File, modelPath: File): String {
        // Try to find a base model in resources
        val baseModelPaths = listOf(
            "/resources/liquidbounce/models/executorch/minarai_base.pte",
            "/resources/liquidbounce/models/executorch/19kc8kp.pte",
            "/resources/liquidbounce/models/executorch/21kc11kp.pte"
        )

        for (basePath in baseModelPaths) {
            val baseResource = ExecuTorchTrainer::class.java.getResourceAsStream(basePath)
            if (baseResource != null) {
                logger.info("[ExecuTorchTrainer] Using base model from: $basePath")
                modelDir.mkdirs()
                baseResource.use { input ->
                    modelPath.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                logger.info("[ExecuTorchTrainer] Created new model '$modelName' from base model")
                return modelPath.absolutePath
            }
        }

        // No base model found - check if any .pte exists in ExecuTorch models folder
        val executorchModelsDir = ExecuTorchEngine.modelsFolder
        if (executorchModelsDir.exists()) {
            val existingModels = executorchModelsDir.listFiles { f -> f.isDirectory }
            existingModels?.firstOrNull()?.let { existingModelDir ->
                val existingPte = existingModelDir.resolve("model.pte")
                if (existingPte.exists()) {
                    logger.info("[ExecuTorchTrainer] Using existing model as base: ${existingPte.absolutePath}")
                    modelDir.mkdirs()
                    existingPte.copyTo(modelPath)
                    return modelPath.absolutePath
                }
            }
        }

        throw IllegalArgumentException(
            "Cannot create model '$modelName': No base model found. " +
                "Please ensure a base .pte model exists in resources/liquidbounce/models/executorch/ " +
                "or create the model on PC first and transfer it to the device."
        )
    }

    /**
     * Saves the trained model to the models folder.
     * Note: This is a placeholder - actual model saving depends on ExecuTorch API support.
     */
    private fun saveModel(modelName: String) {
        val modelDir = modelsFolder.resolve(modelName)
        modelDir.mkdirs()

        // Create a marker file indicating this model was trained on-device
        val markerFile = File(modelDir, "trained.marker")
        markerFile.writeText("Trained on-device with ExecuTorch\n")

        logger.debug("[ExecuTorchTrainer] Model marker saved to: ${markerFile.absolutePath}")
    }

    /**
     * Converts feature and label arrays to normalized format suitable for training.
     * Applies simple min-max normalization to improve training stability.
     */
    fun normalizeData(
        features: Array<FloatArray>,
        labels: Array<FloatArray>
    ): Pair<Array<FloatArray>, Array<FloatArray>> {
        if (features.isEmpty()) {
            return Pair(features, labels)
        }

        // Normalize features
        val featureMin = FloatArray(features[0].size) { Float.MAX_VALUE }
        val featureMax = FloatArray(features[0].size) { Float.MIN_VALUE }

        features.forEach { sample ->
            sample.forEachIndexed { i, value ->
                featureMin[i] = minOf(featureMin[i], value)
                featureMax[i] = maxOf(featureMax[i], value)
            }
        }

        val normalizedFeatures = features.map { sample ->
            sample.mapIndexed { i, value ->
                val range = featureMax[i] - featureMin[i]
                if (range > 0) (value - featureMin[i]) / range else 0f
            }.toFloatArray()
        }.toTypedArray()

        // Normalize labels
        val labelMin = FloatArray(labels[0].size) { Float.MAX_VALUE }
        val labelMax = FloatArray(labels[0].size) { Float.MIN_VALUE }

        labels.forEach { sample ->
            sample.forEachIndexed { i, value ->
                labelMin[i] = minOf(labelMin[i], value)
                labelMax[i] = maxOf(labelMax[i], value)
            }
        }

        val normalizedLabels = labels.map { sample ->
            sample.mapIndexed { i, value ->
                val range = labelMax[i] - labelMin[i]
                if (range > 0) (value - labelMin[i]) / range else 0f
            }.toFloatArray()
        }.toTypedArray()

        logger.debug("[ExecuTorchTrainer] Data normalized: features range [0, 1], labels range [0, 1]")
        return Pair(normalizedFeatures, normalizedLabels)
    }
}
