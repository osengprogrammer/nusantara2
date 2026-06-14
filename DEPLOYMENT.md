# 🚀 Deployment Guide — Tag-Based CI Pipeline (v3.0)

## 🎯 Flow
1. **Local Bump**: `./deploy.sh --bump` (or `./deploy.sh --bump 3.8.0`)
   - Bumps version in `app/build.gradle.kts`.
   - Creates a git tag (e.g., `v3.8.0`).
   - Pushes tag to GitHub.
2. **CI Automation**: GitHub Actions (`deploy.yml`) triggers on tag `v*`.
   - Builds Signed APK.
   - Creates GitHub Release with the tag.
   - Calculates SHA256.
   - Updates `docs/version.json` on `gh-pages` branch.
3. **App Update**: App detects new `versionCode` via `gh-pages` and prompts user.

## 🛡️ Manual Mode (Local Build)
If CI is down or you need a local build for testing:
`./deploy.sh "Release Notes"`
- Builds locally with `--no-daemon`.
- Uploads to GitHub Release manually (if `gh` CLI available).
- Updates `docs/version.json` on `main`.

## 🔄 Helper Scripts
- `update_version_json.sh`: If CI fails to update the JSON, run `./update_version_json.sh v3.8.0` locally to sync.

## 🛠️ Requirements
- `gh` (GitHub CLI) for local uploads and helper script.
- Correct Firebase/GitHub Secrets configured in Actions.
