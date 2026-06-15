# Fork Changes From XposedFakeLocation

LocationMax started from [noobexon1/XposedFakeLocation](https://github.com/noobexon1/XposedFakeLocation), then was customized for the `com.huaMax` package and managed distribution workflow.

## Identity And Packaging

- Renamed application ID, namespace, Xposed entry, resources, and broadcast action prefixes to `com.huaMax`.
- Updated LSPosed module metadata for the LocationMax identity.
- Preserved the MIT license and upstream attribution.

## Distribution And Access Control

- Added a 10-day activation-code flow for managed use.
- Added remote JSON control for enable, disable, and force-update states.
- Added update checking against GitHub Releases.
- Added local and GitHub publishing workflows for signed APK releases.

## Location Simulation Reliability

- Added a foreground mock-provider fallback service that can feed GPS, network, and fused providers.
- Added service-side persistence of the active fake point, play state, and accuracy.
- Added wake-lock and background-thread handling so mock-provider pushes do not stop easily under background restrictions.

## Hook Coverage

- Added system-level hook scope handling for `system`, `android`, and `com.android.phone`.
- Added system-server hooks for last/current location, reported provider locations, GNSS callbacks, Wi-Fi scan data, and geofence requests.
- Added target-app hooks for common location API paths and mock-location flags.

## Automation

- Added optional external broadcast control for start, stop, and set-location actions.
- Kept external control disabled by default because it is exported when enabled.

## Release Management

- `LocationMax-release.apk` is the moving latest APK.
- `LocationMax-release-X.Y.Z.apk` files are rollback copies.
- GitHub Release assets keep the stable latest file name for old app compatibility.
- Repository `releases/` keeps versioned rollback files.
