#!/usr/bin/env bash
set -euo pipefail

# Usage:
#   ./scripts/release-tag.sh 0.11.0
#
# What this script does:
#   - Updates site/.env:
#       VITE_LATEST_STABLE_RELEASE_VERSION="x.y.z"
#   - Updates README.md:
#       openapi2kotlin = "x.y.z" (inside the TOML snippet under libs.versions.toml)
#   - Commits documentation changes
#   - Creates annotated git tag vX.Y.Z
#   - Pushes commit and tag to origin

VERSION="${1:-}"

if [[ -z "${VERSION}" ]]; then
  echo "ERROR: Missing version argument."
  echo "Usage: $0 0.11.0"
  exit 1
fi

# Basic semver validation (allows prerelease suffixes)
if ! [[ "${VERSION}" =~ ^[0-9]+\.[0-9]+\.[0-9]+([-.][0-9A-Za-z.]+)?$ ]]; then
  echo "ERROR: Version '${VERSION}' does not look like a semantic version."
  exit 1
fi

TAG="v${VERSION}"
SITE_ENV="./site/.env"
README="./README.md"

# Ensure required files exist
[[ -f "${SITE_ENV}" ]] || { echo "ERROR: ${SITE_ENV} not found."; exit 1; }
[[ -f "${README}"   ]] || { echo "ERROR: ${README} not found."; exit 1; }

# Ensure clean working tree before release
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "ERROR: Working tree has uncommitted changes."
  echo "Please commit or stash them before releasing."
  exit 1
fi

# Prevent overwriting existing tags
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

# -----------------------------------------------------------------------------
# Update site/.env
# -----------------------------------------------------------------------------
# Replace:
#   VITE_LATEST_STABLE_RELEASE_VERSION="0.10.0"
# with:
#   VITE_LATEST_STABLE_RELEASE_VERSION="0.11.0"

RELEASE_VERSION="${VERSION}" perl -i -pe 'BEGIN{$v=$ENV{RELEASE_VERSION}} s/^VITE_LATEST_STABLE_RELEASE_VERSION="[^"]*"\s*$/VITE_LATEST_STABLE_RELEASE_VERSION="$v"/' "${SITE_ENV}"

# Verify replacement
if ! grep -q "^VITE_LATEST_STABLE_RELEASE_VERSION=\"${VERSION}\"$" "${SITE_ENV}"; then
  echo "ERROR: Failed to update ${SITE_ENV}."
  echo "Expected: VITE_LATEST_STABLE_RELEASE_VERSION=\"${VERSION}\""
  exit 1
fi

# -----------------------------------------------------------------------------
# Update README.md
# -----------------------------------------------------------------------------
# Only update the TOML snippet inside a ```toml fenced block that contains [versions].
# This avoids touching other sections and avoids fragile multiline regex parsing.

RELEASE_VERSION="${VERSION}" perl -i -pe '
  BEGIN { $v=$ENV{RELEASE_VERSION}; $in_toml=0; $seen_versions=0; }
  if (/^```toml\s*$/) { $in_toml=1; $seen_versions=0; }
  elsif ($in_toml && /^```\s*$/) { $in_toml=0; $seen_versions=0; }
  if ($in_toml && /\[versions\]/) { $seen_versions=1; }
  if ($in_toml && $seen_versions && /^openapi2kotlin\s*=\s*"/) {
    s/^openapi2kotlin\s*=\s*"[^"]*"/openapi2kotlin = "$v"/;
  }
' "${README}"

# Verify README update
if ! grep -q "openapi2kotlin = \"${VERSION}\"" "${README}"; then
  echo "ERROR: Failed to update README.md."
  exit 1
fi

# -----------------------------------------------------------------------------
# Commit, tag, and push
# -----------------------------------------------------------------------------

echo
echo "Changes:"
git diff -- "${SITE_ENV}" "${README}" || true
echo

git add "${SITE_ENV}" "${README}"
git commit -m "chore(release): ${TAG}"

git tag -a "${TAG}" -m "${TAG}"

git push origin HEAD
git push origin "${TAG}"

echo "Release ${TAG} completed successfully."
