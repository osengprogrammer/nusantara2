# 🚀 Deployment Guide — Autonomous Update Pipeline
## 🎯 Flow
1. `./deploy.sh 3.7.10 3710 "Notes"` → clean → build → verify → upload GitHub
2. Firebase Console → Update 4 params (`latest_version_code`, `name`, `download_url`, `sha256`) → Publish
3. App → Dialog → Download → SHA256 verify → Install → Restart

## 🛡️ Safety Features
- VERSION GUARD: `aapt` verify (Gradle version == APK internal) before upload
- Auto-clean cache + `--no-daemon` → stable build
- Manual RC publish → visual review + instant rollback
- Fallback: `docs/version.json` auto-pushed to GitHub Pages

## 🔄 Rollback
Firebase Console → Rollback atau turunkan `latest_version_code` → Publish. User tidak dapat dialog update.
