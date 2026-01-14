# LiquidBounce Deploy - Quick Start Guide

## One-Minute Setup

### 1. Prerequisites
```bash
# Install GitHub CLI
pkg install gh

# Authenticate with GitHub
gh auth login
```

### 2. Deploy Latest Build
```bash
# Option A: Using Bash (simplest)
cd ~/prog/PojavBounce
./scripts/deploy_artifact.sh

# Option B: Using Python (more features)
python3 scripts/deploy_helper.py

# Option C: Validate environment first
./scripts/validate_deploy.sh
./scripts/deploy_artifact.sh
```

### 3. Launch in FCL
Open FCL and start playing with the latest LiquidBounce!

---

## Common Commands

```bash
# Deploy nextgen branch (default)
./scripts/deploy_artifact.sh

# Deploy main branch
./scripts/deploy_artifact.sh main

# Deploy with validation
./scripts/validate_deploy.sh

# Python version with options
python3 scripts/deploy_helper.py --branch nextgen --minecraft 1.21.11-Fabric

# Create an alias for quick access
echo "alias deploy-lb='cd ~/prog/PojavBounce && ./scripts/deploy_artifact.sh'" >> ~/.bashrc
source ~/.bashrc
deploy-lb  # Now you can use this anywhere
```

---

## What It Does

1. **Fetches** the latest workflow run from GitHub Actions
2. **Downloads** all artifacts from that build
3. **Finds** the LiquidBounce JAR file
4. **Backs up** any existing LiquidBounce mods
5. **Deploys** the new mod to FCL's mods folder

---

## Troubleshooting

### "gh: command not found"
```bash
pkg install gh
gh auth login
```

### "Could not find workflow runs"
- Check branch name is correct
- Verify GitHub CLI is authenticated: `gh auth status`

### "Permission denied"
- Make sure you have access to FCL's storage
- Try: `ls /data/data/com.termux/files/home/storage/shared/fcl/`

### "No JAR files found"
- Check if GitHub Actions workflow produces JAR artifacts
- Verify the branch has successful builds

---

## Files Created

| File | Purpose |
|------|---------|
| `deploy_artifact.sh` | Pure Bash deployment script |
| `deploy_helper.py` | Python deployment with advanced features |
| `validate_deploy.sh` | Environment validation before deploy |
| `DEPLOY_README.md` | Full documentation |
| `DEPLOY_QUICKSTART.md` | This file |

---

## Next Steps

1. **First time?** Run `./scripts/validate_deploy.sh`
2. **Ready to deploy?** Run `./scripts/deploy_artifact.sh`
3. **Need help?** Read `scripts/DEPLOY_README.md`

---

**Docs:** `/scripts/DEPLOY_README.md`
**Status:** Ready to deploy! ✓
