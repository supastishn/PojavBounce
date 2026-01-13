#!/usr/bin/env python3
"""
Convert TF SavedModel directories created by the Gradle exporter into ONNX files using tf2onnx.
Writes ONNX files into src/main/resources/resources/liquidbounce/models/<lowercase>.onnx

Usage:
  ./scripts/convert_saved_models.py [--saved root] [--out resources_dir] [model1 model2 ...]

Requirements:
  pip install tf2onnx tensorflow onnx onnxruntime
"""

import argparse
import subprocess
import sys
from pathlib import Path
import onnx

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
DEFAULT_SAVED_ROOT = REPO_ROOT / 'build' / 'exports' / 'savedmodels'
DEFAULT_OUT_DIR = REPO_ROOT / 'src' / 'main' / 'resources' / 'resources' / 'liquidbounce' / 'models'


def convert(saved_model_dir: Path, output_file: Path) -> bool:
    print(f"Converting {saved_model_dir} -> {output_file}")

    output_file.parent.mkdir(parents=True, exist_ok=True)

    # Remove any existing model to ensure fresh conversion
    if output_file.exists():
        output_file.unlink()

    cmd = [sys.executable, '-m', 'tf2onnx.convert', '--saved-model', str(saved_model_dir), '--output', str(output_file), '--opset', '9']
    print('Running:', ' '.join(cmd))
    res = subprocess.run(cmd, capture_output=True, text=True)
    if res.returncode != 0:
        print('tf2onnx failed:', res.returncode)
        print(res.stdout)
        print(res.stderr)
        return False

    # Verify and validate the converted model
    try:
        model = onnx.load(str(output_file))

        # Check IR version and opset
        ir_version = model.ir_version
        opset_version = model.opset_import[0].version if model.opset_import else 0
        print(f"Model IR version: {ir_version}, Opset: {opset_version}")

        # ONNX Runtime on Android supports up to IR version 9
        if ir_version > 9:
            print(f"WARNING: IR version {ir_version} exceeds maximum supported version 9")
            print("This model may not work with ONNX Runtime on Android")
            print("The CI pipeline will downgrade to IR v9 if possible")

        onnx.checker.check_model(model)
    except Exception as e:
        print('ONNX validation failed:', e)
        return False

    print('Converted and validated', output_file)
    return True


if __name__ == '__main__':
    p = argparse.ArgumentParser()
    p.add_argument('--saved', default=str(DEFAULT_SAVED_ROOT), help='SavedModels root directory')
    p.add_argument('--out', default=str(DEFAULT_OUT_DIR), help='Output resources models directory')
    p.add_argument('models', nargs='*', help='Specific model names to convert (optional)')
    args = p.parse_args()

    saved_root = Path(args.saved)
    out_dir = Path(args.out)

    if args.models:
        model_names = args.models
    else:
        if not saved_root.exists():
            print('Saved models directory does not exist:', saved_root)
            sys.exit(2)
        model_names = [p.name for p in saved_root.iterdir() if p.is_dir()]

    ok = True
    for name in model_names:
        saved = saved_root / name
        if not saved.exists():
            print('Saved model not found:', saved)
            # Skip missing directory; do not fail the whole conversion for missing directories
            continue
        # Ensure a SavedModel file exists inside the directory before converting
        if not ((saved / 'saved_model.pb').exists() or (saved / 'saved_model.pbtxt').exists()):
            print('SavedModel file not found inside directory, skipping:', saved)
            # Skip directories without a SavedModel file
            continue
        out_file = out_dir / (name.lower() + '.onnx')
        if not convert(saved, out_file):
            ok = False

    sys.exit(0 if ok else 1)
