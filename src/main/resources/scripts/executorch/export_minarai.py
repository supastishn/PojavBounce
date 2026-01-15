#!/usr/bin/env python3
"""
ExecuTorch Minarai model export script

Converts LiquidBounce Minarai DJL models to ExecuTorch .pte format.

The Minarai models are trained for angle smoothing (aiming assistance).
Input: 4 float features (current angle delta)
Output: 2 float features (adjusted angle delta)

Usage:
    python export_minarai.py --model-name 21KC11KP --output-dir ./models
    python export_minarai.py --export-all --output-dir ./models

Requirements:
    pip install torch executorch executorch-backends-xnnpack

Model Architecture:
    Input (4) -> Linear(128) -> BatchNorm -> ReLU ->
              Linear(64) -> BatchNorm -> ReLU ->
              Linear(32) -> BatchNorm -> ReLU ->
              Linear(2) -> Output
"""

import argparse
import torch
from torch.nn import Sequential, Linear, BatchNorm1d, ReLU
from executorch.exir import to_edge_transform_and_lower
from executorch.backends.xnnpack.partition.xnnpack_partitioner import XnnpackPartitioner
import json
import os


def create_minarai_model() -> Sequential:
    """
    Creates a Minarai model with the architecture used in LiquidBounce.

    The model is a simple feedforward neural network trained for angle smoothing.

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


def load_minarai_from_djl(model_path: str) -> Sequential:
    """
    Loads a Minarai model from DJL .params file.

    DJL stores parameters in a specific format. This function attempts to load
    them into a PyTorch model.

    Args:
        model_path: Path to the DJL .params file

    Returns:
        PyTorch model with loaded parameters
    """
    model = create_minarai_model()

    try:
        # Load parameters
        # Note: Adjust this based on actual DJL parameter format
        state_dict = torch.load(model_path, map_location='cpu')
        model.load_state_dict(state_dict)
        print(f"✓ Loaded model parameters from {model_path}")
    except Exception as e:
        print(f"⚠ Could not load parameters from {model_path}: {e}")
        print("  Using randomly initialized model (you may need to retrain or convert manually)")

    model.eval()
    return model


def export_minarai_to_executorch(model_name: str, output_dir: str):
    """
    Exports a Minarai model to ExecuTorch .pte format.

    Args:
        model_name: Name of the model (e.g., "21KC11KP")
        output_dir: Directory to save the .pte file
    """
    print(f"\n{'='*60}")
    print(f"Exporting Minarai model: {model_name}")
    print('='*60)

    # Create output directory
    os.makedirs(output_dir, exist_ok=True)

    # Load or create model
    model_params_path = f"./models/{model_name}/{model_name}.params"
    if os.path.exists(model_params_path):
        print(f"Loading parameters from {model_params_path}...")
        model = load_minarai_from_djl(model_params_path)
    else:
        print(f"⚠ Model parameters not found at {model_params_path}")
        print("  Using model architecture only")
        model = create_minarai_model()

    # Create example input: 4 float features for angle delta
    # Shape: (batch_size=1, features=4)
    example_input = torch.randn(1, 4)
    print(f"Example input shape: {example_input.shape}")

    # Export the model using torch.export.export()
    print("Step 1: Exporting model with torch.export.export()...")
    try:
        exported_program = torch.export.export(
            model,
            (example_input,),
            strict=False
        )
        print("✓ Model exported successfully")
    except Exception as e:
        print(f"✗ Export failed: {e}")
        raise

    # Lower to edge and apply XNNPACK partitioner for CPU optimization
    print("Step 2: Lowering to ExecuTorch format...")
    try:
        edge_manager = to_edge_transform_and_lower(
            exported_program,
            partitioner=[XnnpackPartitioner()]
        )
        print("✓ Successfully lowered to edge")
    except Exception as e:
        print(f"✗ Lowering failed: {e}")
        raise

    # Convert to ExecuTorch format
    print("Step 3: Converting to ExecuTorch format...")
    try:
        executorch_program = edge_manager.to_executorch()
        print("✓ Successfully converted to ExecuTorch")
    except Exception as e:
        print(f"✗ Conversion failed: {e}")
        raise

    # Save the .pte file
    output_path = os.path.join(output_dir, f"{model_name}.pte")
    print(f"Step 4: Saving model to {output_path}...")
    with open(output_path, "wb") as f:
        f.write(executorch_program.buffer)
    print(f"✓ Successfully saved .pte file")

    # Print model info
    model_size_kb = len(executorch_program.buffer) / 1024
    print(f"\nModel Statistics:")
    print(f"  Size: {model_size_kb:.2f} KB")
    print(f"  Input features: 4")
    print(f"  Output features: 2")

    # Save metadata
    metadata = {
        "model_name": model_name,
        "input_features": 4,
        "output_features": 2,
        "model_size_bytes": len(executorch_program.buffer),
        "exported_via": "ExecuTorch",
        "export_script_version": "1.0"
    }
    metadata_path = os.path.join(output_dir, f"{model_name}.json")
    with open(metadata_path, "w") as f:
        json.dump(metadata, f, indent=2)
    print(f"  Metadata: {metadata_path}")


def main():
    parser = argparse.ArgumentParser(
        description="Export Minarai models to ExecuTorch format",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
Examples:
  Export a single model:
    python export_minarai.py --model-name 21KC11KP

  Export all Minarai models:
    python export_minarai.py --export-all

  Specify custom output directory:
    python export_minarai.py --export-all --output-dir /path/to/output
        """
    )
    parser.add_argument(
        "--model-name",
        type=str,
        default="21KC11KP",
        help="Model name to export (default: 21KC11KP)"
    )
    parser.add_argument(
        "--output-dir",
        type=str,
        default="./models/executorch",
        help="Output directory for .pte files (default: ./models/executorch)"
    )
    parser.add_argument(
        "--export-all",
        action="store_true",
        help="Export all known Minarai models"
    )

    args = parser.parse_args()

    if args.export_all:
        # Export all known Minarai models
        models = ["21KC11KP", "19KC8KP"]
        success_count = 0
        failed_count = 0

        print(f"\nExporting {len(models)} Minarai models...")

        for model_name in models:
            try:
                export_minarai_to_executorch(model_name, args.output_dir)
                success_count += 1
            except Exception as e:
                print(f"\n✗ Error exporting {model_name}: {e}")
                failed_count += 1

        print(f"\n{'='*60}")
        print(f"Export Summary:")
        print(f"  ✓ Successful: {success_count}")
        print(f"  ✗ Failed: {failed_count}")
        print('='*60)

        if failed_count > 0:
            exit(1)
    else:
        export_minarai_to_executorch(args.model_name, args.output_dir)
        print(f"\n✓ Export complete!")


if __name__ == "__main__":
    main()
