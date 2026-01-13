# ONNX Model IR Version Compatibility Issue

## Problem
Your ONNX models have **IR version 13** with **opset 25**, but your ONNX Runtime supports maximum **IR version 9**.

### Current State
- **Models**: `src/main/resources/resources/liquidbounce/models/*.onnx`
  - IR version: 13
  - Opset: 25
- **ONNX Runtime Version**: 1.16.3 (from gradle/libs.versions.toml)
  - Max supported IR version: ~11 (but Android build may be restricted to 9)

## Root Cause
The models were generated with a newer ONNX version (1.20+) but your ONNX Runtime doesn't support that IR version.

## Solutions

### Option 1: Upgrade ONNX Runtime (Recommended)
Update `gradle/libs.versions.toml`:
```toml
onnxruntime = "1.20.0"  # or latest stable
```

ONNX Runtime versions and their max IR support:
- 1.16.3 → IR v11
- 1.18.0 → IR v12
- 1.20.0+ → IR v13+

### Option 2: Regenerate Models with Opset 9
If you need to stay on ONNX Runtime 1.16.3:

1. Find the source models (TensorFlow SavedModels in `build/exports/savedmodels/`)
2. Run the conversion script:
   ```bash
   ./scripts/convert_saved_models.py
   ```
   This uses tf2onnx with `--opset 9` flag

### Option 3: Use Pre-generated Opset 9 Models
Check `generated-opset9-models/` directory:
```bash
cp generated-opset9-models/*.onnx src/main/resources/resources/liquidbounce/models/
```

## Verification
Check model IR version:
```python
import onnx
model = onnx.load("model.onnx")
print(f"IR version: {model.ir_version}, Opset: {model.opset_import[0].version}")
```

## Recommended Action
**Upgrade ONNX Runtime to 1.20.0+** to support IR v13 models, or regenerate models with the conversion script if staying on 1.16.3.
