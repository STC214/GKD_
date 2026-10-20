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

# Restore module-managed keep-alive policy and remove the bundled app for the
# Android user recorded at install time.
cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_IN_BACKGROUND default >/dev/null 2>&1
cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_ANY_IN_BACKGROUND default >/dev/null 2>&1
dumpsys deviceidle whitelist -"$GKD_PACKAGE" >/dev/null 2>&1

MARKER=${GKD_MARKER_PATH:-/data/adb/gkd_ksu_sukisu_app_installed}
if [ -f "$MARKER" ]; then
  INSTALLED_USER=$(cat "$MARKER" 2>/dev/null)
  case "$INSTALLED_USER" in ''|*[!0-9]*) INSTALLED_USER="$GKD_USER_ID" ;; esac
  if pm list packages --user "$INSTALLED_USER" "$GKD_PACKAGE" | grep -Fxq "package:$GKD_PACKAGE"; then
    if pm uninstall --user "$INSTALLED_USER" "$GKD_PACKAGE" >/dev/null 2>&1; then
      rm -f "$MARKER"
    else
      echo "GKD uninstall failed for user $INSTALLED_USER; ownership marker retained" >&2
    fi
  else
    rm -f "$MARKER"
  fi
fi
