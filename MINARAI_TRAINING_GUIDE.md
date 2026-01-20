# Minarai Model Training Guide

## Overview

Minarai is a machine learning-based angle smoothing system that learns natural aiming patterns from gameplay data. This guide covers the complete workflow to train a new Minarai model from scratch.

## Architecture

### Neural Network Structure
```
Input (4) → Linear(128) → BatchNorm → ReLU →
          Linear(64) → BatchNorm → ReLU →
          Linear(32) → BatchNorm → ReLU →
          Linear(2) → Output
```

### Input Features (4 values)
1. **Total Delta Yaw** - Angle difference between current and target rotation (yaw)
2. **Total Delta Pitch** - Angle difference between current and target rotation (pitch)
3. **Velocity Delta Yaw** - Previous rotation velocity (yaw direction)
4. **Velocity Delta Pitch** - Previous rotation velocity (pitch direction)

### Output Features (2 values)
1. **Adjusted Yaw Delta** - Smoothed yaw adjustment
2. **Adjusted Pitch Delta** - Smoothed pitch adjustment

## Training Workflow

### Phase 1: Data Collection (In-Game)

#### Option A: Combat Recording

Use `MinaraiCombatRecorder` to record real combat sessions:

1. **Enable Debug Recorder Module**
   - In LiquidBounce settings, enable the DebugRecorder module
   - Select "MinaraiCombat" as the debug recorder mode

2. **Record Combat Data**
   - Engage in actual PvP combat with enemies
   - The recorder automatically:
     - Tracks targets within 10 blocks (configurable)
     - Records rotation data when you're moving towards targets
     - Captures combat sequences when you attack enemies
     - Filters out non-combat states (flying, sneaking, etc.)

3. **Data Output**
   - Combat data is saved as JSON files in the config directory
   - Each recording contains `TrainingData` objects with:
     - Current, previous, target rotation vectors
     - Player and target movement data
     - Distance and hurt time metrics
     - Age (how long you've been tracking the target)

#### Option B: Training Simulation

Use `MinaraiTrainer` for controlled data generation:

1. **Enable MinaraiTrainer Mode**
   - In DebugRecorder, select "MinaraiTrainer"
   - The system spawns test targets (slimes) near you

2. **Training Mechanics**
   - Slimes spawn at random distances (2-3 blocks away)
   - You attack the slimes (InteractPacket)
   - The system records your aiming movements
   - New slimes spawn after each hit
   - Data is collected in sequences

3. **Advantages**
   - Controlled environment
   - Consistent spawn patterns
   - Easier to collect large amounts of data quickly
   - No need for actual opponents

### Phase 2: Data Preparation (Python)

#### Collect Training Data

```bash
# Training data is stored in JSON format
# Location: config_directory/debug_records/minarai*.json

# Example structure:
{
  "a": [0.1, 0.2, 0.3],        # current rotation vector
  "b": [0.05, 0.15, 0.28],     # previous rotation vector
  "c": [0.15, 0.25, 0.35],     # target rotation vector
  "d": [0.05, 0.1],            # velocity delta (yaw, pitch)
  "e": 5,                        # hurt time
  "f": 12,                       # age (ticks)
  "g": [1.0, 0.0, 0.5],        # player movement diff
  "h": [-0.5, 0.0, 1.0],       # target movement diff
  "i": 5.5                       # distance
}
```

#### Create Training Script

Create `train_minarai.py`:

```python
#!/usr/bin/env python3
"""
Train a new Minarai model from collected combat data.

Usage:
    python train_minarai.py \
        --data-dir ./training_data \
        --output-path ./new_model.pth \
        --epochs 50 \
        --batch-size 32
"""

import argparse
import json
import torch
import torch.nn as nn
import torch.optim as optim
from torch.utils.data import Dataset, DataLoader
from pathlib import Path
import numpy as np
from tqdm import tqdm


class TrainingDataset(Dataset):
    """Load training data from JSON files"""

    def __init__(self, data_dir):
        self.data = []
        data_path = Path(data_dir)

        # Load all JSON files
        for json_file in data_path.glob("*.json"):
            with open(json_file, 'r') as f:
                records = json.load(f)
                if isinstance(records, list):
                    self.data.extend(records)
                else:
                    self.data.append(records)

    def __len__(self):
        return len(self.data)

    def __getitem__(self, idx):
        record = self.data[idx]

        # Extract input features from TrainingData structure
        current_vec = record['a']  # currentVector
        previous_vec = record['b']  # previousVector
        target_vec = record['c']    # targetVector
        velocity_delta = record['d'] # velocityDelta

        # Compute rotation deltas (simplified)
        total_delta_yaw = target_vec[0] - current_vec[0]
        total_delta_pitch = target_vec[1] - current_vec[1]

        prev_vel_yaw = current_vec[0] - previous_vec[0]
        prev_vel_pitch = current_vec[1] - previous_vec[1]

        # Create input: [total_delta_yaw, total_delta_pitch, prev_vel_yaw, prev_vel_pitch]
        input_features = torch.tensor([
            total_delta_yaw,
            total_delta_pitch,
            prev_vel_yaw,
            prev_vel_pitch
        ], dtype=torch.float32)

        # Create output: [adjusted_yaw_delta, adjusted_pitch_delta]
        output_features = torch.tensor(velocity_delta, dtype=torch.float32)

        return input_features, output_features


class MinaraiModel(nn.Module):
    """Neural network for angle smoothing"""

    def __init__(self):
        super().__init__()
        self.model = nn.Sequential(
            nn.Linear(4, 128),
            nn.BatchNorm1d(128),
            nn.ReLU(),
            nn.Linear(128, 64),
            nn.BatchNorm1d(64),
            nn.ReLU(),
            nn.Linear(64, 32),
            nn.BatchNorm1d(32),
            nn.ReLU(),
            nn.Linear(32, 2)
        )

    def forward(self, x):
        return self.model(x)


def train_model(model, train_loader, val_loader, epochs, device, output_path):
    """Train the model"""

    criterion = nn.MSELoss()
    optimizer = optim.Adam(model.parameters(), lr=0.001)
    scheduler = optim.lr_scheduler.ReduceLROnPlateau(
        optimizer, mode='min', factor=0.5, patience=5, verbose=True
    )

    best_val_loss = float('inf')
    patience_counter = 0
    max_patience = 15

    for epoch in range(epochs):
        # Training phase
        model.train()
        train_loss = 0.0

        for inputs, targets in tqdm(train_loader, desc=f"Epoch {epoch+1}/{epochs}"):
            inputs = inputs.to(device)
            targets = targets.to(device)

            optimizer.zero_grad()
            outputs = model(inputs)
            loss = criterion(outputs, targets)
            loss.backward()
            optimizer.step()

            train_loss += loss.item()

        train_loss /= len(train_loader)

        # Validation phase
        model.eval()
        val_loss = 0.0

        with torch.no_grad():
            for inputs, targets in val_loader:
                inputs = inputs.to(device)
                targets = targets.to(device)

                outputs = model(inputs)
                loss = criterion(outputs, targets)
                val_loss += loss.item()

        val_loss /= len(val_loader)

        print(f"Epoch {epoch+1}/{epochs} - Train Loss: {train_loss:.6f}, Val Loss: {val_loss:.6f}")

        scheduler.step(val_loss)

        # Early stopping
        if val_loss < best_val_loss:
            best_val_loss = val_loss
            patience_counter = 0

            # Save best model
            torch.save(model.state_dict(), output_path)
            print(f"  Saved best model to {output_path}")
        else:
            patience_counter += 1
            if patience_counter >= max_patience:
                print(f"Early stopping after {epoch+1} epochs")
                break

    print(f"\nTraining complete! Best model saved to {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Train Minarai model")
    parser.add_argument("--data-dir", type=str, required=True, help="Directory containing training JSON files")
    parser.add_argument("--output-path", type=str, required=True, help="Output path for trained model")
    parser.add_argument("--epochs", type=int, default=50, help="Number of training epochs")
    parser.add_argument("--batch-size", type=int, default=32, help="Batch size")
    parser.add_argument("--val-split", type=float, default=0.2, help="Validation split ratio")

    args = parser.parse_args()

    # Check CUDA availability
    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    print(f"Using device: {device}")

    # Load dataset
    print("Loading training data...")
    dataset = TrainingDataset(args.data_dir)
    print(f"Loaded {len(dataset)} training samples")

    # Split into train/val
    val_size = int(len(dataset) * args.val_split)
    train_size = len(dataset) - val_size
    train_dataset, val_dataset = torch.utils.data.random_split(
        dataset, [train_size, val_size]
    )

    # Create data loaders
    train_loader = DataLoader(
        train_dataset, batch_size=args.batch_size, shuffle=True
    )
    val_loader = DataLoader(
        val_dataset, batch_size=args.batch_size, shuffle=False
    )

    # Create and train model
    model = MinaraiModel().to(device)

    print("\nStarting training...")
    train_model(
        model, train_loader, val_loader,
        args.epochs, device, args.output_path
    )


if __name__ == "__main__":
    main()
```

### Phase 3: Model Export (Python)

#### Export to ExecuTorch Format

Once you have a trained PyTorch model, convert it to ExecuTorch for Android deployment:

```bash
python export_minarai.py \
    --model-path ./new_model.pth \
    --output-dir ./models/executorch
```

The export process:
1. Loads your trained PyTorch model
2. Traces it with example input
3. Exports via `torch.export.export()`
4. Lowers to ExecuTorch edge format with XNNPACK optimization
5. Converts to `.pte` format
6. Saves metadata JSON

#### Export Script

Use the existing `export_minarai.py` or modify it for your model:

```python
# In export_minarai.py, update the model creation to load from a checkpoint:
def create_or_load_minarai_model(checkpoint_path=None) -> Sequential:
    model = Sequential(
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

    if checkpoint_path:
        model.load_state_dict(torch.load(checkpoint_path))

    model.eval()
    return model
```

### Phase 4: Integration (Kotlin)

#### Register Model in Code

1. **Add model files to resources**
```bash
mkdir -p src/main/resources/resources/liquidbounce/models/executorch/
cp ./new_model.pte src/main/resources/resources/liquidbounce/models/executorch/
cp ./new_model.json src/main/resources/resources/liquidbounce/models/executorch/
```

2. **Update ModelHolster** (if needed)
   - Modify `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/ModelHolster.kt`
   - Add your model to the available models list

3. **Use in Angle Smoothing**
   - In `MinaraiAngleSmooth`, select your model from the dropdown
   - Configure output multipliers if needed:
     - Yaw Multiplier: 0.5-2.0 (higher = more aggressive smoothing)
     - Pitch Multiplier: 0.5-2.0

## Complete Workflow Example

```bash
# 1. Collect data (in-game)
# Enable MinaraiCombat or MinaraiTrainer in LiquidBounce
# Record 5000+ samples from combat/training sessions
# Data saved to: ~/.liquidbounce/debug_records/minarai_*.json

# 2. Prepare and organize data
mkdir -p ./training_data
cp ~/.liquidbounce/debug_records/minarai_*.json ./training_data/

# 3. Train model
python train_minarai.py \
    --data-dir ./training_data \
    --output-path ./my_model.pth \
    --epochs 100 \
    --batch-size 64

# 4. Export to ExecuTorch
python export_minarai.py \
    --model-path ./my_model.pth \
    --output-dir ./exported_models

# 5. Deploy
cp ./exported_models/*.pte src/main/resources/resources/liquidbounce/models/executorch/
cp ./exported_models/*.json src/main/resources/resources/liquidbounce/models/executorch/

# 6. Rebuild
./gradlew build
```

## Data Quality Tips

### What Makes Good Training Data

1. **Diverse Combat Scenarios**
   - Fight multiple opponent types
   - Vary distances (3-20 blocks)
   - Different angles and heights

2. **Natural Movements**
   - Avoid robotic/scripted patterns
   - Include combat delays and pauses
   - Capture realistic aiming curves

3. **Sufficient Volume**
   - Minimum: 2000-5000 samples
   - Recommended: 10,000+ samples
   - More data = better generalization

4. **Balanced Dataset**
   - Roughly equal distribution across distances
   - Mix of fast and slow aiming
   - Various target velocities

### Data Cleaning

```python
# Remove outliers
def filter_training_data(records):
    filtered = []
    for record in records:
        # Skip if distance is unrealistic (e.g., > 50 blocks)
        if record['i'] > 50:
            continue

        # Skip if rotation deltas are impossibly large
        delta_magnitude = (record['d'][0]**2 + record['d'][1]**2) ** 0.5
        if delta_magnitude > 5.0:
            continue

        filtered.append(record)

    return filtered
```

## Training Tips

### Hyperparameters

- **Learning Rate**: 0.001-0.01 (start with 0.001)
- **Batch Size**: 32-64 (higher for stable training)
- **Epochs**: 50-200 (use early stopping)
- **Validation Split**: 0.15-0.2

### Optimization Techniques

1. **Data Augmentation**
   - Add small noise to inputs (±5% on deltas)
   - Rotate perspectives
   - Scale distances

2. **Loss Functions**
   - MSELoss: For smooth regression (recommended)
   - HuberLoss: For robustness to outliers
   - L1Loss: For sparse adjustments

3. **Regularization**
   - Dropout layers after BatchNorm
   - L2 weight decay (0.0001-0.001)
   - Early stopping (patience=10-20)

## Performance Considerations

### Model Size vs Accuracy Trade-off

- **Current Models**: ~50 KB (DJL) → ~15-20 KB (ExecuTorch)
- **Inference Latency**: 2-5ms on modern Android (ARM64-v8a)
- **Accuracy Impact**: Minimal when exporting to ExecuTorch

### Optimization for Android

1. **XNNPACK Partitioner** (enabled by default)
   - Optimizes for CPU inference
   - Reduces model size
   - Improves speed

2. **Quantization** (optional)
   - Convert float32 → int8
   - Further size reduction
   - Slight accuracy loss (~1-2%)

## Troubleshooting

### Model Not Converging

- **Solution**: Reduce learning rate, increase training data, check data quality
- **Alternative**: Try with momentum optimizer (SGD with momentum=0.9)

### Overfitting

- **Symptoms**: High training accuracy, low validation accuracy
- **Solutions**:
  - Add dropout layers
  - Increase L2 regularization
  - Collect more diverse data

### Poor In-Game Performance

- **Symptoms**: Model runs but aiming feels jittery
- **Solutions**:
  - Adjust OutputMultiplier values (yaw/pitch)
  - Try InterpolationAngleSmooth correction mode
  - Retrain with more balanced data

### Export Failures

- **Check**: PyTorch version, ExecuTorch compatibility
- **Solution**: `pip install torch executorch "executorch-backends-xnnpack>=0.2.0"`

## References

- Training Data Structure: `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/data/TrainingData.kt`
- Combat Recorder: `src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/misc/debugrecorder/modes/MinaraiCombatRecorder.kt`
- Angle Smoothing: `src/main/kotlin/net/ccbluex/liquidbounce/utils/aiming/features/processors/anglesmooth/impl/MinaraiAngleSmooth.kt`
- ExecuTorch Documentation: https://pytorch.org/executorch/stable/
- PyTorch Export: https://pytorch.org/docs/stable/export.html

## Next Steps

1. **Collect Training Data**: Use MinaraiTrainer for quick setup
2. **Create Training Script**: Use the provided `train_minarai.py`
3. **Experiment with Hyperparameters**: Try different batch sizes, learning rates
4. **Export and Test**: Use export_minarai.py to convert to ExecuTorch
5. **Iterate**: Refine based on in-game performance
