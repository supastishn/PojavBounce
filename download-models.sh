#!/bin/bash
#
# Download ONNX models from the latest successful build
# Usage: ./download-models.sh [output_dir]
#

set -euo pipefail

REPO="supastishn/PojavBounce"
BRANCH="nextgen"
OUTPUT_DIR="${1:-./_onnx_models}"
TOKEN="${GITHUB_TOKEN:-}"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

log_info() { echo -e "${BLUE}[INFO]${NC} $*"; }
log_success() { echo -e "${GREEN}[✓]${NC} $*"; }
log_error() { echo -e "${RED}[✗]${NC} $*"; }
log_warn() { echo -e "${YELLOW}[!]${NC} $*"; }

# Check GitHub token
if [[ -z "$TOKEN" ]]; then
    if [[ -f ~/.github_token ]]; then
        TOKEN=$(cat ~/.github_token)
    else
        log_error "GitHub token not found!"
        echo ""
        echo "Set up your token with one of these methods:"
        echo "  1. Export:  export GITHUB_TOKEN='your_token_here'"
        echo "  2. File:    echo 'token' > ~/.github_token && chmod 600 ~/.github_token"
        echo ""
        echo "Get token from: https://github.com/settings/tokens"
        exit 1
    fi
fi

# Get latest successful build
log_info "Fetching latest successful build from $REPO:$BRANCH..."

RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
    "https://api.github.com/repos/$REPO/actions/workflows/build.yml/runs?branch=$BRANCH&status=completed&conclusion=success&per_page=1")

RUN_ID=$(echo "$RESPONSE" | python3 -c "import sys, json; data=json.load(sys.stdin); print(data['workflow_runs'][0]['id'] if data.get('workflow_runs') else '')" 2>/dev/null || echo "")

if [[ -z "$RUN_ID" ]]; then
    log_error "No successful build found"
    echo ""
    log_info "Checking for any recent builds..."
    RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
        "https://api.github.com/repos/$REPO/actions/workflows/build.yml/runs?branch=$BRANCH&per_page=3")
    echo "$RESPONSE" | python3 << 'PYTHON'
import sys, json
data = json.load(sys.stdin)
for run in data.get('workflow_runs', [])[:3]:
    print(f"  Run {run['id']}: {run['status']} ({run.get('conclusion', 'pending')})")
PYTHON
    exit 1
fi

log_success "Found build run: $RUN_ID"

# Get artifacts for this run
log_info "Fetching artifacts for run $RUN_ID..."

ARTIFACTS_RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
    "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID/artifacts")

ONNX_ARTIFACT_ID=$(echo "$ARTIFACTS_RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for artifact in data.get('artifacts', []):
    if 'onnx' in artifact['name'].lower():
        print(artifact['id'])
        break
" 2>/dev/null || echo "")

if [[ -z "$ONNX_ARTIFACT_ID" ]]; then
    log_error "No ONNX artifact found in build"
    echo ""
    log_info "Available artifacts:"
    echo "$ARTIFACTS_RESPONSE" | python3 -c "
import sys, json
data = json.load(sys.stdin)
for artifact in data.get('artifacts', []):
    size_mb = artifact['size_in_bytes'] / (1024*1024)
    print(f\"  - {artifact['name']} ({size_mb:.2f} MB)\")
"
    exit 1
fi

log_success "Found ONNX artifact: $ONNX_ARTIFACT_ID"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Download artifact
DOWNLOAD_URL="https://api.github.com/repos/$REPO/actions/artifacts/$ONNX_ARTIFACT_ID/zip"
TEMP_ZIP="$(mktemp --suffix=.zip)"

log_info "Downloading models to temporary file..."
curl -s -L -H "Authorization: token $TOKEN" \
    -H "Accept: application/vnd.github.v3+json" \
    -o "$TEMP_ZIP" \
    "$DOWNLOAD_URL"

# Extract
log_info "Extracting models..."
unzip -q "$TEMP_ZIP" -d "$OUTPUT_DIR"
rm "$TEMP_ZIP"

# List downloaded files
log_success "Models downloaded to: $OUTPUT_DIR"
echo ""
log_info "Files:"
find "$OUTPUT_DIR" -type f -name "*.onnx" | while read -r file; do
    size=$(du -h "$file" | cut -f1)
    basename "$file"
    echo "  Size: $size"
done

echo ""
log_success "Ready to use!"
echo ""
echo "Next steps:"
echo "  1. Copy models to your app's model folder"
echo "  2. Test with your Android app"
echo "  3. Verify no more 'IR version 13' errors"
echo ""
echo "Location: $OUTPUT_DIR"
