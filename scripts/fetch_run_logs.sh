#!/usr/bin/env bash
set -euo pipefail

# Download and extract the workflow run logs ZIP for a given run id (or the latest run for branch/workflow)
# Usage:
#   ./scripts/fetch_run_logs.sh -r owner/repo [-w workflow_name] [-b branch] [-i run_id] [-o out_dir] [-t token]
# Examples:
#   ./scripts/fetch_run_logs.sh -r supastishn/PojavBounce -i 20924604339
#   ./scripts/fetch_run_logs.sh -r supastishn/PojavBounce -w Build -b nextgen

OWNER_REPO=""
WORKFLOW_NAME="Build"
BRANCH="nextgen"
RUN_ID=""
OUT_DIR="./logs/run-logs"
TOKEN="${GITHUB_TOKEN:-}"

print_usage() {
  echo "Usage: $0 -r owner/repo [-w workflow_name] [-b branch] [-i run_id] [-o out_dir] [-t token]"
}

while getopts ":r:w:b:i:o:t:h" opt; do
  case $opt in
    r) OWNER_REPO="$OPTARG" ;;
    w) WORKFLOW_NAME="$OPTARG" ;;
    b) BRANCH="$OPTARG" ;;
    i) RUN_ID="$OPTARG" ;;
    o) OUT_DIR="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    h) print_usage; exit 0 ;;
    \?) echo "Invalid option: -$OPTARG" >&2; print_usage; exit 2 ;;
  esac
done

if [ -z "$OWNER_REPO" ]; then
  echo "ERROR: owner/repo must be specified with -r" >&2
  print_usage
  exit 2
fi

auth_header=()
if [ -n "$TOKEN" ]; then
  auth_header=( -H "Authorization: token $TOKEN" )
else
  echo "Warning: GITHUB_TOKEN not set; some operations may be rate-limited or blocked." >&2
fi

API_BASE="https://api.github.com/repos/$OWNER_REPO"

if [ -z "$RUN_ID" ]; then
  echo "Finding latest run for workflow '$WORKFLOW_NAME' on branch '$BRANCH'..."
  runs_json=$(curl -sS "${API_BASE}/actions/runs?branch=${BRANCH}&per_page=20" "${auth_header[@]}")
  RUN_ID=$(echo "$runs_json" | jq -r --arg name "$WORKFLOW_NAME" '.workflow_runs[] | select(.name == $name) | .id' | head -n1)
  if [ -z "$RUN_ID" ] || [ "$RUN_ID" == "null" ]; then
    echo "No recent workflow runs found for $WORKFLOW_NAME on branch $BRANCH" >&2
    exit 3
  fi
  echo "Using run id: $RUN_ID"
fi

mkdir -p "$OUT_DIR"
TMP_ZIP=$(mktemp --tmpdir logs_${RUN_ID}.XXXXXX.zip)

# Get the redirect location, then download the ZIP
REDIR_URL=$(curl -sS -D - "${auth_header[@]}" -o /dev/null -w '%{redirect_url}' "${API_BASE}/actions/runs/${RUN_ID}/logs")
if [ -z "$REDIR_URL" ] || [ "$REDIR_URL" == "null" ]; then
  echo "Failed to get download URL for logs. Are you authorized?" >&2
  exit 4
fi

echo "Downloading logs from: $REDIR_URL"
curl -sSL "$REDIR_URL" -o "$TMP_ZIP"

echo "Extracting logs to: $OUT_DIR"
unzip -o "$TMP_ZIP" -d "$OUT_DIR"
rm -f "$TMP_ZIP"

echo "Logs extracted to: $OUT_DIR"
exit 0
