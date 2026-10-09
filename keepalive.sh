#!/system/bin/sh
# UnlimitedKeepAlive core logic
MODDIR="${0%/*}"
. "$MODDIR/config.sh"

: ${PROTECT_PKGS:=com.eg.android.AlipayGphone}
: ${ALL_USERS:=1}
: ${USERS:=0}
: ${FREEZE_POWERKEEPER:=1}
: ${WATCH_INTERVAL:=300}

LOG="$MODDIR/keepalive.log"
log() { echo "$(date '+%m-%d %H:%M:%S') $*" >> "$LOG"; }

discover_users() {
  if [ "$ALL_USERS" = "1" ]; then
    pm list users 2>/dev/null | grep -o 'UserInfo{[0-9]*' | grep -o '[0-9]*$'
  else
    for u in $USERS; do echo "$u"; done
  fi
}

apply() {
  # 1. global doze whitelist
  for pkg in $PROTECT_PKGS; do
    cmd deviceidle whitelist +$pkg >/dev/null 2>&1
  done

  # 2. per-user protections (each clone is an independent user)
  for u in $(discover_users); do
    for pkg in $PROTECT_PKGS; do
      pm path --user "$u" "$pkg" >/dev/null 2>&1 || continue
      cmd appops set --user "$u" "$pkg" RUN_ANY_IN_BACKGROUND allow >/dev/null 2>&1
      cmd appops set --user "$u" "$pkg" RUN_IN_BACKGROUND allow >/dev/null 2>&1
      cmd appops set --user "$u" "$pkg" START_FOREGROUND allow >/dev/null 2>&1
      cmd appops set --user "$u" "$pkg" WAKE_LOCK allow >/dev/null 2>&1
      am set-standby-bucket --user "$u" "$pkg" exempted >/dev/null 2>&1 \
        || am set-standby-bucket --user "$u" "$pkg" active >/dev/null 2>&1
    done
  done

  # 3. PowerKeeper freeze / restore to match config
  if [ "$FREEZE_POWERKEEPER" = "1" ]; then
    pm disable-user --user 0 com.miui.powerkeeper >/dev/null 2>&1
  else
    pm enable com.miui.powerkeeper >/dev/null 2>&1
  fi
}

status() {
  echo "== UnlimitedKeepAlive status =="
  echo "config: pkgs=[$PROTECT_PKGS] all_users=$ALL_USERS freeze_pk=$FREEZE_POWERKEEPER interval=${WATCH_INTERVAL}s"
  echo ""
  echo "-- PowerKeeper --"
  if pm list packages -d 2>/dev/null | grep -q com.miui.powerkeeper; then
    echo "  frozen (disabled)"
  else
    echo "  active"
  fi
  echo ""
  echo "-- per user --"
  for u in $(discover_users); do
    for pkg in $PROTECT_PKGS; do
      pm path --user "$u" "$pkg" >/dev/null 2>&1 || { echo "  user $u: $pkg not installed"; continue; }
      bucket=$(am get-standby-bucket --user "$u" "$pkg" 2>/dev/null)
      case "$bucket" in
        5) b="EXEMPTED";;
        10) b="ACTIVE";;
        20) b="WORKING_SET";;
        30) b="FREQUENT";;
        40) b="RARE";;
        45) b="RESTRICTED";;
        *) b="?($bucket)";;
      esac
      procs=$(ps -A 2>/dev/null | grep -c "$pkg")
      echo "  user $u: $pkg bucket=$b procs_alive=$procs"
    done
  done
  echo ""
  echo "-- last log --"
  tail -5 "$LOG" 2>/dev/null
}

case "$1" in
  apply)
    apply
    log "applied: pkgs=[$PROTECT_PKGS] users=[$(discover_users | tr '\n' ' ')] freeze_pk=$FREEZE_POWERKEEPER"
    echo "UnlimitedKeepAlive: applied."
    ;;
  status)
    status
    ;;
  watch)
    log "watch daemon started (interval=${WATCH_INTERVAL}s)"
    while :; do
      sleep "$WATCH_INTERVAL"
      apply
    done
    ;;
  *)
    echo "usage: sh keepalive.sh apply|status|watch"
    echo "  edit config.sh to change protected apps / users / powerkeeper / interval"
    ;;
esac
