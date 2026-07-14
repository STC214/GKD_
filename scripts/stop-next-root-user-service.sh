#!/system/bin/sh
set -eu

marker="${1:-/data/local/tmp/gkd-stopped-user-service.pid}"
target_count="${2:-1}"
rm -f "$marker"
seen=" "
stopped_count=0

while true; do
  for pid in $(pidof li.songe.gkd:shizuku-user-service 2>/dev/null || true); do
    case "$seen" in
      *" $pid "*) continue ;;
    esac
    kill -STOP "$pid"
    echo "$pid" >> "$marker"
    seen="$seen$pid "
    stopped_count=$((stopped_count + 1))
    if [ "$stopped_count" -ge "$target_count" ]; then
      exit 0
    fi
  done
  sleep 0.02
done
