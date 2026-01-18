# Minarai Quick Reference Guide

## What is Minarai?

A machine learning-based angle smoothing system that learns natural aiming patterns from your gameplay to improve combat assistance.

**Input**: 4 angle/velocity features
**Output**: 2 adjusted angle deltas (yaw, pitch)
**Models**: 21KC11KP, 19KC8KP (built-in) + custom trained

---

## Fastest Way to Get Started

### 1. Collect Training Data (10-30 minutes in-game)
```
Enable ModuleDebugRecorder (in settings)
Select: "MinaraiTrainer" mode
Action: Attack spawned slimes for 20-30 minutes
Result: ~5,000-10,000 training samples saved automatically
```

### 2. Train Model (5-30 minutes, runs in-game)
```
Command: .models create MyModel
Wait: Training completes while you play
Result: Model saved and ready to use
```

### 3. Use Model (Instant)
```
Settings: Rotation System → Angle Smooth
Type: Select "Minarai"
Model: Choose "MyModel" from dropdown
Adjust: Yaw Multiplier 1.0-1.5, Pitch Multiplier 1.0-1.5
```

---

## Three Training Methods

| Method | Time | Skill | Control | Android |
|--------|------|-------|---------|---------|
| **Built-in** (`/.models improve`) | 2-30 min | Beginner | Low | ❌ |
| **Python** (`train_minarai.py`) | 2-10 min | Advanced | High | ✅ |
| **Hybrid** | 3-15 min | Advanced | High | ✅ |

### Built-in Method (Recommended for Most)
```bash
.models create MyModel
# Done! Training happens in-game
# Model automatically selectable
```

### Python Method (Advanced, Android Support)
```bash
python train_minarai.py --data-dir ./data --output-path model.pth
python export_minarai.py --model-path model.pth
# Copy .pte file to Android resources and rebuild
```

---

## Key Commands

```
.models create <name>     # Create new model from all collected data
.models improve <name>    # Train existing model with new data
.models delete <name>     # Delete a model
.models reload           # Reload all models from disk
.models browse           # Open models folder
```

---

## Model Configuration

### In Settings
```
Rotation System
└─ Angle Smooth
   ├─ Type: Minarai
   ├─ Model: [MyModel]
   ├─ Output Multiplier
   │  ├─ Yaw: 1.5 (0.5-2.0)
   │  └─ Pitch: 1.0 (0.5-2.0)
   └─ Correction: InterpolationAngleSmooth
```

### Multiplier Tuning
- **Yaw/Pitch = 0.5-1.0**: Subtle smoothing (realistic but less assistance)
- **Yaw/Pitch = 1.0-1.5**: Normal smoothing (recommended)
- **Yaw/Pitch = 1.5-2.0**: Aggressive smoothing (more assistance)

---

## Data Collection Tips

### MinaraiTrainer (Recommended)
✅ Fastest data collection
✅ Controlled spawn patterns
✅ Consistent training conditions
✅ ~5,000 samples in 20-30 min

**How**: Enable MinaraiTrainer, attack slimes

### MinaraiCombat (Realistic)
✅ Real PvP data
✅ Natural gameplay patterns
✅ Authentic aiming curves
❌ Slower data collection

**How**: Enable MinaraiCombat, fight players

---

## File Locations

```
Training Data:   ~/.liquidbounce/debug_records/minarai_*.json
Trained Models:  ~/.liquidbounce/config/models/
Exported Models: ./src/main/resources/liquidbounce/models/executorch/
```

---

## Input/Output Features

### Input (4 values)
1. **Total Delta Yaw**: Current → Target horizontal angle difference
2. **Total Delta Pitch**: Current → Target vertical angle difference
3. **Prev Velocity Yaw**: Previous horizontal angle velocity
4. **Prev Velocity Pitch**: Previous vertical angle velocity

### Output (2 values)
1. **Adjusted Yaw Delta**: Smoothed horizontal adjustment
2. **Adjusted Pitch Delta**: Smoothed vertical adjustment

---

## Training Parameters (Built-in)

- **Epochs**: 100
- **Batch Size**: 32
- **Learning Rate**: 0.001
- **Loss Function**: L2 Loss (MSE)
- **Optimizer**: Adam
- **Architecture**: 4 → 128 → 64 → 32 → 2

To customize: Edit `ModelWrapper.kt` and rebuild

---

## Troubleshooting

### "No samples found" error
**Solution**: Collect data with MinaraiTrainer (10+ min)

### Training takes forever
**Normal**: 5-30 min depending on data size
**Speed up**: Use fewer epochs (edit source code)

### Model doesn't improve aim
**Try**:
- Increase multiplier values (1.2-1.5)
- Collect more diverse data
- Switch to InterpolationAngleSmooth correction
- Retrain with different data source

### Command not working
**Solution**:
- Check `/help models` for syntax
- Try `/.models create name` instead of `.models`

---

## Performance Metrics

### Built-in Training (DJL)
- **Training Speed**: ~5-30 min (100 epochs)
- **Inference Latency**: 5-10 ms
- **Memory**: ~50-100 MB (loaded)
- **File Size**: ~50 KB (DJL .params)

### Python Training (PyTorch)
- **Training Speed**: ~2-10 min (100 epochs)
- **Inference**: Same as DJL on PC
- **Android Export Size**: ~15-20 KB (.pte)
- **Android Latency**: 2-5 ms

---

## Comparison: Built-in vs Existing Models

```
Built-in Models (21KC11KP, 19KC8KP)
├─ Pros: Always available, pre-trained on large dataset
└─ Cons: Generic, may not match your playstyle

Custom Models (trained by you)
├─ Pros: Tailored to your aiming style, improvable
└─ Cons: Needs training data, quality depends on data
```

---

## Advanced: Improving with `.models improve`

```
# Training cycle:
1. .models create MyModel        # Initial training
2. Use in combat, collect more data
3. .models improve MyModel       # Fine-tune with new data
4. Repeat steps 2-3 for better models

# Benefits:
- Keeps previous learning
- Builds on existing knowledge
- Faster than retraining from scratch
```

---

## For Android Deployment

### Option 1: Built-in Export
```bash
# Requires Python + ExecuTorch setup
python export_minarai.py --model-path ~/.liquidbounce/config/models/MyModel/
```

### Option 2: Python Training + Export
```bash
# Train with Python, export directly
python train_minarai.py --data-dir ./data --output-path model.pth
python export_minarai.py --model-path model.pth
```

### Option 3: Rebuild APK
```bash
# Copy .pte files to resources
cp model.pte src/main/resources/liquidbounce/models/executorch/
# Rebuild
./gradlew build
```

---

## Common Statistics

### Data Collection
- **MinaraiTrainer**: 200-300 samples/minute
- **MinaraiCombat**: 50-100 samples/minute
- **Recommended**: 5,000-10,000 samples minimum

### Training Performance
- **Small Dataset** (1,000 samples): 2 min
- **Medium Dataset** (5,000 samples): 5-10 min
- **Large Dataset** (20,000 samples): 15-30 min

### Model Quality
- **Very Bad Data**: Loss = 0.5+ (won't help)
- **Poor Data**: Loss = 0.1-0.3 (marginal improvement)
- **Good Data**: Loss = 0.01-0.05 (noticeable improvement)
- **Excellent Data**: Loss < 0.01 (significant improvement)

---

## Decision: Which Method?

### I want to start RIGHT NOW
→ `.models improve 21KC11KP`

### I want quick custom model
→ `.models create MyModel`

### I want best possible results
→ Python training + ExecuTorch export

### I want Android deployment
→ Python training + export_minarai.py

### I want to experiment
→ Built-in method (fast iterations)

---

## Learning Resources

### In This Repository
1. `MINARAI_TRAINING_GUIDE.md` - Complete Python training guide
2. `MINARAI_BUILTIN_TRAINING.md` - Built-in method details
3. `MINARAI_METHODS_COMPARISON.md` - Detailed comparison

### Code Files
- `CommandModels.kt` - `.models` command implementation
- `ModelWrapper.kt` - DJL training logic
- `MinaraiAngleSmooth.kt` - Inference at runtime
- `TrainingData.kt` - Data structures

### External
- PyTorch: https://pytorch.org/
- ExecuTorch: https://pytorch.org/executorch/
- DJL: https://docs.djl.ai/

---

## Quick Checklist: Train & Deploy

- [ ] **Data**: Run MinaraiTrainer for 20-30 minutes
- [ ] **Train**: `.models create MyModel` (wait 5-30 min)
- [ ] **Test**: Select MyModel in settings
- [ ] **Tune**: Adjust Yaw/Pitch multipliers (1.0-1.5)
- [ ] **Verify**: Test in PvP combat
- [ ] **Iterate** (optional): `.models improve MyModel` with more data
- [ ] **Export** (Android): `python export_minarai.py` (if needed)
- [ ] **Deploy** (Android): Rebuild APK with .pte files (if needed)

---

## FAQ

**Q: Which model should I start with?**
A: Either `.models improve 21KC11KP` or `.models create MyModel` - both work great.

**Q: How much data do I need?**
A: Minimum 2,000 samples, recommended 5,000-10,000.

**Q: Can I train multiple models?**
A: Yes! Create different models with `.models create name1`, `.models create name2`, etc.

**Q: Is this detectable/against TOS?**
A: It's a rotation smoothing feature integrated into LiquidBounce. Check server rules.

**Q: Can I share my trained model?**
A: Models are in `liquidbounce/config/models/` - you can backup and share files.

**Q: Does Android need special setup?**
A: Yes, requires ExecuTorch .pte files. Built-in DJL models don't work on Android.

**Q: How do I know if my model is good?**
A: Test in combat. If your aim feels smoother/better than before, it's good!

---

Last Updated: 2025-01-18
Repository: PojavBounce (nextgen branch)
