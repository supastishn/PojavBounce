# Minarai Training Methods Comparison & Integration Guide

## Three Ways to Train Minarai Models

```
┌─────────────────────────────────────────────────────────────┐
│ MINARAI TRAINING PIPELINE OVERVIEW                           │
└─────────────────────────────────────────────────────────────┘

DATA COLLECTION
   │
   ├─ MinaraiTrainer (spawned targets)
   └─ MinaraiCombat (real PvP data)

   ↓

TRAINING (Choose one)
   │
   ├─ Built-in (DJL + .models improve) ← Easiest
   ├─ Python (PyTorch training script) ← Most control
   └─ Hybrid (DJL training + Python export) ← Best of both

   ↓

DEPLOYMENT
   │
   ├─ PC (DJL models) ← .models create/improve
   ├─ Android via ExecuTorch (.pte) ← Python export
   └─ Both (supports both backends) ← MinaraiModel class
```

## Method 1: Built-in Training (Recommended for Most Users)

### Summary
Train models directly in-game using DJL without external tools.

### Commands
```
.models create MyModel        # Create new model
.models improve 21KC11KP      # Fine-tune existing model
.models delete MyModel        # Delete model
.models reload               # Reload all models
.models browse               # Open models folder
```

### Workflow

**Step 1: Collect Data** (in-game)
```
# Option A: Quick data collection
- Enable MinaraiTrainer
- Attack slimes for 10-30 minutes
- Collects 5,000-10,000 samples quickly

# Option B: Natural combat data
- Enable MinaraiCombat
- Fight real opponents in PvP
- Collects authentic aiming patterns
```

**Step 2: Train Model** (in-game, takes 2-30 minutes)
```
.models create MyCustomModel
```

**Step 3: Use Model** (in-game)
```
Rotation System → Angle Smooth → Select "Minarai" → Choose "MyCustomModel"
Adjust: Yaw Multiplier (0.5-2.0), Pitch Multiplier (0.5-2.0)
```

### Pros
✅ No setup required
✅ Train while playing
✅ Real-time feedback via overlay
✅ Works on Android (DJL backend)
✅ Instant gratification

### Cons
❌ Limited customization (hardcoded parameters)
❌ Slower than PyTorch (DJL vs PyTorch)
❌ Can't modify architecture
❌ Must have DJL library loaded

### Technical Details
- **Framework**: DJL (Deep Java Library)
- **Loss**: L2Loss (MSE)
- **Optimizer**: Adam (lr=0.001)
- **Epochs**: 100 (hardcoded)
- **Batch Size**: 32 (hardcoded)
- **Storage**: `liquidbounce/config/models/MyModel/`

### When to Use
- You want quick, hassle-free training
- You're not comfortable with Python/command-line
- You want to train multiple iterations fast
- You want real-time training feedback
- You're testing different data collection methods

---

## Method 2: Python PyTorch Training (Advanced Control)

### Summary
Train models using Python for maximum control and flexibility, then integrate back into LiquidBounce.

### Workflow

**Step 1: Collect Data** (in-game)
```
# Use MinaraiTrainer or MinaraiCombat
Files saved to: ~/.liquidbounce/debug_records/minarai_*.json
```

**Step 2: Set Up Python Environment**
```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate

pip install torch
pip install executorch executorch-backends-xnnpack
```

**Step 3: Create Training Script** (see MINARAI_TRAINING_GUIDE.md)
```bash
python train_minarai.py \
    --data-dir ./training_data \
    --output-path ./my_model.pth \
    --epochs 200 \
    --batch-size 64
```

**Step 4: Export to ExecuTorch** (for Android)
```bash
python export_minarai.py \
    --model-path ./my_model.pth \
    --output-dir ./models/executorch
```

**Step 5: Deploy Back to LiquidBounce**
```bash
# For PC (DJL):
# Convert .pth to DJL format using torch2djl utilities

# For Android (ExecuTorch):
cp my_model.pte src/main/resources/resources/liquidbounce/models/executorch/
./gradlew build
```

### Pros
✅ Full customization (architecture, hyperparameters, loss function)
✅ Faster training (PyTorch is optimized)
✅ Can modify network architecture
✅ Better for experimentation
✅ Professional ML workflow

### Cons
❌ Requires Python setup
❌ Steeper learning curve
❌ Need to export to integrate back
❌ More complex workflow
❌ Conversion between PyTorch → DJL required

### Technical Details
- **Framework**: PyTorch
- **Loss**: MSELoss (customizable to HuberLoss, L1Loss, etc.)
- **Optimizer**: Adam (customizable lr)
- **Epochs**: Variable (default 50-100)
- **Batch Size**: Variable (default 32-64)
- **Export**: ExecuTorch .pte format for Android

### When to Use
- You know Python and want full control
- You need to experiment with architectures
- You want the fastest training possible
- You need Android (.pte) deployment
- You're building a research project
- You want to use advanced loss functions

---

## Method 3: Hybrid (Best of Both Worlds)

### Summary
Use built-in DJL training for quick iterations, export to PyTorch for fine-tuning, then re-export to ExecuTorch.

### Workflow

**Step 1: Quick Initial Training (DJL)**
```
.models create MyModel        # Fast training with DJL
```

**Step 2: Export to PyTorch** (optional conversion)
```bash
# Save DJL model as PyTorch checkpoint
# (Requires DJL ↔ PyTorch bridge)
```

**Step 3: Fine-tune or Rebuild with Python**
```bash
python train_minarai.py \
    --pretrained ./my_model.pth \
    --data-dir ./new_training_data \
    --epochs 50  # Fine-tune instead of full train
```

**Step 4: Export to ExecuTorch**
```bash
python export_minarai.py --model-path ./fine_tuned_model.pth
```

**Step 5: Deploy**
```bash
# Android deployment
cp fine_tuned_model.pte src/main/resources/...
./gradlew build

# PC deployment (reimport to DJL)
.models browse  # Manual model management
```

### Pros
✅ Quick initial training with DJL
✅ Fine-tune with PyTorch when needed
✅ Supports both PC and Android
✅ Best performance on each platform
✅ Flexible workflow

### Cons
❌ Most complex setup
❌ Requires both DJL and PyTorch
❌ Need conversion tools
❌ Multiple training cycles

### When to Use
- You want both speed (DJL) and control (PyTorch)
- You're deploying to both PC and Android
- You're doing iterative model refinement
- You want to leverage both ecosystems

---

## Decision Tree

```
START: I want to train a Minarai model
│
├─ Do I know Python?
│  │
│  ├─ NO → Use Built-in (.models improve)
│  │
│  └─ YES → Continue...
│
├─ Do I only target PC or Android too?
│  │
│  ├─ ONLY PC → Use Built-in (.models improve)
│  │
│  └─ PC + ANDROID → Continue...
│
├─ Do I need to customize hyperparameters?
│  │
│  ├─ NO → Use Built-in (.models improve)
│  │
│  └─ YES → Continue...
│
├─ Do I want fast experimentation?
│  │
│  ├─ YES → Use Python Training (Method 2)
│  │
│  └─ NO (want both) → Use Hybrid (Method 3)
│
END: Choose appropriate method
```

## Feature Comparison Table

| Feature | Built-in (DJL) | Python (PyTorch) | Hybrid |
|---------|---|---|---|
| **Setup Time** | 0 | 10-20 min | 10-20 min |
| **Training Speed** | ~5-30 min | ~2-10 min | ~3-15 min |
| **Customization** | None | Full | Full |
| **Architecture Changes** | ❌ | ✅ | ✅ |
| **Hyperparameter Tuning** | ❌ | ✅ | ✅ |
| **PC Inference** | ✅ (DJL) | ✅ (PyTorch) | ✅ (DJL) |
| **Android Inference** | ❌ (requires export) | ✅ (ExecuTorch) | ✅ (ExecuTorch) |
| **Real-time Feedback** | ✅ | ❌ | ✅ (initial) |
| **Model Export** | Manual | Automatic | Automatic |
| **Iteration Speed** | Fast | Medium | Fast |
| **Learning Curve** | Easy | Medium-Hard | Medium-Hard |

## Recommended Workflows by Use Case

### Use Case 1: "I just want better aiming smoothing"
```
→ Use Built-in Method (Command: .models improve 21KC11KP)
  Time: 10 min data collection + 5 min training = 15 min total
  Complexity: Minimal
```

### Use Case 2: "I want to experiment with different training approaches"
```
→ Use Built-in Method iteratively
  1. .models create v1
  2. Collect more data
  3. .models improve v1
  4. Compare results
  5. Repeat
  Time per iteration: ~10-15 min
```

### Use Case 3: "I want the absolute best performance on Android"
```
→ Use Hybrid Method
  1. .models create quick-v1 (DJL training)
  2. Collect large dataset
  3. Train with Python (PyTorch)
  4. Export to ExecuTorch (.pte)
  5. Deploy to Android
  Time: ~30-45 min total
```

### Use Case 4: "I'm doing serious ML research on angle smoothing"
```
→ Use Python Method with modifications
  1. Implement custom architectures
  2. Test different loss functions
  3. Run hyperparameter sweeps
  4. Export best model to ExecuTorch
  Time: As long as needed
```

### Use Case 5: "I want to deploy to both PC and Android optimally"
```
→ Use Hybrid Method
  1. DJL for quick iteration on PC
  2. Python + ExecuTorch for Android optimization
  3. Maintain both models in parallel
```

## Integration Points

### DJL Models (Built-in)
```
Model Training (DJL)
         ↓
Saved to: liquidbounce/config/models/MyModel/
         ↓
Available in: Rotation System → Angle Smooth → Minarai → [Model List]
         ↓
Inference: MinaraiAngleSmooth.process()
```

### PyTorch Models
```
Model Training (PyTorch)
         ↓
Export: torch.export.export() → torch.pth
         ↓
Convert: to_edge_transform_and_lower()
         ↓
Saved to: model.pte (ExecuTorch format)
         ↓
Deploy: src/main/resources/liquidbounce/models/executorch/
         ↓
Rebuild: ./gradlew build
         ↓
Available: On Android devices (ExecuTorchModel)
```

### Hybrid Integration
```
PyTorch Model (model.pth)
    ↓
Export to ExecuTorch (model.pte)
    ↓
Rebuild APK with .pte files
    ↓
Android deployment (uses ExecuTorchModel)
    ↓
On PC: Still can use DJL models (.models create/improve)
```

## Storage Locations

### Built-in Training (DJL)
```
PC: liquidbounce/config/models/MyModel/
    ├── variables/
    │   ├── variables.index
    │   └── variables.data-00000-of-00001
    └── model.params
```

### Python Training (PyTorch)
```
Local: ./my_model.pth
Export: ./my_model.pte (ExecuTorch)
Deploy: src/main/resources/liquidbounce/models/executorch/my_model.pte
```

### Training Data (Both)
```
Collected: ~/.liquidbounce/debug_records/
    ├── minarai_trainer_*.json
    ├── minarai_combat_*.json
    └── ...
```

## Troubleshooting Integration

### Model appears in one method but not another
**Cause**: Different storage locations
**Solution**: Ensure models are in correct folder:
- DJL: `liquidbounce/config/models/`
- ExecuTorch: `src/main/resources/liquidbounce/models/executorch/`

### Training takes forever
- **DJL**: Normal (5-30 min depending on data). Try fewer epochs in source.
- **PyTorch**: Normal (2-10 min). Try smaller batch size or fewer features.

### Model works on PC but not Android
- **Likely cause**: Used DJL model instead of ExecuTorch
- **Solution**: Export to .pte using `export_minarai.py`

### ExecuTorch export fails
```bash
# Install/update ExecuTorch
pip install --upgrade executorch executorch-backends-xnnpack
```

## Next Steps

1. **Quick Start** → Use Built-in Method (`.models improve`)
2. **Optimization** → Once comfortable, try Python Method
3. **Production** → Use Hybrid Method for both platforms
4. **Advanced** → Modify architecture in Python, integrate custom layers

---

## References

- **Built-in Training**: MINARAI_BUILTIN_TRAINING.md (this file)
- **Python Training**: MINARAI_TRAINING_GUIDE.md
- **Code Files**:
  - Command: `src/main/kotlin/.../CommandModels.kt`
  - DJL Training: `src/main/kotlin/.../ModelWrapper.kt`
  - Model Manager: `src/main/kotlin/.../ModelHolster.kt`
  - ExecuTorch: `src/main/kotlin/.../ExecuTorchModel.kt`
  - Scripts: `src/main/resources/scripts/executorch/`
