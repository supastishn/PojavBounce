#!/bin/bash
# Quick reference script for ExecuTorch model conversion workflow

set -e

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║           ExecuTorch Model Conversion Quick Reference          ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Get repository info
REPO_URL=$(git config --get remote.origin.url 2>/dev/null || echo "Not a git repo")
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo "N/A")

echo -e "${BLUE}Repository Information:${NC}"
echo "  URL: $REPO_URL"
echo "  Branch: $CURRENT_BRANCH"
echo ""

# Check if gh CLI is available
if command -v gh &> /dev/null; then
    echo -e "${GREEN}✓ GitHub CLI (gh) found${NC}"
else
    echo -e "${RED}✗ GitHub CLI (gh) not found${NC}"
    echo "  Install from: https://github.com/cli/cli"
    echo ""
fi

# Check if Python is available
if command -v python3 &> /dev/null; then
    PYTHON_VERSION=$(python3 --version 2>&1 | awk '{print $2}')
    echo -e "${GREEN}✓ Python 3 found: $PYTHON_VERSION${NC}"
else
    echo -e "${RED}✗ Python 3 not found${NC}"
fi

# Check required Python packages
echo ""
echo -e "${BLUE}Python Package Status:${NC}"

python3 -c "import torch; print(f'  {GREEN}✓ PyTorch{NC}: ' + torch.__version__)" 2>/dev/null || echo "  ${RED}✗ PyTorch: not installed${NC}"
python3 -c "import executorch; print(f'  {GREEN}✓ ExecuTorch{NC}')" 2>/dev/null || echo "  ${RED}✗ ExecuTorch: not installed${NC}"
python3 -c "import onnx; print(f'  {GREEN}✓ ONNX{NC}')" 2>/dev/null || echo "  ${RED}✗ ONNX: not installed${NC}"
python3 -c "import onnxscript; print(f'  {GREEN}✓ onnxscript${NC}')" 2>/dev/null || echo "  ${RED}✗ onnxscript: not installed${NC}"

echo ""
echo -e "${BLUE}Conversion Workflow Steps:${NC}"
echo ""

echo "1. Trigger Conversion:"
echo -e "   ${YELLOW}Option A: Manual trigger (recommended for testing)${NC}"
echo "     gh workflow run convert-models.yml --repo owner/repo"
echo ""
echo -e "   ${YELLOW}Option B: Automatic on push${NC}"
echo "     Push changes to .params or conversion scripts"
echo ""

echo "2. Monitor Workflow:"
echo "   gh run view --repo owner/repo"
echo "   gh run list --workflow convert-models.yml --repo owner/repo"
echo ""

echo "3. Download Artifacts:"
echo "   chmod +x download_artifacts.sh"
echo "   ./download_artifacts.sh owner repo"
echo ""

echo "4. Verify Downloaded Files:"
echo "   ls -lh src/main/resources/resources/liquidbounce/models/executorch/"
echo ""

echo "5. Commit to Repository:"
echo "   git add src/main/resources/resources/liquidbounce/models/executorch/"
echo "   git commit -m 'Add ExecuTorch .pte model files from GitHub Actions'"
echo "   git push"
echo ""

echo -e "${BLUE}Model Files:${NC}"
echo "  Primary: .pte files (ExecuTorch optimized)"
echo "  Fallback: .pt files (TorchScript)"
echo "  Legacy: .params files (MXNet source)"
echo ""

echo -e "${BLUE}Logs and Diagnostics:${NC}"
echo "  Conversion logs: Download from GitHub Actions artifacts"
echo "  Runtime logs: Search for '[ExecuTorch]' prefix in app logs"
echo "  System info: Check ExecuTorchEngine.collectDiagnosticInfo()"
echo ""

echo -e "${BLUE}Helpful Commands:${NC}"
echo ""

echo "List all workflow runs:"
echo "  gh run list --all --repo owner/repo"
echo ""

echo "Get detailed run info:"
echo "  gh run view <run_id> --repo owner/repo"
echo ""

echo "Download specific artifact:"
echo "  gh run download <run_id> --repo owner/repo --name executorch-models-pte"
echo ""

echo "View conversion logs:"
echo "  gh run download <run_id> --repo owner/repo --name conversion-logs"
echo ""

echo "Check workflow status:"
echo "  gh workflow view convert-models.yml --repo owner/repo"
echo ""

echo -e "${YELLOW}Documentation:${NC}"
echo "  Full guide: EXECUTORCH_INTEGRATION.md"
echo "  Conversion script: convert_to_executorch_with_logging.py"
echo "  Download script: download_artifacts.sh"
echo ""

echo -e "${GREEN}Ready to start? Run step 1 above to trigger the conversion workflow!${NC}"
