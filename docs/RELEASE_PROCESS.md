# Release Process

LocationMax uses three related release surfaces. Keep them separate so update checks, force updates, and rollback downloads stay predictable.

## 1. GitHub Release

Use GitHub Releases for the public latest APK.

Tag format:

```text
16-0.0.16
```

Release title format:

```text
LocationMax 0.0.16
```

Release asset:

```text
LocationMax-release.apk
```

Do not upload both `LocationMax-release.apk` and `LocationMax-release-X.Y.Z.apk` as Release assets. Keeping only one asset avoids confusing the GitHub page and preserves the old app update URL:

```text
https://github.com/gegewu26-source/com.huaMax/releases/latest/download/LocationMax-release.apk
```

## 2. Repository Rollback APKs

Keep versioned rollback APKs in:

```text
releases/
```

Rules:

- `releases/LocationMax-release.apk` is the moving latest copy.
- `releases/LocationMax-release-X.Y.Z.apk` is permanent rollback history.
- Add new versions to the top of both `README.md` and `releases/README.md`.
- Do not delete old versioned APK files unless you deliberately want to remove rollback access.

## 3. Server Force Update

The phone app reads:

```text
http://8.134.217.44/locationmax/control.json
```

Force-update fields should match the newest release:

```json
{
  "enabled": true,
  "minVersionCode": 16,
  "latestVersionName": "0.0.16",
  "updateUrl": "http://8.134.217.44/locationmax/LocationMax-release.apk",
  "message": "当前版本需要更新，请下载最新版后继续使用。"
}
```

The desktop control files under `C:\Users\lihua\OneDrive\桌面\定位` generate and upload this JSON. After each new release, update:

```text
locationmax-server-control.ps1
locationmax-control-force-update更新.json
locationmax-control-enabled可用.json
locationmax-control-disabled停用.json
GitHub-Release填写内容.txt
LocationMax-release.apk
LocationMax-release-X.Y.Z.apk
```

The upload step requires the server SSH password. If the public server still reports an old version, the local files may be correct but the server was not updated.

## 4. Suggested Release Checklist

1. Update `app/build.gradle.kts` fallback version.
2. Build release APK.
3. Copy APK to desktop and repository `releases/`.
4. Update `CHANGELOG.md`, `README.md`, `releases/README.md`, and server control templates.
5. Commit and push source changes.
6. Create or update GitHub Release with exactly one `LocationMax-release.apk` asset.
7. Upload server force-update control files.
8. Verify:

```text
https://github.com/gegewu26-source/com.huaMax/releases/latest
https://github.com/gegewu26-source/com.huaMax/releases/latest/download/LocationMax-release.apk
http://8.134.217.44/locationmax/control.json
```
