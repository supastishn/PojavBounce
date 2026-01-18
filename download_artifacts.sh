#!/bin/bash
# Script to download ExecuTorch .pte model artifacts from GitHub Actions

set -e

# Configuration
REPO_OWNER="${1:-}"
REPO_NAME="${2:-}"
WORKFLOW_ID="${3:-convert-models.yml}"
DOWNLOAD_DIR="./downloaded_models"
MODELS_DIR="./src/main/resources/resources/liquidbounce/models"

# Check if gh CLI is installed
if ! command -v gh &> /dev/null; then
    echo "❌ GitHub CLI (gh) not found. Install from: https://github.com/cli/cli"
    exit 1
fi

# Function to display usage
usage() {
    echo "Usage: $0 <repo_owner> <repo_name> [workflow_id]"
    echo ""
    echo "Example:"
    echo "  $0 myorg myrepo convert-models.yml"
    echo ""
    echo "Environment variables:"
    echo "  GH_TOKEN - GitHub personal access token (or use 'gh auth login')"
    exit 1
}

# Check arguments
if [ -z "$REPO_OWNER" ] || [ -z "$REPO_NAME" ]; then
    usage
fi

echo "📥 GitHub Actions Artifact Downloader"
echo "========================================"
echo "Repository: $REPO_OWNER/$REPO_NAME"
echo "Workflow: $WORKFLOW_ID"
echo "Download directory: $DOWNLOAD_DIR"
echo ""

# Create download directory
mkdir -p "$DOWNLOAD_DIR"

# Get the latest workflow run
echo "Fetching latest workflow run..."
LATEST_RUN=$(gh run list \
    --repo "$REPO_OWNER/$REPO_NAME" \
    --workflow "$WORKFLOW_ID" \
    --limit 1 \
    --json "databaseId,status,conclusion" \
    --jq ".[0]")

RUN_ID=$(echo "$LATEST_RUN" | jq -r '.databaseId')
STATUS=$(echo "$LATEST_RUN" | jq -r '.status')
CONCLUSION=$(echo "$LATEST_RUN" | jq -r '.conclusion')

echo "Latest run ID: $RUN_ID"
echo "Status: $STATUS"
echo "Conclusion: $CONCLUSION"
echo ""

if [ -z "$RUN_ID" ]; then
    echo "❌ No workflow runs found"
    exit 1
fi

# Check if run has artifacts
echo "Checking for artifacts..."
ARTIFACTS=$(gh run view "$RUN_ID" \
    --repo "$REPO_OWNER/$REPO_NAME" \
    --json "artifacts" \
    --jq ".artifacts[] | select(.name | test(\"pte|executorch\"))")

if [ -z "$ARTIFACTS" ]; then
    echo "⚠️  No artifacts found matching 'pte' or 'executorch'"
    echo ""
    echo "Available artifacts:"
    gh run view "$RUN_ID" \
        --repo "$REPO_OWNER/$REPO_NAME" \
        --json "artifacts" \
        --jq ".artifacts[] | .name"
    exit 1
fi

# Download each artifact
echo "Downloading artifacts..."
gh run download "$RUN_ID" \
    --repo "$REPO_OWNER/$REPO_NAME" \
    --dir "$DOWNLOAD_DIR" \
    --pattern "*pte*"

gh run download "$RUN_ID" \
    --repo "$REPO_OWNER/$REPO_NAME" \
    --dir "$DOWNLOAD_DIR" \
    --pattern "*executorch*"

# List downloaded files
echo ""
echo "✓ Downloaded artifacts:"
find "$DOWNLOAD_DIR" -type f | while read -r file; do
    size=$(du -h "$file" | cut -f1)
    echo "  - $file ($size)"
done

# Organize .pte files
echo ""
echo "📦 Organizing .pte files..."
mkdir -p "$MODELS_DIR/executorch"

find "$DOWNLOAD_DIR" -name "*.pte" -type f | while read -r pte_file; do
    filename=$(basename "$pte_file")
    target_path="$MODELS_DIR/executorch/$filename"

    if cp "$pte_file" "$target_path"; then
        echo "✓ Copied $filename to $target_path"
    else
        echo "❌ Failed to copy $filename"
    fi
done

# List final .pte files in project
echo ""
echo "✓ ExecuTorch models in project:"
if [ -d "$MODELS_DIR/executorch" ]; then
    ls -lh "$MODELS_DIR/executorch/"*.pte 2>/dev/null || echo "  (no .pte files found)"
else
    echo "  Directory not found: $MODELS_DIR/executorch"
fi

echo ""
echo "✅ Artifact download and integration complete!"
echo ""
echo "Next steps:"
echo "1. Commit the downloaded .pte files:"
echo "   git add src/main/resources/resources/liquidbounce/models/executorch/"
echo "   git commit -m 'Add ExecuTorch .pte model files from GitHub Actions'"
echo ""
echo "2. Configure your app to load the .pte files (see integration guide)"
