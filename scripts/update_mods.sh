#!/usr/bin/env bash
set -euo pipefail

REPO="supastishn/PojavBounce"
DEST="/data/data/com.termux/files/home/storage/shared/FCL/.minecraft/versions/1.21.11-Fabric/mods"
TMPDIR=$(mktemp -d)
GITHUB_TOKEN="${GITHUB_TOKEN:-}"
if [ -z "$GITHUB_TOKEN" ]; then
  read -s -p "Enter GitHub API token (or leave blank to proceed unauthenticated): " GITHUB_TOKEN
  echo
fi

# Optional auth header if GITHUB_TOKEN is provided
AUTH_HEADER=()
if [ -n "$GITHUB_TOKEN" ]; then
  AUTH_HEADER=( -H "Authorization: token $GITHUB_TOKEN" )
fi

echo "Fetching latest artifact URL from GitHub API..."
API_URL="https://api.github.com/repos/$REPO/actions/artifacts"
# Use python to parse JSON so this script works without jq, but save response for debugging
RESP="$TMPDIR/api_response.json"
HDRS="$TMPDIR/api_headers.txt"
curl -s -S -D "$HDRS" -o "$RESP" "${AUTH_HEADER[@]}" "$API_URL"
API_STATUS=$(head -n1 "$HDRS" | awk '{print $2}' || true)
echo "GitHub API status: ${API_STATUS:-unknown}"
echo "Response saved to $RESP (size: $(wc -c < "$RESP") bytes)"
echo "Response headers:"
sed -n '1,20p' "$HDRS" || true

# Parse artifact URL - filter for Build workflow artifacts (named "liquidbounce-*")
URL=$(python3 -c '
import sys, json
j = json.load(open(sys.argv[1]))
artifacts = j.get("artifacts", [])
# Filter for Build workflow artifacts (liquidbounce-*), not Convert Models artifacts
build_artifacts = [a for a in artifacts if a.get("name", "").startswith("liquidbounce")]
if build_artifacts:
    print(build_artifacts[0].get("archive_download_url", ""))
else:
    print("")
' "$RESP")

if [ -z "$URL" ]; then
  echo "No artifacts found for $REPO or failed to parse URL" >&2
  echo "API response (first 500 chars):"
  head -c 500 "$RESP" | sed -n '1,200p'
  rm -rf "$TMPDIR"
  exit 1
fi

echo "Downloading artifact from: $URL"
DL_HDRS="$TMPDIR/download_headers.txt"
DL_OUT="$TMPDIR/artifact.zip"
# Do not set Accept: application/zip on this request; the API endpoint rejects that header with 415.
# Let curl follow redirects and accept the server content-type for the archive.
curl -L -s -S -D "$DL_HDRS" "${AUTH_HEADER[@]}" -o "$DL_OUT" "$URL"
echo "Download headers:"
sed -n '1,200p' "$DL_HDRS" || true
echo "Downloaded file size: $(wc -c < "$DL_OUT") bytes"
if command -v file >/dev/null 2>&1; then
  echo "File type: $(file -b "$DL_OUT")"
fi

mkdir -p "$TMPDIR/unpack"
if unzip -tqq "$DL_OUT" >/dev/null 2>&1; then
  echo "Artifact looks like a valid zip, extracting..."
  unzip -oq "$DL_OUT" -d "$TMPDIR/unpack"
else
  echo "Downloaded file is not a valid zip. Showing first 1000 bytes for debug:"
  head -c 1000 "$DL_OUT" | sed -n '1,200p'
  rm -rf "$TMPDIR"
  exit 1
fi

# GitHub Actions wraps the artifact in an outer zip
# The actual mod is inside liquidbounce.zip which contains the .jar files
INNER_ZIP=$(find "$TMPDIR/unpack" -name "liquidbounce*.zip" -type f | head -n1)
if [ -n "$INNER_ZIP" ] && [ -f "$INNER_ZIP" ]; then
  echo "Found inner zip: $INNER_ZIP"
  echo "Extracting inner zip to get the .jar..."
  mkdir -p "$TMPDIR/inner"
  unzip -oq "$INNER_ZIP" -d "$TMPDIR/inner"

  # Find the liquidbounce jar
  JAR_FILE=$(find "$TMPDIR/inner" -name "liquidbounce*.jar" -type f | head -n1)
  if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
    echo "Found jar: $JAR_FILE"
    JAR_NAME=$(basename "$JAR_FILE")

    # Remove any old liquidbounce jars from mods folder
    echo "Removing old liquidbounce jars and zips from $DEST..."
    mkdir -p "$DEST"
    find "$DEST" -maxdepth 1 -name "liquidbounce*" -delete 2>/dev/null || true

    echo "Copying $JAR_NAME to $DEST..."
    cp "$JAR_FILE" "$DEST/"
    echo "Installed: $DEST/$JAR_NAME"
  else
    echo "ERROR: Could not find liquidbounce*.jar inside $INNER_ZIP"
    ls -la "$TMPDIR/inner"
    rm -rf "$TMPDIR"
    exit 1
  fi
else
  # Fallback: maybe the jar is directly in the artifact
  JAR_FILE=$(find "$TMPDIR/unpack" -name "liquidbounce*.jar" -type f | head -n1)
  if [ -n "$JAR_FILE" ] && [ -f "$JAR_FILE" ]; then
    echo "Found jar directly in artifact: $JAR_FILE"
    JAR_NAME=$(basename "$JAR_FILE")

    echo "Removing old liquidbounce jars and zips from $DEST..."
    mkdir -p "$DEST"
    find "$DEST" -maxdepth 1 -name "liquidbounce*" -delete 2>/dev/null || true

    echo "Copying $JAR_NAME to $DEST..."
    cp "$JAR_FILE" "$DEST/"
    echo "Installed: $DEST/$JAR_NAME"
  else
    echo "ERROR: Could not find liquidbounce*.jar or liquidbounce*.zip in artifact"
    echo "Contents of artifact:"
    find "$TMPDIR/unpack" -type f
    rm -rf "$TMPDIR"
    exit 1
  fi
fi

echo "Cleaning up..."
rm -rf "$TMPDIR"

echo "Done."
