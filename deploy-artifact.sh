#!/bin/bash

# Load environment variables from .env file
source .env

# Script to fetch the latest GitHub Actions artifact and deploy to FCL mods directory
# Repository: supastishn/PojavBounce
# Target: /storage/emulated/0/FCL/.minecraft/versions/1.21.11-Fabric/mods

set -e  # Exit on error

REPO_OWNER="supastishn"
REPO_NAME="PojavBounce"
BRANCH="nextgen"
TARGET_DIR="/storage/emulated/0/FCL/.minecraft/versions/1.21.11-Fabric/mods"
TEMP_DIR="/tmp/artifact-download-$$"

echo "=== GitHub Actions Artifact Deployer ==="
echo "Repository: ${REPO_OWNER}/${REPO_NAME}"
echo "Branch: ${BRANCH}"
echo "Target: ${TARGET_DIR}"
echo ""

# Create temp directory
mkdir -p "${TEMP_DIR}"

# Fetch latest workflow runs for the branch
echo "Fetching latest workflow runs..."
WORKFLOW_RUNS=$(curl -s -L \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs?branch=${BRANCH}&status=success&per_page=5")

# Extract the most recent successful run ID
RUN_ID=$(echo "${WORKFLOW_RUNS}" | grep -o '"id": [0-9]*' | head -1 | grep -o '[0-9]*')

if [ -z "${RUN_ID}" ]; then
  echo "Error: No successful workflow runs found for branch '${BRANCH}'"
  rm -rf "${TEMP_DIR}"
  exit 1
fi

echo "Latest successful run ID: ${RUN_ID}"

# Fetch artifacts for this run
echo "Fetching artifacts for run ${RUN_ID}..."
ARTIFACTS=$(curl -s -L \
  -H "Accept: application/vnd.github+json" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs/${RUN_ID}/artifacts")

# Extract the first artifact's download URL and name
ARTIFACT_URL=$(echo "${ARTIFACTS}" | grep -o '"archive_download_url": "[^"]*' | head -1 | sed 's/"archive_download_url": "//')
ARTIFACT_NAME=$(echo "${ARTIFACTS}" | grep -o '"name": "[^"]*' | head -1 | sed 's/"name": "//')

if [ -z "${ARTIFACT_URL}" ]; then
  echo "Error: No artifacts found for run ${RUN_ID}"
  rm -rf "${TEMP_DIR}"
  exit 1
fi

echo "Artifact name: ${ARTIFACT_NAME}"
echo "Artifact URL: ${ARTIFACT_URL}"

# GitHub token for authentication (do not hardcode in repo)
: "${GITHUB_TOKEN:?Set GITHUB_TOKEN in your environment (needs access to download Actions artifacts)}"

# Download the artifact
echo "Downloading artifact..."
curl -L \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: Bearer ${GITHUB_TOKEN}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "${ARTIFACT_URL}" \
  -o "${TEMP_DIR}/artifact.zip"

if [ ! -f "${TEMP_DIR}/artifact.zip" ]; then
  echo "Error: Failed to download artifact"
  rm -rf "${TEMP_DIR}"
  exit 1
fi

echo "Artifact downloaded successfully"

# Extract the artifact
echo "Extracting artifact..."
unzip -q "${TEMP_DIR}/artifact.zip" -d "${TEMP_DIR}/extracted"

# Find JAR files in the extracted content
echo "Looking for JAR files..."
JAR_FILES=$(find "${TEMP_DIR}/extracted" -name "*.jar" -type f)

if [ -z "${JAR_FILES}" ]; then
  echo "Error: No JAR files found in artifact"
  rm -rf "${TEMP_DIR}"
  exit 1
fi

# Create target directory if it doesn't exist
mkdir -p "${TARGET_DIR}"

# Remove old LiquidBounce JAR files
echo "Removing old LiquidBounce JAR files from ${TARGET_DIR}..."
rm -f "${TARGET_DIR}"/liquidbounce*.jar "${TARGET_DIR}"/LiquidBounce*.jar

# Copy new JAR files to target directory
echo "Deploying new JAR files..."
while IFS= read -r jar_file; do
  jar_basename=$(basename "${jar_file}")
  echo "  -> ${jar_basename}"
  cp "${jar_file}" "${TARGET_DIR}/${jar_basename}"
done <<< "${JAR_FILES}"

# Cleanup
echo "Cleaning up temporary files..."
rm -rf "${TEMP_DIR}"

echo ""
echo "=== Deployment Complete ==="
echo "JAR files have been deployed to ${TARGET_DIR}"
echo ""
