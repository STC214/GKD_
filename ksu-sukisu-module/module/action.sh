#!/system/bin/sh

CONFIG="${0%/*}/config.conf"
GKD_PACKAGE=li.songe.gkd
[ -f "$CONFIG" ] && . "$CONFIG"

monkey -p "$GKD_PACKAGE" 1 >/dev/null 2>&1 || am start -n "$GKD_PACKAGE/.MainActivity" >/dev/null 2>&1
