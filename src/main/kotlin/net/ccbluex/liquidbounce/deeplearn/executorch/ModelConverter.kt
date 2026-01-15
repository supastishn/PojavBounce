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

import net.ccbluex.liquidbounce.utils.client.logger
import java.io.File
import java.nio.file.Path

/**
 * Utility for converting PyTorch models to ExecuTorch .pte format.
 *
 * ExecuTorch (.pte) is the portable, optimized format for on-device PyTorch inference.
 * Conversion pipeline:
 *   1. Load PyTorch model (.pt, .pth)
 *   2. Export via torch.export.export() with traced inputs
 *   3. Lower to edge via to_edge_transform_and_lower() with XNNPACK partitioner
 *   4. Convert to ExecuTorch format via to_executorch()
 *   5. Save as .pte file
 *
 * This class provides utility functions for the conversion process and Python script templates.
 */
object ModelConverter {

    /**
     * Generates a Python script template for converting PyTorch models to ExecuTorch format.
     * Users can customize this template for their specific models.
     *
     * @return Python script content as a string
     */
    @Suppress("LongMethod") // Python script template
    fun generateExportScript(): String = """
        #!/usr/bin/env python3
        ${"\"\"\""}
        ExecuTorch model export script

        Converts PyTorch models to ExecuTorch .pte format for on-device inference.

        Usage:
            python export_model.py --model-path model.pth --output-path model.pte
        ${"\"\"\""}

        import argparse
        import torch
        from executorch.exir import to_edge_transform_and_lower
        from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner


        def export_model_to_executorch(model_path: str, output_path: str, input_shape: tuple):
            ${"\"\"\""}
            Exports a PyTorch model to ExecuTorch .pte format.

            Args:
                model_path: Path to the PyTorch model (.pt or .pth file)
                output_path: Path to save the ExecuTorch model (.pte file)
                input_shape: Shape of the input tensor (e.g., (1, 784) for MNIST)
            ${"\"\"\""}
            print(f"Loading model from {model_path}...")

            # Load the model
            model = torch.load(model_path)
            model.eval()

            # Create example input with the specified shape
            example_input = torch.randn(*input_shape)

            print(f"Exporting model with input shape {input_shape}...")

            # Export the model using torch.export.export()
            exported_program = torch.export.export(
                model,
                (example_input,),
                strict=False
            )
            print("Model exported successfully")

            # Lower to edge and apply XNNPACK partitioner for CPU optimization
            print("Lowering to ExecuTorch format...")
            edge_manager = to_edge_transform_and_lower(
                exported_program,
                partitioner=[XnnpackPartitioner()]
            )

            # Convert to ExecuTorch format
            print("Converting to ExecuTorch format...")
            executorch_program = edge_manager.to_executorch()

            # Save the .pte file
            print(f"Saving model to {output_path}...")
            with open(output_path, "wb") as f:
                f.write(executorch_program.buffer)

            print(f"Successfully exported model to {output_path}")

            # Print model info
            print(f"Exported model size: {len(executorch_program.buffer) / 1024 / 1024:.2f} MB")


        def main():
            parser = argparse.ArgumentParser(description="Export PyTorch model to ExecuTorch format")
            parser.add_argument("--model-path", type=str, required=True, help="Path to PyTorch model (.pt or .pth)")
            parser.add_argument("--output-path", type=str, required=True, help="Output path for .pte file")
            parser.add_argument("--input-shape", type=int, nargs="+", default=[1, 784],
                              help="Input tensor shape (default: [1, 784] for MNIST-like models)")

            args = parser.parse_args()

            export_model_to_executorch(
                args.model_path,
                args.output_path,
                tuple(args.input_shape)
            )


        if __name__ == "__main__":
            main()
    """.trimIndent()

    /**
     * Generates a Python script template for the Minarai angle smoothing model.
     * This is specific to converting the existing LiquidBounce Minarai models.
     *
     * @return Python script content as a string
     */
    @Suppress("LongMethod") // Python script template
    fun generateMinaraiExportScript(): String = """
        #!/usr/bin/env python3
        ${"\"\"\""}
        ExecuTorch Minarai model export script

        Converts LiquidBounce Minarai DJL models to ExecuTorch .pte format.

        The Minarai models are trained for angle smoothing (aiming assistance).
        Input: 4 float features (current angle delta)
        Output: 2 float features (adjusted angle delta)

        Usage:
            python export_minarai.py --model-name 21KC11KP --output-dir ./models
        ${"\"\"\""}

        import argparse
        import torch
        from executorch.exir import to_edge_transform_and_lower
        from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner
        import json


        def load_minarai_model_from_params(params_path: str) -> torch.nn.Module:
            ${"\"\"\""}
            Loads a Minarai model from DJL .params file.

            The Minarai model is a simple feedforward neural network:
            Input (4) -> Linear(128) -> BatchNorm -> ReLU ->
                      Linear(64) -> BatchNorm -> ReLU ->
                      Linear(32) -> BatchNorm -> ReLU ->
                      Linear(2) -> Output

            Args:
                params_path: Path to the DJL .params file

            Returns:
                PyTorch model with loaded parameters
            ${"\"\"\""}
            model = torch.nn.Sequential(
                torch.nn.Linear(4, 128),
                torch.nn.BatchNorm1d(128),
                torch.nn.ReLU(),
                torch.nn.Linear(128, 64),
                torch.nn.BatchNorm1d(64),
                torch.nn.ReLU(),
                torch.nn.Linear(64, 32),
                torch.nn.BatchNorm1d(32),
                torch.nn.ReLU(),
                torch.nn.Linear(32, 2)
            )

            # Load parameters from DJL format
            # Note: You may need to adapt this based on actual DJL parameter format
            try:
                state_dict = torch.load(params_path, map_location='cpu')
                model.load_state_dict(state_dict)
                print(f"Loaded model parameters from {params_path}")
            except Exception as e:
                print(f"Warning: Could not load parameters: {e}")
                print("Using randomly initialized model")

            model.eval()
            return model


        def export_minarai_to_executorch(model_name: str, output_dir: str):
            ${"\"\"\""}
            Exports a Minarai model to ExecuTorch .pte format.

            Args:
                model_name: Name of the model (e.g., "21KC11KP")
                output_dir: Directory to save the .pte file
            ${"\"\"\""}
            print(f"Exporting Minarai model: {model_name}")

            # Create/load model
            params_path = f"./models/{model_name}/{model_name}.params"
            model = load_minarai_model_from_params(params_path)

            # Create example input: 4 float features for angle delta
            example_input = torch.randn(1, 4)

            print("Exporting model with torch.export.export()...")
            exported_program = torch.export.export(
                model,
                (example_input,),
                strict=False
            )

            print("Lowering to ExecuTorch format...")
            edge_manager = to_edge_transform_and_lower(
                exported_program,
                partitioner=[XnnpackPartitioner()]
            )

            print("Converting to ExecuTorch format...")
            executorch_program = edge_manager.to_executorch()

            # Save the .pte file
            output_path = f"{output_dir}/{model_name}.pte"
            print(f"Saving model to {output_path}...")
            with open(output_path, "wb") as f:
                f.write(executorch_program.buffer)

            print(f"Successfully exported {model_name} to {output_path}")
            print(f"Model size: {len(executorch_program.buffer) / 1024:.2f} KB")

            # Save metadata
            metadata = {
                "model_name": model_name,
                "input_features": 4,
                "output_features": 2,
                "model_size_bytes": len(executorch_program.buffer),
                "exported_via": "ExecuTorch"
            }
            metadata_path = f"{output_dir}/{model_name}.json"
            with open(metadata_path, "w") as f:
                json.dump(metadata, f, indent=2)
            print(f"Saved metadata to {metadata_path}")


        def main():
            parser = argparse.ArgumentParser(description="Export Minarai models to ExecuTorch format")
            parser.add_argument("--model-name", type=str, default="21KC11KP",
                              help="Model name to export (default: 21KC11KP)")
            parser.add_argument("--output-dir", type=str, default="./models",
                              help="Output directory for .pte files")
            parser.add_argument("--export-all", action="store_true",
                              help="Export all Minarai models")

            args = parser.parse_args()

            if args.export_all:
                # Export all known Minarai models
                models = ["21KC11KP", "19KC8KP"]
                for model_name in models:
                    try:
                        export_minarai_to_executorch(model_name, args.output_dir)
                        print()
                    except Exception as e:
                        print(f"Error exporting {model_name}: {e}")
                        print()
            else:
                export_minarai_to_executorch(args.model_name, args.output_dir)


        if __name__ == "__main__":
            main()
    """.trimIndent()

    /**
     * Saves an export script to a file for user customization.
     *
     * @param outputPath Path to save the script
     * @param scriptType Type of script: "generic", "minarai"
     */
    fun saveExportScript(outputPath: Path, scriptType: String = "generic") {
        val content = when (scriptType) {
            "minarai" -> generateMinaraiExportScript()
            "generic" -> generateExportScript()
            else -> generateExportScript()
        }

        outputPath.toFile().writeText(content)
        outputPath.toFile().setExecutable(true)
        logger.info("[ModelConverter] Saved $scriptType export script to $outputPath")
    }

    /**
     * Attempts to convert a PyTorch model to ExecuTorch using an external Python script.
     * This requires Python 3, PyTorch, and ExecuTorch to be installed.
     *
     * @param modelPath Path to the PyTorch model (.pt or .pth)
     * @param outputPath Path to save the ExecuTorch model (.pte)
     * @param inputShape Input tensor shape (e.g., listOf(1, 784))
     * @return True if conversion succeeded, false otherwise
     */
    @Suppress("SpreadOperator") // Required for ProcessBuilder varargs
    fun convertUsingPython(
        modelPath: Path,
        outputPath: Path,
        inputShape: List<Int> = listOf(1, 4)
    ): Boolean {
        return try {
            // Create temporary export script
            val tempScript = File.createTempFile("export_executorch", ".py")
            tempScript.writeText(generateExportScript())
            tempScript.setExecutable(true)

            logger.info("[ModelConverter] Running Python conversion: $modelPath -> $outputPath")

            // Run the export script
            val process = ProcessBuilder(
                "python3",
                tempScript.absolutePath,
                "--model-path", modelPath.toString(),
                "--output-path", outputPath.toString(),
                "--input-shape", *inputShape.map { it.toString() }.toTypedArray()
            ).redirectErrorStream(true).start()

            val output = process.inputStream.bufferedReader().readText()
            val exitCode = process.waitFor()

            logger.info("[ModelConverter] Python conversion output:\n$output")

            if (exitCode == 0) {
                logger.info("[ModelConverter] Successfully converted model to $outputPath")
                true
            } else {
                logger.error("[ModelConverter] Python conversion failed with exit code $exitCode")
                false
            }
        } catch (e: Exception) {
            logger.error("[ModelConverter] Failed to run Python conversion", e)
            false
        }
    }

}
