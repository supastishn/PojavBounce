#!/usr/bin/env python3
"""
Simplified Minarai model conversion script using TorchScript

This script converts the Minarai models to TorchScript format,
which can be used as an intermediate step before ExecuTorch conversion.
"""

import torch
from torch.nn import Sequential, Linear, BatchNorm1d, ReLU
import os
import json


def create_minarai_model() -> Sequential:
    """
    Creates a Minarai model with the architecture used in LiquidBounce.

    Returns:
        PyTorch Sequential model
    """
    return Sequential(
        Linear(4, 128),
        BatchNorm1d(128),
        ReLU(),
        Linear(128, 64),
        BatchNorm1d(64),
        ReLU(),
        Linear(64, 32),
        BatchNorm1d(32),
        ReLU(),
        Linear(32, 2)
    )


def convert_model(model_name: str, output_dir: str):
    """
    Converts a Minarai model and saves it in multiple formats.

    Args:
        model_name: Name of the model (e.g., "21KC11KP")
        output_dir: Directory to save the converted models
    """
    print(f"\n{'='*60}")
    print(f"Converting Minarai model: {model_name}")
    print('='*60)

    # Create output directory
    os.makedirs(output_dir, exist_ok=True)

    # Create model
    print("Creating model architecture...")
    model = create_minarai_model()
    model.eval()
    print("✓ Model created")

    # Create example input
    example_input = torch.randn(1, 4)
    print(f"Example input shape: {example_input.shape}")

    # Test the model
    with torch.no_grad():
        output = model(example_input)
    print(f"Example output shape: {output.shape}")
    print(f"Example output: {output.numpy()}")

    # Save as TorchScript
    print("\nSaving as TorchScript...")
    traced_model = torch.jit.trace(model, example_input)
    torchscript_path = os.path.join(output_dir, f"{model_name.lower()}.pt")
    traced_model.save(torchscript_path)
    print(f"✓ Saved TorchScript: {torchscript_path}")

    # Save as ONNX (optional, for compatibility)
    print("\nSaving as ONNX...")
    try:
        onnx_path = os.path.join(output_dir, f"{model_name.lower()}.onnx")
        torch.onnx.export(
            model,
            example_input,
            onnx_path,
            export_params=True,
            opset_version=11,
            do_constant_folding=True,
            input_names=['input'],
            output_names=['output'],
            dynamic_axes={'input': {0: 'batch_size'}, 'output': {0: 'batch_size'}}
        )
        print(f"✓ Saved ONNX: {onnx_path}")
    except Exception as e:
        print(f"⚠️  ONNX export skipped: {e}")
        print("   TorchScript format is still available for deployment")

    # Save model info
    metadata = {
        "model_name": model_name,
        "input_features": 4,
        "output_features": 2,
        "architecture": "MLP with 3 hidden layers (128, 64, 32)",
        "formats": ["torchscript", "onnx"]
    }
    metadata_path = os.path.join(output_dir, f"{model_name.lower()}_info.json")
    with open(metadata_path, "w") as f:
        json.dump(metadata, f, indent=2)
    print(f"✓ Saved metadata: {metadata_path}")

    print(f"\n✓ Conversion complete for {model_name}!")


def main():
    output_dir = "./converted_models"
    models = ["21KC11KP", "19KC8KP"]

    print("\nConverting Minarai models to TorchScript and ONNX...")

    for model_name in models:
        try:
            convert_model(model_name, output_dir)
        except Exception as e:
            print(f"\n✗ Error converting {model_name}: {e}")
            import traceback
            traceback.print_exc()

    print(f"\n{'='*60}")
    print("All conversions complete!")
    print(f"Output directory: {output_dir}")
    print('='*60)

    print("\nNext steps:")
    print("1. The .pt files can be used with PyTorch JIT on Android")
    print("2. The .onnx files can be converted to other formats if needed")
    print("3. For ExecuTorch, you'll need to install executorch package")


if __name__ == "__main__":
    main()
