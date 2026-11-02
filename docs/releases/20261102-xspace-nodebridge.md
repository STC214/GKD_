# XSpace 999 node bridge trial — 2026-11-02

## Architecture

One matching engine runs in owner user 0. The embedded LSPosed hook in XSpace 999 reads a short-lived, read-only lease from the existing APK-owned root service through an Android abstract local socket. The client verifies that its socket peer is UID 0; the broker derives the caller's real UID and package list from the socket, never from request fields. Only user 999's sole-package `com.tencent.zebra` UID is accepted. The owner controls exclusions and enablement; expired, unavailable, non-root, shared-UID or whitelisted peers are denied. Root protocol is now 4.

This replaces the incorrect assumption that user-0 libxposed remote preferences are automatically shared with user-999 targets. It does not copy or modify LSPosed's database, weaken SELinux, or use screenshots/OCR. The existing user-0 hook path is unchanged.

## Module registration is not a second engine

LSPosed maintains per-user module enablement and scope. XSpace may require registering the same installed APK with `install-existing --user 999`: Android shares the APK bytes, while the profile has its own module registration. Do not open GKD or enable its accessibility service in user 999 for this owner-managed trial. Enable the module and watermark-camera scope manually in LSPosed's user-999 page.

The SukiSU installer now registers the same APK when XSpace 999 already exists (`GKD_REGISTER_XSPACE_MODULE=1`; set 0 to opt out). The keepalive service remains single-user (`GKD_USER_ID=0`). It does not grant secondary Root access, launch a secondary GKD engine, or automate LSPosed through its unstable developer CLI. Newly created profile registrations are recorded separately for cleanup; pre-existing secondary GKD registrations are not claimed for removal. If XSpace is created later, register manually or reinstall the module.

## Verified on the connected Android 17 device

- Baseline user-999 camera: null root, 0 nodes.
- Modified user-999 camera: camera root, 101 nodes, `eligible=true`.
- Target still reports `visibleEnabled=false`, `visibleServices=0`, `visibleSecure=0`.
- Owner-user regression: camera root, 101 nodes.
- Camera added temporarily to owner whitelist: user-999 root null, 0 nodes, `eligible=false`.
- After owner GKD exits: broker connection is refused and clone policy changes to `eligible=false`.
- No GKD main process ran under user 999. All camera test processes were force-stopped immediately after each bounded test.
- 13 new lease-policy tests passed. Five installer/uninstaller cases passed with mocked Android commands on the phone; shell syntax was checked.

No opening advertisement appeared in these clone test samples, so this release does not claim a newly verified user-999 advertisement click. The verified result is the missing node/policy path, not all applications or all Android users. Only owner 0 / XSpace 999 and watermark camera are enabled in code. Other profiles and third-party virtual-container clones remain untested.

The temporary camera and GKD profile registrations used for testing are removed afterward. Original owner settings and whitelist are restored. The new module ZIP is packaged and checked, not flashed/reboot-tested in this run.

## Upgrade / uninstall

Cover-install the module instead of uninstalling it first. The inherited module uninstall behavior still removes its managed primary GKD app and may remove newly registered XSpace GKD; back up configuration before removal. Normal use does not require LSPosed developer mode or hook debugging. The APK retains upstream versionCode 92 and the prior local debug signing certificate, not the upstream production key.
