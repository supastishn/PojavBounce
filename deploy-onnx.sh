#!/bin/bash

set -e

GITHUB_TOKEN="${GITHUB_TOKEN:-}"
REPO="supastishn/PojavBounce"
MODELS_DIR="${MODELS_DIR:-src/main/resources/resources/liquidbounce/models}"
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
    | jq -r '.workflow_runs[0].id')

if [ -z "$LATEST_RUN" ] || [ "$LATEST_RUN" = "null" ]; then
    echo "Error: Could not fetch latest run"
    exit 1
fi

echo "Latest run ID: $LATEST_RUN"

echo "Getting ONNX models artifact from run $LATEST_RUN..."
ARTIFACT=$(curl -s \
    -H "Authorization: token $GITHUB_TOKEN" \
    "https://api.github.com/repos/$REPO/actions/runs/$LATEST_RUN/artifacts" \
    | jq -r '.artifacts[] | select(.name | startswith("onnx-models-opset9-")) | .id' | head -1)

if [ -z "$ARTIFACT" ] || [ "$ARTIFACT" = "null" ]; then
    echo "Error: No ONNX models artifact found"
    echo "Available artifacts:"
    curl -s \
        -H "Authorization: token $GITHUB_TOKEN" \
        "https://api.github.com/repos/$REPO/actions/runs/$LATEST_RUN/artifacts" \
        | jq -r '.artifacts[] | .name'
    exit 1
fi

echo "Found artifact ID: $ARTIFACT"
echo "Downloading ONNX models artifact..."
curl -L \
    -H "Authorization: token $GITHUB_TOKEN" \
    -o "$WORK_DIR/onnx-models.zip" \
    "https://api.github.com/repos/$REPO/actions/artifacts/$ARTIFACT/zip"

echo "Extracting artifact..."
unzip -q "$WORK_DIR/onnx-models.zip" -d "$WORK_DIR/extracted"

mkdir -p "$MODELS_DIR"

echo "Deploying ONNX models to $MODELS_DIR..."
find "$WORK_DIR/extracted" -name "*.onnx" -exec cp {} "$MODELS_DIR" \;

echo "Verifying deployment..."
MODEL_COUNT=$(find "$MODELS_DIR" -name "*.onnx" | wc -l)
echo "✓ Deployment complete! ($MODEL_COUNT models deployed)"

ls -lh "$MODELS_DIR"/*.onnx
