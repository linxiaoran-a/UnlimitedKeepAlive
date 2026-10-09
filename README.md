# UnlimitedKeepAlive

> 作者：林小冉 ｜ 测试设备：Redmi K70 ｜ 系统：澎湃OS 3.x（Android 16）

应用后台保活（主空间 + 全部分身）—— 治本方案：不是"被杀后复活"，而是让系统不再杀。

## 功能

- **可选冻结 PowerKeeper**（MIUI/HyperOS 杀后台主凶，App 内一键开关）
- 应用保护清单，**主空间与每个分身独立生效**，新建分身自动纳入：
  - doze 白名单
  - EXEMPTED 待机豁免桶
  - `RUN_ANY_IN_BACKGROUND` / `START_FOREGROUND` / `WAKE_LOCK` appops 放行
- 周期补刷守护（默认 5 分钟，可调）：豁免状态被系统回收后自动恢复
- **原生设置界面**：勾选即保护（真实应用图标 + 应用名 + 搜索 + 用户应用/系统应用双列表 + 分身空间切换），所有修改即保存即生效
- 桌面图标可显示/隐藏（默认隐藏），从 KSU 管理器 → 模块 → 操作按钮进入

## 使用

1. 需要 KernelSU / SukiSU / APatch 等支持 KSU 模块规范的 root；
2. KSU 管理器刷入模块 zip；
3. 安装 `UnlimitedKeepAlive.apk`（从 [Releases](../../releases) 下载）；
4. KSU 管理器 → 模块 → UnlimitedKeepAlive → 操作按钮（⚙️）进入设置界面，勾选要保护的应用即可。

## 原理简述

MIUI/HyperOS 由 PowerKeeper 在锁屏后执行 `clean_up_mem` 清理缓存态应用（整 uid force-stop），进程内的保活插件无法拦截。本模块从源头处理：冻结执行者 + 逐用户豁免目标应用，三层防护叠加周期补刷。

## License

[MIT](LICENSE)

