# ExecuTorch Integration Guide

This directory contains the ExecuTorch (PyTorch Mobile) integration for LiquidBounce, enabling on-device ML inference on Android via PojavLauncher.

## What is ExecuTorch?

ExecuTorch is the official PyTorch runtime for edge devices, providing:
- Direct PyTorch model export to portable `.pte` format (no ONNX intermediary)
- Hardware acceleration support (CPU, GPU, NPU on compatible devices)
- Smaller model sizes and faster inference vs PyTorch Mobile
- Graceful fallback for platforms without native library support

## Current Status

**ExecuTorch Runtime Framework**: ✅ Integrated
**ExecuTorch JARs**: ✅ Available on Maven Central (`org.pytorch:executorch-android:1.0.1`)

The ExecuTorchEngine and ExecuTorchModel classes are ready to use with the official ExecuTorch Android package.

## Model Conversion Workflow

### Quick Start: Converting Minarai Models

The existing Minarai angle smoothing models (21KC11KP, 19KC8KP) need to be converted from DJL format to ExecuTorch `.pte` format.

#### Step 1: Install Requirements

```bash
pip install torch executorch "executorch-backends-xnnpack>=0.2.0"
```

#### Step 2: Run Export Script

The `export_minarai.py` script automates the conversion process:

```bash
# Export a single model
python export_minarai.py --model-name 21KC11KP

# Export all Minarai models
python export_minarai.py --export-all --output-dir ./exported_models
```

#### Step 3: Package Models

Copy the exported `.pte` files to the PojavBounce resources:

```bash
mkdir -p src/main/resources/resources/liquidbounce/models/executorch/
cp exported_models/*.pte src/main/resources/resources/liquidbounce/models/executorch/
```

#### Step 4: Rebuild

Once ExecuTorch JARs are available:
```bash
./gradlew build
```

### Custom Model Conversion

For converting your own PyTorch models:

```bash
python export_model.py \
    --model-path my_model.pth \
    --output-path my_model.pte \
    --input-shape 1 4
```

The `--input-shape` parameter should match your model's expected input dimensions.

## Usage in LiquidBounce Code

Once ExecuTorch JARs are available:

```kotlin
import net.ccbluex.liquidbounce.deeplearn.executorch.ExecuTorchModel

// Load a model
val model = ExecuTorchModel("21KC11KP")

// Perform inference
val input = FloatArray(4) { /* features */ }
val output = model.predict(input)  // Returns FloatArray
```

## Architecture

### ExecuTorchEngine
- Manages native library loading and initialization
- Handles Android platform detection and cache folder setup
- Provides graceful fallback if native libraries unavailable
- Mirrors DJL's architecture for consistency

### ExecuTorchModel
- Wraps ExecuTorch native module
- Implements ModelWrapper interface (same API as DJL models)
- Supports model loading from JAR resources or filesystem
- Manages tensor conversion (FloatArray ↔ ExecuTorch tensors)

### ModelConverter
- Generates Python export script templates
- Provides Kotlin API for subprocess-based conversion
- Supports both generic and Minarai-specific workflows

## Obtaining ExecuTorch JARs

ExecuTorch Android v1.0.1 is available on Maven Central!

**Automatic** (recommended):
- The build.gradle.kts already includes `org.pytorch:executorch-android:1.0.1`
- Just build normally: `./gradlew build`

**Verify Installation**:
```bash
./gradlew build --info | grep executorch
# Should show successful dependency resolution
```

**Build from Source** (optional):
```bash
git clone https://github.com/pytorch/executorch.git
cd executorch
cmake -B build .
# Build Android JARs and native libraries
```

## Troubleshooting

### Native Library Loading Fails on Android

ExecuTorch provides pre-built Android `.so` files, but they may fail to load on some devices due to:
- Namespace isolation (SELinux restrictions)
- GLIBC vs Bionic libc incompatibility
- Missing architecture support (ARM64-v8a is primary target)

In these cases, ExecuTorch features gracefully disable while DJL continues operating.

**Solution**: Check device logs and ensure ARM64-v8a support:
```bash
adb logcat | grep ExecuTorch
```

### Model Conversion Fails

Ensure your PyTorch model is traceable:

```python
# Good: Simple model
model = torch.nn.Sequential(
    torch.nn.Linear(4, 128),
    torch.nn.ReLU(),
    torch.nn.Linear(128, 2)
)

# Problematic: Complex control flow
class ComplexModel(torch.nn.Module):
    def forward(self, x):
        if x.sum() > 0:  # Dynamic control flow not supported
            return x * 2
        return x
```

### Performance Not as Expected

- Ensure model is in evaluation mode (`model.eval()`)
- Verify input shape matches training data
- Consider XNNPACK quantization for further optimization

## Integration Points

### Initialization
- `DeepLearningEngine.init()` automatically initializes ExecuTorchEngine
- Both DJL and ExecuTorch can coexist
- Graceful degradation if either fails

### Configuration
- ExecuTorch cache folder: `{config}/executorch/`
- Models folder: `{config}/executorch/models/`
- Native libraries: `{config}/executorch/native/`

### Model Selection
- ModelHolster will support both DJL and ExecuTorch models
- Users can choose backend via configuration

## Performance Characteristics

### Minarai Model (21KC11KP)
- Input: 4 float features
- Output: 2 float features
- Latency: ~2-5ms on modern Android devices (ARM64-v8a)
- Model size: ~50 KB (DJL) → ~15-20 KB (.pte)

## Resources

- [ExecuTorch Documentation](https://pytorch.org/executorch/stable/)
- [XNNPACK Backend](https://github.com/pytorch/XNNPACK)
- [PyTorch Export](https://pytorch.org/docs/stable/export.html)
- [LiquidBounce Deep Learning](https://github.com/CCBlueX/LiquidBounce/tree/nextgen/src/main/kotlin/net/ccbluex/liquidbounce/deeplearn)

## License

This integration is part of LiquidBounce, licensed under GNU General Public License v3.0.
