#!/bin/bash

set -e

GITHUB_TOKEN="${GITHUB_TOKEN:-}"
REPO="supastishn/PojavBounce"
MODS_DIR="/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods"
WORK_DIR=$(mktemp -d)

trap "rm -rf $WORK_DIR" EXIT

if [ -z "$GITHUB_TOKEN" ]; then
    echo "Error: GITHUB_TOKEN environment variable not set"
    exit 1
fi

echo "Fetching latest GitHub Actions run..."
LATEST_RUN=$(curl -s \
    -H "Authorization: token $GITHUB_TOKEN" \
    "https://api.github.com/repos/$REPO/actions/runs?per_page=1" \
    | grep -o '"id": [0-9]*' | head -1 | cut -d' ' -f2)

if [ -z "$LATEST_RUN" ]; then
    echo "Error: Could not fetch latest run"
    exit 1
fi

echo "Getting artifacts from run $LATEST_RUN..."
ARTIFACT_ID=$(curl -s \
    -H "Authorization: token $GITHUB_TOKEN" \
    "https://api.github.com/repos/$REPO/actions/runs/$LATEST_RUN/artifacts" \
    | grep -o '"id": [0-9]*' | grep -v "$LATEST_RUN" | head -1 | cut -d' ' -f2)

if [ -z "$ARTIFACT_ID" ]; then
    echo "Error: No artifact found"
    exit 1
fi

echo "Downloading liquidbounce artifact..."
curl -L \
    -H "Authorization: token $GITHUB_TOKEN" \
    -o "$WORK_DIR/artifact.zip" \
    "https://api.github.com/repos/$REPO/actions/artifacts/$ARTIFACT_ID/zip"

echo "Extracting artifact..."
unzip -q "$WORK_DIR/artifact.zip" -d "$WORK_DIR/extracted"

mkdir -p "$MODS_DIR"

echo "Deploying files to mods directory..."
find "$WORK_DIR/extracted" -type f -exec cp {} "$MODS_DIR" \;

echo "Deployment complete!"
