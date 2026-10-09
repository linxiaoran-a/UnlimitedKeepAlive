#!/system/bin/sh
# Runs at late_boot. Apply protections and start the refresh daemon.
MODDIR="${0%/*}"

sh "$MODDIR/keepalive.sh" apply

# restart watch daemon (interval read from config.sh; 0 disables)
grep -q '^WATCH_INTERVAL=0' "$MODDIR/config.sh" 2>/dev/null && exit 0
pkill -f "keepalive.sh watch" 2>/dev/null
(nohup sh "$MODDIR/keepalive.sh" watch >/dev/null 2>&1 &)
