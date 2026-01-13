# Build Monitoring Scripts

Two scripts to monitor your PojavBounce GitHub Actions builds and fetch artifacts.

## Quick Setup

### 1. Configure GitHub Token

Choose one of these methods:

**Option A: Environment Variable (Temporary)**
```bash
export GITHUB_TOKEN="your_github_token_here"
./quick-build-status.sh
```

**Option B: Token File (Permanent)**
```bash
echo "your_github_token_here" > ~/.github_token
chmod 600 ~/.github_token
./quick-build-status.sh
```

**To get a GitHub token:**
1. Go to https://github.com/settings/tokens
2. Click "Generate new token"
3. Select scopes: `repo`, `workflow`, `actions:read`
4. Copy and use the token

## Scripts

### `quick-build-status.sh` (Recommended)
**Simple build status checker** - shows status with minimal output

```bash
# Check latest build
./quick-build-status.sh

# Check specific build by run ID
./quick-build-status.sh 435

# Output example:
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Build Status for Run: 435
# ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
# Run ID:       435
# Status:       IN_PROGRESS
# Conclusion:   PENDING
# ...
# ⏳ BUILD IN PROGRESS...
```

Exit codes:
- `0` = Build successful ✅
- `1` = Build failed ❌
- `2` = Build in progress ⏳
- `3` = Unknown status ⚠️

### `check-build.sh` (Advanced)
**Comprehensive build monitor** - with logs and artifact downloads

```bash
# Check latest build
./check-build.sh

# Check specific run
./check-build.sh -r 435

# Download artifacts
./check-build.sh -d ./my-artifacts

# Get job logs
./check-build.sh -l

# Combine options
./check-build.sh -r 435 -d ./artifacts -l
```

Options:
- `-h, --help` - Show help
- `-r, --run-id ID` - Check specific run ID
- `-d, --download DIR` - Download artifacts to DIR
- `-l, --logs` - Fetch job logs
- `-R, --repo REPO` - Override repo (default: supastishn/PojavBounce)
- `-B, --branch BRANCH` - Override branch (default: nextgen)

## Monitoring Your ONNX Model Build

Your recent fix for ONNX opset compatibility is building:

```bash
# Quick check (recommended)
./quick-build-status.sh

# Full monitoring
./check-build.sh -d ./generated-models -l
```

The build will:
1. Export SavedModels via Gradle
2. Convert them to ONNX with **opset 9** (compatible with your device)
3. Generate artifacts with the `.onnx` files
4. Auto-commit models to the repository

## Automation Example

Monitor build every 30 seconds:
```bash
while true; do
    clear
    ./quick-build-status.sh
    echo ""
    echo "Next check in 30 seconds... (Ctrl+C to stop)"
    sleep 30
done
```

Once build completes, download artifacts:
```bash
./check-build.sh -d ./onnx-models
ls -la ./onnx-models/
```

## Troubleshooting

**"GitHub token not found"**
- Set `GITHUB_TOKEN` environment variable OR
- Create `~/.github_token` file with your token

**"No build run found"**
- Check that your branch has recent commits
- Verify repository name: `supastishn/PojavBounce`

**"python3: command not found"**
- Install Python 3: `apt-get install python3`

**"curl: command not found"**
- Install curl: `apt-get install curl`

## Security Notes

⚠️ **Never commit tokens to git!**
- Use `.gitignore` to exclude `~/.github_token`
- Use environment variables in CI/CD pipelines
- Regularly rotate tokens on https://github.com/settings/tokens

## Related

- Your ONNX fix: `scripts/convert_saved_models.py` (now uses opset 9)
- Commit: `84c4d8f7e`
- GitHub Actions: https://github.com/supastishn/PojavBounce/actions
