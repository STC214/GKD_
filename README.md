# gkd

<p align="center">
<a href="https://gkd.li/"><img src="https://e.gkd.li/2a0a7787-f2dd-4529-a885-93f3b8c857c3" alt="GKD.LI" width="40%" /></a>
</p>

基于 [高级选择器](https://gkd.li/guide/selector) + [订阅规则](https://gkd.li/guide/subscription) + [快照审查](https://github.com/gkd-kit/inspect) 的自定义屏幕点击 Android 应用

通过自定义规则，在指定界面，满足指定条件(如屏幕上存在特定文字)时，点击特定的节点或位置或执行其他操作

- **快捷操作**

  帮助你简化一些重复的流程, 如某些软件自动确认电脑登录

- **跳过流程**

  某些软件可能在启动时存在一些烦人的流程, 这个软件可以帮助你点击跳过这个流程

## 免责声明

**本项目遵循 [GPL-3.0](/LICENSE) 开源，项目仅供学习交流，禁止用于商业或非法用途**

## 安装

<a href="https://gkd.li/guide/"><img src="https://e.gkd.li/f23b704d-d781-494b-9719-393f95683b89" alt="Download from GKD.LI" width="32%" /></a><a href="https://play.google.com/store/apps/details?id=li.songe.gkd"><img src="https://e.gkd.li/f63fabeb-0342-4961-a46d-cac61b0f8856" alt="Download from Google Play" width="32%" /></a><a href="https://github.com/gkd-kit/gkd/releases"><img src="https://e.gkd.li/c1ef2bb9-7472-46d5-9806-81b4c37e5b4d" alt="Download from GitHub releases" width="32%" /></a>

如遇问题请先查看 [疑难解答](https://gkd.li/guide/faq)

## KernelSU / SukiSU Ultra 模块

本 fork 额外提供一个纯保活模块：GKD 需要由用户单独安装，模块不内置、不安装、不更新也不卸载 APK，不自动授予 App 权限或开启无障碍。它只为指定 Android 用户下已安装的 GKD 设置后台运行 AppOps 和电池白名单、开机后台启动，并以默认 5 分钟间隔巡检进程。模块依据同一用户下 GKD 保存的常驻通知开关决定是否恢复该服务，用户主动关闭时不会被反复拉起。

Android 工程现已直接位于仓库根目录，不再使用额外的 `GKD_/` 嵌套目录。`app/`、`gradle/`、`selector/`、`hidden_api/` 和 `ksu-sukisu-module/` 均可从仓库根目录直接访问，本文后续命令也都以仓库根目录为当前目录。

模块源码位于 [`ksu-sukisu-module/`](ksu-sukisu-module/)，在 Windows 下可从仓库根目录执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

生成的可刷入模块包位于：

```text
ksu-sukisu-module\dist\gkd-ksu-sukisu-module.zip
```

模块版本号使用打包时的分钟级时间戳；目标 Android 用户、守护间隔、后台策略刷新周期、日志上限和保活开关均可配置。移除模块时恢复模块设置的后台 AppOps 并清理电池白名单，GKD App、权限和数据保持不变。刷入后如需调整，可编辑：

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

打包过程不运行 Gradle，ZIP 内不包含 APK，并会在生成后硬校验不存在 `.apk` 条目。详细配置和日志说明见 [`ksu-sukisu-module/README.md`](ksu-sukisu-module/README.md)。

## 项目目录

```text
app/                    GKD Android 应用
docs/                   本 fork 的设计与实施文档
gradle/                 Gradle Wrapper 配置
hidden_api/             Android 隐藏 API 声明
selector/               GKD 选择器模块
ksu-sukisu-module/      KernelSU/SukiSU 纯保活模块
local-assets/           本机辅助资料，不纳入 Git
  packages/             LSPosed、Sui 等第三方安装包
  device/               设备专用配置
  diagnostics/          真机诊断日志
```

`local-assets/` 只用于本机排障和保存外部依赖，不参与 Android 构建或模块打包。Gradle 生成目录及 `ksu-sukisu-module/dist/`、`work/` 同样保持 Git 忽略。

## 开发路线

后续开发统一使用以下文档：

- [`docs/current-progress.md`](docs/current-progress.md)：汇总当前已完成阶段、构建与真机状态、未提交工作区内容和下一步任务。
- [`docs/root-runtime-refactor-plan.md`](docs/root-runtime-refactor-plan.md)：规定窗口判断、规则调度、动作执行和 APK 内置 root 服务的实施顺序、验收标准与回滚点。
- [`docs/upstream-code-delta-guide.md`](docs/upstream-code-delta-guide.md)：逐项记录本 fork 与官方 GKD 的代码级差异、行为不变量、冲突风险和上游更新后的迁移方法。
- [`docs/testing/root-runtime-baseline.md`](docs/testing/root-runtime-baseline.md)：记录阶段 0 的 APK、签名、真实规则复现集、固定窗口场景和订阅兼容验收结果。

每个开发阶段都必须同步更新代码差异文档；代码完成但差异记录未更新时，不得将该阶段标记为完成。两份文档共同要求保持现有规则格式、选择器语法、订阅更新、数据库和备份兼容。

当前进度：阶段 0.5～7 已完成。APK 已固定 libsu 6.0.0，并把坐标动作接入显式 `APK RootService → Shizuku/Sui → 无障碍手势` 顺序。Android 16 真机已通过协议 2 常规连接、无 Shizuku 完整规则链、有界自动重连、开关停连、拒绝/8 秒无回调、撤权后进程死亡收口、无障碍回退、保留数据更新和重新授权恢复。SukiSU 外部撤权不会追溯结束既有 UID 0 进程，因此界面明确提示：需要立即撤权时先关闭 GKD 的 Root 增强开关。规则格式、订阅更新、数据库和备份结构均未改变。

最新停点已经记录在 `docs/current-progress.md` 的“本轮开发停点”；阶段 7 不再重复，后续从阶段 8 的多用户、多显示屏与厂商窗口适配开始。

## 截图

|                                                               |                                                               |                                                               |                                                               |
| ------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------- | ------------------------------------------------------------- |
| ![img](https://e.gkd.li/1e8934c1-2303-4182-9ef2-ad4c46882570) | ![img](https://e.gkd.li/01f230d7-9b89-4314-b573-38bd233d22f9) | ![img](https://e.gkd.li/dfa0a782-b21e-473a-96e4-eef27773b71b) | ![img](https://e.gkd.li/641decd1-2e60-4e95-b78c-df38d1d98a4d) |
| ![img](https://e.gkd.li/b216b703-d3de-4798-81ba-29e0ae63264f) | ![img](https://e.gkd.li/76c25ac9-4189-47cd-b40b-b9e72c79b584) | ![img](https://e.gkd.li/7288502e-808b-4d9a-88b5-1085abaa0d46) | ![img](https://e.gkd.li/aa974940-7773-409a-ae84-3c02fee9c770) |

## 订阅

GKD **默认不提供规则**，需自行添加本地规则，或者通过订阅链接的方式获取远程规则

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

## 捐赠

如果 GKD 对你有用, 可以通过以下链接支持该项目

<https://github.com/lisonge/sponsor>

或前往 [Google Play](https://play.google.com/store/apps/details?id=li.songe.gkd) 给个好评

## Star History

[![Stargazers over time](https://starchart.cc/gkd-kit/gkd.svg?variant=adaptive)](https://starchart.cc/gkd-kit/gkd)
