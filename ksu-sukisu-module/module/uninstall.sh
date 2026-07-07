#!/system/bin/sh

MODDIR=${0%/*}
CONFIG="$MODDIR/config.conf"
GKD_PACKAGE=li.songe.gkd
GKD_UNINSTALL_ON_REMOVE=0
[ -f "$CONFIG" ] && . "$CONFIG"

dumpsys deviceidle whitelist -"$GKD_PACKAGE" >/dev/null 2>&1

if [ "$GKD_UNINSTALL_ON_REMOVE" = "1" ]; then
  pm uninstall --user 0 "$GKD_PACKAGE" >/dev/null 2>&1 || pm uninstall "$GKD_PACKAGE" >/dev/null 2>&1
fi
