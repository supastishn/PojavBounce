# Automatic PR Pipeline: PojavBounce → LiquidBounceModifications → LiquidBounce

## Overview

This setup creates an automatic pipeline for sharing improvements:
```
PojavBounce (ExecuTorch + Minarai)
    ↓ (manual sync)
LiquidBounceModifications/improvements branch
    ↓ (auto-push to GitHub)
Ready for PR to CCBlueX/LiquidBounce
```

## Architecture

### PojavBounce
- **Branch**: `nextgen`
- **Content**: ExecuTorch, Minarai, Android native libraries + improvements
- **Manual Sync**: `./sync-to-lb-mods.sh latest`

### LiquidBounceModifications
- **Branches**:
  - `improvements`: Auto-syncs improvements, auto-pushes to GitHub
  - `nextgen`: Tracks original LiquidBounce nextgen
- **Auto-push Hook**: `.git/hooks/post-commit`
- **Remote**: `origin` = GitHub (exploiter-central/LiquidBounceModifications)

### Official LiquidBounce
- **Upstream**: `https://github.com/CCBlueX/LiquidBounce`
- **Target for PRs**: improvements branch → new PR to CCBlueX

## Workflow in Action

### Step 1: You Make Improvements in PojavBounce
```bash
cd /data/data/com.termux/files/home/prog/PojavBounce
# ... make changes ...
git commit -m "improve: optimize GUI rendering"
git push origin nextgen
```

### Step 2: You Sync to LiquidBounceModifications
```bash
./sync-to-lb-mods.sh latest
# Script output:
# Cherry-pick successful!
# Next steps:
#   1. Review changes: git diff HEAD~1
#   2. Push to remote: git push origin improvements-sync
```

### Step 3: Cherry-Picked Commit Goes to Improvements Branch (AUTOMATIC)
```bash
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications

# The sync script already created improvements-sync-xxx branch
# Now we merge it into improvements branch:
git checkout improvements
git merge improvements-sync-xxx
git commit -m "Merge improvement from PojavBounce"
```

### Step 4: Auto-Push Hook Triggers (AUTOMATIC)
```
[LB Mods] Auto-pushing improvements branch...
[LB Mods] ✓ Pushed to origin/improvements
[LB Mods] PR ready at: https://github.com/exploiter-central/LiquidBounceModifications/compare/improvements
```

### Step 5: Create PR to Official LiquidBounce (MANUAL)
```
Go to: https://github.com/CCBlueX/LiquidBounce/compare/nextgen...exploiter-central:LiquidBounceModifications:improvements
Click "Create Pull Request"
```

## What Auto-Pushes vs Manual

### Automatic (Git Hooks)
- ✓ Post-commit in LiquidBounceModifications auto-pushes when on `improvements` branch
- ✓ Saves you typing `git push` every time
- ✓ Keeps `improvements` branch always up-to-date on GitHub

### Manual (You Control)
- ✓ Cherry-picking from PojavBounce to LiquidBounceModifications
- ✓ Reviewing before pushing
- ✓ Creating PR on GitHub (GitHub doesn't auto-create PRs)

## Branches Explained

### PojavBounce
- `nextgen`: Main development branch (ExecuTorch + improvements)

### LiquidBounceModifications
- `nextgen`: Synced with official LiquidBounce nextgen (baseline)
- `improvements`: Where cherry-picked improvements go, auto-pushes to GitHub
- `improvements-sync-*`: Temporary branches created by sync script

### Official LiquidBounce
- `nextgen`: Official upstream

## How to Use

### Daily Development
```bash
# In PojavBounce
git commit -m "your changes"
# Work normally, commit normally
```

### Sync Improvements (When Ready)
```bash
# In PojavBounce
./sync-to-lb-mods.sh latest

# Script will cherry-pick to LiquidBounceModifications
# Review the result
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications
git log HEAD~1..HEAD
```

### Update Improvements Branch
```bash
# In LiquidBounceModifications, merge the sync branch into improvements
git checkout improvements
git merge improvements-sync-xxx
git commit -m "Add improvements from PojavBounce"
# Post-commit hook auto-pushes to origin/improvements
```

### Create PR to Official LiquidBounce
```
Manual step on GitHub:
1. Visit: https://github.com/CCBlueX/LiquidBounce
2. Click "New Pull Request"
3. Base: CCBlueX:nextgen
4. Head: exploiter-central:LiquidBounceModifications:improvements
5. Create PR
```

## Troubleshooting

### Hook Not Auto-Pushing
```bash
# Check if hook is executable
ls -la /data/data/com.termux/files/home/prog/LiquidBounceModifications/.git/hooks/post-commit

# Should show: -rwxr-xr-x
# If not: chmod +x .git/hooks/post-commit
```

### Changes Not Appearing on GitHub
```bash
# Make sure you're on improvements branch
git branch -a

# If on improvements and nothing auto-pushed, push manually
git push origin improvements
```

### Merge Conflicts in LiquidBounceModifications
```bash
# During merge of improvements-sync branch
git diff  # See conflicts
# Fix conflicts in editor
git add .
git commit -m "Resolve merge conflicts"
# Post-commit hook auto-pushes
```

## Current Status

### PojavBounce
- HEAD: `nextgen` branch
- Latest: Custom implementation with ExecuTorch + Minarai
- Ready for: Development and syncing

### LiquidBounceModifications
- `improvements` branch: Created and pushed to GitHub
- Auto-push hook: Installed and active
- Status: Ready to receive cherry-picked improvements
- PR template: Use when creating PRs to CCBlueX/LiquidBounce

### Official LiquidBounce
- Remote: `upstream` in LiquidBounceModifications
- Target: For PRs with improvements

## Quick Reference

```bash
# In PojavBounce
./sync-to-lb-mods.sh latest      # Cherry-pick to LB Mods

# In LiquidBounceModifications
git checkout improvements        # Switch to improvements branch
git merge improvements-sync-xxx  # Merge the sync branch (auto-pushes)
# Go to GitHub to create PR
```

## Benefits of This Setup

1. **Automated**: GitHub always has the latest improvements
2. **Manual Control**: You review everything before it goes upstream
3. **Clean History**: Only improvements go to LiquidBounce, not PojavLauncher-specific code
4. **Easy PRs**: Just visit GitHub link to create PR
5. **Two Repos**: Clear separation between PojavLauncher-specific and upstream improvements
