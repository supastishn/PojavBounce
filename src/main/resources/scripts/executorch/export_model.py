#!/usr/bin/env python3
"""
ExecuTorch model export script

Converts PyTorch models to ExecuTorch .pte format for on-device inference.

Usage:
    python export_model.py --model-path model.pth --output-path model.pte

Requirements:
    pip install torch executorch executorch-backends-xnnpack

Example for LiquidBounce Minarai models:
    python export_model.py \
        --model-path 21KC11KP.pth \
        --output-path 21KC11KP.pte \
        --input-shape 1 4
"""

import argparse
import torch
from executorch.exir import to_edge_transform_and_lower
from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner


def export_model_to_executorch(model_path: str, output_path: str, input_shape: tuple):
    """
    Exports a PyTorch model to ExecuTorch .pte format.

    Args:
        model_path: Path to the PyTorch model (.pt or .pth file)
        output_path: Path to save the ExecuTorch model (.pte file)
        input_shape: Shape of the input tensor (e.g., (1, 784) for MNIST)
    """
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
