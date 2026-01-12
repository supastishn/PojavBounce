package net.ccbluex.liquidbounce.deeplearn.tools

import ai.djl.Model
import ai.djl.nn.Activation
import ai.djl.nn.Blocks
import ai.djl.nn.SequentialBlock
import ai.djl.nn.core.Linear
import ai.djl.nn.norm.BatchNorm
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

// Small utility to export DJL-packaged models (resources/*.params) into TensorFlow SavedModel directories.
//
// Usage from project root:
// ./gradlew exportDjlSavedModels
//
// Output is written to build/exports/savedmodels/<MODEL_NAME>.
object ExportDjlToSavedModel {

    @JvmStatic
    fun main(args: Array<String>) {
        val outRoot: Path = Paths.get("build/exports/savedmodels")
        Files.createDirectories(outRoot)

        // Use base models (avoid referencing ModelHolster to prevent Minecraft bootstrap during static init)
        val baseModels = listOf("21KC11KP", "19KC8KP")
        val modelNames = if (args.isNotEmpty()) args.toList() else baseModels

        println("Exporting DJL models to TensorFlow SavedModel format into: $outRoot")

        for (name in modelNames) {
            try {
                val lower = name.lowercase()
                val resourcePath = "/resources/liquidbounce/models/$lower.params"
                val stream = ExportDjlToSavedModel::class.java.getResourceAsStream(resourcePath)

                if (stream == null) {
                    println("[Export] Resource not found for model $name at $resourcePath; skipping")
                    continue
                }

                println("[Export] Loading model $name from resource $resourcePath")

                val model = Model.newInstance(name).apply { block = createMlpBlock(2) }
                model.load(stream)

                val outDir = outRoot.resolve(name)
                Files.createDirectories(outDir)

                println("[Export] Saving model $name as SavedModel to $outDir")
                model.save(outDir, "tf")

                model.close()
                println("[Export] Done $name")
            } catch (t: Throwable) {
                System.err.println("[Export] Failed to export model $name: ${t.message}")
                t.printStackTrace()
            }
        }

        println("All exports finished")
    }

    private fun createMlpBlock(outputs: Long) = SequentialBlock()
        .add(Linear.builder().setUnits(128).build())
        .add(Blocks.batchFlattenBlock())
        .add(BatchNorm.builder().build())
        .add(Activation.reluBlock())
        .add(Linear.builder().setUnits(64).build())
        .add(Blocks.batchFlattenBlock())
        .add(BatchNorm.builder().build())
        .add(Activation.reluBlock())
        .add(Linear.builder().setUnits(32).build())
        .add(Blocks.batchFlattenBlock())
        .add(BatchNorm.builder().build())
        .add(Activation.reluBlock())
        .add(Linear.builder().setUnits(outputs).build())
}
