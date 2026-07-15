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

- 阶段 7 首个最小切片固定 libsu 6.0.0，引入普通非 daemon APK RootService；当前 AIDL 只暴露协议版本、远端 PID/UID 和服务包名，不包含任意 shell、文件路径、订阅或数据库入口。
- RootService 每次调用校验调用 UID、UID 对应包和共享 UID 下的签名一致性；客户端只接受 UID 0、协议与包名匹配的 Binder，并处理 Binder 死亡、空 Binder、授权异常和 8 秒连接超时。
- “Root 与授权状态”弹窗会按需请求 APK RootService 并显示远端身份；最小握手已真机通过，当前输入动作仍使用原后端，下一步再按结构化接口分批迁移。
- Android 16 真机确认 APK RootService 以 UID 0 运行且协议为 1；杀死 root 子进程后 App 不崩溃，诊断保留 `binder died`，手动重连生成新 PID。强停、覆盖更新和卸载 Debug 包后均无 daemon 残留。
- RootService 第二切片新增 Parcelable 结构化 `tap/swipe` 请求：只接受数值动作、displayId、有限坐标、时长和半开窗口边界，拒绝未知动作、NaN/Infinity、越界坐标、非法时长及非规范 Tap；返回 Completed/Rejected/Unavailable/Failed，不开放命令或路径。
- 增加米游社签到规则兼容层：原神/绝区零分支使用“每日签到”标题锚点，星穹铁道分支使用奖励结构签名并设置 5 秒最短冷却；不改写订阅文件，不影响订阅在线更新。
- 修复自动化先启动、应用规则稍后加载时静止页面不补查的问题。
- 修复规则汇总在监听协程订阅前已加载时，首个真实汇总被 `drop(1)` 跳过的竞态。
- 修复规则查询期间到达的新事件被直接丢弃的问题；事件风暴现在合并为一个后续唤醒，查询事件缓冲保持常量空间。
- 前台判断开始使用多条任务中的 focused/visible/running 状态，并新增焦点窗口、root 包名、用户和显示屏统一快照；冲突快照默认禁止执行。
- 收紧前台快照确认条件：任务必须同时 focused、visible、running，两个 TaskStack 回调统一重新采样焦点任务。
- 前台窗口新增 Application、InputMethod、SystemUI、PermissionController、PictureInPicture、AccessibilityOverlay 和 SystemOverlay 分类；非普通应用覆盖层默认禁止规则动作。
- Task 与焦点窗口冲突改为一次 150ms 有界确认，不做周期轮询；确认后的统一快照现已接管规则查询和动作提交前校验。
- 动作提交前再次核对 taskId、windowId 和 appId，窗口上下文变化时以 `StaleContext` 终止，不向旧窗口发送动作。
- 修复 Android 16 上可见输入法和画中画窗口可能同时为 `focused=false/active=false` 而被漏判的问题；交互窗口列表中存在时仍优先阻断宿主规则。
- 修复权限控制器已成为顶部 Task、但无障碍仍把底层 App 窗口标为 focused 时的漏判；现在同时核对 Task 与 Window 包名。
- 修复静态页面前台冲突复查被旧 Activity 的空规则集提前拦截的问题；延迟任务现在先重新确认前台，再进入规则查询。
- 根节点包与已确认前台不一致时，每个上下文最多补查一次，避免持续错配触发 150ms 无限重试。
- TaskStack 提供 Activity 时不再被同包的旧 `STATE_CHANGED` 事件覆盖；只有 Task Activity 缺失时才使用事件兜底。
- 150ms 前台确认改用单调时钟计时，系统时间校准或回拨不再延长确认窗口。
- 窗口或 root 在切换期暂时为空时，按 50/100/200/400/800ms 有限退避恢复；没有窗口切换信号时不会自行轮询。
- 窗口状态和内容事件引入 generation；旧 generation 已匹配的节点禁止进入动作提交，下一轮查询会整体失效旧 root/子节点缓存。
- 动作前强制刷新目标节点；刷新失败时仅允许从同包、同 windowId、同 displayId、同 generation 的新 root 重新定位一次，不复用旧坐标。
- 修复有限恢复被前台确认提前拦截的问题；焦点应用窗口已存在但 root 尚未挂载时，现在会在任何规则执行前进入五级恢复。
- Content 事件不再推进全局窗口 generation，避免 WebView、倒计时和视频页持续变化时反复取消合法动作；State/App/亮屏/动作仍执行结构失效。
- 五级恢复改为整个切换窗口共享总预算，上下文变化不再重新获得五次机会。
- 节点缓存默认时效缩短为文本 500ms、结构 1000ms并使用单调时钟；保留原 1000/2000ms 保守设备策略。
- displayId 与 rotation 进入窗口令牌；旋转或显示上下文变化会使旧选择结果失效。
- 新增统一 `ActionExecutor`：规则动作提交时携带 displayId、windowId、rotation、窗口区域和可见区域，Root 与无障碍后端回退前重新确认窗口上下文。
- 节点点击或长按 API 明确拒绝时，只允许尝试最近一个可点击父节点；已接受、已完成及未知副作用动作不会自动重复。
- Root 点击、长按和滑动显式指定目标 displayId；非默认显示屏在没有对应无障碍手势能力时拒绝回退，不向错误显示屏发送坐标。
- 动作诊断新增目标类型、执行后端、窗口区域及重试次数，节点、父节点、Root 坐标、无障碍手势和全局动作可以分别统计。
- 可观察动作完成后最多等待 350ms：目标节点消失、窗口变化或 generation 变化时标记 `Verified`；没有观察到变化时标记 `Inconclusive`，保持原动作成功语义且绝不自动重发。
- `none`、`swipe` 和失败动作不进入默认验证等待，避免无意义延迟；验证使用单调时钟并与规则提交前的窗口令牌保持一致。
- B 站一次性安全导航规则已完成 Android 16 真机验证：节点动作在 104ms 内以 `GenerationChanged` 标记为 `Verified`，同时记录 display/window/边界字段，后续旧规则以 `StaleContext` 跳过且没有重复动作。
- 米游社漏执行现在可通过结构化诊断明确区分选择器未命中、窗口不可用和动作失败。
- 脏工作区构建的 APK 版本追加 `-dirty`，避免未提交代码与纯提交版本重名。
- 优化快照结果提示文案
- 修复某些场景无障碍服务状态判断错误
- 修复某些设备新安装应用后对应规则不启用

## 更新方式

- GKD - 设置 - 关于 - 检测更新
- 下列方式之一

<a href="https://gkd.li/guide/"><img src="https://e.gkd.li/f23b704d-d781-494b-9719-393f95683b89" alt="Download from GKD.LI" width="32%" /></a><a href="https://play.google.com/store/apps/details?id=li.songe.gkd"><img src="https://e.gkd.li/f63fabeb-0342-4961-a46d-cac61b0f8856" alt="Download from Google Play" width="32%" /></a><a href="https://github.com/gkd-kit/gkd/releases"><img src="https://e.gkd.li/c1ef2bb9-7472-46d5-9806-81b4c37e5b4d" alt="Download from GitHub releases" width="32%" /></a>
