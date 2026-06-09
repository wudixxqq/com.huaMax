LocationMax 0.0.10

- Reduced high-frequency Xposed location logs to lower CPU and logcat overhead while spoofing.
- Cached hook-side location preferences so target apps no longer trigger repeated JSON parsing on every location read.
- Refresh fake location values before system-level fake locations are created, improving consistency after changing the selected point.

For lawful testing, debugging, and device-owner-controlled use only.
