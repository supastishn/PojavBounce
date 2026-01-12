#!/usr/bin/env bash
set -euo pipefail

# Find open PRs that look like they add generated ONNX models (created by the CI)
# Usage: ./scripts/find_onnx_pr.sh -r owner/repo

OWNER_REPO=""
TOKEN="${GITHUB_TOKEN:-}"

while getopts ":r:t:h" opt; do
  case $opt in
    r) OWNER_REPO="$OPTARG" ;;
    t) TOKEN="$OPTARG" ;;
    h) echo "Usage: $0 -r owner/repo"; exit 0 ;;
    \?) echo "Invalid option: -$OPTARG" >&2; exit 2 ;;
  esac
done

if [ -z "$OWNER_REPO" ]; then
  echo "ERROR: owner/repo must be specified with -r" >&2
  exit 2
fi

auth_header=()
if [ -n "$TOKEN" ]; then
  auth_header=( -H "Authorization: token $TOKEN" )
fi

API_BASE="https://api.github.com/repos/$OWNER_REPO"

curl -sS "${API_BASE}/pulls?state=open&per_page=100" "${auth_header[@]}" | jq -r '.[] | select(.title | test("add generated ONNX models")) | "#\(.number) \(.title) \(.html_url)"'

exit 0
