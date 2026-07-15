# GKD 单 APK Root 运行时可靠性改造计划

## 1. 文档目的

本文档用于指导本 fork 在不开发 LSPosed 模块、不依赖额外刷入式功能模块的前提下，利用 root 权限改进 GKD 的窗口判断、规则调度和动作执行可靠性。

当前阶段、构建产物和真机状态统一汇总在 [`current-progress.md`](current-progress.md)，本文继续作为实施顺序和验收标准的唯一主计划。

后续开发必须按本文阶段顺序推进。每一阶段只有在完成对应验收、记录测试结果并保留可回滚点后，才能进入下一阶段。

## 2. 最终目标

- 更准确地识别当前获得输入焦点的应用、Activity、窗口、显示屏和用户。
- 避免规则事件在查询期间被遗漏，减少“有时执行、有时不执行”。
- 让点击、长按、滑动和返回动作报告真实结果，不再把提交失败误记为成功。
- 在窗口节点延迟挂载、节点缓存过期和界面快速切换时进行有限、可控的恢复。
- 保持现有订阅下载、更新、导入、导出、配置、数据库和 UI 功能。
- 保持现有 `RawSubscription` 格式、选择器语法和第三方规则库兼容。
- root 不可用或被拒绝时，仍可回退到原生无障碍执行通道。
- 将 fork 改动集中在清晰的运行时边界内，降低后续合并上游 GKD 更新的成本。

## 3. 明确不做的内容

- 不开发 LSPosed/Xposed 模块。
- 不 Hook `system_server`。
- 不把整个 GKD App 进程变成 UID 0。
- 不把 Compose UI、Room 数据库、订阅管理或规则编辑器迁入 root 进程。
- 第一轮改造不扩展订阅 JSON 格式，不要求规则作者修改现有规则。
- 不用无限轮询代替事件驱动，不以持续高 CPU 占用换取表面稳定。
- 不对支付、删除、发送等不可安全重复的动作进行无条件自动重试。

## 4. 兼容性红线

以下代码和数据契约在可靠性改造期间保持兼容：

- `RawSubscription` 的现有字段、默认值和 JSON/JSON5 解析语义。
- `selector` 模块的选择器语法和匹配语义。
- 订阅 ID、版本号、`updateUrl`、`checkUpdateUrl` 及更新流程。
- `SubsItem`、`SubsConfig`、分类配置、动作日志等现有 Room 数据。
- `ResolvedRule` 的 `matchDelay`、`actionDelay`、`matchTime`、`actionCd`、`actionMaximum`、`preKeys` 等既有语义。
- 已保存的备份文件、订阅文件和用户配置。

若某阶段必须修改上述契约，必须先暂停实施，单独写兼容设计和迁移方案，不能顺手修改。

## 5. 目标架构

```text
GKD APK 普通应用进程
├─ Compose UI
├─ Room 数据库
├─ 订阅下载、更新、导入和导出
├─ RawSubscription / selector / ResolvedRule
├─ RuleEngine
├─ ForegroundResolver
├─ WindowProvider
├─ ActionExecutor
└─ PrivilegedBridge (AIDL)
            │
            ▼
同一 APK 启动的 root 子进程
├─ ActivityTaskManager / TaskStackListener
├─ WindowManager / InputManager
├─ PackageManager / AppOps
├─ 用户、任务和显示屏信息
└─ root 动作执行
```

root 子进程属于 APK 的内部实现，通过 `su` 启动并使用 Binder/AIDL 通信，不需要用户刷入新的功能模块。

第一版不把规则引擎迁入 root 进程。root 进程只提供需要特权的系统信息和动作能力，避免跨进程复制整个订阅、数据库及节点匹配状态。

## 6. 运行时边界

逐步引入以下接口，禁止规则引擎继续增加对具体 Shizuku、root 或 `A11yService.instance` 的直接依赖：

```kotlin
interface ForegroundResolver
interface WindowProvider
interface ActionExecutor
interface PrivilegedBridge
interface EngineWakeup
```

建议目录：

```text
app/src/main/kotlin/li/songe/gkd/runtime/
  foreground/
  window/
  action/
  engine/
  diagnostics/

app/src/main/kotlin/li/songe/gkd/root/
  ipc/
  service/
  system/
```

现有实现先作为接口适配器保留：

```text
A11yWindowProvider
ShizukuPrivilegedBridge
A11yActionExecutor
RootPrivilegedBridge
RootActionExecutor
```

## 7. 实施顺序

### 阶段 0：冻结兼容基线与建立复现集

目标：在改代码前知道哪些行为必须保持，以及改善是否真实发生。

任务：

- [x] 记录当前 commit、APK 版本、签名和构建命令。
- [x] 导出一份真实用户配置和订阅备份，作为升级/回退样本。
- [x] 选取至少 10 条代表性规则，覆盖节点点击、坐标点击、长按、滑动、返回、前置规则、冷却和延迟。
- [ ] 记录每条规则当前的成功、漏执行、误执行现象及复现步骤。
- [ ] 准备普通全屏、权限弹窗、输入法、通知栏、小窗/分屏、旋转、锁屏、双开等窗口场景。
- [x] 保存一次完整的动作日志、活动日志、无障碍事件日志和 logcat。
- [x] 为订阅更新、导入、导出和备份恢复建立冒烟测试清单。

验收：

- 能在同一设备上重复至少一个“规则不执行”和一个“动作未生效但被视为成功”的案例。
- 备份可在未修改版本中成功恢复。
- 测试规则和操作步骤已记录，后续阶段使用同一套样本回归。

回滚点：阶段开始时的 Git commit 和原 APK。

### 阶段 0.5：Root 桥真实性与自恢复

状态：`已完成（2026-07-14）`。用户确认米游社当前不具备有效签到复现条件，授权先完成不改变规则语义的 Root 桥前置诊断。

目标：不再把“部分 Shizuku Binder 可用”误显示成“Root 完整可用”，并让短暂的 UserService 启动超时能够有限恢复。

任务：

- [x] 将系统 Binder、UiAutomation、`IUserService` 和远端 UID 分开记录。
- [x] 使用只读 `id` 命令确认 UserService 是 root、shell 还是不可用。
- [x] 为初次 UserService 失败增加最多 2 次后台重连，不重建其他 Binder 和 UiAutomation。
- [x] 高级设置状态弹窗显示 Binder 数量、远端 UID、失败类型、尝试次数和最近检测时间。
- [x] 提供用户主动触发的“重新连接/重新检测”。
- [x] 为 UID 解析和 `ROOT/NON_ROOT/PARTIAL/FAILED` 分类增加单元测试。
- [x] 在 Root 真机原地升级后确认远端 UID 为 0；手动重新检测不重启主进程、Root UserService 或 UiAutomation。
- [x] 原地升级和 Root 桥检测后确认 `store.json` 与 3 个订阅 JSON 的 SHA-256 均与阶段 0 基线一致。
- [x] 审查并修复连接超时/取消后的迟到回调、重复解绑、旧上下文安装和禁用后继续重试问题。
- [ ] 用阶段 0 的真实规则重复样本确认规则匹配行为不变（随阶段 0 复现集验收，不阻塞本阶段 Root 桥交付）。

验收：

- 系统 Binder 可用但 `IUserService` 缺失时显示“部分可用”或“连接失败”，不能显示 Root 已连接。
- `id` 返回 `uid=0` 且命令成功时才显示“Root 已连接”。
- 自动重连总次数有硬上限，不形成无限循环或持续拉起进程。
- 重连过程不重建 `ShizukuContext` 的其他 Binder，不关闭现有 UiAutomation。

真机结果：高级设置显示 `Root 已连接`、系统 Binder `8/8`、`IUserService 已连接`、远端 UID `0`、Shell 自检通过和 UiAutomation 已连接。手动“重新检测”前后主进程与 Root UserService PID 保持不变。设备拒绝通知权限时，外部 `ExposeService` 曾因跳过 `startForeground()` 崩溃；前台服务通知不再以 `POST_NOTIFICATIONS` 为启动门槛后，同一条强停冷启动路径通过。

回滚点：删除 Root 桥诊断状态和 UserService 重连入口，恢复 `serviceWrapper` 为只读构造参数。

### 阶段 1：增加决策诊断，不改变执行逻辑

状态：`已完成（2026-07-15）`。代码、单元测试、2048 条容量版、哔哩哔哩成功链路和米游社有效 B01 漏执行现场均已完成；诊断已把故障明确定位到订阅选择器失配，而不是 Root、窗口 root 或动作执行。

目标：让每次未执行都能回答“停在哪一层”。

任务：

- [x] 新增 2048 条有限容量的内存环形诊断记录，避免无限增长。
- [x] 为一次规则判定分配关联 ID。
- [x] 将关联 ID 移到既有限流和连续事件合并之后，避免限流事件与已合并事件产生孤立诊断链。
- [x] 记录事件接收、前台判断、窗口 root、规则状态、选择器结果、动作提交和动作结果。
- [x] 对以下未执行原因建立稳定枚举，而不是仅写自由文本：
  - 服务未连接；
  - 自动匹配关闭；
  - 前台上下文未确认；
  - 窗口或 root 为空；
  - 包名/Activity 不匹配；
  - 规则达到次数上限；
  - 前置规则未满足；
  - 匹配延迟、动作延迟、冷却或匹配超时；
  - 选择器未命中；
  - 动作被拒绝、取消或验证失败。
- [x] 在高级设置提供“最近一次未执行原因”、最近 30 条预览、全部记录复制和内存清空。
- [x] 诊断弹窗按缓冲 revision 实时刷新成功记录、缓存数量和清空结果。
- [x] 支持将完整诊断导出为应用日志目录中的 `decision-diagnostics.txt`，供 ADB 和后续现场脚本读取。
- [x] 默认关闭高频诊断；关闭时不分配关联 ID，逐规则记录入口立即返回。

验收：

- 对基线复现集中的每次漏执行，都能看到明确的终止原因。
- 关闭诊断模式后，不产生明显额外磁盘写入和持续 CPU 占用。
- 不改变任何现有规则的执行次数和时序。

当前真机结果：开启诊断后切换到哔哩哔哩，已看到 `667/app/...` 规则的 `RuleEligible`、`ForcedRuleSkipped` 等稳定原因，并能按同一关联 ID 回看事件、前台和规则阶段。首轮 512 条容量在高频事件下约十秒填满；最终 2048 条版同场景只使用 91 条。返回 GKD 时，自身 `NoApplicableRules` 仅作为普通观察，不覆盖目标应用最近失败；本次保留的最近失败为安全中心事件与哔哩哔哩前台不一致，详情 `foreground=tv.danmaku.bili`。

`2026-07-15` 米游社有效现场中，规则 `667/app/8/0` 在窗口 root 可用后持续终止于 `SelectorMiss`，依赖规则 1–3 均为 `PrerequisiteUnsatisfied`，没有进入动作阶段。已签到/未签到双快照进一步确认 WebView 的 `text` 为空且页面容器结构变化。阶段 1 的目标“每次未执行能回答停在哪一层”已获得真实故障闭环，后续现场采样转为回归验证，不再作为阶段完成阻塞。

### 阶段 2：修复动作结果误判

目标：在引入重试前，先保证成功和失败结果可信。

任务：

- [x] 将 `dispatchGesture(...) != null` 改为等待真实手势结果。
- [x] 使用 `GestureResultCallback` 区分 `onCompleted`、`onCancelled`、提交拒绝和等待超时。
- [x] 让 `SafeInputManager` 和 `InputShellCommand` 返回每次注入的真实结果，不再返回无条件 `Unit` 成功。
- [x] DOWN、MOVE、UP 任一关键事件失败时，整个动作判为失败。
- [x] 动作失败时不得调用 `rule.trigger()`，不得增加次数或进入冷却。
- [x] 用 `ActionResultState` 区分“节点/API 已接受”“输入/手势已完成”和预留的“界面结果已验证”；本阶段不把前两者冒充界面验证。
- [x] 保持现有动作名称和规则格式不变。

验收：

- 人为让手势注入失败时，动作日志明确显示失败，规则次数不增加。
- 手势取消时不进入 `actionCd`。
- 原有成功动作仍能执行，动作日志与实际界面一致。

当前实现结果：Root UserService 的 `clickCenter` 和 `swipe` 已通过 HTTP 检查接口在哔哩哔哩真机执行，均返回 `result=true, shell=true, state=Completed`；动作名、选择器和 `swipeArg` 格式未变化。JVM 回归覆盖 MOVE 失败后成功 UP 不能覆盖整次失败。真机临时切换到无障碍模式并关闭 Shizuku 输入后，重叠手势稳定得到 `Cancelled → Completed`；临时内存规则 `-1/app/9902/0` 的诊断链为 `ActionSubmitted → ActionCancelled(state=Cancelled)`，动作计数前后不变，随后规则仍为 `RuleEligible`，证明没有消耗次数或进入冷却。测试后配置、动作计数、无障碍设置和订阅已原样恢复。

### 阶段 3：重构查询唤醒，消除丢事件窗口

目标：查询期间到达的新事件一定会触发后续查询，但不会无限堆积。

状态：`已完成（2026-07-15）`。启动子场景和查询中事件窗口均已修复：规则汇总晚加载会刷新当前 Activity；查询运行时的新请求合并为一个有界 pending 唤醒，当前轮结束后立即接续。星铁静止页、并发状态机测试和真机事件风暴冒烟均已通过。

任务：

- [x] 用有界 `QueryWakeState` 替换单纯的 `if (querying) return`。
- [x] 查询期间收到事件时合并为一个 pending 请求。
- [x] 当前查询结束后若存在 pending，保持单 runner 所有权并立即接续一轮。
- [x] 同类事件只保留最后两个；混合事件折叠为 root 查询标志，缓冲常量有界。
- [x] 保留既有 `interruptKey` 优先规则中断能力，最新事件仍进入 pending 补查。
- [x] 保留启动、App 切换、动作完成的有限 300ms 后续查询窗口。
- [x] 规则汇总晚于自动化连接时刷新当前 Activity，并触发一次有限 forced query。
- [x] 增加并发、事件风暴、慢查询 handoff 和请求优先级测试。

验收：

- 在查询过程中连续注入事件，最后一次界面状态一定被查询。
- 不再需要等待下一个无关事件才执行遗漏规则。
- 高频事件不会形成无限协程或无限队列。
- 空闲状态没有周期性忙轮询。

### 阶段 4：实现融合式前台和焦点窗口判断

目标：同时识别任务、真实焦点窗口、节点包、显示屏和用户。

状态：`已完成（2026-07-15）`。代码、自动测试及 Android 16 普通应用、SystemUI、输入法、权限弹窗和真实画中画固定场景均已验收。

任务：

- [x] 扩展隐藏 `TaskInfo` 映射，读取 `userId`、`taskId`、`effectiveUid`、`displayId`、`isFocused`、`isVisible` 和 `isRunning`。
- [x] 获取多条任务，不再直接使用 `getTasks().firstOrNull()`。
- [x] 优先选择目标显示屏上 `isFocused && isVisible && isRunning` 的任务。
- [x] 从 `AccessibilityWindowInfo` 中选择 `isFocused` 的窗口，记录 `isActive`、类型、层级、显示屏和窗口 ID。
- [x] 从焦点窗口 root 获取真实节点包名。
- [x] 建立统一的 `ForegroundSnapshot`，至少包含任务、Activity、窗口包、用户、显示屏、时间戳和置信度。
- [x] 收紧 `Confirmed`：Task 必须同时 focused、visible、running，且两个 TaskStack 回调都只触发统一重新采样。
- [x] 为输入法、SystemUI、权限控制器、画中画和覆盖层制定明确策略。
- [x] Task 与窗口短暂冲突时执行一次 150ms 有界确认，不立即切换规则上下文、不周期轮询。
- [x] 未确认时宁可暂缓动作，不在错误窗口执行；动作提交前再次核对 taskId、windowId 和 appId。

判定原则：

```text
包级规则：以焦点窗口 root 的 packageName 为主。
Activity 级规则：以 focused Task 的 topActivity 为主。
执行动作：要求窗口上下文已确认，或规则明确允许系统覆盖层场景。
```

验收：

- 普通全屏应用中 Task、窗口和节点包稳定一致。
- 输入法出现时不会把输入法 Activity 错当成宿主 Activity。
- 权限弹窗/SystemUI 覆盖时不会拿底层 App 的 Activity 配合上层窗口节点误执行。
- 小窗、分屏和画中画场景可明确指出哪个任务和窗口拥有焦点。
- 每次规则动作日志包含 userId、displayId、taskId、windowId 和置信度。

当前实现结果：`ForegroundSurface` 将普通应用与输入法、SystemUI、权限控制器、画中画、无障碍覆盖层及其他系统覆盖层分开；只有 `Confirmed + Application` 可执行。`ForegroundConfirmationState` 对冲突上下文最多等待 150ms，同一冲突到期后拒绝且不继续轮询，并以单调时钟抵抗系统时间回拨。`A11yRuleEngine` 的事件入口、规则查询入口和动作提交前均使用同一快照；延迟确认先重新采样前台再检查新 Activity 的规则集，根节点持续错配时每个 task/window/root 上下文最多补查一次。Activity 以 TaskStack 为权威来源，只有 Task Activity 缺失才接受同包状态事件兜底；动作前 taskId、windowId 或 appId 变化即记录 `StaleContext`。结构化诊断附带置信度、surface、userId、displayId、taskId 和 windowId。真机发现可见输入法与 PiP 窗口可能均为 `focused=false/active=false`，因此交互窗口列表中的 InputMethod/PiP 在普通焦点窗口之前选择；权限控制器同时检查 Task 与 Window 包。App 72/72、Selector 18/18 已通过；Release、Vital Lint 和修复版真机结果见基线文档，阶段 4 验收完成。

### 阶段 5：改进窗口获取、节点新鲜度和短时恢复

目标：处理窗口尚未挂载和节点缓存过期，而不做长期轮询。

任务：

- [x] 窗口或 root 暂时为空时采用有限退避重试：50、100、200、400、800ms。
- [x] 只在窗口切换、App 切换、屏幕开启或动作完成后的短时间重试。
- [x] `TYPE_WINDOW_STATE_CHANGED` 时推进 generation，并在下一轮查询清理窗口相关缓存。
- [x] `TYPE_WINDOW_CONTENT_CHANGED` 时推进 generation，禁止旧选择结果进入动作。
- [x] 动态节点缓存默认缩短为文本 500ms、结构 1000ms并改用单调时钟；保留 1000/2000ms 保守设备策略。
- [x] 使用节点前调用 `refresh()`；失败后重新取得窗口 root。
- [x] 重新取 root 后再次验证 packageName、windowId、displayId 和 generation。
- [x] 旧窗口 generation 的选择结果禁止执行动作。

验收：

- 节点延迟挂载时能在有限窗口内恢复匹配。
- 快速页面切换后不会使用上一页面坐标。
- 重试达到上限后明确报告失败原因并停止，不持续耗电。

当前实现结果：`WindowRootRecoveryState` 只接受显式切换信号，3 秒窗口内由整个切换共享 5 次退避总预算，root 缺失本身不能开启或延长恢复。焦点 Application Window 已存在但 rootAppId 暂空时，会在普通前台确认之前进入恢复，因此五级链路覆盖主要挂载竞态。`WindowGenerationState` 只为 State/App/亮屏/动作以及 display/rotation 变化推进结构 generation；Content 事件继续进入 pending 查询并清理对应事件分支，避免动态页面全局换代饥饿。`A11yContext` 在下一轮结构查询清空旧 root 与遍历缓存，节点默认缓存缩短为 500/1000ms并使用单调时钟，保留 1000/2000ms 保守策略。选择器结果携带 generation、taskId、windowId、appId、displayId 和 rotation；目标节点必须先 `refresh()`，失败时只在上下文完全一致时重取 root 并复跑同一规则。App 88/88、Selector 18/18、Release 与 Vital Lint 已通过；Android 16 最终专项缓存为 2048 条、95 个事件型关联 ID、0 个孤儿链，米游社冷启动真实命中一次第 1 级 50ms 缺 root 恢复入口；约 10ms 后更新 task/window 的新事件取得 root 并取代旧恢复，无恢复耗尽或崩溃。阶段 5 已完成。

### 阶段 6：抽象并增强动作执行器

目标：提高动作定位、执行和验证精度。

任务：

- [x] 引入 `ActionExecutor`，建立规则语义与具体输入后端之间的统一提交边界。
- [x] 动作执行前再次验证窗口 generation 和节点新鲜度；Root 失败转无障碍前再次复核。
- [x] 节点点击失败时，只尝试最近一个可点击父节点。
- [x] 坐标动作携带 displayId、窗口区域、旋转和可见区域信息。
- [x] 对可验证动作，在完成后观察节点消失、窗口变化或 generation 变化；无信号只记为无法确认。
- [x] 只有节点 API 明确拒绝后的最近父节点策略允许重试一次。
- [x] 默认不对已接受、已完成或未知副作用动作重试。
- [x] 保持现有 `click`、`clickNode`、`clickCenter`、`longClick`、`swipe`、`back` 等规则动作兼容。

验收：

- 动作不会发送到错误 display 或已经失焦的窗口。
- 节点动作、坐标动作和动作后验证分别记录结果。
- 不安全动作不会自动重复。
- 原订阅无需增加字段即可继续执行。

当前实现结果（第一批）：新增 `ActionExecutionContext`、`ActionExecutionGuard` 与统一 `ActionExecutor`。规则引擎把阶段 5 的 `WindowContextToken` 转换为动作上下文，携带 displayId、windowId、rotation、windowBounds 和 visibleBounds；提交前及 Root 坐标后端失败转无障碍手势前均重新确认 generation、前台和节点新鲜度。Root 输入链显式设置 displayId，非默认显示屏没有对应无障碍手势能力时拒绝回退。节点 API 明确返回 false 时最多尝试最近一个符合条件且可见的父节点，父节点已拒绝后不继续遍历更多祖先；传统 `click` 的节点到坐标兼容路径仍只在前序 API 明确拒绝后进入。动作结果记录 target、backend、窗口上下文和 retryCount。App 91/91、Selector 18/18、Release 与 Vital Lint 已通过；Android 16 非清数据覆盖安装后主进程、UID 0 Root UserService 和 StatusService 正常，米游社/B 站切换冒烟无新增 GKD 崩溃。动作后界面验证尚未实现，阶段 6 保持进行中。

当前实现结果（第二批）：新增纯状态机 `ActionVerificationStateMachine`。成功的 click/clickNode/clickCenter/longClick/back 等动作以单调时钟最多观察 350ms；目标节点消失、task/window/app/display/rotation 变化或结构 generation 变化任一成立即把结果升级为 `Verified` 并记录信号。`none`、`swipe` 和失败动作跳过观察。超时没有信号时记录 `Inconclusive`，原 `Accepted/Completed` 结果、规则计数和冷却保持不变，且不会重发输入；`ActionVerificationFailed` 继续保留给未来显式且可证明的验证条件，不能用“未观察到变化”冒充失败。App 99/99、Selector 18/18、Release 与 Vital Lint 通过。Android 16 真机使用原生 HTTP 内存订阅加载一次性 B 站“分区”安全导航规则 `-1/app/9903/0`，诊断链为 `ActionSubmitted → ActionSucceeded(state=Verified) → ActionVerified(signal=GenerationChanged, elapsed=104)`；结果同时记录 `target=Node`、`backend=Node`、`display=0`、`window=2447`、rotation 和窗口/可见边界，后续旧规则以 `StaleContext` 跳过而未重复动作。测试后内存订阅、动作日志、触发总数和 HTTP/ADB 临时状态均已恢复，阶段 6 完成；父节点回退无自然样本，继续保留单元测试和未来自然诊断，不为此增加生产后门。

### 阶段 7：接入 APK 内置 RootService

目标：不刷模块，由 APK 自己请求 root 并启动特权 Binder 服务。

任务：

- [x] 选择并固定 root IPC 实现：libsu 6.0.0 普通非 daemon `RootService`。
- [x] 定义最小 AIDL：身份握手、结构化输入和只读前台 Task 已完成；系统窗口信息继续由无障碍侧提供，不重复扩大 root 接口。
- [x] 对每次调用校验调用方 UID、包名和共享 UID 下的签名一致性，拒绝第三方 Binder 调用。
- [x] root 服务不读取和修改订阅数据库，不解析远程规则。
- [x] root 服务不开放任意 shell 字符串执行接口。
- [x] 输入动作使用 Parcelable 结构化参数，校验动作类型、displayId、有限坐标、窗口边界和时长。
- [x] 处理用户拒绝 root、root 被撤销和 App 更新；常规启动连接、Binder 死亡、自动/手动重连及安全回退均已通过。
- [x] 提供 Root、Shizuku/现有特权通道、纯无障碍三个后端的显式状态和回退顺序，并完成 Root/A11y 两条真机动作验收。
- [x] 默认使用非 daemon root 服务；只有真机证明生命周期不足时再评估 daemon 模式。

建议回退顺序：

```text
RootPrivilegedBridge
    ↓不可用
ShizukuPrivilegedBridge
    ↓不可用
A11y-only
```

验收：

- 未刷入本项目功能模块也能由 APK 请求 root 并建立 Binder 连接。
- root 服务死亡后 App 不崩溃，并可重新连接或安全回退。
- root 被拒绝时订阅、设置、规则编辑和无障碍模式仍可使用。
- App 卸载后不遗留项目 daemon、二进制或可执行脚本。
- AIDL 不存在任意命令执行入口。

当前实现结果（第一最小切片）：固定 `com.github.topjohnwu.libsu:service:6.0.0`，新增四个只读握手方法，客户端校验协议、远端 PID、UID 0 与包名，并监听 Binder 死亡、空 Binder及 8 秒连接超时。服务端每次调用都核对安装 App UID、UID 包列表和签名一致性；没有 shell、路径、输入、订阅或数据库入口。“Root 与授权状态”弹窗按需连接，不影响当前动作后端。Android 16 真机首次连接为 PID `26957`/UID 0/协议 1；杀死后发现 `onServiceDisconnected` 会覆盖 `binder died`，修复并覆盖更新后以 PID `29808` 复测，界面稳定保留 `失败（binder died）`，主进程 PID `20830` 未崩溃；再次连接得到 PID `30430`。强停 Debug 主进程后 root 子进程退出，覆盖更新与卸载后也无残留。App 107/107、Selector 18/18、Release/R8/Vital Lint 通过，最小握手切片完成；阶段 7 继续进入结构化输入。

当前实现结果（第二切片）：AIDL 事务 5 接收 `RootInputRequest` Parcelable，不接受字符串。请求仅含动作类型、displayId、两组坐标、时长及整数窗口边界；服务端先拒绝未知动作、负 displayId、非法边界、NaN/Infinity、半开边界外坐标、超过 10 秒的动作、零时长 swipe 和端点不一致的 tap，再通过系统 `IInputManager` 逐事件注入。结果固定为 Completed/Rejected/Unavailable/Failed。客户端仅在已握手 Binder 上调用，远端异常会清除服务引用并进入失败状态。新增 8 项参数测试后 App 115/115、Selector 18/18、Release/R8/Vital Lint 通过；尚未接入规则链，也未用生产调试后门触发真机输入。

当前实现结果（第三切片）：新增 `PrivilegedInputBridge`，`clickCenter`、`longClickCenter` 和 `swipe` 从阶段 6 的 `ActionExecutionContext` 生成结构化请求，边界取 `windowBounds ∩ visibleBounds`，后端与诊断显式区分 `ApkRoot`、`Shizuku`、`Accessibility`。Root 只有明确 Rejected/Unavailable 才在 guard 复核后尝试 Shizuku；Shizuku 无后端时才允许再次复核并进入无障碍，Binder 异常或逐事件 false 均以 Failed 终止，避免可能已产生副作用的动作被重复提交。新增 5 项顺序/失败语义测试后 App 120/120、Selector 18/18、Release/R8/Vital Lint 通过。Android 16 真机对 B 站 `@[vid="category_click_area"]` 的直接 `clickCenter` 返回 `Completed/ApkRoot` 并导航成功；杀死 root PID `9325` 后主进程 PID `29125` 不崩溃，同一动作返回 `Completed/Accessibility` 且只导航一次。一次性规则链因 Debug 缺少可信 Task 采样而在 `Probable` 前台确认阶段被正确阻断，因此下一切片先处理常规 RootService 连接与可信前台采样，再验收完整规则链。

当前实现结果（第四切片）：新增默认关闭的 `enableApkRoot` 设置和 `RootServiceLifecycle`。开启后应用直接绑定普通非 daemon 服务，不要求先打开诊断弹窗；关闭立即解绑。只有 Binder death/disconnect、binding died、同步绑定或只读调用异常标记为可重试，按 750/1500ms 每进程最多自动重连两次；用户拒绝、身份拒绝、null binding 和 8 秒超时不循环。AIDL 协议升至 2，事务 6 返回 `RootForegroundTask` Parcelable，只包含 task/user/effectiveUid/display、focused/visible/running/PiP 和组件名，不接受命令、路径或规则。前台快照先读 Root Task，再回退 Shizuku，原窗口融合和 `Confirmed + Application` 门控不变。新增 1 项字段映射与 2 项旧设置兼容测试后 App 123/123、Selector 18/18、Release/R8/Vital Lint 通过；Release SHA-256 `D9004454A4CA806A1DB295446243B0A93D47E05ADF86CD2DB427C881920280C3`。Android 16 独立 Debug 包确认默认关闭不启动 root；授权开启后常规连接为 UID 0/协议 2。关闭 Shizuku 后，一次性 B 站规则由 Root Task 达到可信前台并通过 `ApkRoot` 导航成功。Root PID `13166` 被杀后主进程保持，约 1 秒重连为 PID `27212`；关闭开关 6 秒不重连，重开 2 秒内恢复 PID `28481`。第四切片真机验收完成；当时剩余的拒绝、撤权和无回调专项现已由下一段收口。

当前实现结果（最终收口）：未授权 Debug UID `10394` 的连接在 8 秒后稳定为 `connection timeout`，再观察 10 秒没有 root 子进程或循环授权。授权连接 PID `20360` 后从 SukiSU 撤权只写入 `allow=0`，不会追溯杀死既有 UID 0 进程；结束该进程后 10 秒无新 root，主进程保持。撤权且 Shizuku 关闭时，B 站安全点击以 `Completed/Accessibility` 单次完成。保留数据覆盖更新保持 UID、设置与无障碍绑定，但不会绕过撤权；重新授权并冷启动恢复 PID `3653`。由于即时撤权必须由客户端主动解绑，“启用 Root 增强”副标题明确提示先关闭开关。阶段 7 至此完成。

### 阶段 8：多用户、多显示屏和厂商窗口适配

目标：覆盖小米双开、小窗、分屏、折叠屏和外接显示屏等高风险场景。

任务：

- [ ] 将 `userId + packageName` 作为运行时应用身份，避免只用包名混淆双开实例。
- [ ] 正确处理 user 0、工作资料和小米 user 999。
- [ ] UiAutomation、TaskInfo、窗口和 PackageManager 查询使用一致的用户上下文。
- [ ] 输入动作明确指定目标 displayId。
- [ ] 为小米自由窗口/小窗补充真机策略，不硬编码单一 ROM 字符串作为唯一依据。
- [ ] 折叠、旋转和分辨率切换时使坐标及窗口缓存全部失效。
- [ ] 对副屏规则默认禁用，只有用户明确开启才执行。

验收：

- 双开实例不会共用错误的 Activity 或配置上下文。
- 小窗获得焦点时动作落在小窗对应 display/region。
- 旋转和折叠切换后不会使用旧坐标。
- 默认不会在非目标副屏误执行规则。

### 阶段 9：稳定性验证与上游维护收口

目标：确认改造可长期使用，并将 fork 维护成本控制在可接受范围。

任务：

- [ ] 连续运行至少 24 小时，覆盖锁屏、解锁、切 App、清后台和网络变化。
- [ ] 重放阶段 0 的全部规则和窗口场景。
- [ ] 验证订阅自动更新、手动更新、导入、导出、备份和恢复。
- [ ] 验证升级安装保留数据，卸载无 root 残留。
- [ ] 检查空闲 CPU、内存、唤醒和日志增长。
- [ ] 将新增运行时接口和与上游的修改点写入维护说明。
- [ ] 每次合并上游后运行固定回归集。
- [ ] 只有经过稳定性验证后，才考虑是否弃用现有模块包装或 Shizuku 路径。

验收：

- 原订阅可更新且无需迁移。
- 原备份可恢复，新备份可在同版本重装后恢复。
- 空闲时没有持续高频轮询。
- root 服务、无障碍服务任一重启均不会造成规则状态永久失效。
- 已知失败均能从诊断记录定位到窗口、节点、规则状态或动作层。

## 8. 固定测试矩阵

每个阶段至少覆盖适用项：

| 类别 | 场景 |
| --- | --- |
| 基础 | 冷启动、热启动、切换前后台、强制停止后重启 |
| 窗口 | Activity、Dialog、Popup、权限弹窗、SystemUI、通知栏、控制中心 |
| 输入 | 软键盘显示/隐藏、节点点击、坐标点击、长按、滑动、返回 |
| 多窗口 | 分屏、小窗、画中画、旋转、折叠、外接显示屏 |
| 用户 | user 0、工作资料、小米双开/user 999 |
| 生命周期 | 锁屏、解锁、息屏、进程被杀、Binder 死亡、root 撤权 |
| 订阅 | 新增、更新、禁用、删除、导入、导出、备份、恢复 |
| 性能 | 事件风暴、慢选择器、大量规则、长时间空闲 |

## 9. 提交与回滚规则

- 每个阶段单独提交，不把多个阶段混入一个不可拆分提交。
- 每个提交附带阶段编号、测试命令和结果。
- 先加测试或诊断，再改行为。
- 每个阶段都必须同步更新 `docs/upstream-code-delta-guide.md`，记录实际文件、符号、行为、不变量、上游迁移方法和验收结果。
- 代码已经完成但差异文档尚未更新时，该阶段不得标记为“已完成”。
- 发现订阅兼容或数据库风险时立即停止，不带病进入下一阶段。
- root 后端必须始终可通过设置关闭。
- 保留无 root 回退，直到 root 路径完成长期验证。
- 不使用 `git reset --hard` 等破坏用户工作区的回滚方式。

建议提交前缀：

```text
runtime(phase-1): ...
runtime(phase-2): ...
root(phase-7): ...
test(phase-9): ...
```

每个阶段提交前还必须完成以下附带步骤：

- [ ] 将对应差异从 `PLANNED` 更新为 `CURRENT`，或记录 `BLOCKED/DROPPED`。
- [ ] 在差异文档中写入实际提交 ID 和上游基线。
- [ ] 记录新增、修改、删除、重命名的文件及核心符号。
- [ ] 记录对规则、订阅、数据库、备份和回退路径的影响。
- [ ] 写明未来官方更新时应采用的合并策略。
- [ ] 写明已执行的自动测试和真机验收结果。

## 10. 阶段状态表

| 阶段 | 状态 | 验收记录 |
| --- | --- | --- |
| 0. 兼容基线与复现集 | 进行中 | Root 真机、升级前后快照、当前 HEAD 原地升级、App 自带备份恢复和 10 条真实规则样本已建立；B01 已完成，继续等待规则重复统计、剩余窗口场景和 B02，见 `docs/testing/root-runtime-baseline.md`。 |
| 0.5 Root 桥真实性与自恢复 | 已完成 | Root UserService UID 0、系统 Binder 8/8、UiAutomation 共存、有限重连、手动检测、订阅哈希和通知权限拒绝下的外部冷启动均已真机验收。 |
| 1. 决策诊断 | 已完成 | 结构化枚举、关联 ID、2048 条环形缓冲、导出和 UI 已完成；米游社有效 B01 已稳定定位为 `WindowRootAvailable → SelectorMiss`，证明诊断可闭环真实漏执行。 |
| 2. 动作结果误判修复 | 已完成 | Root 完成路径、无障碍取消路径、失败不计次数/冷却和订阅不变量均已验收。 |
| 3. 查询唤醒重构 | 已完成 | `QueryWakeState` 单 runner、有界 pending、常量空间事件缓冲、规则晚加载补查、39 项 App 测试和真机事件风暴冒烟均通过。 |
| 4. 前台与焦点窗口融合 | 已完成 | 多任务/窗口融合、覆盖层策略、单调时钟 150ms 确认、规则与动作门控、根错配单次补查及 Activity 来源收口均已完成。 |
| 5. 窗口与节点恢复 | 已完成 | 有限退避、共享总预算、结构 generation、Content 分支失效、单调短缓存、rotation/display 门控和节点重定位均已接入；米游社/B 站专项真机验收通过。 |
| 6. 动作执行器增强 | 已完成 | 统一执行器、窗口/显示屏门控、最近父节点策略和只观察验证均已接入；B 站安全导航真机在 104ms 内命中 `Verified/GenerationChanged`，无重复动作。 |
| 7. APK 内置 RootService | 已完成 | 协议 2、完整规则链、有界重连/停连、拒绝/超时、撤权后收口、A11y 回退、覆盖更新和重新授权均已真机通过。 |
| 8. 多用户与多显示屏 | 未开始 | |
| 9. 稳定性与维护收口 | 未开始 | |

状态仅使用：`未开始`、`进行中`、`已完成`、`阻塞`。

## 11. 当前决策

- 采用“单 APK + APK 内置 root 子进程”的方向。
- 不开发 LSPosed 模块，不依赖 system_server Hook。
- 规则和订阅留在普通 App 进程。
- 先修可观测性、错误结果和调度，再接入 root；禁止把 root 当成掩盖引擎问题的替代品。
- 现有 KernelSU/SukiSU 模块包装在本计划完成前保持独立，不作为新运行时的前置依赖。
