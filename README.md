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

本 fork 额外提供一个轻量模块包装：保留 GKD 作为普通 Android App，由 KernelSU/SukiSU Ultra 模块在开机后完成安装、授权、保活白名单和启动辅助。

模块源码位于 [`ksu-sukisu-module/`](ksu-sukisu-module/)，在 Windows 下可从仓库根目录执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1
```

生成的可刷入模块包位于：

```text
ksu-sukisu-module\dist\gkd-ksu-sukisu-module.zip
```

默认配置不会自动开启无障碍，也不会在移除模块时卸载 GKD。刷入后如需调整，可编辑：

```text
/data/adb/modules/gkd_ksu_sukisu/config.conf
```

如果手机上已安装的 GKD 与模块内 APK 签名一致，不需要先卸载，模块会覆盖安装并保留数据。如果签名不一致，请先在 GKD 内导出/备份配置，再卸载现有 App，或改用同签名 APK 重新打包模块。

如需使用 GKD 的 Shizuku 相关能力，请先确保 Sui/Shizuku 本身已正常运行。若 GKD 提示 Shizuku 授权失败，可参考 [`ksu-sukisu-module/README.md`](ksu-sukisu-module/README.md) 的排障章节检查 Sui 模块是否安装完整。

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
