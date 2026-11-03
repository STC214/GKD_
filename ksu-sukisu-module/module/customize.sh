#!/system/bin/sh

ui_print "*******************************"
ui_print " GKD + Keepalive Module"
ui_print "*******************************"
ui_print "- Runtime config: /data/adb/modules/gkd_ksu_sukisu/config.conf"

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/common/gkd.apk" 0 0 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755

CONFIG="$MODPATH/config.conf"
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
GKD_REGISTER_XSPACE_MODULE=1
[ -f "$CONFIG" ] && . "$CONFIG"
# Owner 0 is the only matching engine. XSpace is a module-host registration only.
GKD_USER_ID=0
GKD_PACKAGE=li.songe.gkd
APK="$MODPATH/common/gkd.apk"
MARKER=${GKD_MARKER_PATH:-/data/adb/gkd_ksu_sukisu_app_installed}
ui_print "- Installing bundled GKD APK for Android user $GKD_USER_ID"

if [ ! -s "$APK" ]; then
    ui_print "! Bundled APK missing or empty: $APK"
    abort "GKD module installation failed"
fi
if ! pm install --user "$GKD_USER_ID" -r "$APK"; then
    ui_print "! APK installation failed for user $GKD_USER_ID"
    abort "GKD module installation failed; check package/signature compatibility"
fi
if ! pm list packages --user "$GKD_USER_ID" "$GKD_PACKAGE" | grep -Fxq "package:$GKD_PACKAGE"; then
    abort "APK install returned success but package verification failed"
fi
mkdir -p "${MARKER%/*}"
printf '%s\n' "$GKD_USER_ID" > "$MARKER"

# Register the same APK in an existing XSpace profile for LSPosed's per-user
# module registry. Do not launch a second GKD engine or enable its accessibility.
if [ "$GKD_USER_ID" = 0 ] && [ "$GKD_REGISTER_XSPACE_MODULE" = 1 ] &&
   pm list users | grep -q 'UserInfo{999:'; then
    if ! pm list packages --user 999 "$GKD_PACKAGE" | grep -Fxq "package:$GKD_PACKAGE"; then
        if ! cmd package install-existing --user 999 "$GKD_PACKAGE"; then
            abort "XSpace module registration failed"
        fi
        if ! pm list packages --user 999 "$GKD_PACKAGE" | grep -Fxq "package:$GKD_PACKAGE"; then
            abort "XSpace module registration verification failed"
        fi
        printf '999\n' > "$MARKER.xspace"
        ui_print "- Same APK registered for XSpace 999; no second engine started"
    fi
    ui_print "- Enable GKD and its target scope separately in LSPosed user 999"
fi
ui_print "- GKD installed and verified for user $GKD_USER_ID"
