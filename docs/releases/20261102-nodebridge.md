# GKD Root Node Bridge — 2026-11-02

- Embedded modern LSPosed API 102 module in the GKD APK (not a separate module APK).
- Experimental node-connection repair is restricted to watermark camera (`com.tencent.zebra`); other apps are not enabled in this release.
- Root-backed detector-folder exclusions and deny-first whitelist policy; expired or excluded targets release owned connections.
- New rounded launcher icon using the supplied artwork.

## Requirements and limitations

- Root and a compatible LSPosed implementation are required for the experimental bridge. Root alone does not activate it.
- Enable GKD in LSPosed, restrict scope to watermark camera, and restart the target app after changes.
- Accessibility service and GKD matching rules remain required. No screenshot/OCR skipping was added.
- Validated on the connected Android 17 device: hidden app-facing accessibility state remained unchanged, node retrieval and skip clicking worked, and whitelist exclusion disabled node repair.
- This is a device-tested experimental build, not a claim of compatibility with all Android versions or apps.

## APK provenance

The release APK is the exact locally built, installed and verified icon-refresh artifact, rather than a CI rebuild. APK versionCode remains 92 (upstream base 1.12.1); build metadata reflects the pre-publication workspace. It uses the same local Android Debug certificate as the preceding test APK, not the upstream production signing key.

SHA-256: `6cf11d029f6ddca4bec8bf06c79072ed0bec73923ec9edb2cd2663ab9c7078f1`

To roll back on the existing test device, reinstall the previously saved pre-icon APK with the same certificate. Do not uninstall GKD unless application-data loss is acceptable.