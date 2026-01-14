#!/bin/bash

# LiquidBounce Deploy Tester & Validator
# Validates environment and prerequisites before deployment

set -e

REPO="CCBlueX/LiquidBounce"
BRANCH="${1:-nextgen}"

echo "=========================================="
echo "LiquidBounce Deploy Validator"
echo "=========================================="
echo ""

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

check_command() {
    if command -v "$1" &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 is installed"
        return 0
    else
        echo -e "${RED}✗${NC} $1 is NOT installed"
        return 1
    fi
}

check_file() {
    if [ -f "$1" ]; then
        echo -e "${GREEN}✓${NC} File exists: $1"
        return 0
    else
        echo -e "${RED}✗${NC} File missing: $1"
        return 1
    fi
}

check_dir() {
    if [ -d "$1" ]; then
        echo -e "${GREEN}✓${NC} Directory exists: $1"
        return 0
    else
        echo -e "${YELLOW}!${NC} Directory missing: $1 (will be created during deploy)"
        return 1
    fi
}

# Prerequisites
echo "[1/5] Checking prerequisites..."
echo ""

MISSING=0

check_command "gh" || MISSING=$((MISSING + 1))
check_command "bash" || MISSING=$((MISSING + 1))

if [ $MISSING -eq 0 ]; then
    echo -e "${GREEN}✓${NC} All prerequisites installed"
else
    echo -e "${YELLOW}⚠${NC} Missing prerequisites (install with: pkg install gh)"
fi

echo ""

# Authentication
echo "[2/5] Checking GitHub CLI authentication..."
echo ""

if gh auth status &> /dev/null; then
    AUTH_USER=$(gh auth status --show-token 2>/dev/null | head -1 | cut -d' ' -f1)
    echo -e "${GREEN}✓${NC} Authenticated as: $AUTH_USER"
else
    echo -e "${RED}✗${NC} NOT authenticated with GitHub CLI"
    echo "  Run: gh auth login"
    exit 1
fi

echo ""

# Repository access
echo "[3/5] Checking repository access..."
echo ""

if gh repo view "$REPO" &> /dev/null; then
    echo -e "${GREEN}✓${NC} Can access repository: $REPO"
else
    echo -e "${RED}✗${NC} Cannot access repository: $REPO"
    exit 1
fi

echo ""

# Check branch
echo "[4/5] Checking branch and workflows..."
echo ""

if gh api "repos/$REPO/branches/$BRANCH" &> /dev/null; then
    echo -e "${GREEN}✓${NC} Branch exists: $BRANCH"
else
    echo -e "${RED}✗${NC} Branch NOT found: $BRANCH"
    echo "  Available branches:"
    gh api "repos/$REPO/branches" --jq '.[].name' | head -5
    exit 1
fi

WORKFLOW_COUNT=$(gh run list --repo "$REPO" --branch "$BRANCH" --limit 10 --json conclusion --jq 'length')
if [ "$WORKFLOW_COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓${NC} Found $WORKFLOW_COUNT recent workflow runs"

    # Show latest runs
    echo ""
    echo "  Latest runs on $BRANCH:"
    gh run list --repo "$REPO" --branch "$BRANCH" --limit 3 --json status,conclusion,createdAt --jq '.[] | "    [\(.status)] [\(.conclusion)] \(.createdAt | .[0:10])"'
else
    echo -e "${YELLOW}!${NC} No recent workflow runs found"
fi

echo ""

# Check deployment target
echo "[5/5] Checking deployment target..."
echo ""

MINECRAFT_VERSION="1.21.11-Fabric"
MODS_DIR="/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/${MINECRAFT_VERSION}/mods"

if [ -d "/data/data/com.termux/files/home/storage/shared/fcl" ]; then
    echo -e "${GREEN}✓${NC} FCL installation found"

    if check_dir "$MODS_DIR"; then
        MOD_COUNT=$(find "$MODS_DIR" -name "*.jar" 2>/dev/null | wc -l)
        echo "  Currently installed mods: $MOD_COUNT"
    else
        mkdir -p "$MODS_DIR"
        echo -e "${GREEN}✓${NC} Created mods directory"
    fi
else
    echo -e "${YELLOW}!${NC} FCL installation not found"
    echo "  Expected: /data/data/com.termux/files/home/storage/shared/fcl"
    echo "  Make sure FCL is properly installed"
fi

echo ""

# Summary
echo "=========================================="
echo "Validation Complete!"
echo "=========================================="
echo ""
echo "Ready to deploy? Run:"
echo ""
echo "  Option 1 (Bash): ./scripts/deploy_artifact.sh"
echo "  Option 2 (Python): python3 scripts/deploy_helper.py"
echo ""
