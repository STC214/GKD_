# GKD KernelSU / SukiSU Ultra 模块

这个目录提供一个轻量模块包装：GKD 仍然作为普通 Android App 运行，KernelSU/SukiSU Ultra 模块只负责 root 侧安装、授权、保活白名单和启动辅助。

模块不改写 GKD 的无障碍规则引擎，也不是 LSPosed 重写版。

## 文件结构

- `module/module.prop`：模块元信息。
- `module/customize.sh`：刷入时校验内置 APK，并设置脚本权限。
- `module/service.sh`：开机后安装 APK、授予权限、加入电池白名单并启动 GKD。
- `module/action.sh`：模块管理器动作按钮，点击后打开 GKD。
- `module/uninstall.sh`：移除模块时清理电池白名单，可选卸载 GKD。
- `module/config.conf`：运行时配置。
- `module/skip_mount`：声明此模块不 overlay 系统文件。
- `scripts/package-ksu-module.ps1`：Windows 打包脚本。

## 打包

在仓库根目录执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

脚本会先构建 GKD APK：

```powershell
.\gradlew.bat app:assembleGkdRelease -PGKD_RENAME_APK_FLAG=1
```

然后生成模块包：

```text
ksu-sukisu-module\dist\gkd-ksu-sukisu-module.zip
```

也可以使用现有 APK 打包：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1 -ApkPath .\app\build\outputs\apk\gkd\release\app-gkd-release.apk
```

## 配置

刷入后可编辑：

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

默认值：

```sh
GKD_AUTO_INSTALL=1
GKD_AUTO_GRANT=1
GKD_AUTO_ENABLE_A11Y=0
GKD_AUTO_START=1
GKD_REQUIRE_BUNDLED_APK=0
GKD_UNINSTALL_ON_REMOVE=0
```

`GKD_AUTO_ENABLE_A11Y=1` 会写入 Android secure settings 来启用 GKD 无障碍服务。此行为较强，且 ROM 兼容性不同，所以默认关闭。

`GKD_REQUIRE_BUNDLED_APK=0` 表示如果模块内 APK 因签名不一致等原因无法覆盖安装，会继续给已安装的 GKD 授权/启动。设为 `1` 时，模块内 APK 安装失败就停止后续动作。

`GKD_UNINSTALL_ON_REMOVE=0` 表示移除模块时不会卸载 GKD，也不会删除 GKD 数据。设为 `1` 时，移除模块会尝试卸载 user 0 下的 GKD。

## 已安装 GKD 的处理

如果已安装 GKD 与模块内 APK 签名一致，不需要先卸载。模块使用 `pm install -r -d` 覆盖安装，并保留 App 数据。

如果 Android 报告更新失败、签名不兼容，说明已安装 GKD 与模块内 APK 不是同一签名。此时应先在 GKD 内导出/备份配置，再卸载现有 App，或者用同签名 APK 重新打包模块。
