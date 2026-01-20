# Manual Sync to LiquidBounceModifications

## Quick Start

When you want to sync improvements to LiquidBounceModifications:

```bash
cd /data/data/com.termux/files/home/prog/PojavBounce
./sync-to-lb-mods.sh latest
```

## What It Does

1. Cherry-picks your latest PojavBounce commit to LiquidBounceModifications
2. Filters out PojavLauncher-specific files (ExecuTorch, Minarai, Android native libs)
3. Creates an `improvements-sync-*` branch
4. Handles conflicts if any

## After Sync

```bash
# 1. Go to LiquidBounceModifications
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications

# 2. Review changes
git diff HEAD~1

# 3. Push to remote
git push origin improvements-sync-*

# 4. Create PR on GitHub
# Go to: https://github.com/CCBlueX/LiquidBounce/pulls
# Click "New Pull Request"
```

## Syncing Specific Commits

```bash
# Sync a specific commit hash
./sync-to-lb-mods.sh abc123def
```

## Handling Conflicts

If cherry-pick fails with conflicts:

```bash
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications

# 1. Resolve conflicts manually in your editor
# 2. Stage resolved files
git add <resolved-files>

# 3. Continue cherry-pick
git cherry-pick --continue
```

## Manual Alternative

If the script doesn't work for you:

```bash
cd /data/data/com.termux/files/home/prog/LiquidBounceModifications
git fetch origin3 nextgen
git checkout -b improvements-sync-$(date +%s) origin3/nextgen
git cherry-pick <COMMIT_HASH>
# Handle any conflicts
git push origin improvements-sync-*
# Create PR on GitHub
```
