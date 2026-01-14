# LiquidBounce Development Resources

## Documentation

### DJL & Android Support
- **`DJL_ANDROID_SUMMARY.md`** - Complete analysis and deployment guide
  - DJL implementation details
  - Android support architecture
  - Troubleshooting guide
  - Enhancement ideas

### Deployment Automation
- **`DEPLOY_QUICKSTART.md`** - One-page quick start guide
  - Prerequisites setup
  - One-command deployment
  - Common commands
  - Quick troubleshooting

- **`scripts/DEPLOY_README.md`** - Comprehensive reference
  - Detailed script documentation
  - Configuration options
  - Automation examples
  - Advanced usage

## Scripts

### Deployment Scripts
- **`scripts/deploy_artifact.sh`** - Bash deployment (simplest)
  ```bash
  ./scripts/deploy_artifact.sh              # Deploy nextgen
  ./scripts/deploy_artifact.sh main         # Deploy main
  ```

- **`scripts/deploy_helper.py`** - Python deployment (more features)
  ```bash
  python3 scripts/deploy_helper.py                      # Deploy nextgen
  python3 scripts/deploy_helper.py --branch main       # Deploy main
  python3 scripts/deploy_helper.py --minecraft 1.20.1  # Different version
  ```

### Validation
- **`scripts/validate_deploy.sh`** - Environment checker
  ```bash
  ./scripts/validate_deploy.sh  # Check all prerequisites
  ```

## Quick Reference

### Setup (First Time)
```bash
pkg install gh
gh auth login
./scripts/validate_deploy.sh
```

### Deploy Latest
```bash
./scripts/deploy_artifact.sh
```

### Check Status
```bash
gh run list --repo CCBlueX/LiquidBounce --branch nextgen --limit 5
```

## Directory Structure

```
/prog/PojavBounce/
├── DJL_ANDROID_SUMMARY.md          ← Read this first
├── DEPLOY_QUICKSTART.md             ← Quick setup
├── RESOURCES.md                      ← This file
├── scripts/
│   ├── deploy_artifact.sh            ← Bash deployment
│   ├── deploy_helper.py              ← Python deployment
│   ├── validate_deploy.sh            ← Environment check
│   ├── DEPLOY_README.md              ← Full documentation
│   ├── djl_to_onnx.py               ← DJL model conversion
│   └── djl_params_to_onnx_fixed.py  ← Parameter conversion
├── src/
│   └── main/
│       ├── kotlin/net/ccbluex/liquidbounce/
│       │   └── deeplearn/
│       │       ├── DeepLearningEngine.kt     ← Android support
│       │       └── ... (other DL files)
│       └── java/net/ccbluex/liquidbounce/
│           └── injection/mixins/djl/
│               └── MixinUtils.java           ← HTTP progress tracking
└── build.gradle.kts                 ← DJL dependencies

/storage/shared/fcl/.minecraft/
└── versions/1.21.11-Fabric/
    └── mods/
        └── liquidbounce-*.jar       ← Deployed mod
```

## Key Files to Know

### Implementation
| File | Purpose | Status |
|------|---------|--------|
| `DeepLearningEngine.kt` | DJL initialization | ✓ Well implemented |
| `MixinUtils.java` | HTTP progress tracking | ✓ Complete |
| `build.gradle.kts` | Dependencies | ✓ Configured |

### Documentation
| File | Purpose | Read If |
|------|---------|---------|
| `DJL_ANDROID_SUMMARY.md` | Complete guide | You want full context |
| `DEPLOY_QUICKSTART.md` | Quick setup | You just want to deploy |
| `scripts/DEPLOY_README.md` | Reference | You need detailed help |
| `RESOURCES.md` | This file | You need a map |

### Scripts
| File | Purpose | When to Use |
|------|---------|-------------|
| `deploy_artifact.sh` | Deploy via Bash | First time, minimal setup |
| `deploy_helper.py` | Deploy via Python | You want more features |
| `validate_deploy.sh` | Validate environment | Troubleshooting setup |

## Common Tasks

### Deploy Latest Build
```bash
cd ~/prog/PojavBounce
./scripts/deploy_artifact.sh
```

### Deploy from Different Branch
```bash
./scripts/deploy_artifact.sh main      # Deploy main branch
./scripts/deploy_artifact.sh develop   # Deploy develop branch
```

### Validate Setup
```bash
./scripts/validate_deploy.sh
```

### Check Recent Builds
```bash
gh run list --repo CCBlueX/LiquidBounce --branch nextgen --limit 10
```

### View Build Artifacts
```bash
gh run view <RUN_ID> --repo CCBlueX/LiquidBounce
```

### Manual Artifact Download
```bash
gh run download <RUN_ID> --repo CCBlueX/LiquidBounce --dir ./my_artifacts
```

## Android Development Flow

```
1. Make code changes
2. Push to GitHub
3. GitHub Actions builds (auto)
4. Run: ./scripts/deploy_artifact.sh
5. Test in FCL
6. Repeat!
```

## Links & Resources

### Official
- **LiquidBounce**: https://github.com/CCBlueX/LiquidBounce
- **DJL Documentation**: https://docs.djl.ai/
- **DJL Android**: https://docs.djl.ai/docs/development/how_to_use_djl_in_android_app.html

### Tools
- **GitHub CLI**: https://cli.github.com/
- **FCL (Minecraft Launcher)**: https://github.com/PojavLauncherTeam/PojavLauncher
- **Termux**: https://termux.dev/

### References
- **PyTorch Android**: https://pytorch.org/mobile/android/
- **Gradle Docs**: https://gradle.org/
- **Kotlin Docs**: https://kotlinlang.org/docs/home.html

## Environment Variables

### DJL Configuration (set in DeepLearningEngine.kt)
- `DJL_CACHE_DIR` - Where DJL caches models and libraries
- `ENGINE_CACHE_DIR` - Where engines store native binaries
- `DJL_DEFAULT_ENGINE` - Engine to use (PyTorch)
- `PYTORCH_FLAVOR` - cpu, cpu-android, gpu, etc.
- `OPT_OUT_TRACKING` - Disable telemetry

### For Scripts
- `REPO` - GitHub repository (CCBlueX/LiquidBounce)
- `BRANCH` - Branch to deploy from (nextgen, main)
- `MINECRAFT_VERSION` - Target version (1.21.11-Fabric)
- `MODS_DIR` - Where FCL mods are stored

## Troubleshooting Quick Links

| Problem | Solution |
|---------|----------|
| `gh: command not found` | `pkg install gh` |
| Not authenticated | `gh auth login` |
| Can't find workflows | `gh run list --repo CCBlueX/LiquidBounce` |
| Can't find JAR file | Check GitHub Actions produces JAR artifacts |
| Permission denied | Check FCL installation path |
| Deploy seems stuck | Check `/tmp/liquidbounce_deploy/` directory |

## Getting Help

1. **First check**: `./scripts/validate_deploy.sh`
2. **Read documentation**: `scripts/DEPLOY_README.md`
3. **Check logs**: Output from scripts includes detailed messages
4. **GitHub Issues**: https://github.com/CCBlueX/LiquidBounce/issues
5. **Discord**: https://discord.gg/ccbluex

---

**Last Updated**: 2025-01-14
**Status**: All systems ready for deployment ✓
