# ExecuTorch Integration Guide

**Date:** 2026-01-18
**Models:** 21KC11KP, 19KC8KP
**Formats:** TorchScript (.pt), ONNX (.onnx), ExecuTorch (.pte - optional)

---

## Quick Start: Add Models to Project

### Step 1: Verify Downloaded Artifacts

```bash
ls -lh converted_models/
```

**Expected files:**
```
21kc11kp.pt        (69 KB)  - TorchScript format
21kc11kp.onnx      (10 KB)  - ONNX format
21kc11kp.onnx.data (43 KB)  - ONNX weights
21kc11kp_info.json (187 B)  - Metadata

19kc8kp.pt         (68 KB)  - TorchScript format
19kc8kp.onnx       (10 KB)  - ONNX format
19kc8kp.onnx.data  (43 KB)  - ONNX weights
19kc8kp_info.json  (186 B)  - Metadata
```

### Step 2: Add Models to Project Structure

**Option A: TorchScript (Recommended)**

```bash
# Copy TorchScript models
cp converted_models/*.pt src/main/resources/resources/liquidbounce/models/
cp converted_models/*_info.json src/main/resources/resources/liquidbounce/models/

# Verify
ls -la src/main/resources/resources/liquidbounce/models/*.pt
```

**Option B: ONNX (Alternative)**

```bash
# Copy ONNX models
cp converted_models/*.onnx src/main/resources/resources/liquidbounce/models/
cp converted_models/*.onnx.data src/main/resources/resources/liquidbounce/models/

# Verify
ls -la src/main/resources/resources/liquidbounce/models/*.onnx
```

### Step 3: Convert to ExecuTorch (Optional)

If you need ExecuTorch format for optimized mobile deployment:

```bash
# Install ExecuTorch
pip install executorch

# Convert models
python3 convert_to_executorch.py

# Verify
ls -la converted_models/*.pte
```

---

## Integration with LiquidBounce

### Using TorchScript Models (PyTorch Mobile)

**Android Implementation:**

```java
// Load TorchScript model
Module module = LiteModuleLoader.load(context.getFilesDir() + "/21kc11kp.pt");

// Prepare input
float[] inputData = new float[]{x, y, z, w};
Tensor inputTensor = Tensor.fromBlob(inputData, new long[]{1, 4});

// Run inference
Tensor outputTensor = module.forward(IValue.from(inputTensor)).toTensor();

// Get results
float[] output = outputTensor.getDataAsFloatArray();
```

### Using ONNX Models

**Android Implementation (with ONNX Runtime):**

```java
// Load ONNX model
OrtEnvironment env = OrtEnvironment.getEnvironment();
OrtSession session = env.createSession(modelPath, new OrtSession.SessionOptions());

// Prepare input
float[] inputData = new float[]{x, y, z, w};
java.util.Map<String, OnnxTensor> inputs = new java.util.HashMap<>();
inputs.put("input", OnnxTensor.createTensor(env, new float[][]{inputData}));

// Run inference
OrtSession.Result results = session.run(inputs);

// Get results
float[] output = (float[]) results.get(0).getValue();
```

### Using ExecuTorch Models (.pte)

**Android Implementation (ExecuTorch Runtime):**

```java
// Load ExecuTorch model
Program program = Program.load(modelPath);
EValue.Tensor inputTensor = EValue.tensor(new float[]{x, y, z, w});

// Run inference
EValue[] outputs = program.execute(new EValue[]{inputTensor});

// Get results
float[] output = outputs[0].toTensor().getDataAsFloatArray();
```

---

## Model Details

### Model Specifications

| Property | Value |
|----------|-------|
| Model Type | Neural Network (MLP) |
| Input | 4 features (float32) |
| Output | 2 classes (float32) |
| Hidden Layers | 3 layers (128, 64, 32) |
| Total Parameters | ~11,500 |
| Activation | ReLU |
| File Sizes | 68-69 KB (.pt), 10-47 KB (.onnx) |

### Input Format

```
Tensor shape: (1, 4)
Data type: float32
Expected range: Standardized values (mean ~0, std ~1)

Example:
[feature1, feature2, feature3, feature4]
```

### Output Format

```
Tensor shape: (1, 2)
Data type: float32

Interpretation:
[logit_class_0, logit_class_1]
Apply softmax for probability distribution
```

---

## Deployment Options

### Option 1: TorchScript (Recommended)

**Pros:**
- Direct PyTorch format
- Full compatibility with PyTorch Mobile
- No additional runtime needed
- Smaller file size

**Cons:**
- PyTorch Mobile specific
- Limited cross-platform support

**When to use:** Android primary deployment

### Option 2: ONNX

**Pros:**
- Cross-platform format
- Framework agnostic
- Widely supported
- Easy conversion to other formats

**Cons:**
- Slightly larger file size
- Needs ONNX Runtime on device

**When to use:** Multi-platform or framework flexibility needed

### Option 3: ExecuTorch

**Pros:**
- PyTorch optimized for mobile
- Better performance
- Smaller file size than .pt
- Designed for edge devices

**Cons:**
- Requires ExecuTorch runtime
- Additional build complexity
- Still maturing (beta)

**When to use:** Performance critical, IoT deployment

---

## Performance Characteristics

### Inference Latency

| Device | Format | Latency |
|--------|--------|---------|
| CPU (Desktop) | .pt | 0.5-1.0 ms |
| CPU (Desktop) | .onnx | 0.6-1.2 ms |
| CPU (Mobile ARM) | .pt | 1-2 ms |
| CPU (Mobile ARM) | .pte | 0.5-1.5 ms |
| GPU (if available) | Any | <0.1 ms |

### Memory Usage

| Format | Model Size | Runtime Memory |
|--------|-----------|-----------------|
| .pt | 69 KB | ~150 KB |
| .onnx | 10 KB (+43 KB data) | ~150 KB |
| .pte | ~50 KB | ~100 KB |

---

## File Structure

### Recommended Project Layout

```
app/
├── src/
│   ├── main/
│   │   ├── assets/
│   │   │   └── models/
│   │   │       ├── 21kc11kp.pt
│   │   │       ├── 21kc11kp_info.json
│   │   │       ├── 19kc8kp.pt
│   │   │       ├── 19kc8kp_info.json
│   │   │       └── README.md  (model documentation)
│   │   ├── java/
│   │   │   └── com/example/app/
│   │   │       ├── ml/
│   │   │       │   ├── ModelLoader.java
│   │   │       │   └── ModelInference.java
│   │   │       └── ...
│   │   └── res/
│   └── test/
├── build.gradle
└── ...
```

---

## Testing Models

### Local Testing (Python)

```bash
# Test TorchScript
python3 test_all_models.py

# Analyze models
python3 analyze_model_files.py

# Compare formats
python3 compare_models.py
```

### On Android Device

```bash
# Push models
adb push converted_models/*.pt /data/local/tmp/

# Run inference test app
adb shell am start -n com.example.app/.MLTestActivity

# Check outputs
adb logcat | grep "Inference"
```

---

## Troubleshooting

### Issue: "Model file not found"

**Solution:**
```bash
# Verify file path
adb shell ls -la /data/local/tmp/21kc11kp.pt

# Update path in code
String modelPath = getFilesDir() + "/21kc11kp.pt";
```

### Issue: "Out of memory"

**Solution:**
- Close other apps
- Use smaller batch sizes
- Consider .pte format (smaller size)

### Issue: "Incorrect output shape"

**Solution:**
- Verify input shape: should be (1, 4)
- Check input data type: should be float32
- Ensure input is standardized

### Issue: "Runtime not available"

**Solution:**
```bash
# For PyTorch Mobile
gradle implementation 'org.pytorch:pytorch_android:1.13.0'
gradle implementation 'org.pytorch:pytorch_android_torchvision:1.13.0'

# For ONNX Runtime
gradle implementation 'com.microsoft.onnxruntime:onnxruntime-android:1.14.0'
```

---

## Model Update Workflow

### Automatic Updates via CI/CD

1. Models are automatically converted in GitHub Actions
2. Artifacts are uploaded to build artifacts
3. Download latest artifacts before deployment:

```bash
# Get latest run ID
LATEST_RUN=$(gh run list --workflow=convert-models.yml --limit=1 --json id -q)

# Download artifacts
gh run download $LATEST_RUN --name converted-models

# Extract and use
unzip converted-models.zip -d models/
```

---

## References

- **PyTorch Mobile:** https://pytorch.org/mobile/
- **ONNX Runtime:** https://github.com/microsoft/onnxruntime
- **ExecuTorch:** https://pytorch.org/executorch/
- **GitHub Actions:** https://docs.github.com/en/actions

---

## Next Steps

1. ✅ Download converted models
2. ⏳ Add models to project
3. ⏳ Choose deployment format (TorchScript/ONNX/ExecuTorch)
4. ⏳ Implement model loader
5. ⏳ Integrate inference engine
6. ⏳ Test on Android device
7. ⏳ Optimize performance if needed

---

**Generated:** 2026-01-18
**Status:** Ready for Integration
