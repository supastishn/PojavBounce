#!/bin/bash
# ONNX FIX - QUICK REFERENCE GUIDE
# Error: "Unsupported model IR version: 13, max supported IR version: 9"

# =============================================================================
# WHAT WAS WRONG
# =============================================================================
# Your models: ONNX opset 13 (newer, more features)
# Device supports: ONNX opset 9 (older, but stable)
# Result: Runtime error when trying to load models

# =============================================================================
# WHAT WAS FIXED
# =============================================================================
# File: scripts/convert_saved_models.py
# Change: --opset 13 → --opset 9
# Commits:
#   84c4d8f7e - The actual opset fix
#   4f6e154e3 - Added monitoring scripts
#   506ea3dc4 - Documentation

# =============================================================================
# CURRENT STATUS
# =============================================================================
# CI Build #435 - IN PROGRESS
# Repository: github.com/supastishn/PojavBounce
# Branch: nextgen

# =============================================================================
# QUICK SETUP (Do This First)
# =============================================================================
# 1. Create GitHub token file (one-time setup)
echo "YOUR_GITHUB_TOKEN" > ~/.github_token
chmod 600 ~/.github_token

# 2. Test the setup
./quick-build-status.sh

# =============================================================================
# MONITORING WORKFLOW
# =============================================================================
# Step 1: Check build status (quick)
./quick-build-status.sh

# Step 2: When build completes successfully, download models
./download-models.sh ./opset9-models

# Step 3: Verify models were generated
ls -la ./opset9-models/
find ./opset9-models/ -name "*.onnx" -exec du -h {} \;

# =============================================================================
# DEPLOYMENT TO DEVICE
# =============================================================================
# After downloading models:
# 1. Copy .onnx files to your app's models folder
# 2. Rebuild APK with new models
# 3. Test on device
# 4. Verify error is gone: grep -r "IR version 13" logcat || echo "Fixed!"

# =============================================================================
# DEBUGGING COMMANDS
# =============================================================================

# Watch build progress (updates every 30 seconds)
watch -n 30 "./quick-build-status.sh"

# Get detailed build info
./check-build.sh -r 435

# Get job logs
./check-build.sh -r 435 -l

# Check for successful builds
curl -s -H "Authorization: token $(cat ~/.github_token)" \
  "https://api.github.com/repos/supastishn/PojavBounce/actions/workflows/build.yml/runs?branch=nextgen&per_page=5" | \
  python3 -c "import sys, json; [print(f'{r[\"id\"]}: {r[\"status\"]} ({r.get(\"conclusion\", \"pending\")})') for r in json.load(sys.stdin)['workflow_runs']]"

# =============================================================================
# KEY FILES
# =============================================================================
# Main fix:
#   scripts/convert_saved_models.py (line 30)
#
# Monitoring tools:
#   quick-build-status.sh     - Status checker
#   check-build.sh            - Full monitor
#   download-models.sh        - Get models
#
# Documentation:
#   ONNX_FIX_SUMMARY.md       - Full explanation
#   BUILD_MONITOR_README.md   - Script docs
#   README_ONNX_FIX.txt       - This file

# =============================================================================
# EXPECTED TIMELINE
# =============================================================================
# Now        - Build #435 running
# ~5-10 min  - Build completes
# After      - Download models with ./download-models.sh
# Later      - Deploy to device
# Final      - Test and verify fix works

# =============================================================================
# VALIDATION CHECKLIST
# =============================================================================
# ☐ Setup GitHub token (~2 minutes)
# ☐ Check build status (./quick-build-status.sh)
# ☐ Wait for build completion (~5-10 minutes)
# ☐ Download models (./download-models.sh)
# ☐ Verify .onnx files generated
# ☐ Copy models to app
# ☐ Rebuild app
# ☐ Test on device
# ☐ Confirm "IR version 13" error is gone ✓

# =============================================================================
# SUPPORT
# =============================================================================
# Problem: Script won't run
#   → chmod +x quick-build-status.sh
#   → Check python3 installed: which python3
#   → Check curl installed: which curl

# Problem: GitHub token issues
#   → Get new token: https://github.com/settings/tokens
#   → Scopes needed: repo, workflow, actions:read
#   → Save to ~/.github_token and chmod 600

# Problem: Build failed
#   → Check logs: ./check-build.sh -r 435 -l
#   → Check GitHub Actions: https://github.com/supastishn/PojavBounce/actions

# Problem: Still getting IR version error
#   → Verify you're using opset 9 models
#   → Check app cache cleared before test
#   → Rebuild app with new model files

# =============================================================================
# REFERENCES
# =============================================================================
# GitHub: https://github.com/supastishn/PojavBounce
# Actions: https://github.com/supastishn/PojavBounce/actions
# ONNX Info: https://github.com/onnx/onnx/blob/main/docs/IR.md
# Tokens: https://github.com/settings/tokens
