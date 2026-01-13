# ONNX Model IR Version Compatibility Issue

## Problem
ONNX models need **IR version 9** for maximum compatibility with ONNX Runtime on Android.

### Current State (FIXED)
- **Conversion Scripts**: All scripts now generate IR v9, opset 9 models ✓
- **ONNX Runtime Version**: 1.16.3 (from gradle/libs.versions.toml)
  - Max supported IR version: ~11 (but Android build requires IR v9)

## Root Cause
Models generated with newer ONNX versions (1.20+) default to IR v13 and opset 25, which don't work on Android.

## Solution (IMPLEMENTED)

All three ONNX conversion scripts have been updated to ensure IR v9 compatibility:

### 1. `scripts/params_to_onnx.py`
Converts DJL parameter dumps to ONNX with IR v9 and opset 9:
```bash
./scripts/params_to_onnx.py model.params <outputs> model.onnx
```

### 2. `scripts/convert_saved_models.py`
Converts TensorFlow SavedModels to ONNX with automatic IR v9 downgrade:
```bash
./scripts/convert_saved_models.py --saved build/exports/savedmodels
```

### 3. `scripts/downconvert_onnx_opset9.py`
Downgrades existing ONNX models to IR v9 and opset 9:
```bash
./scripts/downconvert_onnx_opset9.py model.onnx
```

## Generating Models via CI

The GitHub Actions workflow `.github/workflows/convert-onnx.yml` automatically:
1. Exports DJL SavedModels from the Java code
2. Converts them to ONNX with opset 9
3. Ensures IR version is 9
4. Packages models as downloadable artifacts

## Verification
Check model IR version:
```python
import onnx
model = onnx.load("model.onnx")
print(f"IR version: {model.ir_version}, Opset: {model.opset_import[0].version}")
```

Expected output: `IR version: 9, Opset: 9`

## Notes
- **IR v9** corresponds to ONNX spec 2023-05-05
- **Opset 9** ensures compatibility with older ONNX Runtime versions
- Models cannot be safely downconverted by just changing version numbers - they must be regenerated from source
