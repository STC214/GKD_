# 模块包装更新

- Android 工程与模块目录提升到仓库根目录，移除旧的 `GKD_/` 嵌套层级，并同步打包文档。
- 将第三方安装包、设备配置和诊断日志整理到 Git 忽略的 `local-assets/` 分类目录，保持源码根目录整洁。
- KernelSU/SukiSU Ultra 模块版本号改为分钟级时间戳。
- 开机后后台启动 GKD，不主动弹出 GKD 界面。
- 增加 root 侧低频保活守护：默认每 5 分钟检查 GKD 进程；依据 GKD 保存的开关恢复常驻通知服务，尊重用户主动关闭状态。
- 默认每 30 分钟重新应用后台 AppOps 与电池白名单，并限制日志体积，避免长期运行无限增长。
- 模块管理器动作按钮会打开 GKD，并在执行页显示基础状态和最近的开机服务日志。
- 移除模块时默认同时卸载 user 0 下的 GKD App；卸载前请先备份 GKD 设置。

# 更新内容

- 增加米游社签到规则兼容层：原神/绝区零分支使用“每日签到”标题锚点，星穹铁道分支使用奖励结构签名并设置 5 秒最短冷却；不改写订阅文件，不影响订阅在线更新。
- 修复自动化先启动、应用规则稍后加载时静止页面不补查的问题。
- 修复规则查询期间到达的新事件被直接丢弃的问题；事件风暴现在合并为一个后续唤醒，查询事件缓冲保持常量空间。
- 前台判断开始使用多条任务中的 focused/visible/running 状态，并新增焦点窗口、root 包名、用户和显示屏统一快照；冲突快照默认禁止执行。
- 米游社漏执行现在可通过结构化诊断明确区分选择器未命中、窗口不可用和动作失败。
- 脏工作区构建的 APK 版本追加 `-dirty`，避免未提交代码与纯提交版本重名。
- 优化快照结果提示文案
- 修复某些场景无障碍服务状态判断错误
- 修复某些设备新安装应用后对应规则不启用

## 更新方式

- GKD - 设置 - 关于 - 检测更新
- 下列方式之一

<a href="https://gkd.li/guide/"><img src="https://e.gkd.li/f23b704d-d781-494b-9719-393f95683b89" alt="Download from GKD.LI" width="32%" /></a><a href="https://play.google.com/store/apps/details?id=li.songe.gkd"><img src="https://e.gkd.li/f63fabeb-0342-4961-a46d-cac61b0f8856" alt="Download from Google Play" width="32%" /></a><a href="https://github.com/gkd-kit/gkd/releases"><img src="https://e.gkd.li/c1ef2bb9-7472-46d5-9806-81b4c37e5b4d" alt="Download from GitHub releases" width="32%" /></a>
