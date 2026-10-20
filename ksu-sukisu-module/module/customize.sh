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
[ -f "$CONFIG" ] && . "$CONFIG"
case "$GKD_USER_ID" in ''|*[!0-9]*) GKD_USER_ID=0 ;; esac
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
ui_print "- GKD installed and verified for user $GKD_USER_ID"
