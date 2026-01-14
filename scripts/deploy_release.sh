#!/bin/bash

# LiquidBounce Deploy Script - Release Version
# Downloads latest LiquidBounce release and deploys to FCL .minecraft/mods

set -e

# Configuration
REPO="CCBlueX/LiquidBounce"
MINECRAFT_VERSION="1.21.11-Fabric"
MODS_DIR="/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/${MINECRAFT_VERSION}/mods"
TEMP_DIR="/tmp/liquidbounce_deploy_release"
TAG="${1:-latest}"  # Can specify version tag like v0.35.3

echo "=========================================="
echo "LiquidBounce Deploy Script (Release)"
echo "=========================================="
echo "Repository: $REPO"
echo "Release: $TAG"
echo "Target Minecraft: $MINECRAFT_VERSION"
echo "Mods Directory: $MODS_DIR"
echo ""

# Clean up temp directory
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

# Get latest or specified release
echo "[1/4] Fetching $TAG release..."
if [ "$TAG" = "latest" ]; then
    RELEASE_TAG=$(gh release list --repo "$REPO" --limit 1 --json tagName --jq '.[0].tagName')
else
    RELEASE_TAG=$(gh release view "$TAG" --repo "$REPO" --json tagName --jq '.tagName' 2>/dev/null || echo "")
fi

if [ -z "$RELEASE_TAG" ]; then
    echo "ERROR: Could not find release $TAG"
    echo ""
    echo "Available releases:"
    gh release list --repo "$REPO" --limit 10
    exit 1
fi

echo "Found release: $RELEASE_TAG"

# Download release assets
echo "[2/4] Downloading release assets..."
gh release download "$RELEASE_TAG" --repo "$REPO" --dir "$TEMP_DIR" --pattern "*.jar"

if [ -z "$(ls -A "$TEMP_DIR" 2>/dev/null)" ]; then
    echo "ERROR: Failed to download release assets"
    exit 1
fi

# Find the mod jar file for correct Minecraft version
echo "[3/4] Locating LiquidBounce JAR file..."
MOD_JAR=$(find "$TEMP_DIR" -name "*liquidbounce*.jar" | head -1)

if [ -z "$MOD_JAR" ]; then
    echo "ERROR: Could not find LiquidBounce JAR file"
    echo "Available files:"
    find "$TEMP_DIR" -type f
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
