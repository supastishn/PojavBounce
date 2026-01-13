#!/bin/bash

# Download ONNX models from latest "Convert ONNX Models" workflow run
# Verifies IR version is v9 before downloading
# Usage: GITHUB_TOKEN=<token> ./scripts/download-onnx.sh

set -e

GITHUB_TOKEN="${GITHUB_TOKEN:-}"
REPO="${REPO:-supastishn/PojavBounce}"
WORKFLOW_NAME="Convert ONNX Models"
OUTPUT_DIR="${OUTPUT_DIR:-.}"
SKIP_VERIFICATION="${SKIP_VERIFICATION:-false}"

if [ -z "$GITHUB_TOKEN" ]; then
    echo "Error: GITHUB_TOKEN environment variable not set"
    echo "Usage: GITHUB_TOKEN=<token> $0"
    exit 1
fi

echo "Fetching latest '$WORKFLOW_NAME' workflow run..."
LATEST_RUN=$(curl -s \
    -H "Authorization: token $GITHUB_TOKEN" \
    "https://api.github.com/repos/$REPO/actions/workflows/convert-onnx.yml/runs?per_page=1&status=completed" \
    | jq -r '.workflow_runs[0].id')

if [ -z "$LATEST_RUN" ] || [ "$LATEST_RUN" = "null" ]; then
    echo "Error: Could not fetch latest workflow run"
    exit 1
fi

echo "Latest run ID: $LATEST_RUN"

echo "Fetching artifacts from run $LATEST_RUN..."
ARTIFACT_ID=$(curl -s \
    -H "Authorization: token $GITHUB_TOKEN" \
    "https://api.github.com/repos/$REPO/actions/runs/$LATEST_RUN/artifacts" \
    | jq -r '.artifacts[] | select(.name | contains("onnx-models-v9")) | select(.name | contains("-checksum") | not) | .id' \
    | head -1)

if [ -z "$ARTIFACT_ID" ] || [ "$ARTIFACT_ID" = "null" ]; then
    echo "Error: No ONNX models artifact found in latest run"
    echo "Make sure the 'Convert ONNX Models' workflow has completed successfully"
    exit 1
fi

echo "Artifact ID: $ARTIFACT_ID"

# Create temporary directory
WORK_DIR=$(mktemp -d)
trap "rm -rf $WORK_DIR" EXIT

echo "Downloading onnx-models.zip..."
curl -L \
    -H "Authorization: token $GITHUB_TOKEN" \
    -o "$WORK_DIR/onnx-models.zip" \
    "https://api.github.com/repos/$REPO/actions/artifacts/$ARTIFACT_ID/zip"

if [ ! -f "$WORK_DIR/onnx-models.zip" ]; then
    echo "Error: Failed to download artifact"
    exit 1
fi

# Extract to temporary location
unzip -q "$WORK_DIR/onnx-models.zip" -d "$WORK_DIR/extracted"

# Find the actual models directory (could be nested)
MODELS_FOUND=$(find "$WORK_DIR/extracted" -name "*.onnx" | head -1)

if [ -z "$MODELS_FOUND" ]; then
    echo "Error: No ONNX model files found in artifact"
    exit 1
fi

# Get the models directory
MODELS_DIR=$(dirname "$MODELS_FOUND")
echo "Found $(ls "$MODELS_DIR"/*.onnx 2>/dev/null | wc -l) model files"

# Verify IR versions if not skipped
if [ "$SKIP_VERIFICATION" != "true" ]; then
    echo ""
    echo "=== Verifying IR versions ==="
    python3 << 'EOF'
import sys
import onnx
from pathlib import Path

models_dir = Path(sys.argv[1])
all_v9 = True

for model_file in sorted(models_dir.glob("*.onnx")):
    try:
        model = onnx.load(str(model_file))
        ir_version = model.ir_version
        opset = model.opset_import[0].version if model.opset_import else "unknown"

        status = "✓" if ir_version == 9 else "✗"
        print(f"{status} {model_file.name}: IR v{ir_version}, opset {opset}")

        if ir_version > 9:
            all_v9 = False
            print(f"  WARNING: IR v{ir_version} > 9 (incompatible with Android ONNX Runtime)")
        elif ir_version < 9:
            all_v9 = False
            print(f"  WARNING: IR v{ir_version} < 9 (unusual, expected v9)")
    except Exception as e:
        print(f"✗ {model_file.name}: Error - {e}")
        all_v9 = False

if not all_v9:
    print("\nERROR: Not all models are IR v9!")
    print("Aborting download to prevent incompatible models.")
    sys.exit(1)

print("\n✓ All models are IR v9 - compatible!")
EOF
    if [ $? -ne 0 ]; then
        echo ""
        echo "Verification failed - aborting download"
        exit 1
    fi
else
    echo "Skipping IR version verification (SKIP_VERIFICATION=true)"
fi

echo ""
echo "Downloading to: $OUTPUT_DIR"
mkdir -p "$OUTPUT_DIR"

# Copy files to output directory
cp "$MODELS_DIR"/*.onnx "$OUTPUT_DIR/"
echo "✓ Successfully downloaded $(ls "$OUTPUT_DIR"/*.onnx 2>/dev/null | wc -l) ONNX model files"

echo ""
echo "Model information:"
python3 << 'EOF'
import onnx
from pathlib import Path

output_dir = Path(""" + "\"$OUTPUT_DIR\"" + """)
for model_file in sorted(output_dir.glob("*.onnx")):
    try:
        model = onnx.load(str(model_file))
        ir_version = model.ir_version
        opset = model.opset_import[0].version if model.opset_import else "unknown"
        size_mb = model_file.stat().st_size / (1024 * 1024)
        print(f"  {model_file.name}: IR v{ir_version}, opset {opset}, {size_mb:.2f} MB")
    except Exception as e:
        print(f"  {model_file.name}: Error - {e}")
EOF

echo ""
echo "Done! Models are ready to use."
