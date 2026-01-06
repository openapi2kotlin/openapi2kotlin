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
  echo "ERROR: Missing version argument. Usage: $0 0.11.0"
  exit 1
fi

# Basic semver-ish validation (allows 1.2.3 or 1.2.3-rc.1)
if ! [[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.]+)?$ ]]; then
  echo "ERROR: Version '${VERSION}' does not look like a semver (e.g. 0.11.0)."
  exit 1
fi

TAG="v${VERSION}"
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

# Ensure clean working tree before making changes
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ERROR: Working tree has uncommitted changes. Commit/stash first."
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

echo "Releasing ${TAG}"
echo "Updating ${SITE_ENV} and ${README}"

# --- Update site/.env ---
# Replace the full line:
# VITE_LATEST_STABLE_RELEASE_VERSION="0.10.0"
perl -i -pe 's/^VITE_LATEST_STABLE_RELEASE_VERSION="[^"]*"\s*$/VITE_LATEST_STABLE_RELEASE_VERSION="'"${VERSION}"'
