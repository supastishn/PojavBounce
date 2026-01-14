# LiquidBounce Deploy Tools

Deploy the latest LiquidBounce build from GitHub Actions directly to your FCL Minecraft installation.

## Prerequisites

- **GitHub CLI** (`gh`) installed and authenticated
  ```bash
  # Install on Termux
  pkg install gh

  # Authenticate
  gh auth login
  ```

## Available Scripts

### 1. Bash Script (`deploy_artifact.sh`)

Pure Bash implementation, no external Python dependencies needed.

```bash
# Deploy latest nextgen branch
./scripts/deploy_artifact.sh

# Deploy specific branch
./scripts/deploy_artifact.sh main
```

**Features:**
- Fetches latest completed workflow run
- Downloads artifacts automatically
- Finds LiquidBounce JAR file
- Backs up old mods
- Copies to FCL mods directory

**Output:**
```
==========================================
LiquidBounce Deploy Script
==========================================
Repository: CCBlueX/LiquidBounce
Branch: nextgen
Target Minecraft: 1.21.11-Fabric
Mods Directory: /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods

[1/4] Fetching latest GitHub Actions workflow run...
Found workflow run: 12345678
[2/4] Fetching artifacts from workflow run...
[3/4] Locating LiquidBounce JAR file...
Found JAR: liquidbounce-1.21.11-nextgen-build.12345678.jar
[4/4] Deploying mod to FCL...

==========================================
✓ Deploy Successful!
==========================================
Mod deployed: liquidbounce-1.21.11-nextgen-build.12345678.jar
Location: /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods/liquidbounce-1.21.11-nextgen-build.12345678.jar
File size: 5.2M

Ready to launch with FCL!
```

### 2. Python Script (`deploy_helper.py`)

Advanced version with better error handling and more features.

```bash
# Deploy latest nextgen branch
python3 scripts/deploy_helper.py

# Deploy specific branch
python3 scripts/deploy_helper.py --branch main

# Deploy to different Minecraft version
python3 scripts/deploy_helper.py --minecraft 1.20.1-Fabric

# Combine options
python3 scripts/deploy_helper.py --branch development --minecraft 1.21.11-Fabric
```

**Features:**
- All features from Bash script
- Better error messages
- Timestamped backups
- File size reporting
- Command-line argument support
- Detailed progress reporting

**Output:**
```
==================================================
LiquidBounce Deploy Helper
==================================================
Repository: CCBlueX/LiquidBounce
Branch: nextgen
Target: Minecraft 1.21.11-Fabric
Mods Directory: /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods

[1/5] Fetching latest GitHub Actions workflow run...
✓ Found workflow run: 12345678
[2/5] Downloading artifacts from GitHub Actions...
✓ Downloaded 3 files
[3/5] Locating LiquidBounce JAR file...
✓ Found: liquidbounce-1.21.11-nextgen-build.12345678.jar
[4/5] Managing old mod files...
✓ Backed up: liquidbounce-1.21.11-nextgen-build.12345677.jar
[5/5] Deploying mod to FCL...
✓ Deployed: liquidbounce-1.21.11-nextgen-build.12345678.jar
✓ Size: 5.23 MB

==================================================
✓ Deploy Successful!
==================================================
Location: /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods/liquidbounce-1.21.11-nextgen-build.12345678.jar
Ready to launch with FCL!
```

## Configuration

### Customizing Paths

The scripts have hardcoded paths but can be edited:

**For Bash script:**
```bash
# Edit these lines in deploy_artifact.sh
MINECRAFT_VERSION="1.21.11-Fabric"
MODS_DIR="/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/${MINECRAFT_VERSION}/mods"
BRANCH="${1:-nextgen}"
```

**For Python script:**
```bash
# Use command-line arguments
python3 deploy_helper.py --minecraft 1.20.1-Fabric --branch main
```

## Troubleshooting

### Error: "Could not find any completed workflow runs"

**Cause:** Either the branch doesn't exist, or GitHub CLI isn't authenticated.

**Solution:**
```bash
# Check if gh is installed and authenticated
gh auth status

# Authenticate if needed
gh auth login

# Check available branches
gh repo view CCBlueX/LiquidBounce --json nameWithOwner
```

### Error: "Could not find LiquidBounce JAR file"

**Cause:** The artifact structure changed or no JAR files were downloaded.

**Solution:**
1. Check what files were downloaded:
   ```bash
   ls -la /tmp/liquidbounce_deploy/
   ```

2. Check if GitHub Actions workflow actually produced JAR artifacts in your branch

3. Make sure you're using the correct branch name

### Error: "Permission denied" when accessing mods directory

**Cause:** Storage permissions issue on Android.

**Solution:**
```bash
# Check if mods directory is accessible
ls -la /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/

# Try creating with proper permissions
mkdir -p /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods
```

## Automation

### Create an Alias

```bash
# Add to ~/.bashrc or ~/.profile
alias deploy-lb='python3 ~/prog/PojavBounce/scripts/deploy_helper.py'

# Or with branch selection
alias deploy-lb-nextgen='python3 ~/prog/PojavBounce/scripts/deploy_helper.py --branch nextgen'
alias deploy-lb-main='python3 ~/prog/PojavBounce/scripts/deploy_helper.py --branch main'
```

### Automatic Scheduled Deployment

Using cron (for Termux with termux-services):

```bash
# Edit crontab
crontab -e

# Add scheduled job (daily at 3 AM)
0 3 * * * /data/data/com.termux/files/home/prog/PojavBounce/scripts/deploy_artifact.sh >> /data/data/com.termux/files/home/.deploy_logs 2>&1
```

## Backup Management

The scripts automatically backup old LiquidBounce mods to:
```
/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods/.backup/
```

### Restore Previous Version

```bash
# List backups
ls -la /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods/.backup/

# Restore a specific backup
cp /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods/.backup/liquidbounce*.jar \
   /data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions/1.21.11-Fabric/mods/
```

## GitHub Actions Workflow

These scripts work with LiquidBounce's CI/CD pipeline. For your own projects, ensure:

1. **Workflow produces JAR artifacts** in the build/libs directory
2. **Artifacts are accessible** via GitHub Actions API
3. **Branch name is correct** when calling the script

Example workflow structure:
```yaml
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/upload-artifact@v3
        with:
          name: build
          path: build/libs/*.jar
```

## Development

### Extending the Scripts

**Add support for other Minecraft versions:**
```python
# In deploy_helper.py
MINECRAFT_VERSIONS = {
    "latest": "1.21.11-Fabric",
    "1.20": "1.20.1-Fabric",
    "1.19": "1.19.2-Fabric"
}
```

**Add support for other mods:**
```python
# In deploy_helper.py
MOD_PATTERNS = [
    "*liquidbounce*.jar",
    "*fabric-api*.jar",
    "*sodium*.jar"
]
```

## Support

- **GitHub Issue:** https://github.com/CCBlueX/LiquidBounce/issues
- **Documentation:** https://liquidbounce.net
- **Discord:** https://discord.gg/ccbluex

---

**Last Updated:** 2025-01-14
**Version:** 1.0.0
