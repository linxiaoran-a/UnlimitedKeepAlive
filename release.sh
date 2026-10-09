#!/usr/bin/env bash
# One-click release script for UnlimitedKeepAlive KSU module.
# Usage: ./release.sh <major|minor|patch> [changelog]
#   major: 大版本（架构变更、不兼容更新）
#   minor: 功能新增（向下兼容）
#   patch: bug 修复（小改动）
#   changelog: optional, defaults to git log shortlog
set -euo pipefail
cd "$(dirname "$0")"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <major|minor|patch> [changelog]"
    echo "  major: breaking changes (x.0.0)"
    echo "  minor: new features (?.x.0)"
    echo "  patch: bug fixes (?.?.x)"
    echo "  changelog: release notes (optional, defaults to git shortlog)"
    exit 1
fi

TYPE="$1"
CHANGELOG="${2:-}"

# Read current version from module.prop
CURRENT=$(grep '^version=' module.prop | cut -d'=' -f2 | sed 's/^v//')
if ! [[ "$CURRENT" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
    echo "Error: current version in module.prop is not semantic: $CURRENT"
    exit 1
fi

MAJOR="${BASH_REMATCH[1]}"
MINOR="${BASH_REMATCH[2]}"
PATCH="${BASH_REMATCH[3]}"

# Auto-increment based on type
case "$TYPE" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    *)
        echo "Error: type must be major, minor, or patch"
        exit 1
        ;;
esac

VERSION="$MAJOR.$MINOR.$PATCH"
VERSION_CODE=$((MAJOR * 10000 + MINOR * 100 + PATCH))

echo "==> Auto-increment: $CURRENT → $VERSION (code: $VERSION_CODE)"

# Update module.prop
sed -i "s/^version=.*/version=v$VERSION/" module.prop
sed -i "s/^versionCode=.*/versionCode=$VERSION_CODE/" module.prop
echo "==> Updated module.prop"

# Update changelog.md if provided
if [ -n "$CHANGELOG" ]; then
    echo -e "\n## v$VERSION\n\n$CHANGELOG\n" >> changelog.md
    echo "==> Updated changelog.md"
fi

# Pack module zip
echo "==> Packing module zip..."
powershell.exe -ExecutionPolicy Bypass -File pack.ps1 || {
    echo "Error: pack.ps1 failed (requires Windows PowerShell)"
    exit 1
}

ZIP_FILE="UnlimitedKeepAlive-KSU-v$VERSION.zip"
if [ ! -f "$ZIP_FILE" ]; then
    echo "Error: zip not found: $ZIP_FILE"
    exit 1
fi
echo "==> Packed: $ZIP_FILE"

# Update update.json
cat > update.json <<EOF
{
  "version": "v$VERSION",
  "versionCode": $VERSION_CODE,
  "zipUrl": "https://github.com/linxiaoran-a/UnlimitedKeepAlive/releases/download/v$VERSION/UnlimitedKeepAlive-KSU-v$VERSION.zip",
  "changelog": "https://raw.githubusercontent.com/linxiaoran-a/UnlimitedKeepAlive/main/changelog.md"
}
EOF
echo "==> Updated update.json"

# Commit and push
git add -A
git commit -m "Release v$VERSION ($TYPE)"
git push
echo "==> Pushed to main"

# Create GitHub release
GH_CLI="/c/Program Files/GitHub CLI/gh.exe"
if [ ! -f "$GH_CLI" ]; then
    GH_CLI="gh"
fi

RELEASE_NOTES="${CHANGELOG:-$(git log --oneline -5 | sed 's/^/- /')}"
"$GH_CLI" release create "v$VERSION" "$ZIP_FILE" \
    --title "v$VERSION" \
    --notes "$RELEASE_NOTES"
echo "==> GitHub release created: https://github.com/linxiaoran-a/UnlimitedKeepAlive/releases/tag/v$VERSION"

echo ""
echo "✅ Release v$VERSION complete!"
echo "   Type: $TYPE ($CURRENT → $VERSION)"
echo "   Module zip: $ZIP_FILE"
echo "   Update URL: https://raw.githubusercontent.com/linxiaoran-a/UnlimitedKeepAlive/main/update.json"
