# GKD Global Bridge

**文档导航：** [完整索引](docs/README.md) · [当前架构与维护契约](docs/global-bridge-runtime.md) · [2.0 验证矩阵](docs/testing/global200-release.md)。历史计划与旧发布记录已逐份标注适用版本。

基于 [上游 GKD](https://github.com/gkd-kit/gkd) 的 Android 自动点击定制版，保留高级选择器、订阅规则与原有数据兼容性，增加 Root／Zygisk 全局节点桥接和主用户保活。

**当前正式版：[2.0.1（versionCode 101）](https://github.com/STC214/GKD_/releases/tag/v2.0.1)** · [发布说明](docs/releases/v2.0.1.md) · [全部发布](https://github.com/STC214/GKD_/releases)

## 主要功能

- 普通应用通过全局 Zygisk 桥接进入 GKD 策略，不需要逐个勾选 LSPosed 作用域；GKD 白名单、检测应用文件夹和内部保护包控制实际生效范围。
- 主用户（user 0）与 XSpace／999 共用主用户的一套匹配引擎，不要求双开 GKD。系统、隔离、共享 UID 等身份不明确的进程保持排除。
- 基于系统节点和规则匹配执行点击，不依赖截图／OCR，不修改 HMA 隐藏预设。目标没有可用节点或规则不匹配时不保证跳过。
- 默认含本地公告规则和在线订阅快照；在线规则仍可更新，升级保留用户规则、配置及白名单。
- 模块支持首次安装初始化、有界 Root 授权等待、开机解锁后自动启动与低频保活；已有配置不会被安装脚本重新开启或重写。

## 安装与更新

从本仓库 [Release](https://github.com/STC214/GKD_/releases/tag/v2.0.1) 获取同版本配套产物：

| 文件 | 用途 |
| --- | --- |
| `gkd-2.0.1.apk` | GKD 定制版应用，版本 2.0.1／101 |
| `gkd-ksu-2.0.1.zip` | 内嵌同一 APK 的 KernelSU／SukiSU 模块，版本 2.0.1／2026110403 |
| `SHA256SUMS.txt` | 下载后完整性核对 |
| `verification-summary.md`、`release.diff` | 验证摘要与相对上一公开版的源码差异 |

1. 准备可工作的 KernelSU／SukiSU Root 环境及已启用的兼容 Zygisk 宿主。
2. 在 Root 管理器安装配套模块 ZIP；安装器会安装／覆盖更新主用户 APK。升级前建议备份，不要先卸载 GKD。
3. 按宿主要求重启并解锁一次。首次安装时，在 Root 管理器批准 GKD 自身的 Root 请求；这次确认仍需用户完成。
4. 首次模块安装在没有内／外存配置时初始化 Root＋无障碍路线，并保留其他无障碍服务。已有配置保持原样；沿用 Shizuku 自动化模式的用户仍需保留原 Shizuku 授权。
5. 在 GKD 中管理规则与白名单。全局路线不要求开启 GKD 的 LSPosed 模块、不要求逐个选择目标应用，也不要求安装第二套 GKD 引擎。

只安装 APK 不等同于完成全局模块安装，也不会隐式完成模块首次初始化。普通 Root 授权与 Zygisk 宿主可用性是两个不同条件。详细配置、日志、卸载与回退见 [模块 README](ksu-sukisu-module/README.md)。

定制版的更新入口指向本项目发布页，不从上游普通 APK 渠道自动覆盖更新；在线规则更新独立、不受影响。卸载本模块不卸载 GKD APK、不清用户数据；已经加载的 Native 桥接需按宿主要求重启才能卸载。

## 验证状态与边界

2.0.1修正自适应彩色／主题图标过大留白，启动页使用独立比例；不改原PNG、通知向量、Root或Native。预览101已完成实际桌面目视与100↔101回退；正式标签构建、部署及附件校验以[2.0.1发布说明](docs/releases/v2.0.1.md)和Release验证摘要为准。主题单色／启动页逐资源日夜真机渲染未完成，不把静态检查当作实测通过。

以下保留2.0.0功能基线（非2.0.1重新执行）：

2.0.0 干净标签构建通过 255 项 JVM 测试；Lint 0 错误、58 项警告。主机生命周期 6 个场景各执行 31 轮，安装器服务查找 7 个场景通过；Android 17 ARM64 实机完成 `99 → 100 → 99 → 100` 升级／回退，三个阶段均验证主用户和 999 分身的节点自动点击，配置、规则、白名单和 HMA 保持不变。

2.0 Native DEX／SO 与已完成重启自动开工、白名单即时切换、真实开屏及“朕知道了”公告点击验证的 v99 字节一致；这些属于 v99 功能基线，2.0 未重复重启或真实公告测试。完整范围见 [Release 验证摘要](https://github.com/STC214/GKD_/releases/download/v2.0.0/verification-summary.md)。

默认包含 arm64-v8a／x86_64 双 64 位载荷；x86_64 仅构建及 ELF 结构验证，可选 32 位不在本次默认包中，未作实机支持承诺。全局路线仅承诺已验证的 user0／999 常规应用范围，不代表全部 Android 用户、厂商 ROM 或应用界面都能处理。

## 开发与产物目录

Android 工程直接位于仓库根目录。使用 JDK 21、Android SDK 和项目配置的 NDK／Gradle 环境；先构建 APK，再构建桥接和打包（详细环境与可选 ABI 参数见模块 README）：

```powershell
.\gradlew.bat :app:testGkdDebugUnitTest :app:lintGkdRelease :selector:jvmTest :app:assembleGkdRelease
.\ksu-sukisu-module\scripts\build-global-bridge.ps1
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

打包脚本默认从 Gradle `output-metadata.json` 读取 APK 文件，输出到 `ksu-sukisu-module/dist/`；可通过 `-ApkPath`、`-ExpectedApkSha256`、`-OutputPath` 显式指定。模块版本取自 `module/module.prop`，不随打包时间自动改写。

发布成品单独归档到根目录 `dist/v2.0.1/`，包含 APK、模块 ZIP、校验和、差异与验证摘要；GitHub 下载以 Release 附件为准。`dist/`、`local-assets/`、模块 `work/`／`global/build/` 和构建缓存不纳入 Git。私人备份、第三方 APK 与真机原始日志不上传 Release。

```text
app/                    Android 应用
selector/               高级选择器
hidden_api/             Android 隐藏 API 声明
ksu-sukisu-module/      Root 保活与 Zygisk 全局桥接
docs/                   设计、历史实施记录与发布说明
dist/v2.0.1/            本机已验证发布成品（Git 忽略）
local-assets/           私人备份与诊断证据（Git 忽略）
```

[当前进度索引](docs/current-progress.md) 保留历史记录；当前公开基线以 [2.0.1 发布说明](docs/releases/v2.0.1.md) 和 Release 验证摘要为准，不把早期阶段表当作最新停点。

## 来源与许可

本项目基于上游 [GKD](https://github.com/gkd-kit/gkd)，按仓库 [GPL-3.0 许可证](LICENSE) 分发；保留上游来源、许可证与规则作者元数据。选择器、订阅格式和基础使用方法可参考 [上游文档](https://gkd.li/guide/)。

## 上游界面示例

以下为上游历史截图，定制版图标、设置和版本信息以当前安装包为准。

|                                                               |                                                               |                                                               |                                                               |
| ------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------- |
| ![img](https://e.gkd.li/1e8934c1-2303-4182-9ef2-ad4c46882570) | ![img](https://e.gkd.li/01f230d7-9b89-4314-b573-38bd233d22f9) | ![img](https://e.gkd.li/dfa0a782-b21e-473a-96e4-eef27773b71b) | ![img](https://e.gkd.li/641decd1-2e60-4e95-b78c-df38d1d98a4d) |
| ![img](https://e.gkd.li/b216b703-d3de-4798-81ba-29e0ae63264f) | ![img](https://e.gkd.li/76c25ac9-4189-47cd-b40b-b9e72c79b584) | ![img](https://e.gkd.li/7288502e-808b-4d9a-88b5-1085abaa0d46) | ![img](https://e.gkd.li/aa974940-7773-409a-ae84-3c02fee9c770) |

## 订阅

本定制版默认包含本地规则与既有在线订阅快照：`667/v575`，973 个应用、3 个全局组，另含本地公告规则。保留规则作者元数据，支持后续在线订阅更新和自定义规则；已有用户升级时不覆盖其设置。规则存在不代表每个界面必定匹配。

也可通过 [subscription-template](https://github.com/gkd-kit/subscription-template) 快速构建自己的远程订阅

第三方订阅列表可在 <https://github.com/topics/gkd-subscription> 查看

要加入此列表, 需点击仓库主页右上角设置图标后在 Topics 中添加 `gkd-subscription`

<details>
<summary>示例图片 - 添加至 Topics (点击展开)</summary>

![image](https://e.gkd.li/9e340459-254f-4ca0-8a44-cc823069e5a7)

</details>

## 选择器

一个类似 CSS 选择器的选择器, 能联系节点上下文信息, 更容易也更精确找到目标节点

<https://gkd.li/guide/selector>

[@[vid=\"menu\"] < [vid=\"menu_container\"] - [vid=\"dot_text_layout\"] > [text^=\"广告\"]](https://i.gkd.li/i/14881985?gkd=QFt2aWQ9Im1lbnUiXSA8IFt2aWQ9Im1lbnVfY29udGFpbmVyIl0gLSBbdmlkPSJkb3RfdGV4dF9sYXlvdXQiXSA-IFt0ZXh0Xj0i5bm_5ZGKIl0)

<details>
<summary>示例图片 - 选择器路径视图 (点击展开)</summary>

[![image](https://e.gkd.li/a2ae667b-b8c5-4556-a816-37743347b972)](https://i.gkd.li/i/14881985?gkd=QFt2aWQ9Im1lbnUiXSA8IFt2aWQ9Im1lbnVfY29udGFpbmVyIl0gLSBbdmlkPSJkb3RfdGV4dF9sYXlvdXQiXSA-IFt0ZXh0Xj0i5bm_5ZGKIl0)

</details>

## 支持上游

本项目基于上游 GKD，以下链接用于支持上游作者，并非本 fork 的收款入口。

<https://github.com/lisonge/sponsor>

上游普通版另见 [gkd-kit/gkd](https://github.com/gkd-kit/gkd)；安装本定制版请使用本仓库 Release，避免覆盖掉配套桥接功能。

## Star History

[![Stargazers over time](https://starchart.cc/STC214/GKD_.svg?variant=adaptive)](https://starchart.cc/STC214/GKD_)
