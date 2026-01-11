#!/bin/bash

# Load environment variables from .env file
source .env

# Script to check the latest GitHub Actions workflow status and jobs
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

api_get() {
  curl -s -L \
    -H "Accept: application/vnd.github+json" \
    "${AUTH_HEADERS[@]}" \
    -H "X-GitHub-Api-Version: 2022-11-28" \
    "$1"
}

echo "=== GitHub Actions Status Checker ==="
echo "Repository: ${REPO_OWNER}/${REPO_NAME}"
echo "Branch: ${BRANCH}"
echo ""

echo "Fetching latest workflow runs..."
RUNS_JSON=$(api_get "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs?branch=${BRANCH}&per_page=5")

IFS=$'\t' read -r LATEST_RUN_ID LATEST_STATUS LATEST_CONCLUSION LATEST_NAME LATEST_CREATED < <(
  python -c 'import json,sys
raw=sys.stdin.read().strip()
if not raw:
  sys.exit(0)
try:
  data=json.loads(raw)
except Exception:
  sys.exit(0)
runs=data.get("workflow_runs") or []
if not runs:
  sys.exit(0)
r=runs[0]
fields=[str(r.get("id","")), str(r.get("status","")), str(r.get("conclusion") or "null"), str(r.get("name","")), str(r.get("created_at",""))]
print("\t".join(fields))' <<<"$RUNS_JSON"
)

if [ -z "${LATEST_RUN_ID}" ]; then
  echo "Error: No workflow runs found (or API response could not be parsed)"
  exit 1
fi

echo "Latest Workflow Run:"
echo "  ID: ${LATEST_RUN_ID}"
echo "  Name: ${LATEST_NAME}"
echo "  Status: ${LATEST_STATUS}"
echo "  Conclusion: ${LATEST_CONCLUSION}"
echo "  Created: ${LATEST_CREATED}"
echo ""

echo "Workflow URL: https://github.com/${REPO_OWNER}/${REPO_NAME}/actions/runs/${LATEST_RUN_ID}"
echo ""

# Determine build status
if [ "${LATEST_STATUS}" != "completed" ]; then
  echo "BUILD IN PROGRESS"
elif [ "${LATEST_CONCLUSION}" = "success" ]; then
  echo "BUILD SUCCESSFUL"
elif [ "${LATEST_CONCLUSION}" = "failure" ]; then
  echo "BUILD FAILED"
else
  echo "BUILD STATUS: ${LATEST_STATUS} / ${LATEST_CONCLUSION}"
fi

echo ""

echo "Fetching jobs for this run..."
JOBS_JSON=$(api_get "https://api.github.com/repos/${REPO_OWNER}/${REPO_NAME}/actions/runs/${LATEST_RUN_ID}/jobs")

python -c 'import json,sys
raw=sys.stdin.read().strip()
try:
  data=json.loads(raw) if raw else {}
except Exception:
  data={}
jobs=data.get("jobs") or []
print("Jobs:")
for j in jobs:
  name=j.get("name","")
  status=j.get("status","")
  concl=j.get("conclusion",None)
  concl = concl if concl is not None else "null"
  print(f"  - {name}: {status} / {concl}")' <<<"$JOBS_JSON"

# Show recent commits (requires origin/${BRANCH} to exist locally)
echo ""
echo "=== Recent Commits on ${BRANCH} ==="
git log --oneline -5 "origin/${BRANCH}" || true

echo ""
echo "=== Summary ==="
echo "Latest run status: ${LATEST_STATUS}"
echo "Latest run conclusion: ${LATEST_CONCLUSION}"
