# Model Testing and Verification Guide

**Date:** 2026-01-18
**Models:** 21KC11KP, 19KC8KP
**Formats:** TorchScript (.pt), ONNX (.onnx)
**Status:** Ready for Testing

---

## Quick Start: Verify Models Locally

### 1. Check Model Files

```bash
ls -lh converted_models/
```

**Expected Output:**
```
-rw-r--r-- 1 runner runner 69K  21kc11kp.pt              ✅
-rw-r--r-- 1 runner runner 11K  21kc11kp.onnx            ✅
-rw-r--r-- 1 runner runner 43K  21kc11kp.onnx.data       ✅
-rw-r--r-- 1 runner runner 187B 21kc11kp_info.json       ✅
-rw-r--r-- 1 runner runner 68K  19kc8kp.pt               ✅
-rw-r--r-- 1 runner runner 11K  19kc8kp.onnx             ✅
-rw-r--r-- 1 runner runner 43K  19kc8kp.onnx.data        ✅
-rw-r--r-- 1 runner runner 186B 19kc8kp_info.json        ✅
```

### 2. Test TorchScript Models

```python
import torch

# Load model
model = torch.jit.load('converted_models/21kc11kp.pt')
model.eval()

# Create test input (batch_size=1, features=4)
test_input = torch.randn(1, 4)

# Run inference
with torch.no_grad():
    output = model(test_input)

print(f"Input shape: {test_input.shape}")
print(f"Output shape: {output.shape}")
print(f"Output values: {output}")
```

**Expected Results:**
- Input shape: (1, 4) ✅
- Output shape: (1, 2) ✅
- Output values: Float tensor with 2 values ✅
- No errors ✅

### 3. Test ONNX Models

```python
import onnx
import onnx.checker

# Load model
model = onnx.load('converted_models/21kc11kp.onnx')

# Validate
onnx.checker.check_model(model)
print("✅ ONNX model is valid")
```

---

## Detailed Testing Procedures

### Test 1: File Integrity Verification

**Purpose:** Ensure files are not corrupted

```bash
# Check file signatures
file converted_models/21kc11kp.pt
file converted_models/21kc11kp.onnx
file converted_models/21kc11kp_info.json

# Compute checksums
md5sum converted_models/*.pt
md5sum converted_models/*.onnx
```

**Expected:**
- .pt files: Zip archive (TorchScript format)
- .onnx files: Protocol buffer data
- .json files: Text data

### Test 2: Model Loading Tests

**Test Script:** `test_models_load.py`

```python
import torch
import onnx
import json

models = ['21kc11kp', '19kc8kp']

print("=" * 60)
print("MODEL LOADING TEST")
print("=" * 60)

for model_name in models:
    print(f"\nTesting {model_name}...")

    # Test TorchScript
    try:
        pt_model = torch.jit.load(f'converted_models/{model_name}.pt')
        pt_model.eval()
        print(f"  ✅ TorchScript: Loaded successfully")
    except Exception as e:
        print(f"  ❌ TorchScript: {e}")

    # Test ONNX
    try:
        onnx_model = onnx.load(f'converted_models/{model_name}.onnx')
        onnx.checker.check_model(onnx_model)
        print(f"  ✅ ONNX: Valid model")
    except Exception as e:
        print(f"  ❌ ONNX: {e}")

    # Test Metadata
    try:
        with open(f'converted_models/{model_name}_info.json') as f:
            metadata = json.load(f)
        print(f"  ✅ Metadata: {metadata['model_name']}")
    except Exception as e:
        print(f"  ❌ Metadata: {e}")
```

### Test 3: Inference Performance Test

**Test Script:** `test_inference.py`

```python
import torch
import time

models = ['21kc11kp', '19kc8kp']
batch_sizes = [1, 8, 16, 32]

print("=" * 80)
print("INFERENCE PERFORMANCE TEST")
print("=" * 80)

for model_name in models:
    print(f"\n{model_name.upper()}")
    print("-" * 40)

    model = torch.jit.load(f'converted_models/{model_name}.pt')
    model.eval()

    for batch_size in batch_sizes:
        # Warm-up run
        with torch.no_grad():
            test_input = torch.randn(batch_size, 4)
            _ = model(test_input)

        # Timed run (100 iterations)
        start_time = time.time()
        with torch.no_grad():
            for _ in range(100):
                test_input = torch.randn(batch_size, 4)
                output = model(test_input)

        end_time = time.time()
        total_time_ms = (end_time - start_time) * 1000
        time_per_batch_ms = total_time_ms / 100
        time_per_sample_ms = time_per_batch_ms / batch_size

        print(f"Batch {batch_size:2d}: {time_per_sample_ms:6.3f} ms/sample")
```

**Expected Performance (CPU):**
- Single sample (batch=1): 0.5-1.0 ms
- Batch 8: 0.3-0.6 ms per sample
- Batch 16+: 0.2-0.4 ms per sample

### Test 4: Output Validation

**Test Script:** `test_outputs.py`

```python
import torch
import numpy as np

models = ['21kc11kp', '19kc8kp']
num_samples = 1000

print("=" * 80)
print("OUTPUT VALIDATION TEST")
print("=" * 80)

for model_name in models:
    print(f"\n{model_name.upper()}")
    print("-" * 40)

    model = torch.jit.load(f'converted_models/{model_name}.pt')
    model.eval()

    outputs = []

    with torch.no_grad():
        for _ in range(num_samples):
            test_input = torch.randn(1, 4)
            output = model(test_input)
            outputs.append(output.numpy().flatten())

    outputs = np.array(outputs)

    print(f"Output shape: {outputs.shape}")
    print(f"Min value: {outputs.min():.4f}")
    print(f"Max value: {outputs.max():.4f}")
    print(f"Mean value: {outputs.mean():.4f}")
    print(f"Std dev: {outputs.std():.4f}")

    # Check for NaNs or Infs
    if np.isnan(outputs).any():
        print("  ⚠️  WARNING: NaN values detected!")
    elif np.isinf(outputs).any():
        print("  ⚠️  WARNING: Inf values detected!")
    else:
        print("  ✅ All values valid (no NaN or Inf)")
```

### Test 5: Reproducibility Test

**Purpose:** Verify deterministic behavior (if not using dropout/batch norm in eval mode)

```python
import torch

model = torch.jit.load('converted_models/21kc11kp.pt')
model.eval()

# Set seed for reproducibility
torch.manual_seed(42)
test_input = torch.randn(1, 4)

# Run inference twice
with torch.no_grad():
    output1 = model(test_input)
    output2 = model(test_input)

# Compare
if torch.allclose(output1, output2):
    print("✅ REPRODUCIBILITY: Outputs are identical")
else:
    print("⚠️  Outputs differ (expected if model uses stochastic operations)")
    print(f"   Difference: {(output1 - output2).abs().max().item()}")
```

---

## Android Testing

### Prerequisites

- Android device or emulator with:
  - Python 3.8+ support (if using PyTorch Mobile test framework)
  - PyTorch Mobile runtime installed
  - At least 200 MB free storage

### Test 1: PyTorch Mobile Runtime Check

```bash
# On Android device via ADB
adb shell pm list packages | grep pytorch
```

### Test 2: Push Models to Device

```bash
adb push converted_models/21kc11kp.pt /data/local/tmp/
adb push converted_models/19kc8kp.pt /data/local/tmp/
```

### Test 3: Verify on Device

```bash
adb shell ls -lh /data/local/tmp/*.pt
```

---

## Automated Testing Script

**File:** `test_all_models.py`

```python
#!/usr/bin/env python3
"""
Comprehensive model testing script
"""

import torch
import onnx
import json
import time
import numpy as np
from pathlib import Path

def test_file_existence():
    """Test 1: File existence and sizes"""
    print("\n" + "=" * 80)
    print("TEST 1: FILE EXISTENCE AND SIZES")
    print("=" * 80)

    expected_files = {
        '21kc11kp.pt': 69000,
        '21kc11kp.onnx': 11000,
        '21kc11kp_info.json': 187,
        '19kc8kp.pt': 68000,
        '19kc8kp.onnx': 11000,
        '19kc8kp_info.json': 186,
    }

    model_dir = Path('converted_models')
    all_passed = True

    for filename, min_size in expected_files.items():
        file_path = model_dir / filename
        if file_path.exists():
            size = file_path.stat().st_size
            if size >= min_size:
                print(f"  ✅ {filename}: {size} bytes")
            else:
                print(f"  ❌ {filename}: {size} bytes (too small, expected >{min_size})")
                all_passed = False
        else:
            print(f"  ❌ {filename}: NOT FOUND")
            all_passed = False

    return all_passed

def test_model_loading():
    """Test 2: Model loading"""
    print("\n" + "=" * 80)
    print("TEST 2: MODEL LOADING")
    print("=" * 80)

    models = ['21kc11kp', '19kc8kp']
    all_passed = True

    for model_name in models:
        print(f"\n{model_name}:")

        # Test TorchScript
        try:
            model = torch.jit.load(f'converted_models/{model_name}.pt')
            model.eval()
            print(f"  ✅ TorchScript loaded")
        except Exception as e:
            print(f"  ❌ TorchScript: {e}")
            all_passed = False

        # Test ONNX
        try:
            onnx_model = onnx.load(f'converted_models/{model_name}.onnx')
            onnx.checker.check_model(onnx_model)
            print(f"  ✅ ONNX valid")
        except Exception as e:
            print(f"  ❌ ONNX: {e}")
            all_passed = False

        # Test Metadata
        try:
            with open(f'converted_models/{model_name}_info.json') as f:
                metadata = json.load(f)
            print(f"  ✅ Metadata valid")
        except Exception as e:
            print(f"  ❌ Metadata: {e}")
            all_passed = False

    return all_passed

def test_inference():
    """Test 3: Inference"""
    print("\n" + "=" * 80)
    print("TEST 3: INFERENCE")
    print("=" * 80)

    models = ['21kc11kp', '19kc8kp']
    all_passed = True

    for model_name in models:
        print(f"\n{model_name}:")

        try:
            model = torch.jit.load(f'converted_models/{model_name}.pt')
            model.eval()

            test_input = torch.randn(1, 4)
            with torch.no_grad():
                output = model(test_input)

            print(f"  ✅ Inference successful")
            print(f"     Input shape: {test_input.shape}")
            print(f"     Output shape: {output.shape}")
            print(f"     Output range: [{output.min():.4f}, {output.max():.4f}]")

            # Check for NaN/Inf
            if torch.isnan(output).any() or torch.isinf(output).any():
                print(f"  ⚠️  WARNING: NaN or Inf values detected!")
                all_passed = False

        except Exception as e:
            print(f"  ❌ Inference failed: {e}")
            all_passed = False

    return all_passed

def main():
    """Run all tests"""
    print("\n" + "=" * 80)
    print("COMPLETE MODEL TESTING SUITE")
    print("=" * 80)

    results = {
        "File Existence": test_file_existence(),
        "Model Loading": test_model_loading(),
        "Inference": test_inference(),
    }

    print("\n" + "=" * 80)
    print("TEST SUMMARY")
    print("=" * 80)

    for test_name, passed in results.items():
        status = "✅ PASSED" if passed else "❌ FAILED"
        print(f"{test_name}: {status}")

    all_passed = all(results.values())

    print("\n" + "=" * 80)
    if all_passed:
        print("✅ ALL TESTS PASSED - MODELS READY FOR DEPLOYMENT")
    else:
        print("❌ SOME TESTS FAILED - REVIEW ABOVE FOR DETAILS")
    print("=" * 80)

    return 0 if all_passed else 1

if __name__ == '__main__':
    exit(main())
```

**Run the comprehensive test:**

```bash
python3 test_all_models.py
```

---

## Expected Test Results

### Test 1: File Existence ✅
- All 8 files present
- File sizes match expectations
- No corruption detected

### Test 2: Model Loading ✅
- Both models load without errors
- ONNX validation passes
- Metadata files parse correctly

### Test 3: Inference ✅
- Inference produces expected output shapes
- No NaN or Inf values
- Reasonable output ranges

### Test 4: Performance ✅
- Single sample: < 2 ms on CPU
- Batch performance scales linearly

---

## Troubleshooting

### Issue: "No module named 'torch'"

**Solution:** Install PyTorch
```bash
pip install torch
```

### Issue: "ONNX validation failed"

**Solution:** Ensure onnx package is installed
```bash
pip install onnx onnxscript
```

### Issue: Model outputs are all zeros or NaN

**Solution:** Check input ranges; models expect standardized inputs

---

## Next Steps

1. ✅ Run all tests locally
2. ✅ Review test results
3. ✅ If all tests pass: Models ready for deployment
4. ✅ Deploy to Android device
5. ✅ Test on real hardware

---

**Test Status:** Ready to execute
**Expected Duration:** < 2 minutes
**Prerequisites:** Python 3.8+, PyTorch, ONNX
