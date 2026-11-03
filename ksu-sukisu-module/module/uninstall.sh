#!/system/bin/sh

MODDIR=${MODDIR:-${0%/*}}
CONFIG="$MODDIR/config.conf"
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
[ -f "$CONFIG" ] && . "$CONFIG"
case "$GKD_USER_ID" in
  ''|*[!0-9]*) GKD_USER_ID=0 ;;
esac
GKD_PACKAGE=li.songe.gkd

# Removing keepalive must never remove an existing APK or its user data.
GKD_USER_ID=0
cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_IN_BACKGROUND default >/dev/null 2>&1
cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_ANY_IN_BACKGROUND default >/dev/null 2>&1
dumpsys deviceidle whitelist -"$GKD_PACKAGE" >/dev/null 2>&1

MARKER=${GKD_MARKER_PATH:-/data/adb/gkd_ksu_sukisu_app_installed}
rm -f "$MARKER" "$MARKER.xspace"
# Only this bridge's generated readiness cache is removed; preserve app/user data.
rm -f /data/adb/gkd_node_bridge/protected.v1 /data/adb/gkd_node_bridge/protected.v1.bak /data/adb/gkd_node_bridge/protected.v1.new
echo "Keepalive removed; GKD APK, data and XSpace registration retained"
