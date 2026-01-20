# Training Minarai Models Using `.models improve` Command

## Overview

LiquidBounce has a **built-in training system** using DJL (Deep Java Library) that trains models directly in-game without needing external Python scripts. The `.models` command provides easy access to this training pipeline.

## Quick Start

### Create a New Model
```
.models create MyModel
```

### Improve an Existing Model
```
.models improve 21KC11KP
```

The system will:
1. Collect all combat data from MinaraiCombat and MinaraiTrainer
2. Prepare the dataset
3. Train the model (100 epochs by default)
4. Save the trained model
5. Reload the GUI with the new model available

## How It Works

### Architecture

The built-in training uses **DJL with Adam optimizer**:

```
Input (4 features)
   ↓
Linear(128) + BatchNorm + ReLU
   ↓
Linear(64) + BatchNorm + ReLU
   ↓
Linear(32) + BatchNorm + ReLU
   ↓
Linear(2 outputs)
```

### Training Configuration (in ModelWrapper.kt)

- **Loss Function**: L2 Loss (Mean Squared Error)
- **Optimizer**: Adam with learning rate 0.001
- **Epochs**: 100
- **Batch Size**: 32
- **Initializer**: Xavier Initialization

## Workflow

### Phase 1: Collect Training Data (In-Game)

#### Using MinaraiTrainer (Recommended for Quick Training)

1. **Enable MinaraiTrainer in DebugRecorder**
   ```
   # In settings or via ModuleDebugRecorder
   Select "MinaraiTrainer" mode
   ```

2. **Run Training Session**
   - Slimes spawn near you at random distances (2-3 blocks)
   - Attack each slime
   - System records your aiming movements
   - New slimes spawn after each hit
   - Continue for 10-30 minutes to collect 5000-10000 samples

3. **Data is Saved Automatically**
   - Location: `config_directory/debug_records/minarai_trainer_*.json`

#### Using MinaraiCombat (For Real Combat Data)

1. **Enable MinaraiCombat in DebugRecorder**
   ```
   Select "MinaraiCombat" mode
   ```

2. **Engage in PvP Combat**
   - The system automatically tracks and records targets
   - Only records when you're actively moving toward targets
   - Saves data when combat ends (20+ ticks of inactivity)

3. **Data is Saved Automatically**
   - Location: `config_directory/debug_records/minarai_combat_*.json`

### Phase 2: Train the Model (In-Game)

#### Create New Model

```
.models create CustomModel
```

**What happens:**
1. **Sample Loading** (~1-5 seconds)
   - Loads all JSON files from:
     - `debug_records/minarai_trainer_*.json`
     - `debug_records/minarai_combat_*.json`
   - Converts TrainingData objects to feature arrays
   - Example: "Loaded 8,432 samples in 2.45s"

2. **Dataset Preparation** (~1-10 seconds)
   - Creates input features: `[total_delta_yaw, total_delta_pitch, prev_vel_yaw, prev_vel_pitch]`
   - Creates output labels: `[adjusted_yaw, adjusted_pitch]`
   - Initializes DJL ArrayDataset with batch sampling
   - Example: "Prepared data in 3.21s"

3. **Model Training** (2-30 minutes depending on data size)
   - Shows progress in chat/overlay:
     ```
     Epoch 1/100: Loss = 0.0345
     Epoch 2/100: Loss = 0.0289
     Epoch 3/100: Loss = 0.0245
     ...
     Epoch 100/100: Loss = 0.0018
     ```
   - The model is learning natural aiming patterns
   - Training runs on default executor (non-blocking)
   - You can play while training completes

4. **Save and Activate**
   - Model saved to: `liquidbounce/config/models/CustomModel/`
   - GUI reloads automatically
   - Model is available for selection in MinaraiAngleSmooth

#### Improve Existing Model

```
.models improve 21KC11KP
```

**What happens:**
- Loads the existing model's weights
- Continues training with new data
- Combines old knowledge + new samples
- Better than retraining from scratch

### Phase 3: Use the Model

1. **Select in Settings**
   - Go to Rotation System → Angle Smooth
   - Select "Minarai" smoothing type
   - Choose your model from dropdown

2. **Fine-tune Output**
   - **Yaw Multiplier** (0.5-2.0): Controls horizontal aiming sensitivity
   - **Pitch Multiplier** (0.5-2.0): Controls vertical aiming sensitivity
   - Higher = more aggressive smoothing

3. **Test in Combat**
   - Engage in PvP
   - Observe how your aiming feels
   - Adjust multipliers as needed

## Available Commands

### `.models create <name>`
Creates a new model trained from all collected combat data
- Model name must be alphanumeric and hyphens only
- Example: `my-model-v2`, `combat-trained`

### `.models improve <name>`
Fine-tunes an existing model with new combat data
- Loads existing model + trains with new samples
- Great for iterative improvement
- Example: `21KC11KP` (improves the built-in model)

### `.models delete <name>`
Deletes a model from disk
- Cannot delete built-in models (21KC11KP, 19KC8KP)
- Removes model folder completely
- Example: `old-model-v1`

### `.models reload`
Reloads all models from disk
- Useful if you add models manually
- Updates GUI model list

### `.models browse`
Opens the models folder in file explorer
- Location: `liquidbounce/config/models/`
- Useful for managing model files

## Input/Output Features

### Input (4 features)
1. **Total Delta Yaw** - Horizontal angle difference from current to target
2. **Total Delta Pitch** - Vertical angle difference from current to target
3. **Previous Velocity Yaw** - How much your horizontal angle changed last tick
4. **Previous Velocity Pitch** - How much your vertical angle changed last tick

### Output (2 features)
1. **Adjusted Yaw Delta** - Smoothed horizontal aiming adjustment
2. **Adjusted Pitch Delta** - Smoothed vertical aiming adjustment

## Training Parameters

All hardcoded in `ModelWrapper.kt`:

```kotlin
private const val NUM_EPOCH = 100      // Number of training iterations
private const val BATCH_SIZE = 32      // Samples per batch
// Learning Rate: 0.001 (Adam optimizer)
// Loss: L2 Loss (MSE)
// Initializer: Xavier
```

To customize these, modify the source code and rebuild.

## File Structure

### Saved Models
```
liquidbounce/config/models/
├── MyModel/
│   ├── variables/
│   │   ├── variables.index
│   │   ├── variables.data-00000-of-00001
│   └── model.params
├── 21KC11KP/
│   └── (trained model files)
└── 19KC8KP/
    └── (trained model files)
```

### Training Data
```
liquidbounce/config/debug_records/
├── minarai_trainer_2024-01-15_12-34-56.json
├── minarai_trainer_2024-01-15_13-45-21.json
├── minarai_combat_2024-01-15_14-22-33.json
└── ...
```

Each JSON file contains an array of TrainingData objects:
```json
[
  {
    "a": [0.1, 0.2, 0.3],     // currentVector
    "b": [0.05, 0.15, 0.28],  // previousVector
    "c": [0.15, 0.25, 0.35],  // targetVector
    "d": [0.05, 0.1],         // velocityDelta
    "e": 5,                    // hurtTime
    "f": 12,                   // age
    "g": [1.0, 0.0, 0.5],    // playerDiff
    "h": [-0.5, 0.0, 1.0],   // targetDiff
    "i": 5.5                   // distance
  },
  ...
]
```

## Performance Characteristics

### Training Time
- **With 5,000 samples**: ~2-5 minutes
- **With 10,000 samples**: ~5-15 minutes
- **With 20,000+ samples**: ~15-30 minutes
- Runs asynchronously (non-blocking)

### Model Performance
- **Inference latency**: ~5-10ms per prediction (DJL)
- **Memory usage**: ~50-100 MB (DJL loaded)
- **Accuracy**: Depends on training data quality

### Exporting to ExecuTorch
Once trained, you can export to Android:
```bash
# Use the Python export script
python export_minarai.py --model-path liquidbounce/config/models/MyModel/
```

This creates a `.pte` file (~15-20 KB) for Android deployment.

## Tips for Better Models

### Data Collection Tips

1. **Varied Combat Scenarios**
   - Fight at different distances (3-20 blocks)
   - Vary angles (front, side, above, below)
   - Different speeds (fast snapping, slow tracking)

2. **Sufficient Volume**
   - Minimum: 2,000 samples
   - Recommended: 5,000-10,000 samples
   - Excellent: 20,000+ samples

3. **MinaraiTrainer vs MinaraiCombat**
   - **MinaraiTrainer**: Faster data collection, consistent patterns
   - **MinaraiCombat**: Natural gameplay data, more realistic but slower

### Training Tips

1. **Multiple Iterations**
   - Train v1 with 5,000 samples
   - Use in combat, collect more data
   - Train v2 using `.models improve`
   - Repeat for iterative improvement

2. **Monitor Loss**
   - Loss should decrease each epoch
   - If loss plateaus, you have enough data for this architecture
   - If loss increases, learning rate might be too high

3. **Comparison Testing**
   - Keep a baseline model (21KC11KP)
   - Test your model in same scenarios
   - A/B test with different data

## Troubleshooting

### Command Not Found
- Make sure the `/` prefix is used
- Example: `/.models create MyModel` (if not aliased)
- Or just `.models create MyModel` (if aliased)

### No Samples Found
```
Error: No samples loaded!
```
**Causes:**
- No data collected yet (run MinaraiTrainer/Combat first)
- Data files are empty or corrupted
- Debug recorder is not saving to correct location

**Solutions:**
- Run MinaraiTrainer for 5-10 minutes
- Check `liquidbounce/config/debug_records/` folder
- Enable debug recorder and test a session

### Training Fails/Crashes
```
Error: DeepLearningEngine is not initialized
```
**Causes:**
- DJL failed to initialize at startup
- Missing Java dependencies
- Incompatible Java version

**Solutions:**
- Check logs for DJL initialization errors
- Restart the client
- Ensure Java 17+ is installed

### Model Doesn't Improve Performance
**Possible Causes:**
- Training data was not diverse enough
- Model is overfitting to training data
- Multiplier values need adjustment
- Correction mode setting is suboptimal

**Solutions:**
- Collect more diverse data
- Try different Correction mode (Interpolation recommended)
- Adjust Yaw/Pitch multipliers (try 1.2-1.5)
- Ensure model was actually trained (check loss decreased)

## Comparison: Built-in vs Python Training

| Feature | Built-in (`.models`) | Python Scripts |
|---------|----------------------|----------------|
| Setup | None - built-in | Requires Python + dependencies |
| Data Collection | In-game | In-game |
| Training Location | In-game (Minecraft) | External scripts |
| Speed | DJL (5-30 min) | PyTorch (2-10 min) |
| Inference | DJL on PC/Android | ExecuTorch on Android, PyTorch on PC |
| Customization | Limited (hardcoded) | Full control |
| Android Export | Manual (Python) | Via export scripts |
| Real-time Feedback | Yes (overlay) | No |
| File Format | DJL .params | PyTorch .pt, ExecuTorch .pte |

## Advanced: Modifying Training Parameters

To change NUM_EPOCH, BATCH_SIZE, or learning rate:

1. **Edit ModelWrapper.kt**
   ```kotlin
   private const val NUM_EPOCH = 200  // Was 100
   private const val BATCH_SIZE = 64  // Was 32

   // In optOptimizer section:
   .optLearningRateTracker(Tracker.fixed(0.0005f))  // Was 0.001
   ```

2. **Rebuild the project**
   ```bash
   ./gradlew build
   ```

3. **Train models with new parameters**
   ```
   .models create HighEpochModel
   ```

## References

- Command Implementation: `src/main/kotlin/net/ccbluex/liquidbounce/features/command/commands/deeplearn/CommandModels.kt`
- Model Training: `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/models/ModelWrapper.kt`
- Model Manager: `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/ModelHolster.kt`
- Training Data: `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/data/TrainingData.kt`
- DJL Documentation: https://docs.djl.ai/
- Adam Optimizer: https://en.wikipedia.org/wiki/Stochastic_gradient_descent#Adam
