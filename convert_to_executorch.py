#!/usr/bin/env python3
"""
ExecuTorch Model Converter
Converts TorchScript (.pt) models to ExecuTorch (.pte) format
"""

import torch
import os
from pathlib import Path

try:
    from executorch.exir import to_edge
    from executorch.backends.xnnpack import partition_for_xnnpack
    EXECUTORCH_AVAILABLE = True
except ImportError:
    EXECUTORCH_AVAILABLE = False
    print("⚠️  ExecutorTorch not installed. Install with: pip install executorch")

def convert_to_executorch(pt_file, output_file):
    """
    Convert a TorchScript model to ExecuTorch format
    """
    if not EXECUTORCH_AVAILABLE:
        print(f"❌ ExecuTorch not available. Skipping conversion for {pt_file}")
        return False

    print(f"\nConverting {Path(pt_file).name}...")
    print("-" * 60)

    try:
        # Load the TorchScript model
        print("Loading TorchScript model...")
        model = torch.jit.load(pt_file)
        model.eval()
        print("✓ Model loaded")

        # Create example input
        print("Creating example input...")
        example_inputs = (torch.randn(1, 4),)
        print("✓ Example input created")

        # Convert to ExecuTorch edge representation
        print("Converting to ExecuTorch edge format...")
        edge = to_edge(model, example_inputs=example_inputs)
        print("✓ Edge conversion complete")

        # Partition for XNNPACK (CPU backend)
        print("Partitioning for XNNPACK backend...")
        edge = partition_for_xnnpack(edge)
        print("✓ Partitioning complete")

        # Export to .pte format
        print("Exporting to .pte format...")
        et_program = edge.to_executorch()
        et_program.write_to_file(output_file)
        print(f"✓ Exported to {output_file}")

        # Check file size
        file_size = os.path.getsize(output_file)
        print(f"  File size: {file_size:,} bytes")

        return True

    except Exception as e:
        print(f"❌ Conversion failed: {e}")
        import traceback
        traceback.print_exc()
        return False

def main():
    """Main conversion process"""
    print("\n" + "=" * 80)
    print("EXECUTORCH MODEL CONVERTER")
    print("=" * 80)

    # Models to convert
    models = [
        {
            'input': 'converted_models/21kc11kp.pt',
            'output': 'converted_models/21kc11kp.pte'
        },
        {
            'input': 'converted_models/19kc8kp.pt',
            'output': 'converted_models/19kc8kp.pte'
        }
    ]

    if not EXECUTORCH_AVAILABLE:
        print("\n⚠️  ExecuTorch is not installed")
        print("\nTo use this converter, install ExecuTorch:")
        print("  pip install executorch")
        print("\nNote: May require additional dependencies")
        print("See: https://pytorch.org/executorch/stable/getting-started.html")
        return 1

    print("\nConverting models...")
    results = {}

    for model in models:
        input_file = model['input']
        output_file = model['output']

        if not os.path.exists(input_file):
            print(f"\n❌ Input file not found: {input_file}")
            results[input_file] = False
            continue

        success = convert_to_executorch(input_file, output_file)
        results[input_file] = success

    # Summary
    print("\n" + "=" * 80)
    print("CONVERSION SUMMARY")
    print("=" * 80)

    for input_file, success in results.items():
        status = "✅ SUCCESS" if success else "❌ FAILED"
        print(f"{Path(input_file).name}: {status}")

    print("\n" + "=" * 80)
    print("NEXT STEPS")
    print("=" * 80)
    print("""
1. Verify .pte files:
   ls -lh converted_models/*.pte

2. Copy to Android:
   adb push converted_models/*.pte /data/local/tmp/

3. Use in LiquidBounce:
   - Add .pte files to app assets
   - Load with ExecuTorch Android API
   - Run inference with ExecutorTorch runtime
""")

    return 0 if all(results.values()) else 1

if __name__ == '__main__':
    exit(main())
