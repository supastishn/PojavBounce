#!/usr/bin/env bash
set -euo pipefail

# Extract and show a job log from a workflow run logs archive (by run id and job name)
# Usage:
#   ./scripts/show_job_log.sh -r owner/repo -i run_id -j job_name
# Example:
#   ./scripts/show_job_log.sh -r supastishn/PojavBounce -i 20924604339 -j build

OWNER_REPO=""
RUN_ID=""
JOB_NAME="build"
TOKEN="${GITHUB_TOKEN:-}"

print_usage() {
  echo "Usage: $0 -r owner/repo -i run_id -j job_name"
}

while getopts ":r:i:j:t:h" opt; do
  case $opt in
    r) OWNER_REPO="$OPTARG" ;;
    i) RUN_ID="$OPTARG" ;;
    j) JOB_NAME="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    h) print_usage; exit 0 ;;
    \?) echo "Invalid option: -$OPTARG" >&2; print_usage; exit 2 ;;
  esac
done

if [ -z "$OWNER_REPO" ] || [ -z "$RUN_ID" ] || [ -z "$JOB_NAME" ]; then
  echo "Missing required arguments" >&2
  print_usage
  exit 2
fi

TMP_DIR=$(mktemp -d)
./scripts/fetch_run_logs.sh -r "$OWNER_REPO" -i "$RUN_ID" -o "$TMP_DIR" -t "$TOKEN"

# The zip extracts files like: build/6_Build.txt or extract-versions/3_Extract versions.txt
# We'll find the most likely file by matching the job name prefix
LOG_FILE=$(find "$TMP_DIR" -type f -name "${JOB_NAME}/*" -print | head -n1 || true)

if [ -z "$LOG_FILE" ]; then
  # try to approximate with pattern
  LOG_FILE=$(find "$TMP_DIR" -type f -iname "*${JOB_NAME}*.txt" | head -n1 || true)
fi

if [ -z "$LOG_FILE" ]; then
  echo "Log file for job '$JOB_NAME' not found in extracted logs: $TMP_DIR" >&2
  exit 3
fi

echo "=== Showing tail of log file: $LOG_FILE ==="
tail -n 200 "$LOG_FILE"

exit 0
