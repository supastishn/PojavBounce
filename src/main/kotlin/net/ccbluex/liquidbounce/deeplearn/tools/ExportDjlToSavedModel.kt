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

                // Read the resource bytes once so we can both dump them for debugging and try multiple
                // loading strategies (stream-based and path-based).
                val resourceBytes = ExportDjlToSavedModel::class.java.getResourceAsStream(resourcePath)?.readAllBytes()
                if (resourceBytes == null) {
                    println("[Export] Resource not found for model $name at $resourcePath; skipping")
                    continue
                }

                // Write a debug dump so the CI logs / artifacts can be inspected if load fails
                val debugDir = outRoot.resolve("../export-debug").normalize()
                Files.createDirectories(debugDir)
                val dumpFile = debugDir.resolve("${name}.params.dump")
                Files.write(dumpFile, resourceBytes)
                println("[Export] Wrote debug params to: $dumpFile (size=${resourceBytes.size})")

                // Try loading using a ByteArrayInputStream first. If that fails, try writing to a temp file and
                // loading via path (some engines expect a file path).
                var model = Model.newInstance(name)
                var loaded = false

                try {
                    model.load(java.io.ByteArrayInputStream(resourceBytes))
                    loaded = true
                } catch (t: Throwable) {
                    System.err.println("[Export] model.load failed (stream): ${t.message}. Trying path-based load and TensorFlow fallback...")
                    t.printStackTrace()
                    try {
                        model.close()
                    } catch (_: Exception) { }

                    // Try a path-based load after setting TensorFlow as preferred engine (some resources are TF-specific)
                    System.setProperty("DJL_DEFAULT_ENGINE", "TensorFlow")
                    System.setProperty("TF_FLAVOR", "cpu")
                    model = Model.newInstance(name)

                    val tmpFile = debugDir.resolve("${name}.params.tmp")
                    Files.write(tmpFile, resourceBytes)
                    try {
                        model.load(tmpFile)
                        loaded = true
                    } catch (t2: Throwable) {
                        System.err.println("[Export] model.load failed (path): ${t2.message}")
                        t2.printStackTrace()
                    }
                }

                if (!loaded) {
                    System.err.println("[Export] Skipping model $name: unable to load resource as a DJL model (tried stream and path)")
                    continue
                }

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
