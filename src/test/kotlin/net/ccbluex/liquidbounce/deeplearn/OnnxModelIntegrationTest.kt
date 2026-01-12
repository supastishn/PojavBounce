package net.ccbluex.liquidbounce.deeplearn

import net.ccbluex.liquidbounce.deeplearn.models.OnnxModel
import net.ccbluex.liquidbounce.deeplearn.ModelHolster
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Test

class OnnxModelIntegrationTest {

    @Test
    fun `onnx models load when runtime available`() {
        // Skip test if ONNX runtime is not available on the test host
        try {
            ai.onnxruntime.OrtEnvironment.getEnvironment()
        } catch (e: Throwable) {
            Assumptions.assumeTrue(false, "ONNX runtime not available: ${e.message}")
            return
        }

        // Find at least one .onnx resource to test
        val foundAny = ModelHolster.baseModels.any { name ->
            val lower = name.lowercase()
            val resourcePath = "/resources/liquidbounce/models/$lower.onnx"
            OnnxModel::class.java.getResourceAsStream(resourcePath) != null
        }

        Assumptions.assumeTrue(foundAny, "No ONNX model resources found in project; skipping integration test")

        // Attempt to load each base model; test only that load() succeeds (no exception)
        for (name in ModelHolster.baseModels) {
            try {
                val model = OnnxModel(name, null)
                model.load(name)
                model.close()
            } catch (t: Throwable) {
                throw AssertionError("Failed to load ONNX model '$name'", t)
            }
        }
    }
}
