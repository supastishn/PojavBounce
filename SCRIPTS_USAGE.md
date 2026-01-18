# Log Fetching Scripts - Quick Reference

## Overview
The log fetching functionality has been split into two separate scripts for better organization:

### 1. **get_latest_build_log.sh**
Fetches logs from the latest GitHub Actions workflow run (all workflows).

**Usage:**
```bash
./scripts/get_latest_build_log.sh YOUR_GITHUB_TOKEN
```

Or with environment variable:
```bash
export GITHUB_TOKEN=YOUR_GITHUB_TOKEN
./scripts/get_latest_build_log.sh
```

**Features:**
- Fetches the latest workflow run regardless of name
- Downloads logs for all jobs in that run
- Shows last 50 lines of failed jobs
- Saves logs to `logs/` directory with job ID prefix

---

### 2. **get_latest_executorch_log.sh** (NEW)
Specifically fetches logs from the "Convert Models to ExecuTorch" workflow.

**Usage:**
```bash
./scripts/get_latest_executorch_log.sh YOUR_GITHUB_TOKEN
```

**Features:**
- Filters for only "Convert Models to ExecuTorch" workflow
- Shows full logs for failed jobs
- Shows last 30 lines for successful jobs
- Saves logs to `logs/` directory with `executorch_` prefix
- Lists available workflows if target not found

---

## Output Location
Both scripts save logs to:
```
./logs/
```

### Log File Naming:
- **Build logs:** `logs/job_[JOB_ID]_[JOB_NAME].log`
- **ExecuTorch logs:** `logs/executorch_[JOB_ID]_[JOB_NAME].log`

---

## Error Analysis

### Latest ExecuTorch Run (Run #2)
**Status:** FAILED

**Error:** ExecuTorch CMake directory naming requirement
- CMake requires the repo to be in a directory named exactly `executorch`
- pip's temporary build directory has a random name, causing failure
- **Reference:** https://github.com/pytorch/executorch/issues/6475

**Fix Applied:**
Modified `.github/workflows/convert-models.yml` to clone ExecuTorch to a correctly-named directory before installation.

---

## Key Files Modified

1. **Created:** `scripts/get_latest_build_log.sh` (generic build log fetcher)
2. **Created:** `scripts/get_latest_executorch_log.sh` (ExecuTorch-specific log fetcher)
3. **Modified:** `.github/workflows/convert-models.yml` (fixed dependency installation)
4. **Created:** `ERROR_ANALYSIS.md` (detailed error report)

---

## API Key Stored
Your GitHub Personal Access Token (PAT) should be kept secure. Never commit it to version control.

**Current token permissions needed:**
- `repo:status` - Read workflow status
- `actions:read` - Read GitHub Actions logs

---

## Troubleshooting

### Error: "No workflow runs found"
- Check that the workflow has run at least once
- Verify the token has proper permissions
- Ensure you're using the correct repository path

### Error: jq parse error
- Ensure `jq` is installed: `sudo apt-get install jq`
- Check the GitHub token is valid

### Logs directory not created
- The scripts automatically create `./logs/` if missing
- Ensure you have write permissions in the current directory

---

## Next Steps

1. Re-run the "Convert Models to ExecuTorch" workflow to test the fix
2. Monitor the workflow run using:
   ```bash
   ./scripts/get_latest_executorch_log.sh YOUR_TOKEN
   ```
3. Once successful, the workflow should create a Pull Request with converted `.pte` files

