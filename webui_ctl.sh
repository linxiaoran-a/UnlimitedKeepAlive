#!/system/bin/sh
# UnlimitedKeepAlive WebUI backend: called via ksu.exec from webroot/index.html
# usage: webui_ctl.sh <command> [args]
MODDIR="${0%/*}"
CONF="$MODDIR/config.sh"
. "$CONF"

PKG=com.miui.powerkeeper

json_escape() { sed 's/\\/\\\\/g; s/"/\\"/g' | tr -d '\r\n'; }

discover_users() {
  pm list users 2>/dev/null | grep -o 'UserInfo{[0-9]*' | grep -o '[0-9]*$'
}

ensure_labels() {
  [ -s /data/local/tmp/.ka_l ] || \
    content query --uri content://com.miui.home.launcher.settings/favorites --projection title,intent 2>/dev/null \
    | sed -n 's/.*title=\([^,]*\),.*component=\([a-zA-Z0-9._]*\)\/.*/\2|\1/p' | sort -u > /data/local/tmp/.ka_l
  return 0
}

pkg_label() { # pkg -> app label ("" if unknown)
  grep -m1 "^$1|" /data/local/tmp/.ka_l 2>/dev/null | cut -d'|' -f2 | sed 's/"/\\"/g'
}

list_protect_pkgs() { for p in $PROTECT_PKGS; do echo "$p"; done; }

set_conf() { # key value
  case "$1" in
    FREEZE_POWERKEEPER|ALL_USERS|WATCH_INTERVAL) ;;
    *) echo "ERR bad key"; exit 1 ;;
  esac
  case "$2" in ''|*[!0-9]*) [ "$1" = WATCH_INTERVAL ] || [ "$2" = 0 ] || { echo "ERR bad value"; exit 1; } ;; esac
  sed -i "s|^$1=.*|$1=$2|" "$CONF" && echo OK
}

add_pkg() { # pkg
  # 注意: 本机 shell 的 glob 字符类只支持 ! 否定, 不支持 ^(会被当字面字符)
  case "$1" in ''|*[!a-zA-Z0-9._]*) echo "ERR bad pkg"; exit 1 ;; esac
  for p in $PROTECT_PKGS; do [ "$p" = "$1" ] && { echo EXISTS; exit 0; }; done
  NEW="$(list_protect_pkgs; echo "$1" | grep -v '^$')" 
  NEWP="$(echo "$NEW" | tr '\n' ' ' | sed 's| *$||')"
  sed -i "s|^PROTECT_PKGS=.*|PROTECT_PKGS=\"$NEWP\"|" "$CONF" && echo OK
}

rm_pkg() { # pkg
  NEWP=""
  for p in $PROTECT_PKGS; do [ "$p" = "$1" ] || NEWP="$NEWP $p"; done
  NEWP="$(echo "$NEWP" | sed 's|^ *||')"
  sed -i "s|^PROTECT_PKGS=.*|PROTECT_PKGS=\"$NEWP\"|" "$CONF" && echo OK
}

do_status() {
  ensure_labels
  if pm list packages -d 2>/dev/null | grep -q $PKG; then PK_ST="disabled"; else PK_ST="active"; fi
  if [ -f "$MODDIR/disable" ]; then MOD_EN=0; else MOD_EN=1; fi
  echo -n "{"
  echo -n "\"enabled\":$MOD_EN,"
  echo -n "\"freeze_pk\":$FREEZE_POWERKEEPER,"
  echo -n "\"all_users\":$ALL_USERS,"
  echo -n "\"watch_interval\":$WATCH_INTERVAL,"
  echo -n "\"powerkeeper\":\"$PK_ST\","
  echo -n "\"users\":["
  first=1
  for u in $(discover_users); do
    [ $first = 1 ] || echo -n ","
    first=0
    echo -n "$u"
  done
  echo -n "],"
  echo -n "\"pkgs\":["
  first=1
  for p in $PROTECT_PKGS; do
    [ $first = 1 ] || echo -n ","
    first=0
    LBL="$(pkg_label "$p")"
    echo -n "{\"pkg\":\"$p\",\"label\":\"$LBL\",\"detail\":["
    f2=1
    for u in $(discover_users); do
      pm path --user "$u" "$p" >/dev/null 2>&1 || continue
      [ $f2 = 1 ] || echo -n ","
      f2=0
      bucket=$(am get-standby-bucket --user "$u" "$p" 2>/dev/null)
      procs=$(ps -A 2>/dev/null | grep -c "$p")
      echo -n "{\"u\":$u,\"bucket\":\"$bucket\",\"procs\":$procs}"
    done
    echo -n "]}"
  done
  echo -n "],"
  echo -n "\"log\":\"$(tail -5 "$MODDIR/keepalive.log" 2>/dev/null | json_escape)\""
  echo "}"
}

do_apps() { # user type: 3=third-party, s=system ; output: pkg or pkg|AppLabel
  ensure_labels
  pm list packages -$2 --user "$1" 2>/dev/null | sed 's/package://' | sort > /data/local/tmp/.ka_p
  awk -F'|' 'NR==FNR{l[$1]=$2;next} {print $0 (l[$0]?("|"l[$0]):"")}' /data/local/tmp/.ka_l /data/local/tmp/.ka_p
}

do_icons() { # pkg... : dump real app icons to $MODDIR/icons via app_process tool
  local D="$MODDIR/icons"
  mkdir -p "$D"
  local NEED=""
  for p in "$@"; do
    [ -f "$D/$p.png" ] || NEED="$NEED $p"
  done
  [ -z "$NEED" ] && { echo DONE; return; }
  CLASSPATH="$MODDIR/tools/icedump.dex" app_process /system/bin/ Main "$D" $NEED >/dev/null 2>&1
  echo DONE
}

do_icon64() { # pkg... : output "pkg;base64png" per line (WebView AllowFileAccess=false, data URI is the only way)
  local f
  for p in "$@"; do
    f="$MODDIR/icons/$p.png"
    if [ -f "$f" ]; then
      printf '%s;' "$p"
      base64 "$f" 2>/dev/null | tr -d '\n'
      echo
    fi
  done
}

case "$1" in
  status) do_status ;;
  set) set_conf "$2" "$3" ;;
  addpkg) add_pkg "$2" ;;
  rmpkg) rm_pkg "$2" ;;
  apply) sh "$MODDIR/keepalive.sh" apply >/dev/null 2>&1 && echo OK ;;
  apps) do_apps "$2" "${3:-3}" ;;
  icons) shift; do_icons "$@" ;;
  icon64) shift; do_icon64 "$@" ;;
  users) discover_users ;;
  *) echo "usage: webui_ctl.sh status|set|addpkg|rmpkg|apply|apps <user> <3|s>" ;;
esac
