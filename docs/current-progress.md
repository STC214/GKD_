# GKD Root 加强版当前进度

更新时间：2026-07-15

## 当前结论

项目已经完成 Root 桥真实性与自恢复、结构化规则诊断，以及动作结果可信化改造。米游社星穹铁道未签到页已完成端到端验收：修复第三方旧选择器、规则汇总晚于自动化连接时不补查的启动竞态，并为慢响应页面增加窄范围防重复冷却。所有兼容均不改写订阅文件。当前安装在测试手机上的 GKD 仍以普通 App 身份保留原版订阅、数据库和备份能力，同时通过 UID 0 的 Root UserService 执行特权操作。

阶段 3“查询唤醒状态机”、阶段 4“前台与焦点窗口融合”和阶段 5“窗口与节点恢复”均已完成。窗口切换期 root 缺失在普通前台确认之前进入 50/100/200/400/800ms 共享总预算；State 与 display/rotation 驱动结构 generation，Content 采用分支失效，节点缓存使用单调短时效，旧选择结果动作门控和节点 refresh/同窗口重新定位已经形成闭环。Android 16 真机冷启动采样已自然命中一次缺 root：首次恢复在 `attempt=1/5`、50ms 调度；约 10ms 后更新的米游社 task/window 已取得 root，旧恢复被新事件自然取代，未出现恢复耗尽。

## 阶段状态

| 阶段 | 状态 | 当前结果 |
| --- | --- | --- |
| 0. 兼容基线与复现集 | 进行中 | APK、签名、订阅、备份、Root 原始快照和窗口场景基线已建立；米游社 B01 已定位，B02、规则重复统计和剩余窗口场景继续采样。 |
| 0.5 Root 桥真实性与自恢复 | 已完成 | Root UserService UID 0、系统 Binder 自检、三次有限重连、超时取消、关闭优化中止重连和 ExposeService 前台契约均已真机验收。 |
| 1. 结构化决策诊断 | 已完成 | 米游社有效 B01 在窗口 root 可用后持续终止于 `SelectorMiss`，没有动作提交；诊断已完成真实漏执行闭环。 |
| 2. 动作结果误判修复 | 已完成 | Root 输入逐事件返回真实结果，无障碍手势区分完成/取消/拒绝/超时；失败不消耗规则次数或冷却。 |
| 3. 查询唤醒状态机 | 已完成 | 单 runner、有界 pending、最新事件补查、常量空间事件缓冲和规则晚加载补查均已实现并验证。 |
| 4. 前台与焦点窗口融合 | 已完成 | 覆盖层分类、150ms 有界确认、规则查询/动作前门控和结构化诊断已接入；普通应用、SystemUI、输入法、权限弹窗、真实画中画均已真机验收。 |
| 5. 窗口与节点恢复 | 已完成 | 审查确认的恢复不可达、Content 全局换代、预算重置和缓存时效问题均已修复；米游社/B 站专项采样命中真实缺 root 恢复，未见孤儿事件链、恢复耗尽或崩溃。 |
| 6. 动作执行器 | 已完成 | 统一执行上下文和 350ms 只观察验证已接入；B 站安全导航真机在 104ms 内命中 `Verified/GenerationChanged`，动作字段完整且无重复提交。 |
| 7. APK 内置 RootService | 已完成 | 协议 2 常规连接、无 Shizuku 完整规则链、有界重连/停连、拒绝/超时、撤权收口、A11y 回退、覆盖更新及重新授权均已真机验收。 |
| 8～9 | 未开始 | 后续处理多用户、多显示屏和稳定性维护收口。 |

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
- root 缺失恢复只能由显式切换信号开启，3 秒内最多消费五级退避，成功或耗尽后立即关闭。
- 窗口 State/App/亮屏/动作及 display/rotation 变化推进结构 generation；Content 事件仅失效对应查询分支，旧结构 generation 的匹配节点不能执行。
- 动作前刷新目标节点；失败时仅允许在相同 packageName/windowId/displayId/generation 中重新取 root 并复跑同一规则。
- focused Application Window 已存在但 root 暂空时，可在不放宽动作门控的前提下于普通确认前进入有限恢复。
- Content 事件不推进全局 generation，动态 WebView 不会仅因持续内容更新而饿死动作；State 和 display/rotation 变化仍使旧结果失效。
- 节点默认缓存时效缩短为文本 500ms、结构 1000ms并使用单调时钟，保留原时效 Legacy 策略。
- 阶段 5 最终诊断缓存为 2048 条、95 个事件型关联 ID，事件链孤儿数为 0；其中米游社 62 条、哔哩哔哩 45 条（应用切换使目标集合可重叠），14 个合并事件链均保留后续决策。
- 五次米游社冷启动中自然命中一次 `WindowRootUnavailable → WindowRootRecoveryPending(attempt=1/5, delay=50)`；随后约 10ms 更新后的 task/window 出现 `WindowRootAvailable`，旧恢复被新事件取代，没有 `WindowRootRecoveryExhausted`。
- 统一 `ActionExecutor` 在规则语义与具体动作后端之间接管提交策略；结果携带 target、backend、displayId、windowId、rotation、windowBounds、visibleBounds 和 retryCount。
- Root 坐标输入显式指定 displayId；Root 失败转无障碍手势前再次通过同一窗口 guard，非默认显示屏没有对应手势能力时直接拒绝。
- 节点 API 明确拒绝时最多尝试最近一个可见且可点击的父节点；已接受/已完成动作、更多祖先及 back/swipe 等未知副作用动作不自动重复。
- 成功的 click/longClick/back 等动作最多观察 350ms；节点消失、窗口或 generation 变化升级为 `Verified`，无信号为 `Inconclusive`，两者都不会重复输入。
- `none`、`swipe` 和失败动作跳过默认验证；无法确认不会被写成 `ActionVerificationFailed`，后者只保留给未来显式、可证明的验证条件。
- 坐标点击、长按和滑动由 `PrivilegedInputBridge` 显式选择 APK RootService、Shizuku/Sui、无障碍手势；诊断分别记录 `ApkRoot`、`Shizuku`、`Accessibility`。
- Root 请求边界取已确认窗口与可见区域交集。只有明确拒绝/不可用才允许在窗口 guard 复核后降级；Binder 或逐事件不确定失败不会跨后端重试。

## 当前构建和真机状态

- 当前 Git HEAD：`ff902ba7`；阶段 6、阶段 7 前两切片及对应文档已提交，第三切片显式特权桥仍在工作区。
- Release APK：`app/build/outputs/apk/gkd/release/app-gkd-release.apk`
- 当前本地 APK SHA-256：`0A59223FCCF6752D77CF94D8978FCF9FA5A34FDB463B27A07B150E3E57BF3C12`
- APK 大小：3,373,088 字节。
- App Debug 单元测试：123/123；阶段 7 包含调用者/身份策略、结构化输入参数、特权后端顺序/失败语义、前台 Task AIDL 映射和 2 项旧设置兼容测试。
- Selector JVM 测试：18/18。
- Release 构建与 `lintVital`：通过。
- 阶段 7 使用独立包名 `li.songe.gkd.debug` 真机验收：首次握手 PID `26957`、UID 0、协议 1；修复死亡原因覆盖后 PID `29808` 被杀，主进程 PID `20830` 保持运行并显示 `失败（binder died）`；重连得到 PID `30430`。
- 阶段 7 第三切片再次安装独立 Debug 包，设备侧 `base.apk` 与本地 Debug APK SHA-256 均为 `213CBDFADAB3413A0D7DF34D9718C0E18C955A5E8A9517EE965CAC0C7DDB66EB`。SukiSU 刷新旧 UID `10391` 后为当前 UID `10392` 授权，冷启动连接得到 APK RootService PID `9325`、UID 0、协议 1。
- 在 B 站 `MainActivityV2` 对 `@[vid="category_click_area"]` 执行 `clickCenter`，结果为 `Completed`、`backend=ApkRoot`、`shell=true`、坐标 `(1005.5,274.5)`、display `0`、window `2463`、rotation `0`，窗口/可见边界均为 `(0,0)-(1079,2399)`，随后进入 `GeneralActivity`。
- 杀死 PID `9325` 后 Debug 主进程 PID `29125` 保持运行；返回同一页面再次执行相同动作，结果为 `Completed`、`backend=Accessibility`、`shell=false`，仍只进入一次 `GeneralActivity`，无新增 GKD `AndroidRuntime`。这验证了 Root 不可用时的无崩溃安全降级，不代表 Binder 不确定失败可以重试。
- 一次性规则 `-1/app/9904/0` 未进入动作：Debug 未取得可信 Task 采样时快照为 `Probable`，150ms 前台确认门正确阻断。该结果证明规则门控没有为测试放宽，也暴露出普通规则链仍依赖可信前台任务来源。
- 第四切片独立 Debug 包 UID `10393`：默认关闭时首次启动没有 root 子进程；SukiSU 授权并开启后，无需打开状态弹窗即连接 APK RootService PID `3725`、UID 0、协议 2。关闭 Shizuku 后直接安全点击返回 `Completed/ApkRoot`，完整规则 `-1/app/9906/0` 又从 B 站 `MainActivityV2` 自动导航到 `GeneralActivity`，证明 Root Task 前台确认和 APK Root 输入在无 Shizuku 条件下形成闭环。
- 首次复验规则 `-1/app/9905/0` 未再次执行并非前台误判：决策诊断明确记录 `RuleActionMaximumReached`；更换新组键后导航成功。杀死 Root PID `13166` 时主进程 `5204` 保持，0.5 秒无 root、1.0 秒出现新 PID `27212`；关闭开关 6 秒无重连，重新开启 2 秒内出现 PID `28481`。
- 阶段 7 拒绝/超时专项使用 Debug UID `10394`。SukiSU 超级用户关闭时，Root 增强最终显示 `失败（connection timeout）`；额外观察 10 秒仍只有主进程且没有循环请求。外部撤权保存 `allow=0` 后不会追溯杀死既有 UID 0 PID `20360`，这是 SukiSU 的进程级授权语义；结束该 PID 后 10 秒没有新 root，主进程 `22745` 保持。
- 撤权且 Shizuku 关闭时，B 站安全点击返回 `Completed/Accessibility/shell=false`，只发生一次 `MainActivityV2 → GeneralActivity`。保留数据覆盖更新后 UID `10394`、Root 开关、无障碍绑定和设置均保留，但仍没有 root；重新授权并冷启动后恢复 UID 0 RootService PID `3653`。为避免用户把外部撤权误认为即时终止，Root 开关副标题新增“需立即撤权请先关闭此开关”。
- Debug 包强停后 root 子进程自动退出；覆盖更新时旧进程终止；两轮验收结束后 Debug 包均已卸载，HTTP 服务、ADB 转发、临时无障碍绑定和手机 Download 测试文件已清理。正式版 PID `4014`、Root UserService PID `28853`/UID 0 与前台 StatusService 未受影响。
- 测试设备：Xiaomi Android 16，KernelSU + Sui。
- 最终 Root UserService：UID 0；两个临时探针 App、HTTP 临时服务和 ADB 端口转发均已清除。
- 当前本地 Release 已非清数据覆盖安装到测试手机；版本 `1.12.1-abfc983-dirty`，阶段 6 安全动作清理并重启后主进程 PID `4014`，Root UserService PID `28853`、UID 0，StatusService 为前台服务。米游社/B 站启动、切换和动态页面滚动后进程稳定，无新增 GKD 崩溃。
- 阶段 6 真机证据：一次性规则 `-1/app/9903/0` 点击 B 站“分区”，104ms 内记录 `ActionSucceeded(state=Verified)` 与 `ActionVerified(signal=GenerationChanged)`；`target=Node`、`backend=Node`、`display=0`、`window=2447`、rotation、窗口/可见边界和零重试均已导出。完整记录位于忽略目录 `local-assets/diagnostics/root-runtime/phase6-safe-action/decision-diagnostics-final.txt`。
- 测试清理：HTTP 内存订阅及对应动作日志已由应用删除，ADB 转发已移除，触发总数从测试后的 7728 恢复为测试前 7727；订阅页只保留原有本地/正式订阅。
- 手机重启后 KernelSU `u:r:ksu:s0` 被 SELinux 拒绝直接读取 App 私有目录，因此本轮没有重复声称文件哈希一致；非清数据证据来自 App 自身成功加载原订阅与动作计数。
- 最终检查无新增 GKD `AndroidRuntime` 崩溃。
- 阶段 5 专项采样结束时主进程仍为 PID `24515`，Root UserService PID `18339`、UID 0，StatusService 为前台；诊断开关原本开启且测试后保持开启。

## 当前工作区主要改动

- 米游社兼容：`RuleSelectorCompat.kt`、`ResolvedRule.kt`、`RuleSelectorCompatTest.kt`；原神/绝区零保留“每日签到”标题锚点，星铁使用 `6 + 3 + TextView` 奖励结构签名并设置 5 秒最短冷却。
- 启动补查：`A11yState.kt`、`A11yFeat.kt`、`RuleSummaryRefresh.kt`、`A11yRuleEngine.kt` 在规则汇总晚加载后刷新当前 Activity 规则并 forced query；监听包含订阅时的当前值，不再使用 `drop(1)`。
- 查询唤醒：新增 `QueryWakeState.kt` 和 `QueryWakeStateTest.kt`；`A11yRuleEngine.kt` 使用有界 pending handoff，`DecisionReason.kt` 新增 `QueryDeferred`。
- 前台融合：`TaskInfoHidden.java`、`WindowConfiguration.java` 补齐任务和画中画字段；`ForegroundTask.kt`、`ForegroundSnapshot.kt`、`ForegroundSnapshotProvider.kt` 建立多任务、窗口和覆盖层采样；`ForegroundConfirmationState.kt` 提供 150ms 有界确认；`A11yRuleEngine.kt` 统一查询和动作门控。
- 窗口恢复：新增 `WindowRootRecoveryState.kt`、`WindowGenerationState.kt`；阶段 5 累计 16 项测试，`A11yContext.kt` 按 generation 清缓存，`A11yRuleEngine.kt` 接入五级退避、旧代门控和节点重新定位。
- 阶段 5 审查修复：`ForegroundSnapshot.canRecoverMissingRoot` 将主要 root 挂载竞态接入有限恢复；`NodeCachePolicy.kt` 提供 Default/Legacy 策略；Content/State generation 分流，rotation 进入快照令牌。
- 阶段 6 第一批：新增 `ActionExecutor.kt` 和 `ActionExecutionContextTest.kt`；`GkdAction.kt` 统一动作上下文/结果，`ResolvedRule.kt` 和 `A11yRuleEngine.kt` 接入执行 guard，Root 输入链显式传递 displayId。
- 阶段 6 第二批：新增 `ActionVerification.kt` 和 `ActionVerificationStateMachineTest.kt`；`WindowGenerationState.kt` 提供 generation/窗口上下文独立比较，规则引擎接入只观察验证与结构化诊断。
- 阶段 7 第一轮审查：当前 `IUserService.execCommand(String)` 和基于 shell 拼接的 tap/swipe 仅保留为 Shizuku/Sui 兼容后端；新 RootService 采用结构化最小 AIDL，优先普通非 daemon libsu 方案，首批只做身份握手、调用方校验和死亡回退。
- 阶段 7 第一最小切片：新增 `root/IRootService.aidl`、`GkdRootService.kt`、`RootServiceClient.kt`、调用者策略和身份策略；固定 libsu 6.0.0。AIDL 只有协议、PID、UID、包名四个只读字段，设置弹窗按需连接，尚未接管输入。
- 阶段 7 真机审查修复：Binder 死亡会连续触发 DeathRecipient 与 `onServiceDisconnected`；后者现在保留已有具体失败原因，不再把 `binder died` 降级为普通“未连接”。
- 阶段 7 第二切片：`RootInputRequest` 只携带数值动作、displayId、坐标、时长和窗口边界；AIDL 事务 5 返回 Completed/Rejected/Unavailable/Failed。服务端完成 NaN/Infinity、越界、时长、动作类型和 Tap 规范化门控，尚未从规则链调用。
- 阶段 7 第三切片：新增 `PrivilegedInputBridge.kt` 和 5 项测试；`GkdAction.kt` 的坐标点击、长按、滑动已接入显式 Root → Shizuku → A11y 顺序，后端诊断拆分且不确定失败禁止重复提交。
- 阶段 7 第四切片：`SettingsStore.enableApkRoot` 默认关闭，`RootServiceLifecycle` 负责开关驱动连接及最多两次有界重连；AIDL 协议 2 的事务 6 返回 `RootForegroundTask`，`ForegroundSnapshotProvider` 优先 Root Task、回退 Shizuku。旧设置缺字段仍解码为关闭，规则/订阅/数据库结构不变。
- 阶段 7 最终审查：`RootServiceClient` 改为每次绑定创建独立 `ServiceConnection`，并以连接代次校验回调、超时和死亡通知；显式关闭或新一轮连接后，旧异步回调不能再恢复连接或覆盖当前状态。Root 专项单元测试通过。
- 构建追溯：`app/build.gradle.kts` 为未提交工作区版本追加 `-dirty`。
- 动作结果链路：`GkdAction.kt`、`A11yService.kt`、`InputManager.kt`、`InputShellCommand.kt`、`ShizukuApi.kt`、`A11yRuleEngine.kt`。
- 输入与 Binder 回归：`InputSequenceResult.kt`、`InputSequenceResultTest.kt`、`SafeInvokeShizukuTest.kt`。
- 诊断导出：`AdvancedPage.kt`、`scripts/analyze-decision-diagnostics.ps1`。
- Root 超时故障注入：`scripts/stop-next-root-user-service.sh`。
- 计划、测试和上游合并说明：主计划、基线文档和差异指南。

## 本轮开发停点

- 最后完成项：阶段 7 全部代码与 Android 16 真机收口。拒绝/无回调不循环、撤权后的进程死亡收口、A11y 回退、覆盖更新和重新授权恢复均已通过；SukiSU 不追溯结束既有 root 进程的边界已在界面和文档明确。最终 App 123/123、Selector 18/18、Release/R8/Vital Lint 通过；最终审查另修复显式关闭与旧绑定回调竞态，Root 专项测试和增量 Release 产物验证通过。当前 Release SHA-256 为 `0A59223FCCF6752D77CF94D8978FCF9FA5A34FDB463B27A07B150E3E57BF3C12`。
- 首次标题锚点方案被真机探针否定：快照中的 WebView 文本不可供活动选择器查询；文档和测试现以真实 50 节点拓扑为准，不再保留该假设。
- 下次继续时不需要重复阶段 2 的手势取消、动作计数恢复和订阅哈希验收；只有 APK、签名、规则格式或相关执行链再次变化时才重跑对应基线。
- 阶段 6 已用 B 站“分区”安全导航完成 `ActionVerified` 验收，不需要为了样本点击米游社签到。`Inconclusive` 与父节点回退仍保留单元测试及未来自然采样，不添加生产调试后门，也不阻塞阶段完成。
- 当前 Release APK 为工作区构建 `1.12.1-ff902ba-dirty`；阶段 7 第三、第四切片代码和文档尚未提交。独立 Debug 包已完成真机验收并卸载，正式版未被覆盖。

## 下一步

阶段 7 已完成并通过最终代码审查，后续按以下顺序继续：

1. 阶段 7 作为一个提交边界保留：AIDL/Root 生命周期/动作桥/Task 采样/测试/文档必须一起提交，不拆出会造成中间不可用状态的子提交；不再扩大 RootService AIDL。
2. 进入阶段 8，多用户、多显示屏和厂商窗口适配优先从可控 displayId/userId 基线开始。
3. 阶段 0 的 B02、规则重复统计和剩余窗口自然样本继续并行积累，不阻塞已完成阶段。

详细实施顺序见 [`root-runtime-refactor-plan.md`](root-runtime-refactor-plan.md)，代码级迁移方法见 [`upstream-code-delta-guide.md`](upstream-code-delta-guide.md)，真机证据见 [`testing/root-runtime-baseline.md`](testing/root-runtime-baseline.md)。
