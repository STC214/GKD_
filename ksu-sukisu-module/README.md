# GKD KernelSU / SukiSU Ultra 纯保活模块

这个目录提供一个纯保活辅助模块。GKD 必须由用户单独安装并继续作为普通 Android App 运行；模块不内置 APK，不安装、更新或卸载 GKD，不修改业务设置，也不会自动授予 App 权限或开启无障碍。

模块只做以下事情：

- 开机后为已安装的 GKD 应用后台运行 AppOps 和电池白名单；
- 在不显示 App 界面的情况下请求启动 `ExposeService`；
- 默认每 5 分钟检查一次 GKD 进程和用户已开启的常驻通知服务；
- 被 ROM 清理后进行低频恢复；
- 在模块管理器执行页显示状态、配置并打开 GKD。

模块 ID 仍为 `gkd_ksu_sukisu`，可直接覆盖旧版安装/授权模块。升级后旧的自动安装、授权、自动无障碍和卸载 App 配置不再生效。

## 文件结构

- `module/module.prop`：模块元信息；打包时写入分钟级版本号。
- `module/customize.sh`：提示纯保活边界并设置脚本权限。
- `module/service.sh`：开机启动、后台策略和低频保活守护。
- `module/action.sh`：显示状态和日志，并打开已安装的 GKD。
- `module/uninstall.sh`：移除模块时恢复后台 AppOps 并清理电池白名单，不卸载 GKD。
- `module/config.conf`：保活配置。
- `module/skip_mount`：声明不 overlay 系统文件。
- `scripts/package-ksu-module.ps1`：Windows 打包脚本。

ZIP 中不得出现 `.apk` 文件或 `common/gkd.apk`。打包脚本会对此进行硬校验。

## 前置条件

刷入模块前，请自行安装包名为 `li.songe.gkd` 的 GKD。模块检测不到该包时只写日志并等待，不会下载或安装任何内容。

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

打包过程不运行 Gradle，也不需要 `-ApkPath` 或 `-SkipBuild`。

## 配置

刷入后可编辑：

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

默认值：

```sh
GKD_PACKAGE=li.songe.gkd
GKD_USER_ID=0
GKD_AUTO_START=1
GKD_KEEP_ALIVE=1
GKD_KEEP_ALIVE_INTERVAL=300
GKD_POLICY_REFRESH_INTERVAL=1800
GKD_LOG_MAX_BYTES=262144
```

- `GKD_USER_ID=0`：指定需要保活的 Android 用户；包检测、进程 UID、服务启动、AppOps 和设置读取均使用同一用户。
- `GKD_AUTO_START=1`：开机后后台启动已安装的 GKD，不弹出界面。
- `GKD_KEEP_ALIVE=1`：启用低频进程与常驻通知服务恢复。
- `GKD_KEEP_ALIVE_INTERVAL=300`：巡检间隔秒数；低于 60 时按 60 处理。
- `GKD_POLICY_REFRESH_INTERVAL=1800`：重新应用后台 AppOps 和电池白名单的间隔；低于 300 时按 300 处理。
- `GKD_LOG_MAX_BYTES=262144`：日志超过该大小后保留最近约 1000 行；非法值回退为 262144，低于 4096 时按 4096 处理。

守护读取 GKD 保存的 `enableStatusService` 设置，只在用户已开启常驻通知时恢复对应服务。无法读取设置时只维持已经运行的服务，不会覆盖用户在 App 内主动关闭的状态。

## 模块动作按钮

在 KernelSU 或 SukiSU Ultra 中点击模块“执行”时会显示：

- 模块路径、目标包名和 App 安装状态；
- 当前保活配置；
- 打开 GKD 的命令结果；
- 最近 20 行 `service.log`。

如果 GKD 尚未安装，执行页会明确提示用户单独安装并退出，不会尝试安装 APK。

## 卸载行为

移除模块时把模块设置的两个后台 AppOps 恢复为系统默认，并删除模块添加的电池白名单。GKD App、App 数据、无障碍设置和 App 权限均保持不变。

## 日志排障

查看日志：

```sh
su -c "tail -120 /data/adb/modules/gkd_ksu_sukisu/service.log"
```

常见提示：

- `is not installed`：需要用户单独安装 GKD；模块会继续低频等待。
- `start expose service failed`：检查 GKD 是否被系统限制启动，以及当前 APK 是否仍包含 `.service.ExposeService`。
- `appops ... failed`：厂商 ROM 不支持对应 AppOp 或系统服务尚未就绪；守护会在下一个策略刷新周期重试。

本模块不管理 Sui/Shizuku。相关授权失败应在 GKD、Sui 或 Shizuku 自身日志中排查。
