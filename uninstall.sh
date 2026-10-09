#!/system/bin/sh
# Restore system state on module removal.
MODDIR="${0%/*}"
pkill -f "keepalive.sh watch" 2>/dev/null
if [ -f "$MODDIR/config.sh" ]; then
  . "$MODDIR/config.sh"
  [ "$FREEZE_POWERKEEPER" = "1" ] && pm enable com.miui.powerkeeper >/dev/null 2>&1
fi
exit 0
