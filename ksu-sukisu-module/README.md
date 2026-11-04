# GKD Root／Zygisk 全局节点桥接模块

模块 ID 为 `gkd_ksu_sukisu`。安装包包含同一份 GKD APK、主用户保活脚本，以及 arm64-v8a／x86_64 的全局节点桥接。此目录的全局实现仍需以对应版本的构建、重启和真机记录确认，不以编译成功替代实测。

**当前正式版：[2.0.0](https://github.com/STC214/GKD_/releases/tag/v2.0.0)**，模块 versionCode `2026110402`，内嵌 APK `2.0.0 / 100`。配套成品位于仓库根目录 `dist/v2.0.0/`（本机归档），对外下载以 Release 附件为准。验证范围见 [发布说明](../docs/releases/v2.0.0.md) 与 Release 的 `verification-summary.md`。

## 生效模型

- 匹配与点击只在主用户（user 0）GKD 中执行；目标进程的桥接仅恢复／撤销节点连接，不运行第二套匹配引擎。
- 全局桥接通过已启用的 Zygisk 宿主加载，不需要逐个选择或批量批准 LSPosed 目标作用域，也不要求双开 GKD。
- 主用户及 XSpace／999 的常规应用默认进入全局策略。GKD 白名单、检测应用文件夹和内部保护包仍控制实际生效条件。系统 UID、隔离进程、共享 UID 等不明确身份的情形保持拒绝。
- 桥接不修改 HMA 隐藏预设，不伪造宿主的无障碍开关／服务列表，不通过截图或 OCR 识别广告。
- 目标进程只接受 Root 服务发出的短租约；停用、失联、到期或加入白名单时撤销本桥接拥有的节点连接。检测文件夹等保护包在进程加载前也会被排除；首次策略未就绪时不加载全局助手。
- 安装／更新全局 Native 模块后需要重启一次。Root 宿主的排除／卸载规则和不兼容的私有 API 布局仍可使目标不加载；失败不应破坏宿主原操作。

## 首次安装与升级

安装器使用 `pm install --user 0 -r` 安装内嵌 APK。没有内／外存设置文件的首次安装会完成主用户初始化：写入引导标记、授予写入安全设置权限，尝试通知与特殊前台服务权限，追加 GKD 无障碍服务并保留其他服务。GKD 自身的首次应用 Root 授权仍由 Root 管理器确认。

已有设置文件时不重写配置、不重新开启用户关闭的功能。普通 APK 安装没有模块引导标记，不会隐式获得上述首次配置。沿用已有签名覆盖安装，避免卸载 APK 清掉数据。

默认不再登记 999 用户的 GKD。`GKD_REGISTER_XSPACE_MODULE=1` 仅保留旧版兼容用途：在已有 999 用户中登记同一 APK，不启用该用户的无障碍，不授予该用户 Root，不启动匹配引擎。全局桥接不依赖此开关，旧登记不会被升级脚本擅自删除。

## 构建与打包

先构建应用 release APK，再构建全局助手。Windows 示例：

```powershell
$env:ANDROID_HOME = 'F:\Android\Sdk'
$env:JAVA_HOME = 'F:\Programer\Java\jdk-21'
.\ksu-sukisu-module\scripts\build-global-bridge.ps1
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

`build-global-bridge.ps1` 默认使用 NDK `28.2.13676358`，输出到 `global/build/`。打包脚本不隐式运行 Gradle／NDK，默认从 Gradle `output-metadata.json` 取得真实 APK 文件名；也支持 `-ApkPath`、`-ExpectedApkSha256` 和 `-GlobalBridgeDir`。

打包前校验 APK 结构、全局 DEX 文件头／长度和两个 ELF 的 64 位小端类型及 ABI；打包后重新验证内嵌 APK、DEX、SO 的 SHA-256 与精确条目清单。只有校验通过才替换旧 ZIP。模块版本沿用 `module/module.prop`，不按打包时间改写。

ZIP 应包含十二个文件：

```text
action.sh
config.conf
customize.sh
common/gkd.apk
common/global_bridge.dex
module.prop
post-fs-data.sh
service.sh
skip_mount
uninstall.sh
zygisk/arm64-v8a.so
zygisk/x86_64.so
```

Zygisk 公共头文件的原许可保留在 `global/native/zygisk.hpp`；全局源码和打包记录应与对应 APK 一起校验。

## 保活与配置

刷入后的配置文件为 `/data/adb/modules/gkd_ksu_sukisu/config.conf`：

```sh
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
GKD_REGISTER_XSPACE_MODULE=0
GKD_KEEP_ALIVE_INTERVAL=300
GKD_POLICY_REFRESH_INTERVAL=1800
GKD_LOG_MAX_BYTES=262144
```

包名与主用户固定，不接受其他用户作为匹配引擎。守护等待系统服务就绪；应用未安装时休眠，启动时持续退避，连续三秒确认进程稳定后进入低频巡检。状态通知只在用户已开启时恢复。后台 AppOps 和电池白名单按配置刷新；日志达到上限后保留近期内容。

`post-fs-data.sh` 在每次启动清除上次启动的就绪状态。Root 服务随后发布只含保护包名称的 `/data/adb/gkd_node_bridge/protected.v1`；目录 0700、文件 0600。该文件不是用户规则或配置文件。

模块执行按钮显示状态、配置及近期日志，并打开主用户 GKD。排障日志位于 `/data/adb/modules/gkd_ksu_sukisu/service.log`。不要将“进程存活”单独当作节点桥接和自动点击成功。

## 卸载与回退

卸载脚本只撤回本模块的后台保活策略、安装登记标记和生成的全局桥接保护缓存；不卸载 APK，不清应用数据，不删除用户规则／设置，不移除已有 999 用户登记。

Native 代码已加载的进程通常需要重启才能彻底卸载。全局试验版升级前应保存当前 APK、实际模块文件、配置和哈希，并使用对应验证记录中的回退步骤；不要用其他设备或其他版本的备份覆盖用户配置。

## 2.0 定制版分发与回归

“关于”中的本项目发布入口同时提供 APK 与 Root 模块。定制版不从上游普通 APK 渠道自动覆盖更新；在线规则更新独立，保持原设置和作者来源。上游项目及支持入口单独保留。

可选32位必须同时向构建、打包脚本传入 `-Include32Bit`；默认仍是12条目的双64位包。额外32位 ABI 仅有离线验证，不代表实机支持。

主机回归（无 ADB）：

```powershell
python ksu-sukisu-module/tests/test_bootstrap_lookup.py
python ksu-sukisu-module/tests/test_global_bridge_lifecycle.py --java-home $env:JAVA_HOME --repeat 30
```

后者使用 JVM Android 桩验证生命周期与失败关闭；6 个场景重复31轮，不替代 Android 私有 API 真机测试。