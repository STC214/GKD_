# SukiSU / KernelSU bundle + latest LSPosed APK — 2026-11-02

此版本把已验证的最新 GKD APK（含圆角图标及内置 LSPosed 模块）打入原有 SukiSU / KernelSU 安装与保活模块。

- 模块 ID 保持 `gkd_ksu_sukisu`，可覆盖升级旧模块，无需先卸载。
- SukiSU 模块负责安装 APK、开机启动、后台保活；内置 LSPosed 模块负责节点连接修复。
- LSPosed 节点修复仍仅对水印相机 `com.tencent.zebra` 试运行；仍需单独在 LSPosed 中启用 GKD 并配置作用域，刷 ZIP 不会自动启用 LSPosed。
- 白名单与检测器文件夹排除策略保持不变；没有增加截图/OCR方案。
- 安装器、保活与卸载行为保持原样。注意：卸载此模块会联动卸载 GKD，可能丢失数据，请优先覆盖升级。
- APK versionCode 仍为 92，签名与前一试验版相同（本地 Android Debug 证书，非上游生产签名）。

## 验证

ZIP 完整性通过；仅含一个 `common/gkd.apk`；其 SHA-256 与已发布、已在手机上验证的 APK 完全一致：

`6cf11d029f6ddca4bec8bf06c79072ed0bec73923ec9edb2cd2663ab9c7078f1`

已在连接手机上使用 Android `sh -n` 检查四个打包脚本语法；新增打包哈希校验，错误输入会被拒绝。本轮没有刷入模块或重启手机，不声称完成重启后的保活实测。

## 可复现打包

从本次 Release 下载 APK 后执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1 -ApkPath .\gkd-node-bridge-icon.apk -ExpectedApkSha256 6cf11d029f6ddca4bec8bf06c79072ed0bec73923ec9edb2cd2663ab9c7078f1
```

模块版本号由打包时间生成。ZIP 和 APK 的校验值见随 Release 发布的 `SHA256SUMS.txt`。
