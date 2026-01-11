#!/bin/bash

# Load environment variables from .env file
source .env

# Script to fetch full GitHub Actions build logs for detailed analysis
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

echo "=== GitHub Actions Full Logs Fetcher ==="
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
LATEST_CREATED=$(echo "${RUNS_JSON}" | grep -o '"created_at": "[^"]*' | head -1 | sed 's/"created_at": "//')

if [ -z "${LATEST_RUN_ID}" ]; then
  echo "Error: No workflow runs found"
  exit 1
fi

echo "Latest Workflow Run:"
echo "  ID: ${LATEST_RUN_ID}"
echo "  Status: ${LATEST_STATUS}"
echo "  Conclusion: ${LATEST_CONCLUSION}"
echo "  Created: ${LATEST_CREATED}"
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

# Fetch logs for all jobs, not just failed ones
echo "=== Fetching All Job Logs ==="
JOB_IDS=$(echo "${JOBS_JSON}" | grep -o '"id": [0-9]*' | grep -o '[0-9]*')

for JOB_ID in $JOB_IDS; do
  JOB_NAME=$(echo "${JOBS_JSON}" | grep -A5 "\"id\": $JOB_ID" | grep '"name"' | sed 's/.*"name": "\([^"]*\)".*/\1/')
  JOB_CONCLUSION=$(echo "${JOBS_JSON}" | grep -A5 "\"id\": $JOB_ID" | grep '"conclusion"' | sed 's/.*"conclusion": "\([^"]*\)".*/\1/' | sed 's/null/in_progress/')

  echo ""
  echo "=== Logs for Job: $JOB_NAME (ID: $JOB_ID, Status: $JOB_CONCLUSION) ==="

  # Fetch full logs for this job
  LOGS=$(curl -s -L \
    -H "Accept: application/vnd.github+json" \
    "${AUTH_HEADERS[@]}" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/jobs/${JOB_ID}/logs")

  if [ -n "$LOGS" ]; then
    echo "$LOGS"
  else
    echo "No logs available for this job"
  fi
done

echo ""
echo "=== Summary ==="
echo "Full logs fetched for run: ${LATEST_RUN_ID}"
echo "Web URL: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions/runs/${LATEST_RUN_ID}"