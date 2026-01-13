# ONNX CI/CD Implementation Summary

## What Was Created

### 1. GitHub Actions Workflow: ONNX Models Release
**File:** `.github/workflows/onnx-models-release.yml`

- Automatically exports DJL SavedModels
- Converts to ONNX format with opset 9 (maximum compatibility)
- Packages models as `onnx-models.zip`
- Uploads as GitHub Actions artifact
- Can be triggered manually or on schedule (default: weekly)

**Trigger methods:**
- Manual: Go to Actions → "ONNX Models Release" → "Run workflow"
- Automatic: Weekly schedule (configurable)

### 2. Fetch Script: fetch_onnx_models.sh
**File:** `scripts/fetch_onnx_models.sh`

Downloads the latest ONNX models from GitHub Actions without needing the full build.

**Usage:**
```bash
./scripts/fetch_onnx_models.sh [repo] [output_dir]

# Examples:
./scripts/fetch_onnx_models.sh                                    # Current dir, default repo
./scripts/fetch_onnx_models.sh supastishn/PojavBounce ./models    # Custom directory
GITHUB_TOKEN=xxx ./scripts/fetch_onnx_models.sh                   # With auth
```

### 2b. Deploy Script: deploy-onnx.sh
**File:** `deploy-onnx.sh`

Downloads the latest ONNX models from GitHub Actions artifacts and deploys them to the project.

**Usage:**
```bash
export GITHUB_TOKEN="your_token_here"
./deploy-onnx.sh

# Optional: customize output directory
export MODELS_DIR="custom/path/to/models"
./deploy-onnx.sh
```

**Features:**
- Fetches latest ONNX models directly from CI artifacts
- Uses jq for reliable JSON parsing
- Verifies deployment with model count
- Works with GitHub authentication tokens

### 3. Enhanced Conversion Script
**File:** `scripts/convert_saved_models.py` (updated)

Now includes IR version checking and warnings about compatibility.

### 4. Supporting Scripts

**Downconvert utility:**
- File: `scripts/downconvert_onnx_opset9.py`
- Purpose: Attempt to downgrade ONNX models (limited effectiveness)
- Note: IR version cannot be automatically lowered; requires rebuilding the model

### 5. Documentation
**Files:**
- `docs/ONNX_MODELS_README.md` - Complete guide for users and developers
- `ONNX_IR_VERSION_FIX.md` - Technical details about IR version compatibility

## Workflow: Before vs After

### Before (Manual Process)
```
1. Run gradle task locally: ./gradlew exportDjlSavedModels
2. Run Python script: ./scripts/convert_saved_models.py
3. Manually manage model files
4. Commit/push if regenerated
```

### After (Automated)
```
1. Trigger workflow in GitHub Actions (manual or automatic)
2. Workflow automatically:
   - Exports SavedModels
   - Converts to ONNX opset 9
   - Packages as zip
   - Uploads as artifact
3. Download via: ./scripts/fetch_onnx_models.sh
```

## Integration with Existing Systems

### Existing Build Workflow
The existing `build.yml` workflow continues to work:
- Generates ONNX models during build
- Creates PR with generated models
- Now with improved IR version checking

### New ONNX Release Workflow
New dedicated workflow for standalone ONNX generation:
- Independent from main build
- Focused on model generation only
- Easy artifact download

### Deploy Script Integration
Can chain the fetch script with your deploy script:
```bash
#!/bin/bash
./scripts/fetch_onnx_models.sh ./fetched_models
./deploy.sh
```

## Key Features

✅ **Automated Model Generation** - No local Gradle/Python setup needed
✅ **Opset 9 Compliance** - Maximum compatibility with mobile ONNX Runtime
✅ **IR Version Checking** - Warns about compatibility issues
✅ **Easy Download** - Fetch script handles artifact extraction
✅ **Documentation** - Complete guides for users and developers
✅ **Fallback Support** - params-to-ONNX conversion as fallback

## Files Created/Modified

**New Files:**
- `.github/workflows/onnx-models-release.yml`
- `scripts/fetch_onnx_models.sh`
- `scripts/downconvert_onnx_opset9.py`
- `docs/ONNX_MODELS_README.md`
- `ONNX_IR_VERSION_FIX.md`
- `deploy-onnx.sh` (standalone ONNX model deployment)

**Modified Files:**
- `scripts/convert_saved_models.py` (added IR version checking)

## Next Steps

1. **Push to GitHub** - Commit these files and push to trigger the workflow
2. **Verify Workflow** - Check Actions tab for successful run
3. **Test Fetch Script** - Run `./scripts/fetch_onnx_models.sh` to download models
4. **Update Integration** - Modify deploy/build processes to use fetch script

## Troubleshooting

| Issue | Solution |
|---|---|
| Models still IR v13 | Upgrade ONNX Runtime to 1.20.0+ or find source SavedModels and regenerate |
| Fetch script fails | Ensure workflow has run successfully; use GITHUB_TOKEN for auth |
| No SavedModels exported | Check gradle output; may need Java/JDK issues resolved |
| tf2onnx conversion fails | Update: `pip install --upgrade tf2onnx tensorflow onnx` |

## Related Documentation

- Complete guide: `docs/ONNX_MODELS_README.md`
- IR version details: `ONNX_IR_VERSION_FIX.md`
- Original deploy script: `deploy.sh`
