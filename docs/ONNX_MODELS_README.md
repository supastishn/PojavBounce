# ONNX Models CI/CD Pipeline

This document explains the ONNX model generation and fetching system.

## Overview

The project has a two-part ONNX model system:

1. **Build Workflow** (`.github/workflows/build.yml`)
   - Runs on every push to `nextgen` branch
   - Exports DJL SavedModels
   - Converts to ONNX format (opset 9)
   - Uploads models as artifacts
   - Creates PRs with generated models

2. **ONNX Models Release Workflow** (`.github/workflows/onnx-models-release.yml`)
   - Can be triggered manually or on schedule (weekly)
   - Generates opset 9 ONNX models in a zip file
   - Uploads as artifact for easy download

## For Developers

### Generating Models Locally

#### Prerequisites
```bash
pip install tf2onnx tensorflow onnx onnxruntime
```

#### Export DJL SavedModels
```bash
./gradlew exportDjlSavedModels
```

This creates `build/exports/savedmodels/` with TensorFlow SavedModel directories.

#### Convert to ONNX (opset 9)
```bash
./scripts/convert_saved_models.py
```

Output goes to: `src/main/resources/resources/liquidbounce/models/`

#### Or use the fallback params-to-ONNX converter
```bash
for dump in build/exports/export-debug/*.params.dump; do
  name=$(basename "$dump" .params.dump)
  python ./scripts/params_to_onnx.py "$dump" 2 "src/main/resources/resources/liquidbounce/models/${name}.onnx"
done
```

## For Users/Integration

### Option 1: Fetch from Latest GitHub Actions Run

Use the provided fetch script:
```bash
./scripts/fetch_onnx_models.sh [repo] [output_dir]
```

**Examples:**
```bash
# Fetch to current directory
./scripts/fetch_onnx_models.sh

# Fetch to specific directory
./scripts/fetch_onnx_models.sh supastishn/PojavBounce ./my_models

# With GitHub token (for private repos or higher rate limits)
GITHUB_TOKEN=github_pat_xxx ./scripts/fetch_onnx_models.sh
```

**Environment Variables:**
- `GITHUB_TOKEN` - Optional GitHub API token
- `WORKFLOW_NAME` - Workflow name to fetch from (default: "ONNX Models Release")

**Output:** Extracts models to `models/` directory

### Option 2: Manual Download from GitHub Actions

1. Go to https://github.com/supastishn/PojavBounce/actions
2. Find the latest "ONNX Models Release" or "Build" workflow run
3. Download the `onnx-models-*` artifact
4. Extract the zip file

### Option 3: Integration with Your Deploy Script

Combine with the existing deploy script:
```bash
#!/bin/bash
# Download latest ONNX models
./scripts/fetch_onnx_models.sh

# Then use your deploy script
./deploy.sh
```

## Model Specifications

### IR Version vs Opset vs ONNX Runtime

| ONNX Runtime | Max IR Version | Max Opset | Notes |
|---|---|---|---|
| 1.16.3 (current) | 11 | 19 | May be limited to IR v9 on Android |
| 1.20.0+ | 13 | 25+ | Full support for recent ONNX specs |

**Current Status:**
- Generated models: **IR version 9, opset 9** (maximum compatibility)
- Target runtime: ONNX Runtime 1.16.3 on Android

## Troubleshooting

### Models have wrong IR version

If models are still being generated with IR version > 9:

1. **Check conversion script** - Ensure `--opset 9` flag is being used
2. **Check tf2onnx version** - May need to update:
   ```bash
   pip install --upgrade tf2onnx
   ```
3. **Check ONNX version** - May need compatible version:
   ```bash
   pip install "onnx<1.17"  # For ONNX v1.x
   ```

### Fetch script fails

**No artifacts found:**
- Ensure the workflow has run successfully at least once
- Check that you're using the right repo name

**Rate limit exceeded:**
- Set `GITHUB_TOKEN` environment variable with a personal access token

**Permission denied:**
- For private repos, you need a GitHub token with `repo` scope

## CI Integration

The workflows are triggered:
- **Build workflow**: Every push to `nextgen` branch
- **ONNX Release workflow**: Manual trigger or weekly schedule

To modify schedule, edit `.github/workflows/onnx-models-release.yml`:
```yaml
schedule:
  - cron: '0 0 * * 0'  # Weekly at midnight UTC
```

## Related Scripts

- `scripts/convert_saved_models.py` - Convert TF SavedModels to ONNX
- `scripts/params_to_onnx.py` - Convert DJL params dumps to ONNX (heuristic)
- `scripts/downconvert_onnx_opset9.py` - Attempt to downgrade ONNX models
- `scripts/fetch_onnx_models.sh` - Fetch models from GitHub Actions
- `deploy.sh` - Deploy liquidbounce artifact to Minecraft mods folder
