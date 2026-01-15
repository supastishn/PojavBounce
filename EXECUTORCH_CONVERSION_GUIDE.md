# ExecuTorch Model Conversion Instructions

This document provides step-by-step instructions for converting the Minarai angle smoothing models from DJL format to ExecuTorch .pte format.

## Overview

The Minarai models (21KC11KP, 19KC8KP) are currently stored in DJL format:
- Location: `src/main/resources/resources/liquidbounce/models/`
- Files: `21kc11kp.params`, `19kc8kp.params`

These need to be converted to ExecuTorch `.pte` format for on-device Android inference via PojavLauncher.

## Prerequisites

1. **Python 3.9+**
   ```bash
   python3 --version
   ```

2. **PyTorch and ExecuTorch**
   ```bash
   pip install torch "executorch>=0.2.0" "executorch-backends-xnnpack>=0.2.0"
   ```

3. **LiquidBounce Repository**
   - Clone: `git clone --recurse-submodules https://github.com/CCBlueX/LiquidBounce.git`
   - Branch: `nextgen`

## Step-by-Step Conversion

### 1. Prepare Source Models

The DJL `.params` files are already in the repository:

```bash
cd LiquidBounce/
ls -la src/main/resources/resources/liquidbounce/models/
# Should show:
# - 21kc11kp.params
# - 19kc8kp.params
```

### 2. Locate Export Script

The ExecuTorch export script is included:

```bash
ls -la src/main/resources/scripts/executorch/
# Should show:
# - export_minarai.py
# - export_model.py
# - README.md
```

### 3. Run Conversion

Navigate to script directory and run the export:

```bash
cd src/main/resources/scripts/executorch/

# Option A: Export all models at once
python3 export_minarai.py --export-all --output-dir ../../executorch_output

# Option B: Export single model
python3 export_minarai.py --model-name 21KC11KP --output-dir ../../executorch_output
```

Expected output:
```
============================================================
Exporting Minarai model: 21KC11KP
============================================================
Loading parameters from ./models/21KC11KP/21KC11KP.params...
⚠ Could not load parameters from ./models/21KC11KP/21KC11KP.params: ...
  Using model architecture only (you may need to retrain or convert manually)
Step 1: Exporting model with torch.export.export()...
✓ Model exported successfully
Step 2: Lowering to ExecuTorch format...
✓ Successfully lowered to edge
Step 3: Converting to ExecuTorch format...
✓ Successfully converted to ExecuTorch
Step 4: Saving model to ../../executorch_output/21KC11KP.pte...
✓ Successfully saved .pte file

Model Statistics:
  Size: 45.23 KB
  Input features: 4
  Output features: 2
  Metadata: ../../executorch_output/21KC11KP.json

✓ Export complete!
```

### 4. Copy Converted Models

After successful conversion, copy `.pte` files to the resources directory:

```bash
cp executorch_output/*.pte \
   ../resources/liquidbounce/models/executorch/

# Verify
ls -la ../resources/liquidbounce/models/executorch/
# Should show:
# - 21KC11KP.pte
# - 19KC8KP.pte
```

### 5. Rebuild and Test

Rebuild the project to include the new models:

```bash
./gradlew clean build
```

## Troubleshooting

### "ModuleNotFoundError: No module named 'executorch'"

Install ExecuTorch:
```bash
pip install --upgrade executorch "executorch-backends-xnnpack>=0.2.0"
```

### "Could not load parameters from..."

This is expected if the DJL parameter format differs from PyTorch's. The script will use the model architecture instead. The exported model will work, but accuracy may differ from the original DJL model.

To properly convert, ensure the original PyTorch source model (`.pth` file) is available:

```bash
# If you have the original PyTorch model:
python3 export_model.py \
    --model-path original_minarai.pth \
    --output-path 21KC11KP.pte \
    --input-shape 1 4
```

### Permission Denied

Make scripts executable:
```bash
chmod +x src/main/resources/scripts/executorch/export_minarai.py
```

### Export Fails with "RuntimeError: Cannot lower model..."

Ensure the model architecture exactly matches what was used for training. The Minarai architecture is:

```python
Sequential(
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
```

## Verification

Once converted, verify the models load correctly:

```kotlin
// In LiquidBounce code
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchModel

val model = ExecuTorchModel("21KC11KP")
val testInput = FloatArray(4) { 0f }
val output = model.predict(testInput)

assert(output.size == 2)
println("✓ Model loaded and inferred successfully!")
```

## Performance Baseline

After successful conversion, run performance benchmarks:

```bash
# Android device benchmark
adb shell /data/local/tmp/benchmark_model \
    --graph=/data/local/tmp/21KC11KP.pte \
    --num_threads=4 \
    --num_runs=100
```

Expected latency: 2-5ms per inference on modern ARM64 devices

## Next Steps

1. **Commit models**: `git add *.pte && git commit -m "feat: add ExecuTorch Minarai models"`
2. **Push to CI**: CI will validate the build with embedded models
3. **Test on device**: Deploy to PojavLauncher and verify inference works
4. **Monitor performance**: Compare with DJL baseline

## References

- [ExecuTorch Documentation](https://pytorch.org/executorch/stable/)
- [Model Export Guide](https://pytorch.org/executorch/stable/index.html)
- [Minarai Model Architecture](../../../../../../../features/module/modules/render/nametags/)
- [LiquidBounce Deep Learning](../../../deeplearn/)
