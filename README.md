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

本 fork 额外提供一个轻量模块包装：保留 GKD 作为普通 Android App，由 KernelSU/SukiSU Ultra 模块在开机后完成安装、授权、保活白名单和后台启动辅助，并以默认 5 分钟间隔巡检进程。模块依据 GKD 自身保存的常驻通知开关决定是否恢复该服务，用户主动关闭时不会被反复拉起。开机和守护启动不会主动弹出 GKD 界面；在模块管理器中点击“执行”会打开 GKD，并在执行页显示基础状态和最近的服务日志。

Android 工程现已直接位于仓库根目录，不再使用额外的 `GKD_/` 嵌套目录。`app/`、`gradle/`、`selector/`、`hidden_api/` 和 `ksu-sukisu-module/` 均可从仓库根目录直接访问，本文后续命令也都以仓库根目录为当前目录。

模块源码位于 [`ksu-sukisu-module/`](ksu-sukisu-module/)，在 Windows 下可从仓库根目录执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

生成的可刷入模块包位于：

```text
ksu-sukisu-module\dist\gkd-ksu-sukisu-module.zip
```

模块版本号使用打包时的分钟级时间戳。默认配置不会自动开启无障碍；守护间隔、定期重授权和保活开关均可配置。移除模块时会同时尝试卸载 user 0 下的 GKD App；卸载本模块之前，请确保已经备份 GKD 设置。刷入后如需调整，可编辑：

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

如果手机上已安装的 GKD 与模块内 APK 签名一致，不需要先卸载，模块会覆盖安装并保留数据。如果签名不一致，请先在 GKD 内导出/备份配置，再卸载现有 App，或改用同签名 APK 重新打包模块。

如需使用 GKD 的 Shizuku 相关能力，请先确保 Sui/Shizuku 本身已正常运行。若 GKD 提示 Shizuku 授权失败，可参考 [`ksu-sukisu-module/README.md`](ksu-sukisu-module/README.md) 的排障章节检查 Sui 模块是否安装完整。

## 项目目录

```text
app/                    GKD Android 应用
docs/                   本 fork 的设计与实施文档
gradle/                 Gradle Wrapper 配置
hidden_api/             Android 隐藏 API 声明
selector/               GKD 选择器模块
ksu-sukisu-module/      KernelSU/SukiSU 安装、授权与保活模块
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

当前进度：阶段 0.5～4 已完成，阶段 5 代码已完成并进入专项真机验收。阶段 4“前台与焦点窗口融合”已在 Android 16 真机完成普通应用、SystemUI、输入法、权限弹窗和真实画中画验收；阶段 5 已加入真正可达的 root 有限退避、窗口 generation、按代缓存失效、短时单调缓存和动作前节点刷新/重新定位。Content 动态事件采用分支失效而非全局换代，避免持续变化页面饿死规则动作。

最新停点已经记录在 `docs/current-progress.md` 的“本轮开发停点”；恢复开发时继续阶段 5 的可控 root 缺失故障注入和米游社/哔哩哔哩慢页面专项统计。

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
