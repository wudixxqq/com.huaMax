# Changelog

## 0.0.17

- Increased authorization-code validity from 10 days to 30 days.
- Updated authorization code generator defaults and validation limits to match the 30-day app limit.
- Updated authorization UI text and publishing templates for the new validity window.

## 0.0.16

- Fixed manual update checks by reading the GitHub latest release tag instead of treating raw APK URLs as redirects.
- Kept `LocationMax-release.apk` as the stable latest download asset for older app versions.
- Kept versioned rollback APKs in the repository `releases/` folder.

## 0.0.15

- Added the `system` LSPosed scope for system-server location hooks.
- Stabilized the mock provider service with a background ticker and wake lock.
- Added repository-hosted APK files for force-update downloads and rollback.

## 0.0.13

- Added a foreground mock-location provider fallback that continuously feeds GPS, network, and fused providers from the selected spoof point.
- Mirrored the selected point, active state, and accuracy into local preferences so the fallback provider can keep running independently of LSPosed remote preference reads.
- Restarted the fallback provider when the manager process starts while spoofing is still active.
- Hooked mock-location flags in scoped target apps.

## 0.0.12

- Restored authorization and remote gates after earlier location-hook experiments.
- Hardened system location hooks for newer Android location paths.

## 0.0.11

- Fixed the location spoofing gate so disabled or unauthorized states do not continue spoofing.

## 0.0.10

- Reduced high-frequency Xposed location logs.
- Cached hook-side location preferences to reduce repeated parsing.
- Refreshed fake location values before system-level fake locations are created.

## Earlier Releases

Earlier releases cover the initial package rename, map workflow preservation, authorization flow, remote control, GitHub publishing setup, and LSPosed module metadata changes.
