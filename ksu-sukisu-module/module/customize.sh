#!/system/bin/sh

ui_print "*******************************"
ui_print " GKD Keepalive Helper"
ui_print "*******************************"
ui_print "- No APK is bundled or installed"
ui_print "- Install GKD separately before using this module"
ui_print "- Installed GKD is started until its process is confirmed"
ui_print "- Missing GKD leaves the module in low-frequency sleep"
ui_print "- Runtime config: /data/adb/modules/gkd_ksu_sukisu/config.conf"

set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/action.sh" 0 0 0755
set_perm "$MODPATH/uninstall.sh" 0 0 0755
