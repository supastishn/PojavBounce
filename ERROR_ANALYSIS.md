# ExecuTorch Model Conversion - Error Analysis Report

## Date: 2026-01-18
## Workflow Run ID: 21110683897
## Job ID: 60708762473
## Status: FAILED

---

## Primary Error

### CMake Directory Naming Restriction

**Error Message:**
```
CMake Error at CMakeLists.txt:298 (message):
  The ExecuTorch repo must be cloned into a directory named exactly
  `executorch`; found `pip-req-build-1l_41c3o`.  See
  https://github.com/pytorch/executorch/issues/6475 for progress on a fix for
  this restriction.
```

**Location:** Log lines 566-570

**Root Cause:**
ExecuTorch's CMake build system has a hard-coded validation that requires the repository to be in a directory named exactly `executorch`. When pip tries to install ExecuTorch from source with `git+https://github.com/pytorch/executorch.git`, it creates a temporary build directory with a random name (`pip-req-build-1l_41c3o`), which violates this requirement.

---

## Secondary Error

**Error Message:**
```
subprocess.CalledProcessError: Command '[... cmake ...]' returned non-zero exit status 1.
```

**Root Cause:** Cascading failure from CMake error above

---

## Affected Build Steps

1. ✓ Set up job - SUCCESS
2. ✓ Checkout repository - SUCCESS
3. ✓ Set up Python - SUCCESS
4. ✗ **Install dependencies - FAILURE** (at step 4)
   - Failed at: `pip install git+https://github.com/pytorch/executorch.git`
   - CMake validation prevented successful wheel building

---

## Full Error Stack Trace

```
Building wheel for executorch (pyproject.toml): finished with status 'error'
error: subprocess-exited-with-error
...
CMake Error at CMakeLists.txt:298 (message):
  The ExecuTorch repo must be cloned into a directory named exactly
  `executorch`; found `pip-req-build-1l_41c3o`.
...
subprocess.CalledProcessError: Command '['cmake',
  '-DPYTHON_EXECUTABLE=/opt/hostedtoolcache/Python/3.11.14/x64/bin/python',
  '-DCMAKE_PREFIX_PATH=/opt/hostedtoolcache/Python/3.11.14/x64/lib/python3.11/site-packages',
  '-DCMAKE_BUILD_TYPE=Release',
  '--preset', 'pybind',
  '-B', '/tmp/pip-req-build-1l_41c3o/pip-out/temp.linux-x86_64-cpython-311/cmake-out']'
  returned non-zero exit status 1.
```

---

## Recommended Fixes

### Option 1: Pre-clone ExecuTorch (RECOMMENDED)
Instead of installing from source via pip, clone the repository to a correctly-named directory and install from there:

```bash
git clone --depth 1 https://github.com/pytorch/executorch.git /tmp/executorch
cd /tmp/executorch
pip install -e .
```

### Option 2: Use Pre-built Wheels (BEST if available)
Check if ExecuTorch provides pre-built wheels on PyPI or GitHub Releases:
```bash
pip install executorch  # If wheels are available
```

### Option 3: Build in Correctly-Named Directory
Modify the pip cache or build settings to ensure the build directory is named `executorch`.

---

## GitHub Workflow File

**Location:** `.github/workflows/convert-models.yml`

**Current Problematic Line (Line 30):**
```yaml
pip install git+https://github.com/pytorch/executorch.git
```

---

## Implementation Files Needed

- Workflow file: `.github/workflows/convert-models.yml` (needs updating)
- Python script: `src/main/resources/scripts/executorch/convert_with_mxnet.py` (check if it exists)

---

## Next Steps

1. Modify the workflow to pre-clone ExecuTorch to the correct directory
2. Test the modified workflow manually
3. Ensure proper cleanup of temporary directories
4. Verify the model conversion succeeds

---

## References

- ExecuTorch CMakeLists.txt directory check: https://github.com/pytorch/executorch/issues/6475
- GitHub Actions Runner: ubuntu-latest (GitHub-hosted runner)
- Python Version: 3.11.14
- PyTorch: CPU-only variant from official index
