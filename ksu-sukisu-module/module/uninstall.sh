#!/system/bin/sh

MODDIR=${0%/*}
CONFIG="$MODDIR/config.conf"
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
[ -f "$CONFIG" ] && . "$CONFIG"
case "$GKD_USER_ID" in
  ''|*[!0-9]*) GKD_USER_ID=0 ;;
esac

# Remove only the keep-alive policy owned by this module. The GKD app, app
# permissions and data always remain installed.
cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_IN_BACKGROUND default >/dev/null 2>&1
cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_ANY_IN_BACKGROUND default >/dev/null 2>&1
dumpsys deviceidle whitelist -"$GKD_PACKAGE" >/dev/null 2>&1
