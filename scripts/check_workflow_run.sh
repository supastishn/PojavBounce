#!/usr/bin/env bash
set -euo pipefail

# Poll GitHub Actions for a workflow run status and print progress (jobs/steps).
# Usage:
#   ./scripts/check_workflow_run.sh [-r owner/repo] [-w workflow_name] [-b branch] [-i interval_seconds] [-t token] [run_id]
# Examples:
#   ./scripts/check_workflow_run.sh supastishn/PojavBounce Build nextgen
#   ./scripts/check_workflow_run.sh -i 60 20923939670

OWNER_REPO=""
WORKFLOW_NAME="Build"
BRANCH="nextgen"
INTERVAL=30
TOKEN="${GITHUB_TOKEN:-}"
WAIT_FIRST=0

print_usage() {
  echo "Usage: $0 [-r owner/repo] [-w workflow_name] [-b branch] [-i interval_seconds] [-t token] [run_id]"
  echo "If run_id is omitted, the latest run for workflow_name on branch will be used."
}

while getopts ":r:w:b:i:t:s:h" opt; do
  case $opt in
    r) OWNER_REPO="$OPTARG" ;;
    w) WORKFLOW_NAME="$OPTARG" ;;
    b) BRANCH="$OPTARG" ;;
    i) INTERVAL="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    s) WAIT_FIRST="$OPTARG" ;;
    h) print_usage; exit 0 ;;
    \?) echo "Invalid option: -$OPTARG" >&2; print_usage; exit 2 ;;
  esac
done
shift $((OPTIND-1))

RUN_ID="$1"

if [ -z "$OWNER_REPO" ]; then
  # Try to infer from git remote
  if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    origin=$(git remote get-url origin 2>/dev/null || true)
    if [ -n "$origin" ]; then
      # parse origin for owner/repo
      if [[ $origin =~ ^git@github.com:(.+)/(.+).git$ ]]; then
        OWNER_REPO="${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
      elif [[ $origin =~ ^https?://github.com/(.+)/(.+)(.git)?$ ]]; then
        OWNER_REPO="${BASH_REMATCH[1]}/${BASH_REMATCH[2]}"
      fi
    fi
  fi
fi

if [ -z "$OWNER_REPO" ]; then
  echo "ERROR: owner/repo must be specified with -r or available via git remote 'origin'" >&2
  print_usage
  exit 2
fi

if [ -z "$TOKEN" ]; then
  echo "Warning: GITHUB_TOKEN not set; unauthenticated requests may be rate limited or blocked." >&2
fi

if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl is required" >&2
  exit 2
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq is required to parse API responses" >&2
  exit 2
fi

API_BASE="https://api.github.com/repos/$OWNER_REPO"

auth_header=()
if [ -n "$TOKEN" ]; then
  auth_header=( -H "Authorization: token $TOKEN" )
fi

if [ -z "$RUN_ID" ]; then
  echo "Finding latest run for workflow '$WORKFLOW_NAME' on branch '$BRANCH'..."
  runs_json=$(curl -sS "${API_BASE}/actions/runs?branch=${BRANCH}&per_page=20" "${auth_header[@]}")
  RUN_ID=$(echo "$runs_json" | jq -r --arg name "$WORKFLOW_NAME" '.workflow_runs[] | select(.name == $name) | .id' | head -n1)
  if [ -z "$RUN_ID" ] || [ "$RUN_ID" == "null" ]; then
    echo "No recent workflow runs found for $WORKFLOW_NAME on branch $BRANCH" >&2
    exit 3
  fi
  echo "Using run id: $RUN_ID"
else
  echo "Using provided run id: $RUN_ID"
fi

if [ "$WAIT_FIRST" -gt 0 ]; then
  echo "Waiting $WAIT_FIRST seconds before first status check..."
  sleep "$WAIT_FIRST"
fi

while true; do
  echo "--- Checking run $RUN_ID (repo: $OWNER_REPO) ---"

  run_json=$(curl -sS "${API_BASE}/actions/runs/${RUN_ID}" "${auth_header[@]}")
  status=$(echo "$run_json" | jq -r '.status')
  conclusion=$(echo "$run_json" | jq -r '.conclusion')
  updated_at=$(echo "$run_json" | jq -r '.updated_at')

  echo "status: $status  conclusion: ${conclusion:-N/A}  updated_at: ${updated_at:-N/A}"

  # Print jobs and steps
  jobs_json=$(curl -sS "${API_BASE}/actions/runs/${RUN_ID}/jobs" "${auth_header[@]}")
  echo "Jobs summary:";
  echo "$jobs_json" | jq -r '.jobs[] | "- Job: \(.name) (id:\(.id)) status:\(.status) conclusion:\(.conclusion)"'

  echo "Steps per job (most recent jobs):"
  echo "$jobs_json" | jq -r '.jobs[] | "Job: \(.name)" , ( .steps[] | "  - Step: \(.name) status:\(.status) conclusion:\(.conclusion)" )'

  if [ "$status" == "completed" ]; then
    echo "Run completed with conclusion: $conclusion"
    break
  fi

  echo "Waiting $INTERVAL seconds before next check..."
  sleep "$INTERVAL"
done

exit 0
