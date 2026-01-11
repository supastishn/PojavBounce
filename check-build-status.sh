#!/bin/bash

# Script to check the latest GitHub Actions workflow status and logs
# Repository: supastishn/PojavBounce

set -e

REPO_OWNER="supastishn"
REPO_NAME="PojavBounce"
BRANCH="nextgen"

# Optional: set GITHUB_TOKEN in your environment to avoid API rate limits and access logs.
GITHUB_TOKEN="${GITHUB_TOKEN:-}"
AUTH_HEADERS=()
if [ -n "${GITHUB_TOKEN}" ]; then
  AUTH_HEADERS=(-H "Authorization: Bearer ${GITHUB_TOKEN}")
fi

echo "=== GitHub Actions Status Checker ==="
echo "Repository: ${REPO_OWNER}/${REPO_NAME}"
echo "Branch: ${BRANCH}"
echo ""

# Fetch latest workflow runs
echo "Fetching latest workflow runs..."
RUNS_JSON=$(curl -s -L \
  -H "Accept: application/vnd.github+json" \
  "${AUTH_HEADERS[@]}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs?branch=${BRANCH}&per_page=5")

# Extract latest run details
LATEST_RUN_ID=$(echo "${RUNS_JSON}" | grep -o '"id": [0-9]*' | head -1 | grep -o '[0-9]*')
LATEST_STATUS=$(echo "${RUNS_JSON}" | grep -o '"status": "[^"]*' | head -1 | sed 's/"status": "//')
LATEST_CONCLUSION=$(echo "${RUNS_JSON}" | grep -o '"conclusion": "[^"]*' | head -1 | sed 's/"conclusion": "//' | sed 's/null/in_progress/')
LATEST_NAME=$(echo "${RUNS_JSON}" | grep -o '"name": "[^"]*' | head -1 | sed 's/"name": "//')
LATEST_CREATED=$(echo "${RUNS_JSON}" | grep -o '"created_at": "[^"]*' | head -1 | sed 's/"created_at": "//')

if [ -z "${LATEST_RUN_ID}" ]; then
  echo "Error: No workflow runs found"
  exit 1
fi

echo "Latest Workflow Run:"
echo "  ID: ${LATEST_RUN_ID}"
echo "  Name: ${LATEST_NAME}"
echo "  Status: ${LATEST_STATUS}"
echo "  Conclusion: ${LATEST_CONCLUSION}"
echo "  Created: ${LATEST_CREATED}"
echo ""

# Determine build status
if [ "${LATEST_CONCLUSION}" = "success" ]; then
  echo "✅ BUILD SUCCESSFUL"
elif [ "${LATEST_CONCLUSION}" = "failure" ]; then
  echo "❌ BUILD FAILED"
elif [ "${LATEST_STATUS}" = "in_progress" ] || [ "${LATEST_STATUS}" = "queued" ]; then
  echo "🔄 BUILD IN PROGRESS"
else
  echo "⚠️  BUILD STATUS: ${LATEST_STATUS} / ${LATEST_CONCLUSION}"
fi

echo ""
echo "Workflow URL: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions/runs/${LATEST_RUN_ID}"
echo ""

# Fetch jobs for this run
echo "Fetching jobs for this run..."
JOBS_JSON=$(curl -s -L \
  -H "Accept: application/vnd.github+json" \
  "${AUTH_HEADERS[@]}" \
  -H "X-GitHub-Api-Version: 2022-11-28" \
  "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs/${LATEST_RUN_ID}/jobs")

echo "Jobs:"
echo "${JOBS_JSON}" | grep -E '"name":|"status":|"conclusion":' | sed 's/^[ \t]*/  /'
echo ""

# Offer to fetch logs if build failed
if [ "${LATEST_CONCLUSION}" = "failure" ]; then
  echo "=== Fetching Failed Job Logs ==="

  # Get first failed job ID
  FAILED_JOB_ID=$(echo "${JOBS_JSON}" | grep -B5 '"conclusion": "failure"' | grep -o '"id": [0-9]*' | head -1 | grep -o '[0-9]*')

  if [ -n "${FAILED_JOB_ID}" ]; then
    echo "Fetching logs for failed job ID: ${FAILED_JOB_ID}"
    echo ""

    curl -s -L \
      -H "Accept: application/vnd.github+json" \
      "${AUTH_HEADERS[@]}" \
      -H "X-GitHub-Api-Version: 2022-11-28" \
      "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/jobs/${FAILED_JOB_ID}/logs" | tail -100
  fi
fi

# Show recent commits
echo ""
echo "=== Recent Commits on ${BRANCH} ==="
git log --oneline -5 origin/${BRANCH}

echo ""
echo "=== Summary ==="
echo "Latest build: ${LATEST_CONCLUSION}"
echo "Check full logs at: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions/runs/${LATEST_RUN_ID}"
