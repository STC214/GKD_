#!/system/bin/sh
# A cached whitelist from a previous boot must not authorize pre-bootstrap loading.
mkdir -p /data/adb/gkd_node_bridge
chmod 0700 /data/adb/gkd_node_bridge
printf 'NOTREADY\n' > /data/adb/gkd_node_bridge/protected.v1
chmod 0600 /data/adb/gkd_node_bridge/protected.v1
