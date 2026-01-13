#!/bin/bash
#
# Quick build status checker - simplified version
# Usage: ./quick-build-status.sh [run_id]
#

REPO="supastishn/PojavBounce"
TOKEN="${GITHUB_TOKEN:-}"

if [[ -z "$TOKEN" ]]; then
    if [[ -f ~/.github_token ]]; then
        TOKEN=$(cat ~/.github_token)
    else
        echo "Error: GITHUB_TOKEN not set. Set it or create ~/.github_token"
        exit 1
    fi
fi

RUN_ID="${1:-}"

if [[ -z "$RUN_ID" ]]; then
    echo "Fetching latest build run..."
    API_RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
        "https://api.github.com/repos/$REPO/actions/workflows/build.yml/runs?branch=nextgen&per_page=1")
    RUN_ID=$(echo "$API_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['workflow_runs'][0]['id'])" 2>/dev/null)
fi

if [[ -z "$RUN_ID" ]]; then
    echo "Error: Could not find run ID"
    exit 1
fi

echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "Build Status for Run: $RUN_ID"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

RESPONSE=$(curl -s -H "Authorization: token $TOKEN" \
    "https://api.github.com/repos/$REPO/actions/runs/$RUN_ID")

python3 << 'PYTHON'
import sys, json, subprocess
from datetime import datetime

data = json.loads(sys.stdin.read())

run_id = data.get('id')
status = data.get('status')
conclusion = data.get('conclusion', 'pending')
commit_sha = data.get('head_commit', {}).get('id', 'N/A')[:8]
commit_msg = data.get('head_commit', {}).get('message', 'N/A').split('\n')[0]
created = data.get('created_at', '')
updated = data.get('updated_at', '')

# Parse timestamps
if created:
    created_dt = datetime.fromisoformat(created.replace('Z', '+00:00'))
    created = created_dt.strftime('%Y-%m-%d %H:%M:%S UTC')

if updated:
    updated_dt = datetime.fromisoformat(updated.replace('Z', '+00:00'))
    updated = updated_dt.strftime('%Y-%m-%d %H:%M:%S UTC')

print(f"Run ID:       {run_id}")
print(f"Status:       {status.upper()}")
print(f"Conclusion:   {conclusion.upper() if conclusion else 'N/A'}")
print(f"Commit:       {commit_sha} - {commit_msg}")
print(f"Created:      {created}")
print(f"Updated:      {updated}")
print()
print(f"Full URL:     https://github.com/{data.get('repository', {}).get('full_name', 'unknown')}/actions/runs/{run_id}")
print()

# Color-coded status
if status == "completed":
    if conclusion == "success":
        print("✅ BUILD SUCCESSFUL!")
        sys.exit(0)
    else:
        print(f"❌ BUILD FAILED ({conclusion})")
        sys.exit(1)
elif status == "in_progress":
    print("⏳ BUILD IN PROGRESS...")
    sys.exit(2)
else:
    print(f"⚠️  BUILD STATUS: {status}")
    sys.exit(3)
PYTHON
