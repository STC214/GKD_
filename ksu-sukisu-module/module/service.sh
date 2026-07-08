#!/system/bin/sh

MODDIR=${0%/*}
APK="$MODDIR/common/gkd.apk"
CONFIG="$MODDIR/config.conf"
LOG="$MODDIR/service.log"

log() {
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
  GKD_REQUIRE_BUNDLED_APK=0
  GKD_UNINSTALL_ON_REMOVE=0
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

{
  load_config
  log "service started"
  if wait_boot_completed; then
    wait_system_services
    install_apk && {
      grant_permissions
      enable_accessibility
      start_gkd
    }
    log "service finished"
  else
    log "boot wait timeout"
  fi
} &
