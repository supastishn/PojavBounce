# Automatic Sync Workflow: PojavBounce ↔ LiquidBounceModifications

## Overview
This document describes the automatic synchronization between **PojavBounce** (PojavLauncher-specific version) and **LiquidBounceModifications** (improvements for upstream LiquidBounce).

## Repository Structure

```
PojavBounce (supastishn/PojavBounce)
├─ nextgen: Your main branch
│   ├─ ExecuTorch + Minarai (PojavLauncher-specific)
│   ├─ Android native libraries
│   └─ GUI migration (can be shared)
├─ origin: GitHub remote (supastishn fork)
├─ origin2: Official LiquidBounce (CCBlueX)
└─ origin3: Local LiquidBounceModifications folder

LiquidBounceModifications (local folder)
└─ nextgen: Sync point with official LiquidBounce
```

## Synchronization Methods

### Method 1: Post-Commit Hook (Automatic Notification)
The `.git/hooks/post-commit` hook automatically:
- Detects changes after each commit
- Filters out PojavLauncher-specific files
- Notifies you when improvements are ready to sync
- **Does NOT auto-push** (requires manual review)

### Method 2: Manual Sync Script (Recommended)
Use the provided `sync-to-lb-mods.sh` script:

```bash
# Sync the latest commit
./sync-to-lb-mods.sh latest

# Sync a specific commit
./sync-to-lb-mods.sh abc123def456
```

The script will:
1. Fetch latest from origin3/nextgen
2. Create an improvements branch
3. Cherry-pick the commit
4. Handle any conflicts
5. Guide you through push/PR steps

## What Gets Synced

### ✓ Synced (Improvements for Upstream)
- GUI backend migrations (JCEF → native Minecraft)
- Build system improvements (gradle, buildSrc)
- Mixin compatibility updates
- Performance optimizations
- Bug fixes in core modules

### ✗ NOT Synced (PojavLauncher-Specific)
- `src/main/resources/native/` (Android libraries)
- `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/executorch/`
- `src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/models/MinaraiModel.kt`
- `.github/workflows/convert-models.yml`
- Any file with "MINARAI" or "ExecutorTorch" in path

## Workflow Examples

### Example 1: Making a PojavLauncher-Only Change
```bash
# In PojavBounce repo
git commit -m "fix: ExecuTorch initialization on Android"

# Post-commit hook runs:
# [PojavBounce Hook] No syncing changes (PojavLauncher-specific only)
```

### Example 2: Making an Improvement for Upstream
```bash
# In PojavBounce repo
git commit -m "improve: optimize GUI rendering performance"

# Post-commit hook runs:
# [PojavBounce Hook] Syncing improvements to LiquidBounceModifications...
# [PojavBounce Hook] Ready to cherry-pick: abc123d improve: optimize GUI...
# [PojavBounce Hook] Manual review recommended before pushing

# Then manually sync:
./sync-to-lb-mods.sh latest

# Then push to LiquidBounceModifications and create PR
```

## Submitting PRs to Official LiquidBounce

Once improvements are in LiquidBounceModifications:

```bash
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications
git push origin improvements-sync
# Create PR on GitHub: https://github.com/CCBlueX/LiquidBounce
```

## Handling Conflicts

If cherry-pick fails:

```bash
# 1. Resolve conflicts manually
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications
# Fix conflicted files in your editor

# 2. Stage resolved files
git add <resolved-files>

# 3. Continue cherry-pick
git cherry-pick --continue
```

## Maintenance

### Update Hook Patterns
Edit `.git/hooks/post-commit` if you add new PojavLauncher-specific directories:

```bash
EXCLUDE_PATTERNS=(
    "src/main/resources/native/"
    "src/main/kotlin/net/ccbluex/liquidbounce/deeplearn/executorch/"
    # Add new patterns here
)
```

### Check Sync Status
```bash
git log origin2/nextgen..nextgen --oneline  # Changes in PojavBounce
git log origin3/nextgen..origin/nextgen --oneline  # Changes awaiting sync
```

## Current Status

- **PojavBounce HEAD**: `0ec5d244e` (Squashed custom implementation)
- **Base**: `origin2/nextgen` (LiquidBounce official)
- **Distance**: 1 commit ahead (all changes squashed)
- **ExecuTorch**: Fully integrated (PojavLauncher-specific)
- **GUI Migration**: Ready for upstream (in LiquidBounceModifications)

## Next Steps

1. Continue development in PojavBounce
2. Post-commit hook will notify about improvements
3. Use `./sync-to-lb-mods.sh` when ready to share
4. Review and push to LiquidBounceModifications
5. Create PRs to official LiquidBounce (CCBlueX/LiquidBounce)
