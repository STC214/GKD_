# GKD Root 加强版当前进度

更新时间：2026-07-15

## 当前结论

项目已经完成 Root 桥真实性与自恢复、结构化规则诊断，以及动作结果可信化改造。米游社星穹铁道未签到页已完成端到端验收：修复第三方旧选择器、规则汇总晚于自动化连接时不补查的启动竞态，并为慢响应页面增加窄范围防重复冷却。所有兼容均不改写订阅文件。当前安装在测试手机上的 GKD 仍以普通 App 身份保留原版订阅、数据库和备份能力，同时通过 UID 0 的 Root UserService 执行特权操作。

阶段 3“查询唤醒状态机”和阶段 4“前台与焦点窗口融合”均已完成。多任务候选、焦点窗口/root 包、统一置信度、覆盖层分类、单调时钟 150ms 有界确认及规则动作门控已形成单一链路；最终审查又收口了根错配无限补查、旧空规则集提前拦截、事件 Activity 覆盖 Task Activity 三条控制流缺口。Android 16 的输入法、权限控制器和真实画中画特殊拓扑均已形成回归测试并由最终 APK 重放。

## 阶段状态

| 阶段 | 状态 | 当前结果 |
| --- | --- | --- |
| 0. 兼容基线与复现集 | 进行中 | APK、签名、订阅、备份、Root 原始快照和窗口场景基线已建立；米游社 B01 已定位，B02、规则重复统计和剩余窗口场景继续采样。 |
| 0.5 Root 桥真实性与自恢复 | 已完成 | Root UserService UID 0、系统 Binder 自检、三次有限重连、超时取消、关闭优化中止重连和 ExposeService 前台契约均已真机验收。 |
| 1. 结构化决策诊断 | 已完成 | 米游社有效 B01 在窗口 root 可用后持续终止于 `SelectorMiss`，没有动作提交；诊断已完成真实漏执行闭环。 |
| 2. 动作结果误判修复 | 已完成 | Root 输入逐事件返回真实结果，无障碍手势区分完成/取消/拒绝/超时；失败不消耗规则次数或冷却。 |
| 3. 查询唤醒状态机 | 已完成 | 单 runner、有界 pending、最新事件补查、常量空间事件缓冲和规则晚加载补查均已实现并验证。 |
| 4. 前台与焦点窗口融合 | 已完成 | 覆盖层分类、150ms 有界确认、规则查询/动作前门控和结构化诊断已接入；普通应用、SystemUI、输入法、权限弹窗、真实画中画均已真机验收。 |
| 5～9 | 未开始 | 按主计划依次处理窗口恢复、动作增强、内置 RootService、多用户和稳定性收口。 |

阶段 0 尚未关闭的是规则重复统计、B02 和部分依赖真实 App 时机的窗口样本；这些长期采样不阻止已经具备独立测试证据的可靠性修复继续推进。米游社 B01 已补入基线和诊断文档。

## 已完成的关键能力

- Root UserService 使用真实远端 `id` 和 UID 判断能力，不再把“已授权”误写成“Root 已可用”。
- Root 连接超时、取消、迟到回调和重连期间关闭优化均有确定的释放与停止语义。
- 规则决策可以按关联 ID 回看事件、前台、窗口、规则状态、选择器和动作结果。
- 哔哩哔哩高频样本包含 740 条记录、200 个关联 ID，事件型关联 ID 没有孤立 `EventReceived`。
- Root 点击、长按和滑动只有完整输入序列成功才返回 `Completed`。
- 无障碍手势只有系统 `onCompleted` 才成功；`onCancelled`、提交拒绝和超时均失败。
- Shizuku 远端 Binder 异常统一转换为不可用结果，输入动作可以安全回退，不再让异常中断本轮动作链。
- 诊断文件在 IO 调度器写入，外部存储异常会被记录和提示，不阻塞 Compose 主线程。
- `ActionResultState` 区分 `Accepted`、`Completed`、`Verified`、`Cancelled`、`Rejected` 和 `TimedOut`，不再把节点接受或手势提交冒充界面验证。
- 规则级取消真机证据为 `ActionSubmitted → ActionCancelled(state=Cancelled)`；动作计数不变，随后规则仍为 `RuleEligible`，未进入次数上限或冷却。
- 原版动作字符串、选择器、订阅 JSON/JSON5、在线更新、导入导出、Room 数据和备份格式保持兼容。
- 星铁真机规则目标由第 14 天未签到扁平节点精确转换为已签到三子节点块，累计签到从 13 天变为 14 天；签到成功弹窗已由后续规则关闭。
- 自动化先连接、应用规则后加载时，会以 `scene=RuleSummary` 刷新当前 Activity 并执行受 forcedTime 约束的补查。
- 查询运行期间的新请求不再以 `QueryAlreadyRunning` 直接丢弃，而是记录 `QueryDeferred` 并合并为一个后续唤醒。
- 同类事件风暴最多保留最后两个节点事件；混合事件只保留“改用新 root 查询”的标志，缓冲空间不随事件数量增长。
- Task 前台判断查询最多 16 条候选，并在 Android 12+ 优先选择目标显示屏上 focused、visible、running 的任务；Android 10/11 因平台缺少字段而明确保留任务顺序回退。
- `ForegroundSnapshot` 同时保留 taskId、userId、displayId、windowId、Activity 和焦点窗口 root 包；Task/窗口包冲突时标记 `Conflict` 且 `canExecute=false`。
- 只有 focused、visible、running 的 Task 与 focused Window root 包一致时才产生 `Confirmed`；TaskStack 两种系统回调都重新采样同一个前台选择器。
- 窗口 surface 显式区分普通应用、输入法、SystemUI、权限控制器、画中画、无障碍覆盖层和其他系统覆盖层；只有 `Confirmed + Application` 允许执行。
- Task/窗口冲突仅等待一次 150ms 并合并复查，同一冲突到期即拒绝，不做周期轮询。
- 根节点包持续错配时，每个 task/window/root 上下文最多补查一次；延迟确认在空规则检查前先应用新前台。
- TaskStack Activity 非空时保持权威，状态事件只在 Activity 缺失时兜底；确认期限使用单调时钟。
- 规则查询和动作提交前都重新确认同一融合快照；taskId、windowId 或 appId 变化时以 `StaleContext` 终止。

## 当前构建和真机状态

- 当前 Git HEAD：`3d7e18e3`；阶段 3、米游社兼容和阶段 4 第一批审查修复已提交，本轮覆盖层分类、150ms 确认、规则接入、测试和文档仍在工作区，尚未提交。
- Release APK：`app/build/outputs/apk/gkd/release/app-gkd-release.apk`
- 当前本地 APK SHA-256：`F0814E2C6A33DEA4DE2AE79A4CDDEE2DC4F2D963F6E48F92EC05354D6B189EC1`
- APK 大小：3,320,187 字节。
- App Debug 单元测试：72/72；除覆盖层/确认及真实拓扑回归外，新增根错配单次补查、Activity 来源和墙上时间回拨测试。
- Selector JVM 测试：18/18。
- Release 构建与 `lintVital`：通过。
- 测试设备：Xiaomi Android 16，KernelSU + Sui。
- 最终 Root UserService：UID 0；两个临时探针 App、HTTP 临时服务和 ADB 端口转发均已清除。
- 当前本地 Release 已非清数据覆盖安装到测试手机；版本 `1.12.1-3d7e18e-dirty`，主进程 PID `20425`，Root UserService PID `2793`、UID 0。安装后显式重启主进程和 Root UserService，StatusService 保持前台，Android 16 未出现 GKD 崩溃。
- 手机重启后 KernelSU `u:r:ksu:s0` 被 SELinux 拒绝直接读取 App 私有目录，因此本轮没有重复声称文件哈希一致；非清数据证据来自 App 自身成功加载原订阅与动作计数。
- 最终检查无新增 GKD `AndroidRuntime` 崩溃。

## 当前工作区主要改动

- 米游社兼容：`RuleSelectorCompat.kt`、`ResolvedRule.kt`、`RuleSelectorCompatTest.kt`；原神/绝区零保留“每日签到”标题锚点，星铁使用 `6 + 3 + TextView` 奖励结构签名并设置 5 秒最短冷却。
- 启动补查：`A11yState.kt`、`A11yFeat.kt`、`RuleSummaryRefresh.kt`、`A11yRuleEngine.kt` 在规则汇总晚加载后刷新当前 Activity 规则并 forced query；监听包含订阅时的当前值，不再使用 `drop(1)`。
- 查询唤醒：新增 `QueryWakeState.kt` 和 `QueryWakeStateTest.kt`；`A11yRuleEngine.kt` 使用有界 pending handoff，`DecisionReason.kt` 新增 `QueryDeferred`。
- 前台融合：`TaskInfoHidden.java`、`WindowConfiguration.java` 补齐任务和画中画字段；`ForegroundTask.kt`、`ForegroundSnapshot.kt`、`ForegroundSnapshotProvider.kt` 建立多任务、窗口和覆盖层采样；`ForegroundConfirmationState.kt` 提供 150ms 有界确认；`A11yRuleEngine.kt` 统一查询和动作门控。
- 构建追溯：`app/build.gradle.kts` 为未提交工作区版本追加 `-dirty`。
- 动作结果链路：`GkdAction.kt`、`A11yService.kt`、`InputManager.kt`、`InputShellCommand.kt`、`ShizukuApi.kt`、`A11yRuleEngine.kt`。
- 输入与 Binder 回归：`InputSequenceResult.kt`、`InputSequenceResultTest.kt`、`SafeInvokeShizukuTest.kt`。
- 诊断导出：`AdvancedPage.kt`、`scripts/analyze-decision-diagnostics.ps1`。
- Root 超时故障注入：`scripts/stop-next-root-user-service.sh`。
- 计划、测试和上游合并说明：主计划、基线文档和差异指南。

## 本轮开发停点

- 最后完成项：阶段 4 最终审查收口。根错配有界补查、确认重试顺序、Activity 权威来源和单调时钟四项均已修复，App 72/72、Selector 18/18、Release、Vital Lint 和真机进程冒烟通过。
- 首次标题锚点方案被真机探针否定：快照中的 WebView 文本不可供活动选择器查询；文档和测试现以真实 50 节点拓扑为准，不再保留该假设。
- 下次继续时不需要重复阶段 2 的手势取消、动作计数恢复和订阅哈希验收；只有 APK、签名、规则格式或相关执行链再次变化时才重跑对应基线。
- 恢复入口：阶段 4 已关闭，直接进入阶段 5 的窗口 generation、root 有限恢复和节点新鲜度改造。
- 工作区尚未提交；脏工作区 APK 版本为 `1.12.1-3d7e18e-dirty`。继续开发前保留本轮 `ForegroundConfirmationState`、覆盖层分类、`A11yRuleEngine` 接入、对应测试和文档改动。

## 下一步

进入阶段 5，按以下顺序实施：

1. 为窗口/root 暂时为空建立 50、100、200、400、800ms 的有界恢复状态机，只在切换窗口内启动。
2. 引入窗口 generation，并在窗口状态、内容、旋转和显示屏变化时失效旧上下文。
3. 使用节点前执行 `refresh()`；失败后重新取得 root 并核对 packageName、windowId、displayId 和 generation。
4. 禁止旧 generation 的选择结果进入动作提交，补齐并发与过期节点测试。
5. 在米游社和哔哩哔哩慢页面上进行真机回归，确认有限恢复提高命中率且不形成长期轮询。

详细实施顺序见 [`root-runtime-refactor-plan.md`](root-runtime-refactor-plan.md)，代码级迁移方法见 [`upstream-code-delta-guide.md`](upstream-code-delta-guide.md)，真机证据见 [`testing/root-runtime-baseline.md`](testing/root-runtime-baseline.md)。
