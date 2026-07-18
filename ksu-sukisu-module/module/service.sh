#!/system/bin/sh

MODDIR=${0%/*}
CONFIG="$MODDIR/config.conf"
LOG="$MODDIR/service.log"

log() {
  if [ -f "$LOG" ]; then
    local log_size
    local max_bytes
    max_bytes="$(sanitize_number "${GKD_LOG_MAX_BYTES:-262144}" 262144 4096 10485760)"
    log_size="$(wc -c < "$LOG" 2>/dev/null)"
    if [ "${log_size:-0}" -gt "$max_bytes" ]; then
      tail -n 1000 "$LOG" > "$LOG.tmp" 2>/dev/null && mv -f "$LOG.tmp" "$LOG"
    fi
  fi
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"
}

load_config() {
  GKD_PACKAGE=li.songe.gkd
  GKD_USER_ID=0
  GKD_KEEP_ALIVE_INTERVAL=300
  GKD_POLICY_REFRESH_INTERVAL=1800
  GKD_LOG_MAX_BYTES=262144
  [ -f "$CONFIG" ] && . "$CONFIG"
  case "$GKD_USER_ID" in
    ''|*[!0-9]*)
      log "invalid GKD_USER_ID=$GKD_USER_ID; falling back to user 0"
      GKD_USER_ID=0
      ;;
  esac
}

wait_boot_completed() {
  local tries=0
  while true; do
    [ "$(getprop sys.boot_completed 2>/dev/null)" = "1" ] && return 0
    tries=$((tries + 1))
    if [ $((tries % 30)) -eq 0 ]; then
      log "waiting for sys.boot_completed=1"
    fi
    sleep 2
  done
}

wait_system_services() {
  local tries=0
  while true; do
    if pm path android >/dev/null 2>&1 &&
      cmd activity get-current-user >/dev/null 2>&1; then
      return 0
    fi
    tries=$((tries + 1))
    if [ $((tries % 30)) -eq 0 ]; then
      log "waiting for PackageManager and ActivityManager"
    fi
    sleep 2
  done
}

package_installed() {
  pm path --user "$GKD_USER_ID" "$GKD_PACKAGE" >/dev/null 2>&1
}

package_uid() {
  pm list packages -U --user "$GKD_USER_ID" "$GKD_PACKAGE" 2>/dev/null |
    awk -v target="package:$GKD_PACKAGE" '
      $1 == target && $2 ~ /^uid:[0-9]+$/ {
        sub(/^uid:/, "", $2)
        print $2
        exit
      }
    '
}

run_once() {
  local label="$1"
  shift
  local output
  local status

  output="$("$@" 2>&1)"
  status=$?
  if [ "$status" -eq 0 ]; then
    [ -n "$output" ] && log "$label: $output"
    return 0
  fi
  log "$label failed ($status): $output"
  return "$status"
}

apply_background_policy() {
  package_installed || return 1

  log "refreshing keep-alive appops and battery whitelist"
  run_once "appops RUN_IN_BACKGROUND" cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_IN_BACKGROUND allow
  run_once "appops RUN_ANY_IN_BACKGROUND" cmd appops set --user "$GKD_USER_ID" "$GKD_PACKAGE" RUN_ANY_IN_BACKGROUND allow
  run_once "deviceidle whitelist" dumpsys deviceidle whitelist +"$GKD_PACKAGE"
}

start_gkd_once() {
  if ! package_installed; then
    log "skip start: $GKD_PACKAGE is not installed; this module does not bundle or install the app"
    return 1
  fi

  log "starting $GKD_PACKAGE in background"
  run_once "start expose service" am start-foreground-service \
    --user "$GKD_USER_ID" \
    -n "$GKD_PACKAGE/.service.ExposeService" \
    --ei expose -1
}

start_gkd_until_running() {
  local reason="$1"
  local attempt=1
  local delay=2

  while package_installed; do
    if confirm_gkd_process_stable; then
      log "$reason: stable GKD process confirmed for user $GKD_USER_ID"
      return 0
    fi

    log "$reason: startup attempt $attempt"
    if start_gkd_once; then
      if confirm_gkd_process_stable; then
        log "$reason: stable GKD process startup confirmed for user $GKD_USER_ID"
        return 0
      fi
      log "$reason: start command completed but process stability was not confirmed"
    fi

    log "$reason: startup not confirmed; retrying in ${delay}s"
    sleep "$delay"
    [ "$delay" -lt 30 ] && delay=$((delay + 2))
    attempt=$((attempt + 1))
  done

  log "$reason: $GKD_PACKAGE is no longer installed; returning to sleep state"
  return 1
}

sanitize_number() {
  local value="$1"
  local fallback="$2"
  local minimum="$3"
  local maximum="$4"

  case "$value" in
    ''|*[!0-9]*) value="$fallback" ;;
  esac
  while [ "${value#0}" != "$value" ]; do
    value="${value#0}"
  done
  [ -n "$value" ] || value=0

  # All supported maxima fit within nine decimal digits. Clamp longer input
  # before integer comparison so vendor shells never see an overflowing value.
  if [ "${#value}" -gt 9 ]; then
    echo "$maximum"
  elif [ "$value" -lt "$minimum" ]; then
    echo "$minimum"
  elif [ "$value" -gt "$maximum" ]; then
    echo "$maximum"
  else
    echo "$value"
  fi
}

gkd_process_running() {
  local expected_uid
  local pid
  local process_uid
  expected_uid="$(package_uid)"
  [ -n "$expected_uid" ] || return 1

  for pid in $(pidof "$GKD_PACKAGE" 2>/dev/null); do
    [ -r "/proc/$pid/status" ] || continue
    process_uid="$(awk '/^Uid:/ { print $2; exit }' "/proc/$pid/status" 2>/dev/null)"
    [ "$process_uid" = "$expected_uid" ] && return 0
  done
  return 1
}

confirm_gkd_process_stable() {
  local checks=0

  while [ "$checks" -lt 3 ]; do
    sleep 1
    gkd_process_running || return 1
    checks=$((checks + 1))
  done
  return 0
}

status_service_running() {
  dumpsys activity services --user "$GKD_USER_ID" "$GKD_PACKAGE/.service.StatusService" 2>/dev/null |
    grep -F "$GKD_PACKAGE/.service.StatusService" >/dev/null 2>&1
}

status_service_requested() {
  local file
  local internal_files="/data/user/$GKD_USER_ID/$GKD_PACKAGE/files"

  if [ -f "$internal_files/.gkd" ]; then
    file="$internal_files/store/store.json"
    if [ -f "$file" ]; then
      grep -E '"enableStatusService"[[:space:]]*:[[:space:]]*true' "$file" >/dev/null 2>&1
      return $?
    fi
  else
    for file in \
      "/storage/emulated/$GKD_USER_ID/Android/data/$GKD_PACKAGE/files/store/store.json"; do
      if [ -f "$file" ]; then
        grep -E '"enableStatusService"[[:space:]]*:[[:space:]]*true' "$file" >/dev/null 2>&1
        return $?
      fi
    done
  fi

  # Preserve an already running service when the preference file is not readable,
  # but never override an explicit in-app disable action.
  status_service_running
}

wait_for_gkd_installation() {
  local interval

  while true; do
    load_config
    if package_installed; then
      log "package detected for user $GKD_USER_ID: $GKD_PACKAGE"
      return 0
    fi

    interval="$(sanitize_number "$GKD_KEEP_ALIVE_INTERVAL" 300 60 86400)"
    log "$GKD_PACKAGE is not installed for user $GKD_USER_ID; sleeping ${interval}s"
    sleep "$interval"
  done
}

keep_alive_loop() {
  local elapsed=0
  local interval
  local refresh_interval

  log "keep-alive watchdog started"
  while true; do
    load_config
    interval="$(sanitize_number "$GKD_KEEP_ALIVE_INTERVAL" 300 60 86400)"
    refresh_interval="$(sanitize_number "$GKD_POLICY_REFRESH_INTERVAL" 1800 300 604800)"
    sleep "$interval"
    elapsed=$((elapsed + interval))

    if ! package_installed; then
      log "keep-alive: $GKD_PACKAGE was removed; returning to sleep state"
      return 1
    fi

    if ! gkd_process_running; then
      log "keep-alive: GKD process is not running; restarting"
      start_gkd_until_running "keep-alive" || return 1
    elif status_service_requested && ! status_service_running; then
      log "keep-alive: status service is not running; requesting recovery"
      start_gkd_once
    fi

    if [ "$elapsed" -ge "$refresh_interval" ]; then
      apply_background_policy
      elapsed=0
    fi
  done
}

{
  load_config
  log "keep-alive service started"
  wait_boot_completed
  wait_system_services
  while true; do
    wait_for_gkd_installation
    apply_background_policy
    if start_gkd_until_running "startup"; then
      keep_alive_loop
    fi
  done
} &
