#!/system/bin/sh

MODDIR=${0%/*}
APK="$MODDIR/common/gkd.apk"
CONFIG="$MODDIR/config.conf"
LOG="$MODDIR/service.log"

log() {
  if [ -f "$LOG" ]; then
    local log_size
    log_size="$(wc -c < "$LOG" 2>/dev/null)"
    if [ "${log_size:-0}" -gt 262144 ]; then
      tail -n 1000 "$LOG" > "$LOG.tmp" 2>/dev/null && mv -f "$LOG.tmp" "$LOG"
    fi
  fi
  echo "$(date '+%Y-%m-%d %H:%M:%S') $*" >> "$LOG"
}

wait_boot_completed() {
  local boot
  local tries=0
  while [ "$tries" -lt 120 ]; do
    boot="$(getprop sys.boot_completed 2>/dev/null)"
    [ "$boot" = "1" ] && return 0
    tries=$((tries + 1))
    sleep 2
  done
  return 1
}

load_config() {
  GKD_PACKAGE=li.songe.gkd
  GKD_AUTO_INSTALL=1
  GKD_AUTO_GRANT=1
  GKD_AUTO_ENABLE_A11Y=0
  GKD_AUTO_START=1
  GKD_KEEP_ALIVE=1
  GKD_KEEP_ALIVE_INTERVAL=300
  GKD_REGRANT_INTERVAL=1800
  GKD_REQUIRE_BUNDLED_APK=0
  GKD_UNINSTALL_ON_REMOVE=1
  [ -f "$CONFIG" ] && . "$CONFIG"
}

package_installed() {
  pm path "$GKD_PACKAGE" >/dev/null 2>&1
}

run_retry() {
  local label="$1"
  shift
  local tries=0
  local delay=2
  local output
  local status

  while [ "$tries" -lt 8 ]; do
    output="$("$@" 2>&1)"
    status=$?
    if [ "$status" -eq 0 ]; then
      [ -n "$output" ] && log "$label: $output"
      return 0
    fi

    log "$label failed ($status): $output"
    tries=$((tries + 1))
    sleep "$delay"
    [ "$delay" -lt 10 ] && delay=$((delay + 2))
  done

  return 1
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

wait_system_services() {
  local tries=0
  while [ "$tries" -lt 60 ]; do
    if pm path android >/dev/null 2>&1 &&
      cmd activity get-current-user >/dev/null 2>&1; then
      return 0
    fi
    tries=$((tries + 1))
    sleep 2
  done
  log "system service wait timeout; continue anyway"
  return 1
}

install_apk() {
  [ "$GKD_AUTO_INSTALL" = "1" ] || return 0
  if [ ! -f "$APK" ]; then
    log "skip install: missing $APK"
    return 1
  fi

  log "installing $APK for package $GKD_PACKAGE"
  install_output="$(pm install -r -d --user 0 "$APK" 2>&1)"
  install_status=$?
  log "$install_output"
  if [ "$install_status" -ne 0 ]; then
    install_output="$(pm install -r -d "$APK" 2>&1)"
    install_status=$?
    log "$install_output"
  fi

  if [ "$install_status" -ne 0 ]; then
    log "install command failed; if GKD is already installed with a different signature, uninstall it or build this module with the same signing key"
    if [ "$GKD_REQUIRE_BUNDLED_APK" = "1" ]; then
      log "stop: GKD_REQUIRE_BUNDLED_APK=1 and bundled APK install failed"
      return 1
    fi
  fi

  if package_installed; then
    log "$GKD_PACKAGE is installed; continue with grants"
    return 0
  fi

  log "$GKD_PACKAGE is not installed after install attempt"
  return 1
}

grant_permissions() {
  [ "$GKD_AUTO_GRANT" = "1" ] || return 0
  if ! package_installed; then
    log "skip grants: $GKD_PACKAGE is not installed"
    return 1
  fi

  log "granting permissions and appops for $GKD_PACKAGE"
  run_retry "grant POST_NOTIFICATIONS" pm grant "$GKD_PACKAGE" android.permission.POST_NOTIFICATIONS
  run_retry "grant WRITE_SECURE_SETTINGS" pm grant "$GKD_PACKAGE" android.permission.WRITE_SECURE_SETTINGS

  run_retry "appops SYSTEM_ALERT_WINDOW" cmd appops set "$GKD_PACKAGE" SYSTEM_ALERT_WINDOW allow
  run_retry "appops RUN_IN_BACKGROUND" cmd appops set "$GKD_PACKAGE" RUN_IN_BACKGROUND allow
  run_retry "appops RUN_ANY_IN_BACKGROUND" cmd appops set "$GKD_PACKAGE" RUN_ANY_IN_BACKGROUND allow
  run_retry "appops GET_USAGE_STATS" cmd appops set "$GKD_PACKAGE" GET_USAGE_STATS allow

  run_retry "deviceidle whitelist" dumpsys deviceidle whitelist +"$GKD_PACKAGE"
}

refresh_background_policy() {
  [ "$GKD_AUTO_GRANT" = "1" ] || return 0
  package_installed || return 1

  log "refreshing background appops and battery whitelist"
  run_once "appops RUN_IN_BACKGROUND" cmd appops set "$GKD_PACKAGE" RUN_IN_BACKGROUND allow
  run_once "appops RUN_ANY_IN_BACKGROUND" cmd appops set "$GKD_PACKAGE" RUN_ANY_IN_BACKGROUND allow
  run_once "deviceidle whitelist" dumpsys deviceidle whitelist +"$GKD_PACKAGE"
}

enable_accessibility() {
  [ "$GKD_AUTO_ENABLE_A11Y" = "1" ] || return 0
  if ! package_installed; then
    log "skip accessibility enable: $GKD_PACKAGE is not installed"
    return 1
  fi

  local component="$GKD_PACKAGE/com.google.android.accessibility.selecttospeak.SelectToSpeakService"
  local enabled
  enabled="$(settings get secure enabled_accessibility_services 2>/dev/null)"

  case ":$enabled:" in
    *":$component:"*)
      log "accessibility already enabled: $component"
      ;;
    *)
      if [ -z "$enabled" ] || [ "$enabled" = "null" ]; then
        settings put secure enabled_accessibility_services "$component" >> "$LOG" 2>&1
      else
        settings put secure enabled_accessibility_services "$enabled:$component" >> "$LOG" 2>&1
      fi
      settings put secure accessibility_enabled 1 >> "$LOG" 2>&1
      log "requested accessibility enable: $component"
      ;;
  esac
}

start_gkd() {
  [ "$GKD_AUTO_START" = "1" ] || return 0
  if ! package_installed; then
    log "skip start: $GKD_PACKAGE is not installed"
    return 1
  fi

  log "starting $GKD_PACKAGE in background"
  run_retry "start expose service" am start-foreground-service \
    -n "$GKD_PACKAGE/.service.ExposeService" \
    --ei expose -1
}

sanitize_interval() {
  case "$1" in
    ''|*[!0-9]*) echo "$2" ;;
    *)
      if [ "$1" -lt "$3" ]; then
        echo "$3"
      else
        echo "$1"
      fi
      ;;
  esac
}

gkd_process_running() {
  pidof "$GKD_PACKAGE" >/dev/null 2>&1 && return 0
  ps -A 2>/dev/null | grep -F "$GKD_PACKAGE" | grep -v grep >/dev/null 2>&1
}

status_service_running() {
  dumpsys activity services "$GKD_PACKAGE/.service.StatusService" 2>/dev/null |
    grep -F "$GKD_PACKAGE/.service.StatusService" >/dev/null 2>&1
}

status_service_requested() {
  local file
  local internal_files="/data/user/0/$GKD_PACKAGE/files"

  if [ -f "$internal_files/.gkd" ]; then
    file="$internal_files/store/store.json"
    if [ -f "$file" ]; then
      grep -E '"enableStatusService"[[:space:]]*:[[:space:]]*true' "$file" >/dev/null 2>&1
      return $?
    fi
  else
    for file in \
      "/storage/emulated/0/Android/data/$GKD_PACKAGE/files/store/store.json" \
      "/sdcard/Android/data/$GKD_PACKAGE/files/store/store.json"; do
      if [ -f "$file" ]; then
        grep -E '"enableStatusService"[[:space:]]*:[[:space:]]*true' "$file" >/dev/null 2>&1
        return $?
      fi
    done
  fi

  # If the preference file cannot be located, only preserve a service that is
  # already running. This avoids overriding an explicit in-app disable action.
  status_service_running
}

keep_alive_loop() {
  local elapsed=0
  local interval
  local regrant_interval

  log "keep-alive watchdog started"
  while true; do
    load_config
    [ "$GKD_KEEP_ALIVE" = "1" ] || {
      log "keep-alive watchdog disabled"
      return 0
    }

    interval="$(sanitize_interval "$GKD_KEEP_ALIVE_INTERVAL" 300 60)"
    regrant_interval="$(sanitize_interval "$GKD_REGRANT_INTERVAL" 1800 300)"
    sleep "$interval"
    elapsed=$((elapsed + interval))

    if ! package_installed; then
      log "keep-alive: package missing; retrying install"
      if [ "$GKD_AUTO_INSTALL" != "1" ]; then
        log "keep-alive: automatic install is disabled"
        continue
      fi
      install_apk || continue
    fi

    if ! gkd_process_running; then
      log "keep-alive: GKD process is not running; restarting"
      start_gkd
    elif status_service_requested && ! status_service_running; then
      log "keep-alive: status service is not running; requesting recovery"
      start_gkd
    fi

    if [ "$elapsed" -ge "$regrant_interval" ]; then
      refresh_background_policy
      enable_accessibility
      elapsed=0
    fi
  done
}

{
  load_config
  log "service started"
  if wait_boot_completed; then
    wait_system_services
    install_apk && {
      grant_permissions
      enable_accessibility
      start_gkd
      keep_alive_loop
    }
    log "service stopped"
  else
    log "boot wait timeout"
  fi
} &
