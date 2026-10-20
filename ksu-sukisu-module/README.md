# GKD KernelSU / SukiSU Ultra 内置应用模块

这个目录提供一个内置 GKD APK 的模块。刷入时会为配置的 Android 用户（默认 user 0）安装 `common/gkd.apk`；卸载模块时移除同一用户下的 GKD。若设备上已有 GKD，安装器通过 `pm install -r` 执行就地更新。模块不会自动授予 App 权限或开启无障碍。

模块只做以下事情：

- 开机后为已安装的 GKD 应用后台运行 AppOps 和电池白名单；
- 在不显示 App 界面的情况下请求启动 `ExposeService`；
- 已安装但未运行时持续退避重试，直到连续 3 秒确认目标用户下的 GKD 进程稳定存在；
- 默认每 5 分钟检查一次 GKD 进程和用户已开启的常驻通知服务；
- 被 ROM 清理后进行低频恢复；
- 在模块管理器执行页显示状态、配置并打开 GKD。

模块 ID 仍为 `gkd_ksu_sukisu`，可直接覆盖旧版安装/授权模块。升级后旧的自动安装、授权、自动无障碍和卸载 App 配置不再生效。

## 文件结构

- `module/module.prop`：模块元信息；打包时写入分钟级版本号。
- `module/customize.sh`：安装内置 APK、校验安装结果并设置脚本权限。
- `module/service.sh`：开机启动、后台策略和低频保活守护。
- `module/action.sh`：显示状态和日志，并打开已安装的 GKD。
- `module/uninstall.sh`：移除模块时卸载 GKD，并恢复后台 AppOps、清理电池白名单。
- `module/config.conf`：保活配置。
- `module/skip_mount`：声明不 overlay 系统文件。
- `scripts/package-ksu-module.ps1`：Windows 打包脚本，默认取用 release APK，也支持 `-ApkPath`。

ZIP 必须包含且只包含 `common/gkd.apk` 这一 APK 条目；打包脚本会对此进行硬校验。

## 打包前置条件

先构建项目的 GKD release APK，默认路径为 `app/build/outputs/apk/gkd/release/app-gkd-release.apk`。也可使用 `-ApkPath <路径>` 指定另一个 APK；包名须为 `li.songe.gkd`，签名须与设备上已有版本兼容。

通知权限、无障碍、Root 增强和其他业务权限均由用户在 GKD 或系统设置中管理。

## 打包

在仓库根目录执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

生成：

```text
ksu-sukisu-module\dist\gkd-ksu-sukisu-module.zip
```

打包脚本本身不运行 Gradle；它把 release APK 放入模块 ZIP 的 `common/gkd.apk`。

## 配置

刷入后可编辑：

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

默认值：

```sh
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
GKD_KEEP_ALIVE_INTERVAL=300
GKD_POLICY_REFRESH_INTERVAL=1800
GKD_LOG_MAX_BYTES=262144
```

- `GKD_USER_ID=0`：指定需要保活的 Android 用户；包检测、进程 UID、服务启动、AppOps 和设置读取均使用同一用户。
- 模块固定执行启动与保活，不提供关闭核心职责的配置项。开机完成后会持续等待 PackageManager 和 ActivityManager 就绪；目标用户未安装 GKD 时休眠，已安装则持续退避拉起，连续 3 秒确认进程稳定后才进入保活循环。
- `GKD_KEEP_ALIVE_INTERVAL=300`：未安装休眠和已启动巡检的间隔秒数，限制为 60～86400。
- `GKD_POLICY_REFRESH_INTERVAL=1800`：重新应用后台 AppOps 和电池白名单的间隔，限制为 300～604800。
- `GKD_LOG_MAX_BYTES=262144`：日志超过该大小后保留最近约 1000 行，限制为 4096～10485760；非法值回退为 262144。

守护读取 GKD 保存的 `enableStatusService` 设置，只在用户已开启常驻通知时恢复对应服务。读取失败时只维持已经运行的服务，不覆盖用户在 App 内主动关闭的状态。

## 模块动作按钮

在 KernelSU 或 SukiSU Ultra 中点击模块“执行”时会显示：

- 模块路径、目标包名和 App 安装状态；
- 当前保活配置；
- 打开 GKD 的命令结果；
- 最近 20 行 `service.log`。

如果 GKD 尚未安装，执行页会提示对应 Android 用户重新安装模块以恢复内置 App。

## 卸载行为

移除模块时把模块设置的两个后台 AppOps 恢复为系统默认、删除模块添加的电池白名单，并卸载安装时记录的 Android 用户下的 GKD。GKD 应用数据按 Android 系统卸载行为处理；其他用户下的实例不受影响。

## 日志排障

查看日志：

```sh
su -c "tail -120 /data/adb/modules/gkd_ksu_sukisu/service.log"
```

常见提示：

- `is not installed`：检查模块安装时的 APK 安装结果和目标 Android 用户；模块会低频等待并重新检查。
- `start expose service failed`：检查 GKD 是否被系统限制启动，以及当前 APK 是否仍包含 `.service.ExposeService`。
- `startup not confirmed`：启动尚未稳定确认；模块使用从 2 秒递增、最高 30 秒的退避持续重试，直到进程连续存在 3 秒或 App 被卸载。
- `appops ... failed`：厂商 ROM 不支持对应 AppOp 或系统服务尚未就绪；守护会在下一个策略刷新周期重试。

本模块不管理 Sui/Shizuku。相关授权失败应在 GKD、Sui 或 Shizuku 自身日志中排查。
