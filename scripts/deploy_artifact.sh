#!/bin/bash

# LiquidBounce Deploy Script
# Downloads latest artifact from GitHub Actions and deploys to FCL .minecraft/mods

set -e

# Configuration
REPO="CCBlueX/LiquidBounce"
MINECRAFT_VERSION="1.21.11-Fabric"
MODS_DIR="/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/${MINECRAFT_VERSION}/mods"
TEMP_DIR="/tmp/liquidbounce_deploy"
BRANCH="${1:-nextgen}"

echo "=========================================="
echo "LiquidBounce Deploy Script"
echo "=========================================="
echo "Repository: $REPO"
echo "Branch: $BRANCH"
echo "Target Minecraft: $MINECRAFT_VERSION"
echo "Mods Directory: $MODS_DIR"
echo ""

# Clean up temp directory
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Get latest workflow run ID
echo "[1/4] Fetching latest GitHub Actions workflow run..."
WORKFLOW_RUN=$(gh run list --repo "$REPO" --branch "$BRANCH" --status completed --limit 1 --json databaseId --jq '.[0].databaseId')

if [ -z "$WORKFLOW_RUN" ]; then
    echo "ERROR: Could not find any completed workflow runs for branch '$BRANCH'"
    exit 1
fi

echo "Found workflow run: $WORKFLOW_RUN"

# Get artifacts list
echo "[2/4] Fetching artifacts from workflow run..."
ARTIFACTS=$(gh run download "$WORKFLOW_RUN" --repo "$REPO" --dir "$TEMP_DIR" 2>&1 | grep -i "Downloaded" || echo "")

if [ -z "$(ls -A "$TEMP_DIR" 2>/dev/null)" ]; then
    echo "ERROR: Failed to download artifacts"
    exit 1
fi

# Find the mod jar file
echo "[3/4] Locating LiquidBounce JAR file..."
MOD_JAR=$(find "$TEMP_DIR" -name "*liquidbounce*.jar" -o -name "*LiquidBounce*.jar" | head -1)

if [ -z "$MOD_JAR" ]; then
    echo "Available files in temp directory:"
    find "$TEMP_DIR" -type f
    echo "ERROR: Could not find LiquidBounce JAR file"
    exit 1
fi

echo "Found JAR: $(basename "$MOD_JAR")"

# Create mods directory if it doesn't exist
mkdir -p "$MODS_DIR"

# Backup old mod if it exists
if [ -f "$MODS_DIR"/liquidbounce*.jar ]; then
    BACKUP_DIR="$MODS_DIR/.backup"
    mkdir -p "$BACKUP_DIR"
    mv "$MODS_DIR"/liquidbounce*.jar "$BACKUP_DIR/" 2>/dev/null || true
    echo "Backed up old versions to $BACKUP_DIR"
fi

# Deploy the mod
echo "[4/4] Deploying mod to FCL..."
cp "$MOD_JAR" "$MODS_DIR/"
DEPLOYED_FILE=$(basename "$MOD_JAR")

# Cleanup
rm -rf "$TEMP_DIR"

echo ""
echo "=========================================="
echo "✓ Deploy Successful!"
echo "=========================================="
echo "Mod deployed: $DEPLOYED_FILE"
echo "Location: $MODS_DIR/$DEPLOYED_FILE"
echo "File size: $(du -h "$MODS_DIR/$DEPLOYED_FILE" | cut -f1)"
echo ""
echo "Ready to launch with FCL!"
