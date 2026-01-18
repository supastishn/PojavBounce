#!/bin/bash

# Script to fetch the latest ExecuTorch model conversion logs
# This script filters for the "Convert Models to ExecuTorch" workflow
# Usage: GITHUB_TOKEN=your_token ./scripts/get_latest_executorch_log.sh
# Or: ./scripts/get_latest_executorch_log.sh your_token

set -e

# Get token from argument or environment variable
TOKEN="${1:-$GITHUB_TOKEN}"

if [ -z "$TOKEN" ]; then
    echo "Error: GitHub token not provided"
    echo "Usage: GITHUB_TOKEN=your_token $0"
    echo "   or: $0 your_token"
    exit 1
fi

# Repository details
REPO_OWNER="supastishn"
REPO_NAME="PojavBounce"
API_BASE="https://api.github.com"
WORKFLOW_NAME="Convert Models to ExecuTorch"

echo "Fetching latest '$WORKFLOW_NAME' workflow runs for $REPO_OWNER/$REPO_NAME..."

# Get workflow runs and filter for the specific workflow
WORKFLOW_RUNS=$(curl -s -H "Authorization: token $TOKEN" \
    "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/actions/runs?per_page=10" | \
    jq -r ".workflow_runs[] | select(.name == \"$WORKFLOW_NAME\")")

if [ -z "$WORKFLOW_RUNS" ]; then
    echo "Error: No runs found for workflow '$WORKFLOW_NAME'"
    echo "Available workflows:"
    curl -s -H "Authorization: token $TOKEN" \
        "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/actions/runs?per_page=10" | \
        jq -r '.workflow_runs[].name' | sort | uniq
    exit 1
fi

# Get the latest one (first in results)
WORKFLOW_RUN=$(echo "$WORKFLOW_RUNS" | head -1)
RUN_ID=$(echo "$WORKFLOW_RUN" | jq -r '.id')
RUN_STATUS=$(echo "$WORKFLOW_RUN" | jq -r '.status')
RUN_CONCLUSION=$(echo "$WORKFLOW_RUN" | jq -r '.conclusion')
RUN_NAME=$(echo "$WORKFLOW_RUN" | jq -r '.name')
RUN_URL=$(echo "$WORKFLOW_RUN" | jq -r '.html_url')
CREATED_AT=$(echo "$WORKFLOW_RUN" | jq -r '.created_at')

echo ""
echo "Latest ExecuTorch Workflow Run:"
echo "  ID: $RUN_ID"
echo "  Name: $RUN_NAME"
echo "  Status: $RUN_STATUS"
echo "  Conclusion: $RUN_CONCLUSION"
echo "  Created: $CREATED_AT"
echo "  URL: $RUN_URL"
echo ""

# Get jobs for this run
echo "Fetching jobs for run $RUN_ID..."
JOBS=$(curl -s -H "Authorization: token $TOKEN" \
    "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/actions/runs/$RUN_ID/jobs")

JOB_COUNT=$(echo "$JOBS" | jq -r '.jobs | length')
echo "Found $JOB_COUNT job(s)"
echo ""

# Process each job
for i in $(seq 0 $((JOB_COUNT - 1))); do
    JOB=$(echo "$JOBS" | jq -r ".jobs[$i]")
    JOB_ID=$(echo "$JOB" | jq -r '.id')
    JOB_NAME=$(echo "$JOB" | jq -r '.name')
    JOB_STATUS=$(echo "$JOB" | jq -r '.status')
    JOB_CONCLUSION=$(echo "$JOB" | jq -r '.conclusion')

    echo "Job $((i + 1))/$JOB_COUNT: $JOB_NAME"
    echo "  ID: $JOB_ID"
    echo "  Status: $JOB_STATUS"
    echo "  Conclusion: $JOB_CONCLUSION"

    # Download logs
    LOG_FILE="logs/executorch_${JOB_ID}_${JOB_NAME//[ \/]/_}.log"
    mkdir -p logs

    echo "  Downloading logs to $LOG_FILE..."
    curl -s -L -H "Authorization: token $TOKEN" \
        "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/actions/jobs/$JOB_ID/logs" \
        -o "$LOG_FILE"

    if [ -f "$LOG_FILE" ]; then
        LOG_SIZE=$(wc -c < "$LOG_FILE")
        echo "  Downloaded $LOG_SIZE bytes"

        # Show full logs for failed jobs, summary for successful ones
        if echo "$JOB_CONCLUSION" | grep -qi "failure"; then
            echo "  === FAILED JOB - Full Log ==="
            cat "$LOG_FILE"
            echo "  ==================================="
        else
            echo "  === SUCCESSFUL JOB - Last 30 lines ==="
            tail -30 "$LOG_FILE"
            echo "  ==================================="
        fi
    else
        echo "  Failed to download logs"
    fi
    echo ""
done

echo "ExecuTorch logs downloaded to ./logs/ directory with 'executorch_' prefix"
echo "To view a specific log:"
echo "  cat logs/executorch_*.log"
echo "To search for errors:"
echo "  grep -i error logs/executorch_*.log"
