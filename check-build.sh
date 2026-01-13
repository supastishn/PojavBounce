#!/bin/bash
#
# Monitor PojavBounce GitHub Actions build status and download logs/artifacts
# Usage: ./check-build.sh [run_id]
#

set -euo pipefail

# Configuration
REPO="${REPO:-supastishn/PojavBounce}"
BRANCH="${BRANCH:-nextgen}"
WORKFLOW="${WORKFLOW:-Build}"
PYTHON="${PYTHON:-python3}"
CURL="${CURL:-curl}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Helper functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $*"
}

log_success() {
    echo -e "${GREEN}[✓]${NC} $*"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*"
}

log_warn() {
    echo -e "${YELLOW}[WARN]${NC} $*"
}

# Check dependencies
check_dependencies() {
    if ! command -v "$CURL" &> /dev/null; then
        log_error "curl is required but not installed"
        exit 1
    fi
    if ! command -v "$PYTHON" &> /dev/null; then
        log_error "python3 is required but not installed"
        exit 1
    fi
    if ! command -v jq &> /dev/null; then
        log_warn "jq not found, will use basic JSON parsing"
    fi
}

# Get GitHub token from environment or config
get_github_token() {
    if [[ -n "${GITHUB_TOKEN:-}" ]]; then
        echo "$GITHUB_TOKEN"
    elif [[ -f ~/.github_token ]]; then
        cat ~/.github_token
    else
        log_error "GitHub token not found. Set GITHUB_TOKEN env var or create ~/.github_token"
        exit 1
    fi
}

# Fetch JSON from GitHub API
fetch_github_api() {
    local endpoint="$1"
    local token="$2"

    "$CURL" -s \
        -H "Authorization: token $token" \
        -H "Accept: application/vnd.github.v3+json" \
        "https://api.github.com$endpoint"
}

# Get latest workflow run
get_latest_run() {
    local token="$1"
    local run_id="${2:-}"

    if [[ -n "$run_id" ]]; then
        fetch_github_api "/repos/$REPO/actions/runs/$run_id" "$token"
    else
        fetch_github_api "/repos/$REPO/actions/workflows/build.yml/runs?branch=$BRANCH&status=in_progress" "$token" | \
            "$PYTHON" -c "import sys, json; data=json.load(sys.stdin); print(json.dumps(data['workflow_runs'][0] if data.get('workflow_runs') else {}, indent=2))"
    fi
}

# Parse and display run status
display_run_status() {
    local run_data="$1"

    local run_id=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('id', 'N/A'))")
    local status=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('status', 'N/A'))")
    local conclusion=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('conclusion', 'N/A'))")
    local commit=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('head_commit', {}).get('message', 'N/A').split(chr(10))[0])")
    local created_at=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('created_at', 'N/A'))")
    local updated_at=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('updated_at', 'N/A'))")

    log_info "Workflow Run Details"
    echo "  Run ID:      $run_id"
    echo "  Status:      $status"
    echo "  Conclusion:  $conclusion"
    echo "  Commit:      $commit"
    echo "  Created:     $created_at"
    echo "  Updated:     $updated_at"

    case "$status" in
        "completed")
            if [[ "$conclusion" == "success" ]]; then
                log_success "Build completed successfully!"
            else
                log_error "Build failed with conclusion: $conclusion"
            fi
            ;;
        "in_progress")
            log_warn "Build is still in progress..."
            ;;
        *)
            log_warn "Build status: $status"
            ;;
    esac

    echo ""
    echo "  View on GitHub: https://github.com/$REPO/actions/runs/$run_id"
}

# Get job logs
get_job_logs() {
    local token="$1"
    local run_id="$2"

    log_info "Fetching job logs for run $run_id..."

    local jobs=$(fetch_github_api "/repos/$REPO/actions/runs/$run_id/jobs" "$token")

    echo "$jobs" | "$PYTHON" -c "
import sys, json
data = json.load(sys.stdin)
for job in data.get('jobs', []):
    print(f\"Job: {job['name']} (ID: {job['id']}, Status: {job['status']})\")
    print(f\"  Logs: {job['logs_url']}\")
"
}

# Download build artifacts
download_artifacts() {
    local token="$1"
    local run_id="$2"
    local output_dir="${3:-./artifacts}"

    log_info "Fetching artifacts for run $run_id..."

    mkdir -p "$output_dir"

    local artifacts=$(fetch_github_api "/repos/$REPO/actions/runs/$run_id/artifacts" "$token")

    echo "$artifacts" | "$PYTHON" -c "
import sys, json
data = json.load(sys.stdin)
if not data.get('artifacts'):
    print('No artifacts found')
    sys.exit(0)
for artifact in data.get('artifacts', []):
    print(f\"Found: {artifact['name']} (Size: {artifact['size_in_bytes']} bytes)\")
"
}

# Display help
show_help() {
    cat << EOF
Usage: $(basename "$0") [OPTIONS]

Monitor PojavBounce GitHub Actions builds and download artifacts.

OPTIONS:
    -h, --help              Show this help message
    -r, --run-id ID         Check specific run ID (default: latest)
    -d, --download DIR      Download artifacts to DIR (default: ./artifacts)
    -l, --logs              Fetch job logs
    -R, --repo REPO         GitHub repo (default: $REPO)
    -B, --branch BRANCH     Git branch (default: $BRANCH)

EXAMPLES:
    # Check latest build
    ./check-build.sh

    # Check specific build
    ./check-build.sh -r 435

    # Download artifacts
    ./check-build.sh -d ./my-artifacts

    # Get logs for latest build
    ./check-build.sh -l

REQUIREMENTS:
    - curl
    - python3
    - jq (optional, for better JSON parsing)
    - GITHUB_TOKEN environment variable OR ~/.github_token file

EOF
}

# Main
main() {
    local run_id=""
    local output_dir="./artifacts"
    local get_logs=false

    # Parse arguments
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                show_help
                exit 0
                ;;
            -r|--run-id)
                run_id="$2"
                shift 2
                ;;
            -d|--download)
                output_dir="$2"
                shift 2
                ;;
            -l|--logs)
                get_logs=true
                shift
                ;;
            -R|--repo)
                REPO="$2"
                shift 2
                ;;
            -B|--branch)
                BRANCH="$2"
                shift 2
                ;;
            *)
                log_error "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done

    log_info "PojavBounce Build Monitor"
    log_info "Repository: $REPO"
    log_info "Branch: $BRANCH"
    echo ""

    check_dependencies

    local token
    token=$(get_github_token)

    log_info "Fetching build status..."
    local run_data
    run_data=$(get_latest_run "$token" "$run_id")

    if [[ -z "$run_data" ]] || [[ "$run_data" == "{}" ]]; then
        log_error "No build run found"
        exit 1
    fi

    display_run_status "$run_data"

    local actual_run_id
    actual_run_id=$(echo "$run_data" | "$PYTHON" -c "import sys, json; print(json.load(sys.stdin).get('id', ''))")

    if [[ -n "$actual_run_id" ]]; then
        if [[ "$get_logs" == true ]]; then
            get_job_logs "$token" "$actual_run_id"
        fi

        download_artifacts "$token" "$actual_run_id" "$output_dir"
    fi

    log_success "Done!"
}

main "$@"
