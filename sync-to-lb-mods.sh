#!/bin/bash
# Manual sync script: Cherry-pick improvements from PojavBounce to LiquidBounceModifications
# Usage: ./sync-to-lb-mods.sh [commit-hash or "latest"]

set -e

POJAV_REPO="/data/data/com.termux/files/home/prog/PojavBounce"
LB_MODS_REPO="/data/data/com.termux/files/home/prog/LiquidBounceModifications"

# Determine commit to sync
if [ "$1" == "latest" ] || [ -z "$1" ]; then
    COMMIT=$(git -C "$POJAV_REPO" rev-parse HEAD)
    echo "[Sync] Using latest commit: $COMMIT"
else
    COMMIT="$1"
fi

# Verify commit exists
if ! git -C "$POJAV_REPO" rev-parse "$COMMIT" > /dev/null 2>&1; then
    echo "[Error] Invalid commit hash: $COMMIT"
    exit 1
fi

echo "[Sync] Preparing LiquidBounceModifications repo..."
cd "$LB_MODS_REPO"

# Fetch latest from origin
git fetch origin3 nextgen
git checkout nextgen

# Create improvements branch
git checkout -b improvements-sync-$(date +%s) origin3/nextgen 2>/dev/null || true

echo "[Sync] Attempting to cherry-pick from PojavBounce..."
echo "[Sync] Commit: $(git -C "$POJAV_REPO" log -1 --oneline "$COMMIT")"

# Try cherry-pick
if git cherry-pick "$COMMIT" --no-edit 2>/dev/null; then
    echo "✓ Cherry-pick successful!"
    echo ""
    echo "[Sync] Next steps:"
    echo "  1. Review changes: git diff HEAD~1"
    echo "  2. Push to remote: git push origin improvements-sync"
    echo "  3. Create PR on GitHub (CCBlueX/LiquidBounce)"
else
    echo "[Sync] Cherry-pick failed (likely conflicts)"
    echo "[Sync] Manual resolution needed:"
    echo "  1. Resolve conflicts manually"
    echo "  2. git add <resolved-files>"
    echo "  3. git cherry-pick --continue"
    echo ""
    echo "Files in conflict:"
    git diff --name-only --diff-filter=U
fi
