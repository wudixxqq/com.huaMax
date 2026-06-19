# LocationMax

LocationMax is a customized Android Xposed/LSPosed location simulation module based on [XposedFakeLocation](https://github.com/noobexon1/XposedFakeLocation).

It keeps the original map-based location selection workflow, then adds package rename work, managed authorization, remote control, force-update support, a mock-provider fallback service, and stronger system-level hook coverage for newer Android devices.

## Download

Latest stable APK:

[Download LocationMax-release.apk](https://github.com/gegewu26-source/com.huaMax/releases/latest/download/LocationMax-release.apk)

Rollback APKs are kept in [releases/](releases/).

## Current Version

```text
0.0.17
```

## Package

| Item | Value |
| --- | --- |
| App name | `LocationMax` |
| Application ID | `com.huaMax` |
| Namespace | `com.huaMax` |
| Xposed entry | `com.huaMax.xposed.ModuleEntry` |

## Main Changes From Upstream

- Renamed the project package and module identity to `com.huaMax`.
- Added a managed activation-code flow.
- Added remote enable, disable, and force-update control through `control.json`.
- Added optional external broadcast control for automation.
- Added a foreground mock-location provider fallback for GPS, network, and fused providers.
- Added system-level hook scope handling for `system`, `android`, and `com.android.phone`.
- Added hardened location, Wi-Fi, GNSS, and mock-flag hook paths for target apps.
- Added GitHub release, rollback APK, and server-control publishing workflow.

See [CHANGELOG.md](CHANGELOG.md) for version history and [docs/FORK_CHANGES.md](docs/FORK_CHANGES.md) for the fork-level modification summary.

## Requirements

- Rooted Android device.
- Android 11 / API 30 or newer.
- LSPosed or another compatible Xposed framework.
- Location permission granted to LocationMax.
- LocationMax enabled in LSPosed.
- Target apps selected in LSPosed scope.
- For the strongest system-level behavior, include the `system` scope and reboot after changing scope.

## Basic Setup

1. Install the latest APK.
2. Enable `LocationMax` in LSPosed.
3. Select `system`, `android`, `com.android.phone`, and the target apps that should receive the simulated location.
4. Reboot the device after changing LSPosed scope.
5. Open LocationMax, grant permissions, enter an activation code, choose a point, and start simulation.

## Build

```powershell
.\gradlew.bat :app:assembleDebug :app:assembleRelease
```

Release APK output:

```text
app/build/outputs/apk/release/app-release.apk
```

Local release signing uses `keystore.properties`, which must stay private and ignored by Git.

## Documentation

- [CHANGELOG.md](CHANGELOG.md): version history.
- [docs/FORK_CHANGES.md](docs/FORK_CHANGES.md): what this fork changed from the original project.
- [docs/RELEASE_PROCESS.md](docs/RELEASE_PROCESS.md): how APK publishing, rollback files, GitHub Release, and server force updates fit together.
- [docs/PUBLISHING.md](docs/PUBLISHING.md): publishing notes for GitHub and LSPosed/Xposed module submission.
- [docs/EXTERNAL_CONTROL.md](docs/EXTERNAL_CONTROL.md): optional broadcast automation API.

## Security Notes

Do not upload signing certificates, keystore passwords, activation private keys, server passwords, SSH credentials, or tokens to GitHub.

Private local files include:

```text
keystore/
keystore.properties
```

## Legal Notice

This project is provided only for lawful testing, debugging, research, and device-owner-controlled scenarios. Do not use it to bypass platform rules, misrepresent real-world location for fraud, evade compliance checks, invade privacy, or perform illegal activity. Users are responsible for complying with local laws and third-party service terms.

## License

This project remains under the MIT License. The original copyright notice from [XposedFakeLocation](https://github.com/noobexon1/XposedFakeLocation) is preserved in [LICENSE](LICENSE).
