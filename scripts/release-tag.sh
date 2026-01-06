#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/release-tag.sh 0.11.0
#
# What it does:
#   - Updates site/.env VITE_LATEST_STABLE_RELEASE_VERSION="x.y.z"
#   - Updates README.md TOML snippet openapi2kotlin = "x.y.z"
#   - Commits changes
#   - Tags vX.Y.Z
#   - Pushes commit + tag

VERSION="${1:-}"

if [[ -z "${VERSION}" ]]; then
  echo "ERROR: Missing version argument."
  echo "Usage: $0 0.11.0"
  exit 1
fi

# Basic semver-ish check (allows 0.11.0, 1.2.3, 1.2.3-rc.1)
if ! [[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.]+)?$ ]]; then
  echo "ERROR: Version '${VERSION}' does not look like a semantic version (e.g. 0.11.0)."
  exit 1
fi

TAG="v${VERSION}"

# Ensure we're in a git repo root-ish (optional but helpful)
if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  echo "ERROR: Not inside a git repository."
  exit 1
fi

# Prevent accidental release from a dirty tree (except changes we are about to make)
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ERROR: Working tree has uncommitted changes. Commit/stash them first."
  exit 1
fi

# Ensure tag doesn't already exist (local or remote)
if git rev-parse -q --verify "refs/tags/${TAG}" >/dev/null; then
  echo "ERROR: Tag '${TAG}' already exists locally."
  exit 1
fi
if git ls-remote --tags origin "${TAG}" | grep -q "${TAG}"; then
  echo "ERROR: Tag '${TAG}' already exists on origin."
  exit 1
fi

SITE_ENV="./site/.env"
README="./README.md"

if [[ ! -f "${SITE_ENV}" ]]; then
  echo "ERROR: ${SITE_ENV} not found."
  exit 1
fi

if [[ ! -f "${README}" ]]; then
  echo "ERROR: ${README} not found."
  exit 1
fi

echo "Releasing ${TAG}"
echo "Updating ${SITE_ENV} and ${README}"

# --- Update site/.env ---
# Replace the entire line, preserving quotes style you already use
python3 - <<'PY'
import re, sys, pathlib, os

version = os.environ["VERSION"]
path = pathlib.Path(os.environ["SITE_ENV"])
text = path.read_text(encoding="utf-8")

pattern = r'^(VITE_LATEST_STABLE_RELEASE_VERSION=)"[^"\n]*"\s*$'
replacement = r'\1"' + version + '"'

new_text, n = re.subn(pattern, replacement, text, flags=re.MULTILINE)
if n == 0:
    raise SystemExit(f"ERROR: Did not find VITE_LATEST_STABLE_RELEASE_VERSION in {path}")
path.write_text(new_text, encoding="utf-8")
PY
# Pass env vars to the python snippet
VERSION="${VERSION}" SITE_ENV="${SITE_ENV}" python3 -c 'import os;'

# --- Update README.md ---
# Only update within the ```toml fenced block under "libs.versions.toml"
python3 - <<'PY'
import re, os, pathlib

version = os.environ["VERSION"]
path = pathlib.Path(os.environ["README"])
text = path.read_text(encoding="utf-8")

# Find TOML code fence blocks and update only the one containing '[versions]'
def update_block(block: str) -> str:
    # Replace openapi2kotlin = "x" ONLY if it appears (so we don't touch other content)
    return re.sub(
        r'^(openapi2kotlin\s*=\s*)"[^\n"]*"\s*$',
        r'\1"' + version + '"',
        block,
        flags=re.MULTILINE
    )

def repl(match):
    block = match.group(1)
    # Only modify the toml block that looks like the versions snippet
    if "[versions]" in block and "openapi2kotlin" in block:
        return "```toml\n" + update_block(block) + "\n```"
    return match.group(0)

new_text, n = re.subn(r"```toml\n(.*?)\n```", repl, text, flags=re.DOTALL)

if n == 0:
    raise SystemExit("ERROR: No ```toml fenced block found in README.md (or not matching expected format).")

if new_text == text:
    raise SystemExit("ERROR: README.md TOML block was found but no openapi2kotlin version line was updated.")

path.write_text(new_text, encoding="utf-8")
PY
VERSION="${VERSION}" README="${README}" python3 -c 'import os;'

# Show changes
echo
git diff -- "${SITE_ENV}" "${README}" || true
echo

# Stage + commit
git add "${SITE_ENV}" "${README}"
git commit -m "chore(release): ${TAG}"

# Tag (annotated)
git tag -a "${TAG}" -m "${TAG}"

# Push commit + tag
git push origin HEAD
git push origin "${TAG}"

echo "Done: pushed commit and tag ${TAG}"
