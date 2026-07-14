# GKD Root 加强版当前进度

更新时间：2026-07-14

## 当前结论

项目已经完成 Root 桥真实性与自恢复、结构化规则诊断，以及动作结果可信化改造。当前安装在测试手机上的 GKD 能以普通 App 身份保留原版订阅、数据库和备份能力，同时通过 UID 0 的 Root UserService 执行特权操作。

阶段 2“动作结果误判修复”已经完成，下一编码阶段为阶段 3“查询唤醒状态机”，目标是保证查询期间到达的新事件不会被丢弃。

## 阶段状态

| 阶段 | 状态 | 当前结果 |
| --- | --- | --- |
| 0. 兼容基线与复现集 | 进行中 | APK、签名、订阅、备份、Root 原始快照和窗口场景基线已建立；米游社签到等受时间限制的 B01/B02 现场继续保留采样。 |
| 0.5 Root 桥真实性与自恢复 | 已完成 | Root UserService UID 0、系统 Binder 自检、三次有限重连、超时取消、关闭优化中止重连和 ExposeService 前台契约均已真机验收。 |
| 1. 结构化决策诊断 | 进行中 | 代码、2048 条内存环形缓冲、关联 ID、导出和哔哩哔哩高频事件分析已完成；继续等待米游社等真实漏执行现场。 |
| 2. 动作结果误判修复 | 已完成 | Root 输入逐事件返回真实结果，无障碍手势区分完成/取消/拒绝/超时；失败不消耗规则次数或冷却。 |
| 3. 查询唤醒状态机 | 未开始 | 下一步实施。 |
| 4～9 | 未开始 | 按主计划依次处理前台融合、窗口恢复、动作增强、内置 RootService、多用户和稳定性收口。 |

阶段 0/1 未关闭的是依赖真实 App 时机的长期现场采样，不阻止已经具备独立测试证据的可靠性修复继续推进；一旦复现 B01/B02，仍须补回基线和诊断文档。

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

## 当前构建和真机状态

- 当前 Git HEAD：`d33249d1`；阶段 2 代码和本轮文档仍在工作区，尚未提交。
- Release APK：`app/build/outputs/apk/gkd/release/app-gkd-release.apk`
- 当前本地 APK SHA-256：`B83171CD124BD7ED27043CCD221D3DDB622018FA7E8362947DBBE513F6E0A548`
- APK 大小：3,320,183 字节。
- App Debug 单元测试：17/17。
- Selector JVM 测试：18/18。
- Release 构建与 `lintVital`：通过。
- 测试设备：Xiaomi Android 16，KernelSU + Sui。
- 最终 Root UserService：UID 0；HTTP 临时服务和内存订阅均已清除。
- 当前本地 Release 已非清数据覆盖安装到测试手机；主进程 PID `28892`，Root UserService PID `10058`、UID 0。本轮安装前后 `store.json` 与 3 个订阅文件 SHA-256 完全一致，`action_count.txt` 仍为 `7703`，清空 logcat 后未出现新增 GKD `AndroidRuntime`。
- `action_count.txt` 已恢复测试前的 `7703`。
- `store.json` 与 `-2.json`、`666.json`、`667.json` 的 SHA-256 均恢复测试前基线。
- 最终检查无新增 GKD `AndroidRuntime` 崩溃。

## 当前工作区主要改动

- 动作结果链路：`GkdAction.kt`、`A11yService.kt`、`InputManager.kt`、`InputShellCommand.kt`、`ShizukuApi.kt`、`A11yRuleEngine.kt`。
- 输入与 Binder 回归：`InputSequenceResult.kt`、`InputSequenceResultTest.kt`、`SafeInvokeShizukuTest.kt`。
- 诊断导出：`AdvancedPage.kt`、`scripts/analyze-decision-diagnostics.ps1`。
- Root 超时故障注入：`scripts/stop-next-root-user-service.sh`。
- 计划、测试和上游合并说明：主计划、基线文档和差异指南。

## 本轮开发停点

- 最后完成项：阶段 2 审查收口，包括远端 Binder 异常降级、诊断导出后台写入、App 第 17 项回归测试、Release/Vital Lint 和非清数据真机覆盖。
- 当前没有遗留的阶段 2 代码缺陷；米游社 B01/B02 仍属于等待真实时机的长期采样，不是当前编码阻塞项。
- 下次继续时不需要重复阶段 2 的手势取消、动作计数恢复和订阅哈希验收；只有 APK、签名、规则格式或相关执行链再次变化时才重跑对应基线。
- 恢复编码入口：从 `A11yRuleEngine.startQueryJob()` 的 `querying` 早退分支开始，先建立查询期间事件到达、当前查询结束和追加查询之间的状态转换测试，再替换现有直接返回逻辑。
- 工作区尚未提交；继续开发前保留现有修改，不要清理或覆盖本轮代码、测试脚本和文档。

## 下一步

进入阶段 3，按以下顺序实施：

1. 用 conflated channel、原子 pending/dirty 标志或等价状态机替换查询中直接返回的丢事件窗口。
2. 查询期间收到事件时只保留必要的最新状态，并在当前查询结束后立即追加一轮。
3. 保留优先规则中断语义，同时保证中断后消费最新事件。
4. 增加慢选择器、事件风暴、并发事件和动作完成后唤醒测试。
5. 真机复测米游社、哔哩哔哩以及已有固定窗口场景，并同步更新代码差异文档。

详细实施顺序见 [`root-runtime-refactor-plan.md`](root-runtime-refactor-plan.md)，代码级迁移方法见 [`upstream-code-delta-guide.md`](upstream-code-delta-guide.md)，真机证据见 [`testing/root-runtime-baseline.md`](testing/root-runtime-baseline.md)。
