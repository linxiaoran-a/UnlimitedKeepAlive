#!/system/bin/sh
# ============================================================
#  UnlimitedKeepAlive 配置文件
#  改完保存后执行:  sh /data/adb/modules/unlimited_keepalive/keepalive.sh apply
#  (或重启手机, 开机自动生效)
# ============================================================

# 受保护的应用包名, 空格分隔, 想加微信就往里追加 com.tencent.mm
PROTECT_PKGS="com.eg.android.AlipayGphone"

# 1 = 自动发现并保护所有用户(含主空间和全部多开分身, 新建分身自动纳入)
# 0 = 只保护下面 USERS 列出的用户
ALL_USERS=1

# ALL_USERS=0 时生效; 0 是主空间, 分身 ID 用 "pm list users" 查
USERS="0"

# 1 = 冻结 PowerKeeper (MIUI杀后台主凶, 省电策略会失效, 待机耗电略增)
# 0 = 不冻结 (只靠白名单/豁免桶保活, 效果弱一些)
FREEZE_POWERKEEPER=1

# 周期重刷间隔(秒): 豁免桶可能被系统回收, 定期补刷; 0 = 只在开机时应用一次
WATCH_INTERVAL=300
