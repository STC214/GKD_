#!/system/bin/sh

MODDIR=${0%/*}
CONFIG="$MODDIR/config.conf"
LOG="$MODDIR/service.log"
GKD_PACKAGE=li.songe.gkd
[ -f "$CONFIG" ] && . "$CONFIG"

echo "GKD KernelSU/SukiSU Helper"
echo "Module path: $MODDIR"
echo "Package: $GKD_PACKAGE"
echo

if pm path "$GKD_PACKAGE" >/dev/null 2>&1; then
  echo "GKD app: installed"
else
  echo "GKD app: not installed"
  echo "Open failed: package is missing."
  exit 1
fi

echo "Config:"
echo "  GKD_AUTO_INSTALL=${GKD_AUTO_INSTALL:-1}"
echo "  GKD_AUTO_GRANT=${GKD_AUTO_GRANT:-1}"
echo "  GKD_AUTO_ENABLE_A11Y=${GKD_AUTO_ENABLE_A11Y:-0}"
echo "  GKD_AUTO_START=${GKD_AUTO_START:-1}"
echo "  GKD_KEEP_ALIVE=${GKD_KEEP_ALIVE:-1}"
echo "  GKD_KEEP_ALIVE_INTERVAL=${GKD_KEEP_ALIVE_INTERVAL:-300}"
echo "  GKD_REGRANT_INTERVAL=${GKD_REGRANT_INTERVAL:-1800}"
echo "  GKD_UNINSTALL_ON_REMOVE=${GKD_UNINSTALL_ON_REMOVE:-1}"
echo

echo "Opening GKD..."
if monkey -p "$GKD_PACKAGE" 1 >/dev/null 2>&1; then
  echo "Open command: monkey succeeded"
elif am start -n "$GKD_PACKAGE/.MainActivity"; then
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
