#!/usr/bin/env python3
"""
Downconvert ONNX models to IR version 9 and opset 9 for compatibility with ONNX Runtime on Android.

This script sets both the IR version and opset to 9 for maximum compatibility with mobile devices.
Models generated with newer ONNX versions (IR v10+, opset 10+) may not work on Android.

Usage:
  ./scripts/downconvert_onnx_opset9.py <model_file> [<output_file>]

  If output_file is not specified, the input file will be overwritten.

Requirements:
  pip install onnx onnxruntime
"""

import sys
import argparse
from pathlib import Path
import onnx
from onnx import helper, TensorProto
import onnxruntime as rt


def downconvert_to_opset9(model_path: Path, output_path: Path = None) -> bool:
    """Downconvert ONNX model to IR version 9 and opset 9."""
    if output_path is None:
        output_path = model_path

    print(f"Loading model: {model_path}")
    try:
        model = onnx.load(str(model_path))
    except Exception as e:
        print(f"Error loading model: {e}")
        return False

    original_ir = model.ir_version
    original_opset = model.opset_import[0].version if model.opset_import else 0

    print(f"Original model - IR version: {original_ir}, Opset: {original_opset}")

    # Check if already at IR v9 and opset 9 or lower
    if original_ir <= 9 and original_opset <= 9:
        print("Model is already at IR v9 and opset 9 or lower. No conversion needed.")
        return True

    try:
        # Update IR version to 9 for ONNX Runtime compatibility on Android
        if original_ir > 9:
            model.ir_version = 9
            print(f"Updated IR version to 9")
        
        # Update opset version to 9
        for opset_import in model.opset_import:
            if opset_import.domain == "" or opset_import.domain == "ai.onnx":
                opset_import.version = 9
                print(f"Updated opset to version 9")

        # Run shape inference to ensure tensor shapes are valid
        print("Running shape inference...")
        model = onnx.shape_inference.infer_shapes(model)

        # Save the model
        print(f"Saving downconverted model to: {output_path}")
        output_path.parent.mkdir(parents=True, exist_ok=True)
        onnx.save(model, str(output_path))

        # Validate the output
        print("Validating converted model...")
        model = onnx.load(str(output_path))
        onnx.checker.check_model(model)

        new_ir = model.ir_version
        new_opset = model.opset_import[0].version if model.opset_import else 0
        print(f"Conversion successful! New model - IR version: {new_ir}, Opset: {new_opset}")

        # Test if ONNX Runtime can load it
        print("Testing with ONNX Runtime...")
        try:
            sess = rt.InferenceSession(str(output_path), providers=['CPUExecutionProvider'])
            print("ONNX Runtime loaded model successfully!")
        except Exception as e:
            print(f"Warning: ONNX Runtime failed to load model: {e}")
            print("Model may still have compatibility issues.")
            return False

        return True

    except Exception as e:
        print(f"Error during conversion: {e}")
        import traceback
        traceback.print_exc()
        return False


if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description='Downconvert ONNX models to IR v9 and opset 9 for mobile compatibility'
    )
    parser.add_argument('model', help='Input ONNX model file')
    parser.add_argument('--output', '-o', help='Output model file (default: overwrite input)')
    args = parser.parse_args()

    model_path = Path(args.model)
    if not model_path.exists():
        print(f"Error: Model file not found: {model_path}")
        sys.exit(1)

    output_path = Path(args.output) if args.output else model_path

    success = downconvert_to_opset9(model_path, output_path)
    sys.exit(0 if success else 1)
