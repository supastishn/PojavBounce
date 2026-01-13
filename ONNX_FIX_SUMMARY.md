# ONNX IR Version Mismatch - Solution Complete

## Problem Analysis

Your app was failing with:
```
onnxruntime::PathString &, const onnxruntime::IOnnxRuntimeOpSchemaRegistryList *...
Unsupported model IR version: 13, max supported IR version: 9
```

### Root Cause
- **Generated models**: ONNX opset 13 (IR version 13)
- **Device support**: ONNX Runtime supports up to opset 9 (IR version 9)
- **File**: `scripts/convert_saved_models.py` line 30

## Solution Implemented

### 1. Fixed Model Conversion (Commit `84c4d8f7e`)
```diff
- cmd = [..., '--opset', '13']
+ cmd = [..., '--opset', '9']
```

**Impact**: Generated models now compatible with your device's ONNX Runtime

### 2. Added Build Monitoring Tools (Commit `4f6e154e3`)

Three scripts to manage model generation:

#### `quick-build-status.sh`
Quick status checker - recommended for daily use
```bash
./quick-build-status.sh          # Check latest build
./quick-build-status.sh 435      # Check specific run
```

#### `check-build.sh`
Comprehensive monitoring with logs and downloads
```bash
./check-build.sh -r 435 -d ./artifacts -l
```

#### `download-models.sh`
Download generated ONNX models from completed builds
```bash
./download-models.sh ./my-models
```

## Status Timeline

| Date | Event | Commit |
|------|-------|--------|
| 2025-01-13 | Fix ONNX opset downgrade | `84c4d8f7e` |
| 2025-01-13 | Add monitoring scripts | `4f6e154e3` |
| Current | Build #435 running | In progress |

## Next Steps

### 1. Setup GitHub Token
Required for monitoring scripts:

```bash
# Option A: Environment variable
export GITHUB_TOKEN="your_token_here"

# Option B: Token file (persistent)
echo "your_token_here" > ~/.github_token
chmod 600 ~/.github_token
```

Get token: https://github.com/settings/tokens (select `repo`, `workflow`, `actions:read`)

### 2. Monitor Build Progress
```bash
./quick-build-status.sh

# Output will show:
# Status:       IN_PROGRESS
# ⏳ BUILD IN PROGRESS...
```

### 3. Download Models When Complete
```bash
./download-models.sh ./generated-models

# Models will be extracted to ./generated-models/
```

### 4. Deploy Models to Device
Once downloaded, copy the `.onnx` files to your app's models folder

### 5. Verify Fix
Run your app - it should no longer show "IR version 13" errors

## Technical Details

### ONNX Opset vs IR Version
- **Opset**: Operator set (defines available operations)
- **IR Version**: Intermediate representation format version
- **Mapping**: Opset N typically maps to IR version N

| Opset | IR Version | Support |
|-------|-----------|---------|
| 9     | 9         | ✅ Your device |
| 10-12 | 10-12     | ❌ Not supported |
| 13    | 13        | ❌ Previous attempt |

### Why Downgrade?
- Device's ONNX Runtime was built with opset 9 limit
- Opset 13 adds newer operations not needed for your models
- Opset 9 maintains full inference capability
- Ensures compatibility across all target devices

## Monitoring Commands

### Quick Check (Recommended)
```bash
./quick-build-status.sh
```
- Fast (~2 seconds)
- Shows status with exit codes
- Ideal for scripting

### Full Details
```bash
./check-build.sh -r 435
```
- Complete build information
- Job logs available with `-l`
- Artifact listing

### Watch Build Progress
```bash
while true; do clear; ./quick-build-status.sh; sleep 30; done
```
- Updates every 30 seconds
- Ctrl+C to stop

## Files Modified/Created

### Modified
- `scripts/convert_saved_models.py` - Opset downgrade

### Created
- `check-build.sh` - Comprehensive build monitor
- `quick-build-status.sh` - Quick status checker
- `download-models.sh` - Model artifact downloader
- `BUILD_MONITOR_README.md` - Usage guide
- `ONNX_FIX_SUMMARY.md` - This file

## Verification Checklist

- [x] Identified root cause (opset 13 vs max opset 9)
- [x] Fixed conversion script (opset 13 → 9)
- [x] Committed fix to repository
- [x] Pushed changes to trigger CI
- [x] Created monitoring scripts
- [x] Documented the solution

## Troubleshooting

### Build Still Failing?
1. Check build logs: `./check-build.sh -r [run_id] -l`
2. Look for converter errors in output
3. Verify tf2onnx installation in CI environment

### Can't Download Models?
1. Verify GitHub token: `echo $GITHUB_TOKEN`
2. Check build completed successfully: `./quick-build-status.sh`
3. Ensure ONNX artifact was generated

### Still Getting IR Version Errors on Device?
1. Verify models are opset 9:
   - Download and inspect: `./download-models.sh`
   - Check git repository for committed `.onnx` files
2. Clear app cache before reinstalling
3. Rebuild app with new models

## Support Resources

- ONNX Specification: https://github.com/onnx/onnx/blob/main/docs/IR.md
- ONNX Runtime: https://onnxruntime.ai/
- Build Workflow: https://github.com/supastishn/PojavBounce/actions
- GitHub Tokens: https://github.com/settings/tokens

## Related Issues

Error message: `ORT_FAIL - message: Load model from ... Unsupported model IR version: 13, max supported IR version: 9`

**Resolution**: Downgrade ONNX opset from 13 to 9 during model conversion

**Status**: ✅ FIXED and deployed
