# Minarai Training Documentation - Complete Summary

## What You Now Have

I've created **4 comprehensive guides** for training Minarai models in PojavBounce:

### 1. **MINARAI_QUICK_REFERENCE.md** ⭐ START HERE
- **Best for**: Everyone (quickest overview)
- **Contains**: 3-step quickstart, command cheatsheet, FAQ
- **Read time**: 5 minutes
- **Covers**:
  - Fastest way to get started
  - All commands at a glance
  - Troubleshooting for common issues

### 2. **MINARAI_BUILTIN_TRAINING.md**
- **Best for**: In-game training using `.models improve`
- **Contains**: Complete guide to DJL-based training
- **Read time**: 15-20 minutes
- **Covers**:
  - How `.models create` and `.models improve` work
  - Built-in training parameters
  - Real-time training feedback
  - File structure and storage locations
  - Performance characteristics

### 3. **MINARAI_TRAINING_GUIDE.md**
- **Best for**: Advanced users wanting full control with Python
- **Contains**: Complete PyTorch training pipeline
- **Read time**: 20-30 minutes
- **Covers**:
  - Python training script (ready to use)
  - Data preparation from JSON files
  - Hyperparameter tuning and optimization
  - ExecuTorch export for Android
  - Advanced techniques and troubleshooting

### 4. **MINARAI_METHODS_COMPARISON.md**
- **Best for**: Choosing the right training approach
- **Contains**: Detailed comparison of all methods
- **Read time**: 10-15 minutes
- **Covers**:
  - Built-in vs Python vs Hybrid approaches
  - Decision tree for choosing method
  - Feature comparison table
  - Integration points for each approach

---

## Three Ways to Train

### Quick Answer
```
Want easiest?  → .models improve 21KC11KP
Want control?  → python train_minarai.py
Want both?     → Hybrid approach (both)
```

### Method 1: Built-in (Recommended for Most)
```bash
# 1. Collect data (20-30 min in-game)
Enable MinaraiTrainer → Attack slimes

# 2. Train model (5-30 min, runs in-game)
.models create MyModel

# 3. Use immediately
Settings → Rotation System → Angle Smooth → Select MyModel
```

✅ No setup
✅ Real-time feedback
✅ Works right now
❌ Limited customization

### Method 2: Python (Most Control)
```bash
# 1. Collect data (in-game)
Enable MinaraiTrainer/Combat → Collect samples

# 2. Train with Python (2-10 min)
python train_minarai.py --data-dir ./data --output-path model.pth

# 3. Export for Android
python export_minarai.py --model-path model.pth --output-dir ./models

# 4. Deploy
cp ./models/*.pte src/main/resources/liquidbounce/models/executorch/
./gradlew build  # Rebuild for Android
```

✅ Full customization
✅ Faster training
✅ Android support
❌ Requires Python setup

### Method 3: Hybrid (Best of Both)
- Use `.models improve` for quick iteration
- Use Python for fine-tuning and Android export
- Get speed of DJL + control of PyTorch

---

## What Gets Trained

### Input Features (4 values)
1. **Total Delta Yaw** - Horizontal angle to target
2. **Total Delta Pitch** - Vertical angle to target
3. **Velocity Yaw** - Previous horizontal rotation speed
4. **Velocity Pitch** - Previous vertical rotation speed

### Output Features (2 values)
1. **Adjusted Yaw Delta** - Smoothed horizontal adjustment
2. **Adjusted Pitch Delta** - Smoothed vertical adjustment

### Architecture
```
Input (4) → Linear(128) → BatchNorm → ReLU →
          Linear(64) → BatchNorm → ReLU →
          Linear(32) → BatchNorm → ReLU →
          Linear(2) → Output
```

---

## Data Collection

### MinaraiTrainer (Recommended)
```
Enable → Attack spawned slimes → 20-30 minutes
Result → 5,000-10,000 samples
Pros   → Fast, consistent, controlled
```

### MinaraiCombat (Realistic)
```
Enable → Fight real players → Variable time
Result → Authentic PvP data
Pros   → Real gameplay patterns
```

### How Much Data?
- **Minimum**: 2,000 samples
- **Good**: 5,000-10,000 samples
- **Excellent**: 20,000+ samples

---

## Quick Commands

```bash
# Built-in Training (DJL)
.models create MyModel          # Train new model
.models improve 21KC11KP        # Improve existing model
.models delete MyModel          # Delete model
.models reload                  # Reload all models
.models browse                  # Open models folder

# Python Training (PyTorch)
python train_minarai.py --data-dir ./data --output-path model.pth
python export_minarai.py --model-path model.pth --output-dir ./out
```

---

## Model Configuration (In-Game)

```
Rotation System
└─ Angle Smooth
   ├─ Type: Minarai
   ├─ Model: [Select your model]
   ├─ Output Multiplier
   │  ├─ Yaw: 1.5 (adjust 0.5-2.0)
   │  └─ Pitch: 1.0 (adjust 0.5-2.0)
   └─ Correction: InterpolationAngleSmooth (recommended)
```

### Multiplier Values
- **0.5-1.0**: Subtle smoothing
- **1.0-1.5**: Normal (recommended)
- **1.5-2.0**: Aggressive

---

## Training Times (Typical)

| Phase | Duration | Method |
|-------|----------|--------|
| Data Collection | 20-30 min | In-game (any method) |
| Built-in Training | 5-30 min | DJL (depends on data) |
| Python Training | 2-10 min | PyTorch (faster) |
| Export to Android | 2-5 min | ExecuTorch conversion |

---

## File Locations

```
Training Data:    ~/.liquidbounce/debug_records/minarai_*.json
Trained Models:   ~/.liquidbounce/config/models/
Python Export:    ./exported_models/
Android Deploy:   src/main/resources/liquidbounce/models/executorch/
```

---

## Performance Metrics

### Built-in (DJL)
- Inference: 5-10 ms per prediction
- Model Size: ~50 KB (.params)
- Platforms: PC only

### Python/ExecuTorch
- Inference: 2-5 ms per prediction (Android)
- Model Size: ~15-20 KB (.pte)
- Platforms: Android + PC

---

## Decision Guide

```
START: I want to train a Minarai model

IF no Python experience
  → Use Built-in (.models improve)

IF want Android deployment
  → Use Python (export to ExecuTorch)

IF want fastest training
  → Use Python (PyTorch is faster)

IF want zero setup
  → Use Built-in (.models improve)

IF want maximum control
  → Use Python (full customization)

IF want both speed and control
  → Use Hybrid (DJL + Python)
```

---

## Example Workflow

### Day 1: Quick Training
```
1. Enable MinaraiTrainer (20 min) → ~5,000 samples
2. .models create day1-model (10 min) → Model ready
3. Test in combat → Evaluate quality
```

### Day 2: Improve Model
```
1. Collect more data with MinaraiTrainer (20 min) → ~3,000 new samples
2. .models improve day1-model (8 min) → Fine-tuned model
3. Test vs Day 1 → Better performance expected
```

### Day 3: Prepare for Android
```
1. Final data collection (30 min) → Large dataset
2. python train_minarai.py (5 min) → Best model
3. python export_minarai.py (2 min) → Android .pte file
4. ./gradlew build → Deploy to Android
```

---

## Key Insights

### Why Minarai Works
- Learns from YOUR aiming style (personalized)
- Captures natural movement patterns
- Improves with more quality data
- Adapts to different combat scenarios

### What Makes Good Training Data
- ✅ Diverse combat scenarios
- ✅ Multiple distances and angles
- ✅ Mix of fast/slow aiming
- ✅ Sufficient volume (5000+ samples)
- ✅ Natural gameplay (not scripted)

### What to Avoid
- ❌ Very small datasets (<1000 samples)
- ❌ Only one type of combat
- ❌ Robotic/scripted movements
- ❌ Data from servers with lag

---

## Getting Help

### Troubleshooting
Check appropriate guide:
- **Quick fix**: MINARAI_QUICK_REFERENCE.md → FAQ section
- **Built-in issues**: MINARAI_BUILTIN_TRAINING.md → Troubleshooting
- **Python issues**: MINARAI_TRAINING_GUIDE.md → Troubleshooting

### Code References
- Command implementation: `src/main/kotlin/.../CommandModels.kt`
- Training logic: `src/main/kotlin/.../ModelWrapper.kt`
- Model manager: `src/main/kotlin/.../ModelHolster.kt`
- Inference: `src/main/kotlin/.../MinaraiAngleSmooth.kt`

---

## Next Steps

### For Immediate Use
1. Read: **MINARAI_QUICK_REFERENCE.md** (5 min)
2. Do: Collect data with MinaraiTrainer (20 min)
3. Do: `.models create MyModel` (5-30 min)
4. Test: In combat

### For Optimization
1. Read: **MINARAI_BUILTIN_TRAINING.md** (15 min)
2. Do: Multiple training iterations with `.models improve`
3. Tune: Multiplier values for best feel

### For Advanced Control
1. Read: **MINARAI_TRAINING_GUIDE.md** (20 min)
2. Setup: Python environment
3. Do: Train with custom hyperparameters
4. Export: To ExecuTorch for Android

---

## Summary Stats

📊 **Documentation Created**
- 4 comprehensive guides
- ~1,750 lines of documentation
- Multiple code examples
- Decision trees and comparisons

🎯 **Coverage**
- Beginner to advanced workflows
- Quick reference to deep dives
- Built-in and external methods
- PC and Android deployment

⚡ **Key Topics**
- Data collection (in-game)
- Training (DJL or PyTorch)
- Deployment (PC or Android)
- Troubleshooting and optimization

---

## Commit Information

```
Commit: ca439ed6c
Author: Claude <noreply@anthropic.com>
Files: 4 new documentation files
Lines: ~1,750 total
Time: 2025-01-18
```

---

## Final Notes

The documentation covers everything needed to:
1. **Understand** what Minarai is and how it works
2. **Collect** training data from gameplay
3. **Train** models using your preferred method
4. **Deploy** models on PC and/or Android
5. **Optimize** and iterate on training
6. **Troubleshoot** common issues

All information is **practical and actionable** with real code examples and step-by-step instructions.

Start with the **Quick Reference**, progress to your chosen method guide, and refer to the Comparison guide if you're unsure which approach to take.

**Happy training!** 🚀
