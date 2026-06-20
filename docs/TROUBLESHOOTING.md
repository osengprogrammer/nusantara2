# 🐛 Troubleshooting Quick Reference
| Symptom | Fix |
|---------|-----|
| Kotlin daemon crash | `./gradlew --stop && pkill -f kotlin-compiler && rm -rf .gradle/configuration-cache` |
| VersionCode tidak berubah | `rm -rf app/build/ && ./gradlew :app:assembleRelease --no-daemon --refresh-dependencies` |
| `gh release`: unknown flag `--clobber` | `--clobber` hanya untuk `upload`, bukan `create` |
| Dialog tidak muncul | RC cache 1 jam. `adb shell pm clear ...` atau tunggu |
| Install ditolak | Pastikan APK `versionCode` > version di HP |
