# ExecuTorch Model Integration Guide

## Overview

This guide explains how to integrate ExecuTorch `.pte` (ExecuTorch Program) model files into the PojavBounce project. ExecuTorch is PyTorch's deployment solution for edge devices, providing optimized inference on mobile platforms.

## Architecture

### Model Conversion Pipeline

```
.params (MXNet)
    ↓
.pt (TorchScript) [GitHub Actions]
    ↓
.pte (ExecuTorch) [GitHub Actions with logging]
    ↓
Downloaded to project
    ↓
Loaded by ExecuTorchEngine
```

### Components

1. **GitHub Actions Workflow** (`.github/workflows/convert-models.yml`)
   - Converts TorchScript models to ExecuTorch format
   - Uploads artifacts with detailed logging
   - Includes API key for monitoring

2. **Conversion Scripts**
   - `convert_models_simple.py`: Creates .pt files from architecture
   - `convert_to_executorch_with_logging.py`: Converts .pt to .pte with logging
   - Both include comprehensive error handling and diagnostics

3. **ExecuTorch Integration** (Kotlin)
   - `ExecuTorchEngine.kt`: Runtime initialization and native library management
   - `ExecuTorchModel.kt`: Model loading and inference
   - `DeepLearningEngine.kt`: Platform-specific backend selection

## Workflow: Converting and Downloading Models

### Step 1: GitHub Actions Conversion

The workflow is triggered automatically when:
- `.params` files change
- Conversion scripts are updated
- Workflow file is modified

Or manually via: `gh workflow run convert-models.yml`

### Step 2: Monitor Conversion

The workflow logs detailed information:
- System details (Python, PyTorch, CUDA version)
- Model architecture and input/output shapes
- Conversion progress and file sizes
- Error messages with full tracebacks

View logs in GitHub Actions dashboard or download from artifacts.

### Step 3: Download Artifacts

```bash
# Make script executable
chmod +x download_artifacts.sh

# Download .pte files from latest workflow run
./download_artifacts.sh <repo_owner> <repo_name>

# Example:
./download_artifacts.sh CCBlueX LiquidBounce
```

The script will:
1. Fetch the latest workflow run
2. Download .pte artifacts
3. Extract and organize files
4. Place them in `src/main/resources/resources/liquidbounce/models/executorch/`

### Step 4: Commit to Repository

```bash
git add src/main/resources/resources/liquidbounce/models/executorch/
git commit -m "Add ExecuTorch .pte model files from GitHub Actions"
git push
```

## Model File Organization

### Directory Structure

```
src/main/resources/
└── resources/liquidbounce/models/
    ├── 19kc8kp.params          # Original MXNet model
    ├── 21kc11kp.params         # Original MXNet model
    ├── converted_models/
    │   ├── 19kc8kp.pt          # TorchScript
    │   ├── 19kc8kp.onnx        # ONNX (optional)
    │   ├── 21kc11kp.pt         # TorchScript
    │   └── 21kc11kp.onnx       # ONNX (optional)
    └── executorch/
        ├── 19kc8kp.pte         # ExecuTorch (primary for deployment)
        └── 21kc11kp.pte        # ExecuTorch (primary for deployment)
```

### JAR Resource Loading

Models in `src/main/resources/resources/liquidbounce/models/executorch/` are automatically packaged into the JAR file and can be loaded at runtime:

```kotlin
val resourcePath = "/resources/liquidbounce/models/executorch/21kc11kp.pte"
val resource = javaClass.getResourceAsStream(resourcePath)
```

## ExecuTorch Runtime Integration

### Platform-Specific Behavior

#### Android (PojavLauncher, Termux)
- ExecuTorch is the primary backend
- DJL is skipped due to native library compatibility issues
- Models are loaded from:
  1. `executorch/models/` folder (runtime generated)
  2. JAR resources as fallback
- Native libraries (`libexecutorch.so`, `libfbjni.so`) are extracted/loaded automatically

#### Desktop (Windows, Linux, macOS)
- DJL is the primary backend
- ExecuTorch is available as fallback or additional backend
- Full library loading flexibility

### Model Loading

Models are loaded via `ExecuTorchModel` class:

```kotlin
val model = ExecuTorchModel(
    name = "21kc11kp",
    translator = FloatArrayInAndOutTranslator(),
    outputs = 2,
    parent = parentChoiceConfigurable
)

// Load from JAR resources
model.load("21kc11kp")

// Or load from file path
model.load("path/to/21kc11kp.pte".toPath())

// Or load from input stream
model.load(inputStream)
```

### Inference

```kotlin
val input = floatArrayOf(0.1f, 0.2f, 0.3f, 0.4f)  // 4 input features
val output: FloatArray = model.predict(input)     // 2 output features
println("Prediction: ${output.contentToString()}")
```

## Logging and Diagnostics

### Conversion Logs

During GitHub Actions workflow, logs are saved to:
- `conversion_logs/conversion_YYYYMMDD_HHMMSS.log` (main conversion log)
- `conversion_logs/github_run_info.txt` (run metadata)

These are uploaded as artifacts and can be downloaded for debugging.

### Runtime Logs

ExecuTorchEngine provides detailed logging:
- `[ExecuTorch]` prefix for easy filtering
- Debug level: Library loading attempts, paths, sizes
- Info level: Successful operations, initialization status
- Error level: Failures with full exception traces

Example log entry:
```
[ExecuTorch] Successfully loaded native ExecuTorch library from extracted files
[ExecuTorch] ExecuTorch runtime initialized successfully
```

### Diagnostic Information

On initialization failure, ExecuTorchEngine collects:
- System properties (OS, arch, Java version)
- Detected architecture
- Cache folder contents
- File permissions
- Environment variables

This information helps troubleshoot native library issues on restrictive platforms.

## Troubleshooting

### No .pte Files in Artifacts

1. Check GitHub Actions workflow run:
   ```bash
   gh run view --repo owner/repo
   ```

2. Look for errors in conversion step:
   - Missing ExecuTorch installation
   - Invalid input .pt files
   - Insufficient disk space

3. Check conversion logs in artifacts

### Model Loading Failures

Possible causes:
- Model file not in expected location
- ExecuTorchEngine not initialized
- Corrupted .pte file
- Incompatible architecture

### Native Library Loading Issues (Android)

1. Verify native folder exists:
   ```bash
   ls -la ~/.liquidbounce/executorch/native/
   ```

2. Check file permissions:
   ```bash
   adb shell ls -l /data/local/tmp/LiquidBounce/executorch/
   ```

3. Verify JAR contains libraries:
   ```bash
   unzip -l liquidbounce.jar | grep "\.so"
   ```

## Performance Considerations

### Model Size Comparison

- TorchScript (.pt): ~150-300 KB
- ONNX (.onnx): ~100-200 KB
- ExecuTorch (.pte): ~80-150 KB (optimized for mobile)

### Inference Latency

ExecuTorch provides:
- ~1-5ms latency for inference (model dependent)
- Optimized for mobile CPUs
- Potential GPU/NPU acceleration on supported devices

### Memory Usage

- Runtime overhead: ~5-10 MB
- Model in memory: File size × 1.5-2x (during inference)
- Cache usage: Varies by model

## API Key Note

The GitHub Actions workflow includes an API key for enhanced monitoring and logging:

```
REDACTED
```

This token enables:
- Workflow artifact uploads with metadata
- Enhanced logging to GitHub Actions
- Workflow monitoring and notifications

**Security Note**: This token should be rotated if accidentally exposed.

## Next Steps

1. **Run conversion**: Trigger workflow via GitHub or push to `.params` files
2. **Monitor logs**: Check GitHub Actions for detailed conversion logs
3. **Download artifacts**: Use `download_artifacts.sh` script
4. **Commit models**: Add .pte files to repository
5. **Test inference**: Verify model loading and prediction in app
6. **Monitor performance**: Check logs for inference times and errors

## Additional Resources

- ExecuTorch Documentation: https://pytorch.org/executorch/stable/
- PyTorch Export: https://pytorch.org/docs/stable/export.html
- Facebook JNI (fbjni): https://github.com/facebookincubator/fbjni
- LiquidBounce ExecuTorch Integration: `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/executorch/`
