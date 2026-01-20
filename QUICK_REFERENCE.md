# Quick Reference: Your Development Setup

## Two Main Repos You'll Use

### 1. PojavBounce (Development Hub)
```bash
cd /data/data/com.termux/files/home/prog/PojavBounce
git checkout nextgen
# Develop here normally
git commit -m "your changes"
git push origin nextgen
```

### 2. LiquidBounceModifications (Improvements Staging)
```bash
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications
git checkout improvements
# Changes auto-push to GitHub when you commit
```

## When You Want to Share Improvements

```bash
# Step 1: From PojavBounce, sync improvements
cd /data/data/com.termux/files/home/prog/PojavBounce
./sync-to-lb-mods.sh latest

# Step 2: In LiquidBounceModifications, merge them
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications
git checkout improvements
git merge improvements-sync-*
git commit -m "Merge improvement from PojavBounce"
# Auto-pushes to GitHub via hook

# Step 3: Create PR manually on GitHub (when ready)
# Visit: https://github.com/CCBlueX/LiquidBounce/pulls
```

## Key Files

- **PojavBounce/AUTOMATIC_PR_PIPELINE.md** - Full documentation
- **PojavBounce/MANUAL_SYNC.md** - Quick sync guide
- **PojavBounce/sync-to-lb-mods.sh** - Cherry-pick script

## Remotes Configured

### PojavBounce
- `origin` → GitHub (supastishn/PojavBounce)
- `origin2` → Official LiquidBounce (CCBlueX)
- `origin3` → Local LiquidBounceModifications

### LiquidBounceModifications
- `origin` → GitHub (exploiter-central/LiquidBounceModifications)
- `pojav` → Local PojavBounce
- `upstream` → Official LiquidBounce (CCBlueX)

## Current Status

- PojavBounce/nextgen: Clean, ready for development
- LiquidBounceModifications/improvements: Ready to receive synced improvements
- Auto-push hook: Active in LiquidBounceModifications

Good luck with development! 🚀
