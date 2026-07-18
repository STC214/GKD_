#!/system/bin/sh

MODDIR=${0%/*}
CONFIG="$MODDIR/config.conf"
LOG="$MODDIR/service.log"
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
[ -f "$CONFIG" ] && . "$CONFIG"
case "$GKD_USER_ID" in
  ''|*[!0-9]*) GKD_USER_ID=0 ;;
esac

echo "GKD KernelSU/SukiSU Keepalive Helper"
echo "Module path: $MODDIR"
echo "Package: $GKD_PACKAGE"
echo "Android user: $GKD_USER_ID"
echo

if pm path --user "$GKD_USER_ID" "$GKD_PACKAGE" >/dev/null 2>&1; then
  echo "GKD app: installed"
else
  echo "GKD app: not installed"
  echo "This module does not bundle or install GKD. Install the app separately."
  exit 1
fi

echo "Config:"
echo "  GKD_USER_ID=$GKD_USER_ID"
echo "  GKD_KEEP_ALIVE_INTERVAL=${GKD_KEEP_ALIVE_INTERVAL:-300}"
echo "  GKD_POLICY_REFRESH_INTERVAL=${GKD_POLICY_REFRESH_INTERVAL:-1800}"
echo "  GKD_LOG_MAX_BYTES=${GKD_LOG_MAX_BYTES:-262144}"
echo

echo "Opening GKD..."
if monkey --user "$GKD_USER_ID" -p "$GKD_PACKAGE" 1 >/dev/null 2>&1; then
  echo "Open command: monkey succeeded"
elif am start --user "$GKD_USER_ID" -n "$GKD_PACKAGE/.MainActivity"; then
  echo "Open command: am start succeeded"
else
  echo "Open command: failed"
  exit 1
fi

echo
if [ -f "$LOG" ]; then
  echo "Recent service log:"
  tail -n 20 "$LOG"
else
  echo "Recent service log: $LOG does not exist yet"
fi
