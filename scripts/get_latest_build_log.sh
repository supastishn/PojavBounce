#!/bin/bash

# Script to fetch the latest GitHub Actions build logs
# Usage: GITHUB_TOKEN=your_token ./scripts/get_latest_build_log.sh
# Or: ./scripts/get_latest_build_log.sh your_token

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

echo "Fetching latest workflow runs for $REPO_OWNER/$REPO_NAME..."

# Get the latest workflow run
WORKFLOW_RUN=$(curl -s -H "Authorization: token $TOKEN" \
    "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/actions/runs?per_page=1" | \
    jq -r '.workflow_runs[0]')

if [ "$WORKFLOW_RUN" = "null" ]; then
    echo "Error: No workflow runs found"
    exit 1
fi

RUN_ID=$(echo "$WORKFLOW_RUN" | jq -r '.id')
RUN_STATUS=$(echo "$WORKFLOW_RUN" | jq -r '.status')
RUN_CONCLUSION=$(echo "$WORKFLOW_RUN" | jq -r '.conclusion')
RUN_NAME=$(echo "$WORKFLOW_RUN" | jq -r '.name')
RUN_URL=$(echo "$WORKFLOW_RUN" | jq -r '.html_url')

echo ""
echo "Latest Workflow Run:"
echo "  ID: $RUN_ID"
echo "  Name: $RUN_NAME"
echo "  Status: $RUN_STATUS"
echo "  Conclusion: $RUN_CONCLUSION"
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
    LOG_FILE="logs/job_${JOB_ID}_${JOB_NAME//[ \/]/_}.log"
    mkdir -p logs

    echo "  Downloading logs to $LOG_FILE..."
    curl -s -L -H "Authorization: token $TOKEN" \
        "$API_BASE/repos/$REPO_OWNER/$REPO_NAME/actions/jobs/$JOB_ID/logs" \
        -o "$LOG_FILE"

    if [ -f "$LOG_FILE" ]; then
        LOG_SIZE=$(wc -c < "$LOG_FILE")
        echo "  Downloaded $LOG_SIZE bytes"

        # Show last 50 lines if there are errors
        if echo "$JOB_CONCLUSION" | grep -qi "failure"; then
            echo "  === Last 50 lines of failed job ==="
            tail -50 "$LOG_FILE"
            echo "  ==================================="
        fi
    else
        echo "  Failed to download logs"
    fi
    echo ""
done

echo "All logs downloaded to ./logs/ directory"
echo "To view a specific log:"
echo "  cat logs/job_*.log"
echo "To search for errors:"
echo "  grep -i error logs/*.log"
