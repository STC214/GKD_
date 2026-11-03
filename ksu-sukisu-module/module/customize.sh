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

# Bootstrap only an unconfigured owner install. Never rewrite an existing store.
APP_FILES=${GKD_BOOTSTRAP_FILES_DIR:-/data/user/0/li.songe.gkd/files}
EXTERNAL_STORE=${GKD_BOOTSTRAP_EXTERNAL_STORE:-/storage/emulated/0/Android/data/li.songe.gkd/files/store/store.json}
if [ ! -f "$APP_FILES/store/store.json" ] && [ ! -f "$EXTERNAL_STORE" ]; then
    APP_UID=$(pm list packages -U --user 0 "$GKD_PACKAGE" | awk -v target="package:$GKD_PACKAGE" '
        $1 == target && $2 ~ /^uid:[0-9]+$/ { sub(/^uid:/, "", $2); print $2; exit }')
    case "$APP_UID" in ''|*[!0-9]*) abort "Fresh bootstrap UID lookup failed" ;; esac
    mkdir -p "$APP_FILES"
    printf '1\n' > "$APP_FILES/module_bootstrap_v1"
    touch "$APP_FILES/.gkd"
    set_perm "$APP_FILES" "$APP_UID" "$APP_UID" 0700
    set_perm "$APP_FILES/module_bootstrap_v1" "$APP_UID" "$APP_UID" 0600
    set_perm "$APP_FILES/.gkd" "$APP_UID" "$APP_UID" 0600
    restorecon -R "$APP_FILES" 2>/dev/null || true
    pm grant --user 0 "$GKD_PACKAGE" android.permission.WRITE_SECURE_SETTINGS || abort "Fresh bootstrap secure-settings grant failed"
    pm grant --user 0 "$GKD_PACKAGE" android.permission.POST_NOTIFICATIONS 2>/dev/null || true
    cmd appops set --user 0 "$GKD_PACKAGE" android:foreground_service_special_use allow 2>/dev/null || true
    # Append only our service; retain every other accessibility service.
    A11Y_SERVICE=li.songe.gkd/.service.A11yService
    CURRENT_SERVICES=$(settings --user 0 get secure enabled_accessibility_services)
    [ "$CURRENT_SERVICES" = null ] && CURRENT_SERVICES=
    case ":$CURRENT_SERVICES:" in
        *":$A11Y_SERVICE:"*|*":li.songe.gkd/li.songe.gkd.service.A11yService:"*) ;;
        *) settings --user 0 put secure enabled_accessibility_services "${CURRENT_SERVICES:+$CURRENT_SERVICES:}$A11Y_SERVICE" || abort "Fresh bootstrap accessibility setup failed" ;;
    esac
    settings --user 0 put secure accessibility_enabled 1 || abort "Fresh bootstrap accessibility enable failed"
    ui_print "- Fresh owner setup enabled; confirm initial Root/LSPosed approval once"
else
    ui_print "- Existing configuration retained; no automatic re-enable or permission rewrite"
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
