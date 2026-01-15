# DJL Alternatives for Android Support - Comprehensive Analysis

## Current DJL Integration Status
- **Framework**: Deep Java Library (DJL) 0.36.0
- **Engine**: PyTorch (pytorch-engine)
- **Platform**: PojavLauncher/Termux on Android
- **Model Format**: DJL `.params` format

---

## Alternative ML Frameworks for Android

### 1. **TensorFlow Lite (TFLite)**
**Platform Support**: ✓ Excellent Android support  
**Conversion from DJL .params**: ❌ **NO DIRECT SUPPORT**

**Pros:**
- Native Android library with optimized performance
- Lightweight and specifically designed for mobile/embedded devices
- Official Android support with TensorFlow Lite Interpreter
- GPU acceleration via GPU Delegate
- NNAPI support for hardware acceleration
- Pre-trained model zoo with common tasks
- Well-documented Android integration

**Cons:**
- Requires model conversion: DJL PyTorch → PyTorch → ONNX → TFLite
- Multiple conversion steps = higher complexity
- Limited to TFLite model format (.tflite)
- Less flexibility for custom training workflows

**Conversion Path**: 
```
DJL .params → PyTorch .pt → ONNX → TFLite (.tflite)
Tools: tf2onnx, TensorFlow Lite Converter
```

**Android Integration**: Gradle dependency
```gradle
implementation 'org.tensorflow:tensorflow-lite:+'
implementation 'org.tensorflow:tensorflow-lite-gpu:+'
```

---

### 2. **ONNX Runtime**
**Platform Support**: ✓ Good Android support (Java/Native bindings)  
**Conversion from DJL .params**: ⚠️ **PARTIAL - Via PyTorch**

**Pros:**
- Open standard format (ONNX) with broad framework support
- Direct conversion: PyTorch → ONNX format
- DJL can work with ONNX Runtime as engine
- Better than TFLite for model interoperability
- Supports multiple hardware backends
- Can run models from PyTorch, TensorFlow, scikit-learn, etc.

**Cons:**
- Android support less mature than TFLite
- Larger runtime footprint
- More complex setup compared to TFLite
- Fewer pre-optimized mobile models available

**Conversion Path**:
```
DJL .params → PyTorch .pt → ONNX (.onnx)
Tools: torch.onnx, ONNX Optimizer
```

**Android Integration**: Gradle dependency
```gradle
implementation 'com.microsoft.onnxruntime:onnxruntime-android:+'
```

---

### 3. **PyTorch Mobile**
**Platform Support**: ✓ Official Android support via JNI  
**Conversion from DJL .params**: ✓ **YES - Direct conversion**

**Pros:**
- **DIRECT conversion from DJL PyTorch models**
- Official PyTorch Android library
- TorchScript format (.pt) optimized for mobile
- Full PyTorch API compatibility
- GPU support via Vulkan
- Pre-trained model zoo (ImageNet, NLP, etc.)
- Best for PyTorch ecosystem

**Cons:**
- Larger native library footprint
- Steeper learning curve for Android developers
- More memory-intensive than TFLite
- Runtime can be slower on older/low-end devices

**Conversion Path**:
```
DJL .params → PyTorch .pt → TorchScript (.pt)
Direct in PyTorch: torch.jit.trace() or torch.jit.script()
```

**Android Integration**: Gradle dependency
```gradle
implementation 'org.pytorch:pytorch_android:+'
implementation 'org.pytorch:pytorch_android_torchvision:+'
```

**Key Advantage**: Easiest migration from DJL, minimal code changes

---

### 4. **MediaPipe**
**Platform Support**: ✓ Excellent for vision/pose tasks  
**Conversion from DJL .params**: ❌ **NO - Task-specific, pre-trained only**

**Pros:**
- Google's production-ready ML solution
- Optimized pre-built solutions (pose detection, hand tracking, object detection)
- Excellent Android integration with native performance
- GPU/CPU options, hardware acceleration support
- No model training/conversion needed for common tasks
- Minimal code needed for complex vision pipelines

**Cons:**
- Limited to predefined MediaPipe tasks
- Can't convert DJL models to MediaPipe format
- Not suitable for custom ML workflows
- Only for vision/audio tasks, not general-purpose

**When to Use**: 
- Pose estimation, hand/face tracking
- Object detection, segmentation
- Audio classification

**Android Integration**: Gradle dependency
```gradle
implementation 'com.google.mediapipe:tasks-vision:+'
```

---

### 5. **Gluon (MXNet)**
**Platform Support**: ⚠️ Limited Android support  
**Conversion from DJL .params**: ✓ **YES - Partial**

**Pros:**
- DJL also supports MXNet engine
- Some model interoperability with PyTorch
- Can load Gluon models via DJL

**Cons:**
- MXNet has smaller ecosystem than PyTorch/TensorFlow
- Android support is declining (MXNet less maintained)
- Less practical for mobile deployment
- DJL's MXNet engine is commented out in your codebase

**Status**: Not recommended for new Android projects

---

### 6. **Core ML (Apple) / DirectML (Microsoft)**
**Platform Support**: ❌ Platform-specific (iOS/Windows only)  

**Not applicable for Android**

---

## Comparison Matrix

| Framework | Android Support | DJL Conversion | Mobile Performance | Complexity | Recommendation |
|-----------|-----------------|-----------------|-------------------|------------|-----------------|
| **TensorFlow Lite** | ✓✓✓ Excellent | ❌ Multi-step | ✓✓✓ Best | Medium | ✓ For optimization |
| **ONNX Runtime** | ✓✓ Good | ⚠️ Partial | ✓✓ Good | Medium | ✓ For interop |
| **PyTorch Mobile** | ✓✓✓ Excellent | ✓✓✓ Direct | ✓✓ Good | Medium | ✓✓ BEST for DJL |
| **MediaPipe** | ✓✓✓ Excellent | ❌ N/A | ✓✓✓ Best | Low | ✓ For vision tasks |
| **Gluon/MXNet** | ⚠️ Limited | ✓ Possible | ⚠️ Moderate | Medium | ❌ Not recommended |

---

## Recommended Strategy for PojavBounce

### Tier 1: Primary Choice
**PyTorch Mobile** - Minimal migration effort from DJL
- Keep existing model training pipeline
- Direct .pt conversion
- Leverage PyTorch ecosystem
- Added Android-specific optimizations

### Tier 2: For Performance-Critical Features
**TensorFlow Lite** - Maximum mobile optimization
- Run alongside PyTorch Mobile for specific tasks
- Optimized for inference-heavy features
- Better battery life on resource-constrained devices
- Use for model compression (quantization, pruning)

### Tier 3: For Vision-Specific Features
**MediaPipe** - Pre-built optimized solutions
- Pose detection (anti-cheat detection)
- Object tracking
- Gesture recognition
- Zero model conversion needed

---

## Implementation Path: PyTorch Mobile

### Step 1: Export DJL Models to TorchScript
```python
import torch
import liquidbounce.deeplearn as lb

# Load DJL model
model = lb.load_model("model_name")

# Convert to TorchScript
traced_model = torch.jit.trace(model, example_input)
traced_model.save("model.pt")

# Or with scripting (supports control flow)
scripted_model = torch.jit.script(model)
scripted_model.save("model.pt")
```

### Step 2: Add PyTorch Mobile Gradle Dependency
```gradle
dependencies {
    implementation 'org.pytorch:pytorch_android:+'
}
```

### Step 3: Load and Infer in Kotlin
```kotlin
import org.pytorch.IValue
import org.pytorch.Module

val module = Module.load(assetPath)
val input = IValue.from(inputTensor)
val output = module.forward(input)
val result = output.toTensor()
```

### Step 4: Performance Optimization (Optional)
- **Quantization**: Convert FP32 → INT8 for 4x size reduction
- **Pruning**: Remove unused weights
- **Mobile-specific model architectures**: MobileNet, EfficientNet
- **Hardware acceleration**: Vulkan GPU delegate

---

## Conversion Tool Requirements

### For PyTorch Mobile
```bash
pip install torch torchscript torch-jit
# Built-in torch functionality, no extra tools needed
```

### For TensorFlow Lite (Multi-step)
```bash
pip install tensorflow tensorflow-lite tf2onnx onnx
# Step 1: PyTorch → ONNX
python -m tf2onnx.convert --model pytorch_model.pt --output model.onnx

# Step 2: ONNX → TFLite
python -m tensorflow.lite.python.lite --input_format=2 --input_file=model.onnx --output_file=model.tflite
```

### For ONNX Runtime
```bash
pip install onnx onnxruntime onnx-simplifier
# Convert PyTorch → ONNX
torch.onnx.export(model, dummy_input, "model.onnx")
```

---

## Risks & Mitigation

| Risk | Mitigation |
|------|-----------|
| Model size too large | Use quantization (INT8: 4x reduction) |
| Inference too slow | Profile on target device, use hardware acceleration |
| Memory constraints | Batch size=1, reduce model size via pruning |
| Conversion errors | Test conversion thoroughly, validate outputs |
| Runtime crashes on Android | Use exception handling, graceful fallbacks |

---

## Conclusion

**Recommended Next Steps:**
1. **Primary**: Integrate **PyTorch Mobile** for direct DJL model support
2. **Optional**: Add **MediaPipe** for vision-specific optimization
3. **Future**: Consider **TensorFlow Lite** for performance-critical features
4. **Not Recommended**: Gluon/MXNet (declining ecosystem)

**Current DJL approach is solid** for PojavLauncher environment. PyTorch Mobile adds native Android optimization while maintaining model compatibility.

