# GKD KernelSU / SukiSU Ultra module

This wrapper keeps GKD as a normal Android app and uses a KernelSU-compatible
module to install it, grant useful permissions, and start it after boot.

It is intentionally small:

- `module/module.prop` identifies the flashable module.
- `module/customize.sh` validates the bundled APK during installation.
- `module/service.sh` runs after boot and performs install/grant/start actions.
- `module/action.sh` opens GKD from the module manager action button.
- `module/uninstall.sh` removes the battery whitelist and optionally uninstalls GKD.
- `module/config.conf` controls optional behavior.
- `module/skip_mount` marks the module as script-only; it does not overlay system files.
- `scripts/package-ksu-module.ps1` builds/copies the APK and creates the zip.

## Build on Windows

From the repo root:

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

The script runs:

```powershell
.\gradlew.bat app:assembleGkdRelease -PGKD_RENAME_APK_FLAG=1
```

Then it writes the module zip to:

```text
ksu-sukisu-module\dist\gkd-ksu-sukisu-module.zip
```

To package an existing APK instead of building:

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1 -ApkPath .\app\build\outputs\apk\gkd\release\app-gkd-release.apk
```

## Runtime config

After flashing, edit this file in the module directory if needed:

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

Defaults:

```sh
GKD_AUTO_INSTALL=1
GKD_AUTO_GRANT=1
GKD_AUTO_ENABLE_A11Y=0
GKD_AUTO_START=1
GKD_REQUIRE_BUNDLED_APK=0
GKD_UNINSTALL_ON_REMOVE=0
```

`GKD_AUTO_ENABLE_A11Y=1` writes Android secure settings to enable the GKD
accessibility service. This is powerful and ROM-dependent, so it is off by
default.

`GKD_UNINSTALL_ON_REMOVE=0` means removing the module will not remove the app or
its data. Set it to `1` only if you want module removal to uninstall GKD for
user 0.

`GKD_REQUIRE_BUNDLED_APK=0` allows the helper to continue granting/starting an
already installed GKD when the bundled APK cannot replace it, usually because of
a signature mismatch. Set it to `1` if you want the module to stop instead.

## Existing GKD install

You do not need to remove the app first when the installed GKD and the bundled
APK are signed with the same key. The module uses `pm install -r -d` and keeps
the app data.

If Android reports an update/signature incompatibility, the installed GKD was
signed with a different key. In that case, export or back up your GKD data first,
then uninstall the existing app before flashing or rebooting with this module.
Alternatively, build this module with an APK signed by the same key as the
installed app.

## Scope

This is not an LSPosed rewrite and it does not replace GKD's accessibility rule
engine. The module only supplies root-side deployment and permission automation.
