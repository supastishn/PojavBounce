#!/usr/bin/env python3
"""
LiquidBounce Deploy Helper
Assists with downloading and deploying latest LiquidBounce artifacts from GitHub Actions
"""

import subprocess
import json
import os
import sys
import shutil
from pathlib import Path
from datetime import datetime

class LiquidBounceDeploy:
    def __init__(self, minecraft_version="1.21.11-Fabric", branch="nextgen"):
        self.repo = "CCBlueX/LiquidBounce"
        self.minecraft_version = minecraft_version
        self.branch = branch
        self.mods_dir = Path("/data/data/com.termux/files/home/storage/shared/fcl/.minecraft/versions") / minecraft_version / "mods"
        self.temp_dir = Path("/tmp/liquidbounce_deploy")

    def run_command(self, cmd, check=True):
        """Run shell command and return output"""
        try:
            result = subprocess.run(
                cmd,
                shell=True,
                capture_output=True,
                text=True,
                check=check
            )
            return result.stdout.strip(), result.returncode
        except subprocess.CalledProcessError as e:
            return e.stderr, e.returncode

    def get_latest_workflow_run(self):
        """Get the latest completed workflow run ID"""
        print("[1/5] Fetching latest GitHub Actions workflow run...")
        cmd = f'gh run list --repo {self.repo} --branch {self.branch} --status completed --limit 1 --json databaseId --jq ".[0].databaseId"'
        output, code = self.run_command(cmd)

        if not output or code != 0:
            print(f"ERROR: Could not find workflow runs for branch '{self.branch}'")
            print("Make sure you have 'gh' CLI installed and authenticated.")
            return None

        print(f"✓ Found workflow run: {output}")
        return output

    def download_artifacts(self, workflow_id):
        """Download artifacts from workflow run"""
        print("[2/5] Downloading artifacts from GitHub Actions...")
        self.temp_dir.mkdir(parents=True, exist_ok=True)

        cmd = f'gh run download {workflow_id} --repo {self.repo} --dir "{self.temp_dir}"'
        output, code = self.run_command(cmd, check=False)

        if code != 0:
            print(f"ERROR: Failed to download artifacts: {output}")
            return False

        # Check if any files were downloaded
        files = list(self.temp_dir.glob("**/*"))
        if not files:
            print("ERROR: No files downloaded")
            return False

        print(f"✓ Downloaded {len(files)} files")
        return True

    def find_mod_jar(self):
        """Find the LiquidBounce JAR file"""
        print("[3/5] Locating LiquidBounce JAR file...")

        # Search for JAR files
        jar_files = list(self.temp_dir.glob("**/*.jar"))

        if not jar_files:
            print("ERROR: No JAR files found in artifacts")
            print("Available files:")
            for f in self.temp_dir.glob("**/*"):
                print(f"  - {f.relative_to(self.temp_dir)}")
            return None

        # Look for liquidbounce-specific JAR
        for jar in jar_files:
            if "liquidbounce" in jar.name.lower():
                print(f"✓ Found: {jar.name}")
                return jar

        # Fallback to first JAR
        if jar_files:
            print(f"✓ Using first JAR: {jar_files[0].name}")
            return jar_files[0]

        return None

    def backup_old_mods(self):
        """Backup existing mod files"""
        print("[4/5] Managing old mod files...")

        if not self.mods_dir.exists():
            print("✓ No existing mods directory")
            return True

        old_mods = list(self.mods_dir.glob("liquidbounce*.jar"))

        if not old_mods:
            print("✓ No existing LiquidBounce mods found")
            return True

        backup_dir = self.mods_dir / ".backup"
        backup_dir.mkdir(parents=True, exist_ok=True)

        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

        for old_mod in old_mods:
            backup_path = backup_dir / f"{old_mod.stem}__{timestamp}.jar"
            shutil.move(str(old_mod), str(backup_path))
            print(f"✓ Backed up: {old_mod.name}")

        return True

    def deploy_mod(self, mod_jar):
        """Deploy the mod to mods directory"""
        print("[5/5] Deploying mod to FCL...")

        self.mods_dir.mkdir(parents=True, exist_ok=True)

        target = self.mods_dir / mod_jar.name
        shutil.copy2(str(mod_jar), str(target))

        size_bytes = target.stat().st_size
        size_mb = size_bytes / (1024 * 1024)

        print(f"✓ Deployed: {mod_jar.name}")
        print(f"✓ Size: {size_mb:.2f} MB")

        return True

    def cleanup(self):
        """Clean up temporary files"""
        if self.temp_dir.exists():
            shutil.rmtree(self.temp_dir)

    def run(self):
        """Run the full deployment process"""
        print("=" * 50)
        print("LiquidBounce Deploy Helper")
        print("=" * 50)
        print(f"Repository: {self.repo}")
        print(f"Branch: {self.branch}")
        print(f"Target: Minecraft {self.minecraft_version}")
        print(f"Mods Directory: {self.mods_dir}")
        print()

        try:
            # Step 1: Get workflow run
            workflow_id = self.get_latest_workflow_run()
            if not workflow_id:
                return 1

            # Step 2: Download artifacts
            if not self.download_artifacts(workflow_id):
                return 1

            # Step 3: Find JAR
            mod_jar = self.find_mod_jar()
            if not mod_jar:
                return 1

            # Step 4: Backup old mods
            if not self.backup_old_mods():
                return 1

            # Step 5: Deploy
            if not self.deploy_mod(mod_jar):
                return 1

            # Success!
            print()
            print("=" * 50)
            print("✓ Deploy Successful!")
            print("=" * 50)
            print(f"Location: {self.mods_dir / mod_jar.name}")
            print("Ready to launch with FCL!")
            return 0

        except Exception as e:
            print(f"ERROR: {e}")
            return 1
        finally:
            self.cleanup()

def main():
    import argparse
    parser = argparse.ArgumentParser(description="Deploy LiquidBounce from GitHub Actions")
    parser.add_argument("--minecraft", default="1.21.11-Fabric", help="Minecraft version")
    parser.add_argument("--branch", default="nextgen", help="GitHub branch")

    args = parser.parse_args()

    deployer = LiquidBounceDeploy(minecraft_version=args.minecraft, branch=args.branch)
    return deployer.run()

if __name__ == "__main__":
    sys.exit(main())
