#!/system/bin/sh

APK="$MODPATH/common/gkd.apk"

ui_print "*******************************"
ui_print " GKD KernelSU/SukiSU Helper"
ui_print "*******************************"

if [ ! -f "$APK" ]; then
  abort "Missing common/gkd.apk. Build the module with scripts/package-ksu-module.ps1."
fi

ui_print "- Found bundled APK"
ui_print "- Runtime config: /data/adb/modules/gkd_ksu_sukisu/config.conf"
ui_print "- Accessibility auto-enable is disabled by default"

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
