# Root 运行时阶段 0 基线与复现记录

## 1. 状态

- 阶段：`0. 兼容基线与复现集`
- 状态：`进行中`
- 建立日期：`2026-07-14`
- 尚缺条件：可重复的米游社/哔哩哔哩故障样本、真实分屏/画中画/双开等窗口样本，以及 10 条规则的重复统计

本文件是后续所有运行时阶段共用的验收底稿。没有填写真实设备、规则和故障证据前，阶段 0 不得标记为完成，也不得把阶段 1 标记为完成。

## 2. 代码与上游基线

| 项目 | 基线值 |
| --- | --- |
| fork 仓库 | `https://github.com/STC214/GKD_.git` |
| fork 分支 | `main` |
| fork commit | `35c5d67a0252b6bf684be975be81efa8b6383efb` |
| 官方仓库 | `https://github.com/gkd-kit/gkd.git` |
| 官方 commit | `0b75375c0f40df62e93866e0e157d4e1ebc45c67` |
| 共同祖先 | `0b75375c0f40df62e93866e0e157d4e1ebc45c67` |
| 基线构建命令 | `.\gradlew.bat app:assembleGkdRelease --stacktrace` |
| 构建结果 | `BUILD SUCCESSFUL in 6m 8s`，87 个 task 已执行 |

## 3. 基线 APK

| 项目 | 值 |
| --- | --- |
| 文件 | `app/build/outputs/apk/gkd/release/app-gkd-release.apk` |
| applicationId | `li.songe.gkd` |
| variant | `gkdRelease` |
| versionCode | `92` |
| versionName | `1.12.1-35c5d67` |
| APK SHA-256 | `F44CB4FEEA3777806E40C4FB72E6BD55A7604E57F582B4B1554F8A6745262CDF` |
| 签名方案 | APK Signature Scheme v2 |
| 证书 DN | `C=US, O=Android, CN=Android Debug` |
| 证书 SHA-256 | `993B6D94D5AB2F34C95D95DFC6AB002E5BD939E6404BF36BC334998DBD8D9A9A` |

该 APK 使用本机 Debug 证书，是当前源码的开发基线包，不是正式发布签名包。安装前必须确认手机上现有 `li.songe.gkd` 的签名；签名不一致时先从 App 内导出备份，禁止直接卸载后假定数据仍然存在。

目录中旧的 `gkd-v1.12.1-5ab462d.apk` 仅是历史构建产物，不作为本阶段比较基线。

## 4. 真机信息

| 项目 | 设备 A | 设备 B（可选） |
| --- | --- | --- |
| 厂商/型号 | Xiaomi `23117RK66C` | 待填写 |
| Android 版本/API | Android 16 / API 36 | 待填写 |
| ROM/版本号 | `OS3.0.302.0.WNMCNXM`，build `BP2A.250605.031.A3` | 待填写 |
| Root 方案/版本 | KernelSU `4.1.3`（versionCode `40796`） | 待填写 |
| Root 授权通道 | Sui，`li.songe.gkd:shizuku-user-service` 以 root 运行 | 待填写 |
| 当前用户 ID | `0`；另检测到 user `999`，GKD 未安装到 user 999 | 待填写 |
| 屏幕分辨率/密度 | 物理 `1440x3200 @560`，覆盖 `1080x2400 @440` | 待填写 |
| 导航模式 | `navigation_mode=2`（手势导航） | 待填写 |
| GKD 自动化模式 | `automatorMode=2`（自动化/UiAutomation），`enableShizuku=true` | 待填写 |
| GKD 安装版本 | 采集前 `1.12.1-5ab462d`；已原地升级为 `1.12.1-35c5d67` | 待填写 |
| `adb shell su -c id` | `uid=0(root) gid=0(root) context=u:r:ksu:s0` | 待填写 |

## 4.1 自动测试基线

| 命令 | 结果 | 说明 |
| --- | --- | --- |
| `.\gradlew.bat app:assembleGkdRelease --stacktrace` | 通过 | 当前 HEAD Release APK 构建成功 |
| `.\gradlew.bat app:testGkdDebugUnitTest --stacktrace` | 通过 | App Debug 单元测试通过 |
| `.\gradlew.bat selector:jvmTest --stacktrace` | 通过，18/18 | 首次运行时 `QueryUnitTest.example3` 的旧期望把 `<2` 误写为 `<`；只修测试断言后全套通过，未改选择器实现 |
| `adb install -r app-gkd-release.apk` | 通过 | 相同证书、相同 versionCode 下从 `5ab462d` 原地升级到 `35c5d67`，App 数据保留 |

## 5. 配置和订阅备份

测试前必须从未修改版本导出一份可恢复备份，并复制到忽略目录：

```text
local-assets/device/root-runtime-baseline/
├── gkd-backup-<date>.zip
├── subscription-list.md
└── restore-result.md
```

记录：

- [x] 当前 HEAD APK 可以通过 App 自带流程导出备份。
- [x] 备份文件已计算 SHA-256，并通过 ZIP 完整性检查。
- [x] 在尚未修改 Root 运行时的当前 HEAD APK 上完成恢复；日志确认两次导入均到达“导入成功”。
- [x] 恢复后的订阅数量、设置、订阅文件哈希和局部规则配置一致。
- [x] 本地副本只保存在 `local-assets/`，不提交用户数据、设备序列号或订阅私密内容。

App 自带导出结果：

| 项目 | 结果 |
| --- | --- |
| 手机文件 | `Download/gkd-backup-1783996512454.zip` |
| 本地忽略副本 | `local-assets/device/root-runtime-baseline/gkd-backup-1783996512454.zip` |
| 文件大小 | 492,423 bytes |
| SHA-256 | `2FA650D5AABE4C126F5393BC59DFF96213724ADDCD3C0E359FEFF2054FD582C7` |
| ZIP 完整性 | 通过，9 个条目，无损坏条目 |
| 内容摘要 | 3 个订阅 JSON、3 条 `subsItems`、22 条 `subsConfigs`、5 个设置文件 |
| 安全状态 | `enableMatch=true`、`enableShizuku=true`、`automatorMode=2`；未在文档输出订阅链接或规则正文 |

App 自带导入结果：

| 项目 | 结果 |
| --- | --- |
| 导入日志 | `10:40:26.222` 和 `10:40:38.672` 开始，两次均在约 0.1 秒后记录“导入成功” |
| UI 现象 | 用户只看到“导入备份中...”，没有看到短暂的成功 Toast；日志和数据均证明导入成功，登记为提示可见性问题而非导入失败 |
| 恢复后快照 | `local-assets/device/root-runtime-baseline/post-import-20260714_104158/` |
| 文件一致性 | `store.json`、`-2.json`、`666.json`、`667.json` 与导入前 SHA-256 全部一致 |
| 配置计数 | 导入前后均为 `subs_item=3`、`subs_config=22`、`category_config=0`、`app_config=0` |
| 日志计数 | `action_log` 保持 110；`activity_log_v2` 从 500 增至 598，属于测试期间新增访问记录，不属于恢复配置 |

除 App 自带备份外，已建立一份仅用于灾难回退的 Root 原始快照：

| 项目 | 结果 |
| --- | --- |
| 升级前快照 | `local-assets/device/root-runtime-baseline/raw-20260714_101257/` |
| 快照文件数/原始大小 | 18 个文件 / 6,486,002 bytes |
| 压缩包 SHA-256 | `E98DEC7621466AF1EB6825D22224B42D1785FC09F23FBE4A23D4BB289791E9C8` |
| 升级后快照 | `local-assets/device/root-runtime-baseline/post-upgrade-20260714_102354/` |
| 不变文件 | `store.json`、`-2.json`、`666.json`、`667.json` 的升级前后 SHA-256 全部一致 |
| 数据库计数 | `subs_item=3`、`subs_config=22`、`action_log=110`、`activity_log_v2=500` |

原始快照已成功解包并生成逐文件 SHA-256，但它不能代替 App 自带导出/导入兼容测试。

## 6. 代表性规则复现表

必须填入实际订阅、实际规则和明确操作步骤。每条规则至少重复 10 次；“偶尔失败”必须记录失败次数，不能只写主观描述。

| 编号 | 覆盖能力 | 订阅/应用/组/规则 | 前置条件与操作步骤 | 成功/总数 | 当前异常 | 证据路径 |
| ---: | --- | --- | --- | ---: | --- | --- |
| R01 | 节点点击 | `667/tv.danmaku.bili/g7/r0/key1`，局部广告-视频页广告 | 打开哔哩哔哩视频页并出现对应广告；历史动作日志已有 3 次命中 | 0/0 | 待重复测试 | 待采集 |
| R02 | 坐标点击 | `666/com.tencent.mm/g0/r1/key1`，朋友圈广告 | 打开朋友圈并出现广告卡片 | 0/0 | 待重复测试 | 待采集 |
| R03 | 长按 | `667/com.openai.chatgpt/g2/r0/key0`，`longClickCenter` | 在明确理解该功能组行为后单独启用并触发；测试前确认不会影响重要会话 | 0/0 | 待重复测试 | 待采集 |
| R04 | 滑动 | `667/com.google.android.gm/g2/r0/key1`，`swipe` | Gmail 出现规则对应的信息流广告 | 0/0 | 待重复测试 | 待采集 |
| R05 | 返回 | `666/org.telegram.messenger/g1/r0`，通知权限提示 | 触发 Telegram 通知权限提示 | 0/0 | 待重复测试 | 待采集 |
| R06 | 前置规则 `preKeys` | `666/com.google.android.gm/g2/r1/key1` | 与 R04 同场景，确认前置规则顺序和次数 | 0/0 | 待重复测试 | 待采集 |
| R07 | 匹配延迟 `matchDelay` | `667/com.xunmeng.pinduoduo/g22/r2/key2`，自动处方流程 | 仅在可安全退出的测试流程中触发 | 0/0 | 待重复测试 | 待采集 |
| R08 | 动作延迟 `actionDelay` | `666/com.tencent.mm/g10/r0`，微信小程序开屏广告 | 打开会出现开屏广告的小程序并计时 | 0/0 | 待重复测试 | 待采集 |
| R09 | 冷却/次数上限 | `666/com.tencent.mobileqq/g7/r0/key1`，登录授权 | 触发登录授权页面，核对同一页面重复执行次数 | 0/0 | 待重复测试 | 待采集 |
| R10 | 强制匹配/超时 | `666/com.mfcloudcalculate.networkdisk/g1/r3/key3`，弹窗广告 | 打开目标应用并等待弹窗出现/超时 | 0/0 | 待重复测试 | 待采集 |

设备当前订阅脱敏统计：3 个订阅，其中 `666` 含 3217 条规则、`667` 含 3718 条规则；能力覆盖包括 571 条 `preKeys`、82 条 `actionDelay`、6 条 `matchDelay`、175 条次数上限、31 条 `forcedTime`、127 条 `clickCenter`、16 条长按、2 条显式滑动和 113 条返回动作。上表只完成“真实规则选择”，尚未完成每条 10 次的统计验收。

至少锁定两个核心案例：

- [x] B01：规则应该执行但未执行，可稳定或统计性复现。`2026-07-15` 米游社有效现场已留存完整诊断链和正负快照结构。
- [ ] B02：动作实际未生效，但旧引擎将其计为成功，可稳定或统计性复现。

### 6.1 B01 候选证据：前台来源竞态

对升级前保存的 7 个 GKD 日志文件进行离线统计，得到：

| 指标 | 结果 |
| --- | ---: |
| `updateTopActivity` 总数 | 5600 |
| TaskStack 来源 | 2916 |
| A11yRuleEngine 来源 | 2681 |
| 单秒最大更新数 | 10 |
| 每秒至少 5 次更新的秒数 | 31 |
| TaskStack 与 A11y 在 250ms 内指向不同 `package + activity` | 149 |
| 上述真实冲突涉及的秒级窗口 | 131 |
| `A11yRuleEngine.fixAppId` 主动纠正 | 140 |

已排除仅末尾内部序号不同、但 `package + activity` 相同的正常更新。可确认的样例包括：

- `com.miui.home/.launcher.Launcher` 与 `com.tencent.mobileqq/.activity.SplashActivity` 在 117ms 内先后覆盖；
- `tv.danmaku.bili/.MainActivityV2` 与视频详情 Activity 在 9ms 内互相覆盖；
- `com.tencent.mm/.plugin.webview.ui.tools.MMWebViewUI` 与 `com.tencent.mm/.ui.LauncherUI` 在 4ms 内互相覆盖；
- 系统分身选择页与游戏 Activity 在同一毫秒内分别由 TaskStack/A11y 上报。

这组数据证明当前前台上下文存在高频竞态，符合“规则偶发不执行”的风险条件，但旧版日志没有查询关联 ID和终止原因，尚不能证明某一次冲突必然导致某条规则漏执行。因此它登记为 `B01-CANDIDATE-FOREGROUND-RACE`，必须在阶段 1 结构化诊断启用后与规则查询记录关联，当前不把 B01 标记为已验收。

日志还记录过一次 Sui/Shizuku 用户服务 `connect timeout`，随后约 12 秒显示“自动化已启动”。该事件登记为启动通道候选，不与前台竞态合并为同一故障。

### 6.2 米游社签到页：已签到负样本

用户确认历史上最常见的漏执行场景是米游社签到页，其次是哔哩哔哩。`2026-07-14 10:53:56` 建立第一份米游社现场，但当天签到已经完成，因此本次只作为负样本，不计入规则成功或失败次数。

当前规则事实：

- 只有订阅 `667` 启用，旧订阅 `666` 已停用，不存在两份订阅同时执行。
- 当前订阅版本为 `541`。
- 米游社明确启用组 `3/6/7/8`：自动打卡、全屏游戏版本活动、分段游戏版本活动、米游自动签到全家桶。
- 签到全家桶组只匹配 `.web2.MiHoYoWebActivity`，共有 4 条链式规则。
- 规则 0 负责查找可签到入口；规则 1–3 分别依赖前置规则 0、1、2。

现场时序：

```text
10:53:56 SplashActivity
10:53:59 HyperionMainActivity
10:53:59 MiHoYoWebActivity
10:54:03 HyperionMainActivity
10:54:06 MiHoYoWebActivity
10:54:08 HyperionMainActivity
```

在两次进入 `MiHoYoWebActivity` 时，规则 0 状态为 `ok` 但没有选择器命中和动作日志；规则 1–3 均为“需要提前触发某个规则”。由于当天已经签到，可签到目标节点理应不存在，因此“无动作、无 Toast”符合规则链设计，不能作为漏执行证据。

同时确认：

- WindowManager 和 ActivityTaskManager 对当前包名、Activity、display 0、user 0 的判断一致。
- 外部 `uiautomator dump` 没有生成节点文件；当前 GKD 正占用 UiAutomation 通道，后续不再把外部 uiautomator 作为主要节点证据。
- 下次有效复现时使用 APK 内已有的 `ExposeService --ei expose 0` 在 GKD 自身通道中生成快照，再与结构化诊断关联。

有效复现条件：次日首次进入尚未签到的签到页，若页面停留且没有动作/Toast，立即保存 GKD 快照、Window/Task 状态、动作日志计数和时间戳。

截至 `2026-07-14` 首次负样本，当时业务状态不具备再次签到条件，用户同意先跳过并继续其他阶段 0 项目；当时 B01 保持未验收。后续有效现场见下一节。

### 6.2.1 米游社签到页：有效 B01 现场与选择器兼容修复

`2026-07-15 08:34–08:36` 在同一设备上完成“已签到页面停留约 30 秒 → 切换未签到页面”的有效对照。订阅仍为 `667/v541`，目标组仍为 `app/8`，目标 Activity 仍为 `.web2.MiHoYoWebActivity`。

结构化诊断在切换窗口内记录 1654 条米游社相关记录、151 个关联 ID。主要终止原因如下：

| 原因 | 数量 | 含义 |
| --- | ---: | --- |
| `MatchTimeout` | 576 | 其他规则超过匹配窗口 |
| `PrerequisiteUnsatisfied` | 398 | 规则 1–3 因规则 0 未触发而停止 |
| `RuleEligible` / `WindowRootAvailable` / `SelectorMiss` | 各 155 | 规则可执行且窗口 root 存在，但选择器未命中 |
| `QueryStarted` | 144 | 查询实际持续运行 |
| `QueryAlreadyRunning` | 7 | 页面切换期间的少量合并噪声 |
| `StaleContext` | 2 | 页面切换瞬间的旧上下文 |

关键链路反复稳定出现：

```text
667/app/8/0 RuleEligible
667/app/8/0 WindowRootAvailable
667/app/8/0 SelectorMiss
667/app/8/1..3 PrerequisiteUnsatisfied
```

没有任何 `ActionSubmitted`。因此本次故障发生在选择器层，Root、动作执行器和查询唤醒均未进入失败路径；少量 `QueryAlreadyRunning` 不是本次漏执行主因。

同一时段保存的已签到和未签到快照均为 74 个节点。两者的 WebView 自身 `text` 均为空，而规则 0 的两条入口分别依赖：

```text
WebView[text*="签到"] >4 View[childCount=11] ...
WebView[text*="签到"] >4 View[childCount=10] ...
```

当前未签到页的真实目标路径仍为 `WebView >4 View[childCount=11] > View[childCount=3] > Image[index=0]`；已签到页同位置奖励图标为 `index=2`。首版兜底只保留结构条件，审查发现 `.MiHoYoWebActivity` 还是其他米游社网页共用的通用容器，存在误触风险，因此最终兼容选择器先从标题节点确认同一 WebView 内存在可见的“每日签到”页面，再沿原结构定位目标：

```text
[text$="每日签到"][visibleToUser=true] <<n WebView >4 View[childCount=11] > @View[childCount=3][visibleToUser=true] > Image[index=0][text!=null]
```

实现不修改设备上的 `667.json`，而由 `RuleSelectorCompat` 仅在 `com.mihoyo.hyperion` 且规则仍包含已确认失效的完整旧选择器时追加兜底。官方订阅删除旧选择器后兼容逻辑自动停用；官方直接加入同一兜底时不会重复添加。缩减节点树回归使用现场相同的标题、四层 WebView 路径、奖励容器和图片 index：未签到页命中目标 View，已签到页因 `index=2` 不命中，普通“参量质变仪提醒”网页因没有“每日签到”标题不命中。

该轮兼容测试增加到 7 项，App 单元测试总数为 24；Selector JVM 基线仍为 18，Release 与 Vital Lint 通过。当时因签到 WebActivity 已退出，真实点击尚未验收；后续有效现场如下。

#### 6.2.1.1 星穹铁道未签到页端到端验收

`2026-07-15 09:21–09:49` 在用户保持打开的“《崩坏：星穹铁道》签到福利”页面完成第二次有效现场。订阅已经在线更新为 `667/v542`，原订阅 JSON 未被改写。安装前快照 `1784078848053` 和最终修复前快照均为 50 个节点：奖励容器为 `View[childCount=10]`，其前三个子节点依次为资料块 `View[childCount=6]`、第 13 天已签到块 `View[childCount=3]`、第 14 天未签到扁平 `TextView("×5000第14天")`。

现场证明该 WebView 的文本虽然可被快照导出，但活动自动化查询中的所有文本选择器均返回未命中；原先使用“签到福利”标题的缩减树测试因此属于假阳性。最终星铁兜底改为应用包名和旧选择器双重门控下的结构签名：

```text
WebView >4 View[childCount=10]
  > View[childCount=6]
  + View[childCount=3]
  + TextView[childCount=0][visibleToUser=true]
```

该选择器经 `/api/execSelector` 在当前真机页面返回匹配；普通 `childCount=10` WebView 的负例因缺少 `6 + 3 + TextView` 前缀而不匹配。

同一现场还暴露启动竞态：自动化服务先连接，而 `ruleSummaryFlow` 稍后才完成应用规则构建；首次查询只加载全局规则，规则汇总变化后原代码既不刷新 `activityRuleFlow` 也不补查，静止页面会一直等待下一次有效事件。新增 `ActivityScene.RuleSummary` 和 `initRuleSummaryRefresh()` 后，规则汇总变化会原子刷新当前 Activity 规则，并由现有引擎执行一次 forced query。

修复版在 `09:43:59.360` 命中 `667/app/8/0`，目标为第 14 天 `TextView`，Root `clickCenter` 返回 `shell=true`、`state=Completed`；随后规则 1 关闭签到成功弹窗。最终快照 `1784079885535` 显示累计签到由 13 天变为 14 天，第 14 天节点由扁平 `TextView` 变为含文本和图片的 `View[childCount=3]`，构成端到端成功证据。

页面响应前曾按默认 1 秒冷却重复提交两次相同点击。兼容层现仅对米游社且仍包含星铁完整旧选择器的规则，把最短动作冷却提高到 5 秒；上游若配置更长冷却则保持上游值。最终已签到页覆盖安装前后动作计数均为 `7721`，未发生误点。

该次现场结束时 App 测试 29/29、Selector JVM 18/18、Release 与 Vital Lint 全部通过；后续阶段 3 构建与安装结果见 6.4。

### 6.3 哔哩哔哩冷启动：规则链成功样本

`2026-07-14 12:33:00` 对 `tv.danmaku.bili` 执行一次不清数据的冷启动。ActivityTaskManager 判断为 `tv.danmaku.bili/.MainActivityV2`，AccessibilityManager 显示 UiAutomation 正在监听窗口状态和内容变化；GKD 内部快照 `1784003590642` 保存成功，记录 `appId=tv.danmaku.bili`、`activityId=tv.danmaku.bili.MainActivityV2`、`231` 个节点和 `1080×2400` 屏幕。

订阅 `667` 的 `g10`“分段广告-首页推荐视频卡片广告”在本次启动中形成完整动作链：

| 时间 | 规则 | 目标 | 旧引擎结果 |
| --- | --- | --- | --- |
| `12:33:11.916` | `g10/r0/key0` | `tv.danmaku.bili:id/more` | `clickNode=true` |
| `12:33:12.262` | `g10/r2/key50` | “不感兴趣/相似内容过多”类可点击项 | `clickNode=true` |

第二步依赖 `preKeys=[0,1]`，能够紧随第一步命中，说明本次规则链实际推进，登记为哔哩哔哩成功对照样本，不计为 B01/B02。需要保留的观察点是：第一步动作日志中的节点为 `visibleToUser=false`，纵坐标超出当前屏幕且高度为负；这可能来自 RecyclerView 节点复用或快照/动作并发。现有 `performAction=true` 只能证明系统接受调用，后续阶段仍需通过动作后界面验证确认真实效果。

证据位于忽略目录 `local-assets/diagnostics/root-runtime/bili-20260714_123300/`，包含完整 GKD 当日日志、快照 JSON、最小 JSON、PNG 和原始归档。

### 6.4 阶段 3 查询唤醒状态机验收

旧实现的 `startQueryJob()` 在 `querying=true` 时以 `QueryAlreadyRunning` 直接返回，查询窗口内到达的新事件可能没有后续查询；`queryEvents` 还会随同类事件持续追加。新实现引入 `QueryWakeState`：只允许一个 runner，并把运行期间的请求合并为一个有界 pending；当前查询结束时仍持有所有权并直接 handoff，避免“先置空闲、后启动”之间再次丢事件。被合并请求记录为 `QueryDeferred`。

节点事件由 `QueryEventBuffer` 约束：连续同类事件最多保留最后两个；混合事件只留下重新读取当前 root 的标志。这样既保留既有增量查询语义，又保证事件风暴下内存为常量级。

新增 10 项单元测试覆盖单 runner、64 路并发请求、1000 次请求风暴、10000 次同类事件风暴、无空隙 handoff、forced/normal/delay 合并优先级、混合事件降级和空缓冲区分。全量结果为 App 39/39、Selector JVM 18/18、Release 构建与 Vital Lint 全部通过。

最终 APK 为 `1.12.1-f2e60bd-dirty`，路径 `app/build/outputs/apk/gkd/release/app-gkd-release.apk`，SHA-256 `ACFBF7FA16C04675F1C3EF852AC24204E704D32D724CEE9BE5CF14E63E93E171`，3,320,191 字节。非清数据覆盖安装后主进程 PID `17570`，Root UserService PID `17725`、UID 0，StatusService 为前台。

真机冒烟在米游社星铁已签到页执行 12 次交替横向滑动以制造窗口内容事件。测试前后前台均为 `com.mihoyo.hyperion/.web2.MiHoYoWebActivity`，动作计数均为 `7721`，两个进程保持存活且没有新增 GKD `AndroidRuntime`。忽略目录证据为 `local-assets/diagnostics/root-runtime/gkd-20260715-phase3-smoke.log`。后续遇到天然慢选择器页面时可继续积累真实 `QueryDeferred` 诊断样本，但不作为阶段 3 状态机收口的阻塞项。

### 6.5 阶段 4 第一批任务/窗口采样验收

任务源由单条 `getTasks(1).firstOrNull()` 改为最多 16 条候选，按平台可用性读取 `taskId/userId/effectiveUid/displayId/isFocused/isVisible/isRunning`，并优先选择目标显示屏上 focused、visible、running 的任务。AOSP 历史源码复核后已为字段加版本门控：Android 10/11 不访问 Android 12 才加入的 focused/visible，Android 12～15 不访问 Android 16 才加入的 effectiveUid。焦点窗口采样记录 windowId、displayId、类型、层级、focused/active 和 root 包名；Task 与 focused root 包一致才产生可执行的 `Confirmed` 快照，冲突快照默认 `canExecute=false`。

新增 `ForegroundTaskTest` 6 项和 `ForegroundSnapshotTest` 8 项。全量结果为 App 53/53、Selector JVM 18/18、Release 与 Vital Lint 通过。APK 路径 `app/build/outputs/apk/gkd/release/app-gkd-release.apk`，SHA-256 `8812A0E6AC2C977B2081548FCDB054AF23658FDFD9407D6C364B2BCA3E69A546`，3,320,191 字节。

APK 非清数据覆盖安装到 Android 16 测试机后，主进程 PID `1809`，Root UserService PID `1872`、UID 0。先启动 GKD 主界面，再把已有米游社任务带回前台，TaskStack 路径完成两次切换；没有 `NoSuchFieldError`、`NoSuchMethodError` 或 GKD `AndroidRuntime`。本批模型尚未接管规则上下文，下一次验收需覆盖输入法/SystemUI/权限弹窗/画中画策略和冲突延迟确认。

### 6.6 阶段 4 第一批审查修复验收

审查发现并修复三条控制流缺口：规则汇总 collector 不再使用可能丢掉已加载真实首值的 `drop(1)`；`Confirmed` 现在同时要求 Task focused、visible、running 和 focused Window root 包一致；TaskStack 的 taskId/taskInfo 两个回调统一重新采样 `getForegroundTask()`，不再直接信任回调任务或 `topActivity`。

新增 `RuleSummaryRefreshTest` 1 项，覆盖 collector 晚于真实汇总加载仍收到当前值；`ForegroundSnapshotTest` 增加 2 项，覆盖非 focused、不可见和停止任务不得确认。全量结果为 App 56/56、Selector JVM 18/18、Release 与 Vital Lint 通过。

最终 APK 为 `1.12.1-ee8c484-dirty`，SHA-256 `E00AD97B656F37FE7C21D23472292674EA38A3DA25A7724BE9C4623A658897F8`，3,320,195 字节。非清数据覆盖安装后主进程 PID `15270`，Root UserService PID `15379`、UID 0，StatusService 为前台。启动 GKD 后恢复已有米游社任务，两个进程保持存活，前台恢复为 `com.mihoyo.hyperion/.main.HyperionMainActivity`，无新增隐藏字段错误、方法错误或 GKD `AndroidRuntime`。

### 6.7 阶段 4 覆盖层分类、有限确认与规则接入验收

`ForegroundSurface` 已显式区分普通应用、输入法、SystemUI、权限控制器、画中画、无障碍覆盖层和其他系统覆盖层；只有 `Confirmed + Application` 可执行。Task/窗口冲突第一次出现时只安排一次 150ms 合并复查，同一冲突到期后拒绝并停止，不做周期轮询。规则事件入口、查询开始和动作提交前统一使用融合快照；动作前 taskId、windowId 或 appId 变化即以 `StaleContext` 终止。诊断详情包含置信度、surface、userId、displayId、taskId、windowId 及 Task/Window 包名。

新增 `ForegroundSnapshotTest` 6 项覆盖输入法、SystemUI/权限控制器、画中画、无障碍/系统覆盖层及两类真机反直觉拓扑；新增 `ForegroundConfirmationStateTest` 5 项覆盖立即接受、150ms Pending/Rejected、冲突变化重启窗口和确认后清理状态。全量 App 67/67、Selector JVM 18/18、Release 构建与显式 `lintVitalGkdRelease` 均通过。

最终 APK 为 `1.12.1-3d7e18e-dirty`，路径 `app/build/outputs/apk/gkd/release/app-gkd-release.apk`，SHA-256 `56251D21286F8682219C568F1BF8DFD92351671DE2B573520B0F1BE547BC3C12`，3,320,187 字节。非清数据覆盖安装后主进程 PID `18932`，Root UserService PID `19076`、UID 0。最终已恢复米游社 `com.mihoyo.hyperion/.main.HyperionMainActivity`；两个进程保持存活，未出现 `NoSuchFieldError`、`NoSuchMethodError` 或新增 GKD `AndroidRuntime`。

输入法现场 `20260715_111705_phase4_ime_visible` 显示讯飞输入法窗口为 `TYPE_INPUT_METHOD`，但 `focused=false/active=false`，宿主 Settings 仍为 focused/active；现按类型优先识别输入法。权限现场 `20260715_112245_phase4_permission_dialog` 显示顶部 Task 已是 `com.android.permissioncontroller/.permission.ui.GrantPermissionsActivity`，但 Accessibility focused Window 仍属于底层临时探针；现同时检查 Task 与 Window 包。PiP 现场 `20260715_112617_phase4_pip_visible` 显示探针任务 `mode=pinned`、窗口 `pictureInPicture=true`，但 PiP 窗口同样为 `focused=false/active=false`，底层 GKD 仍 focused；现采集窗口自身 PiP 标志并优先阻断。

上述临时权限/画中画探针仅位于 Git 忽略的 `local-assets/diagnostics/root-runtime/`，最终 APK 重放后均已从手机卸载；GKD 通知权限保持 granted，ADB 无残留转发。阶段 4 固定窗口验收完成。

### 6.8 阶段 4 最终代码审查收口

最终审查修复四条控制流缺口：持续相同的 node root/前台错配不再每 150ms 无限唤醒，而由 `RootMismatchRetryState` 对每个 taskId/windowId/前台包/root 包上下文只允许一次补查；前台确认延迟任务先重新采样并应用新 Activity，再执行 `startQueryJob()` 的规则集检查，避免从无规则 App 切入有规则静态页时被旧空规则集丢弃；TaskStack Activity 非空时不再被同包旧 `STATE_CHANGED` 事件覆盖；150ms 确认期限改用单调时钟，不受系统时间校准或回拨影响。

新增 5 项回归测试：根错配同上下文单次补查、上下文变化重新允许、成功匹配清理历史、墙上时间回拨不延长确认、事件 Activity 仅在 Task Activity 缺失时兜底。全量 App Debug 单元测试为 72/72，Selector JVM 为 18/18，Release 与显式 `lintVitalGkdRelease` 通过。

最终 APK 为 `1.12.1-3d7e18e-dirty`，SHA-256 `F0814E2C6A33DEA4DE2AE79A4CDDEE2DC4F2D963F6E48F92EC05354D6B189EC1`，3,320,187 字节。非清数据覆盖安装成功；为确保远端进程加载新 APK 代码，安装后显式停止旧 Root UserService 并重启 GKD，最终主进程 PID `20425`，Root UserService PID `2793`、UID 0，StatusService 为前台，清空日志后的启动过程无新增 GKD `AndroidRuntime`。

### 6.9 阶段 5 第一批有限恢复、generation 与节点刷新

`WindowRootRecoveryState` 将切换期 root 缺失恢复限制为 50/100/200/400/800ms 五次；只有窗口状态、App、亮屏或成功动作信号能开启 3 秒恢复窗，缺失本身不能启动或延长期限。`WindowGenerationState` 在接受的窗口状态/内容事件上推进 generation，查询开始捕获 taskId/windowId/appId/displayId，动作前再次核对；`A11yContext` 切代时清空 root 和遍历缓存。目标节点动作前必须刷新，失败时只在完全相同的窗口上下文中重新取得 root 并复跑一次规则。

新增 `WindowRootRecoveryStateTest` 5 项和 `WindowGenerationStateTest` 3 项，全量 App Debug 单元测试 80/80、Selector JVM 18/18、Release 与 `lintVitalGkdRelease` 通过。最终 APK 为 `1.12.1-3bca44d-dirty`，SHA-256 `348A16AC01E3F16472C2DADE138CC5B631E81AD08E1ABAA0134ADC2E48B61893`，3,320,183 字节。非清数据覆盖安装并重启 GKD 后，主进程 PID `2098`，Root UserService PID `29415`、UID 0，StatusService 为前台；清空日志后的启动过程无新增 GKD `AndroidRuntime`，最终恢复米游社 `HyperionMainActivity`。本批尚未通过故障注入实际命中五级 root 恢复，慢页面命中率与耗电统计留给阶段 5 专项真机回归。

### 6.10 阶段 5 审查修复

审查确认初版五级恢复位于 `getConfirmedForeground()` 之后：当 `window.root` 尚未挂载时快照为 `Probable`，查询在到达 `nodeVal == null` 前已经返回。最终新增严格的 `canRecoverMissingRoot`，仅允许 focused/visible/running Task 加 focused Application Window 且 rootAppId 暂空；该状态在普通确认前直接消费有限恢复预算，但仍不能执行规则。恢复次数改为整个 3 秒切换窗共享五次，上下文变化不再重置。

初版还为每个 Content 事件推进全局 generation，可能使持续更新的 WebView/视频页在动作前不断命中 `StaleContext`。最终仅 State/App/亮屏/成功动作及 display/rotation 变化推进结构 generation；Content 保留事件分支清缓存和单 runner pending 查询，动作节点仍必须刷新。节点缓存默认缩短为文本 500ms、结构 1000ms并改用单调时钟，Legacy 策略保留原 1000/2000ms。

新增 8 项审查回归后，全量 App 88/88、Selector 18/18、Release 与 Vital Lint 通过。最终 APK 为 `1.12.1-3bca44d-dirty`，SHA-256 `1151E53FA7DCAD8F4B8212D2D04F0C81663F133EFCF52609C00DCE23C68C620D`，3,320,183 字节。非清数据覆盖安装后主进程 PID `24515`，Root UserService PID `18339`、UID 0，StatusService 为前台。普通 shell 在 Android 16 被 `INJECT_EVENTS` 拒绝；改用 root 执行 6 次米游社主页横向往返滑动后，前台、两个 GKD 进程和前台服务均保持稳定，清空后的 logcat 无新增 GKD `AndroidRuntime`。本次未伪造“真实命中 root 暂空”，该项仍需下一轮可控故障注入形成真机证据。

### 6.11 阶段 5 最终专项真机验收

在同一 Android 16 设备保持规则决策诊断开启，先导出既有 2048 条缓存，再冷启动哔哩哔哩并滚动动态页面，随后连续五次强停/冷启动米游社。最终缓存仍为 2048 条，包含 140 个关联 ID、95 个事件型关联 ID，孤儿事件链为 0；米游社目标事件 62 个、哔哩哔哩目标事件 45 个，14 个合并事件链也全部存在后续决策。诊断原文保存在本地忽略目录 `local-assets/diagnostics/root-runtime/phase5-decision-diagnostics-postsample.txt`。

第五次切换采样中自然命中一次真实窗口挂载竞态：关联 ID 1041 先记录 `WindowRootUnavailable`，随后记录 `WindowRootRecoveryPending`，明细为 `attempt=1/5 delay=50`。约 10ms 后，同一米游社 Activity 的更新 task/window 已由新事件查询记录 `WindowRootAvailable`，因此旧恢复被更新上下文自然取代；这证明恢复入口真机可达，但不把新事件取得 root 误记为定时恢复本身的功劳。整个缓存没有 `WindowRootRecoveryExhausted`。另有 2 条 `StaleContext`，分别来自 B 站到米游社的快速切换和优先级中断，均以跳过旧查询结束，没有动作提交或误动作。

测试后 GKD 主进程仍为 PID `24515`，Root UserService PID `18339`、UID 0，StatusService `isForeground=true`，logcat 无新增 GKD `AndroidRuntime`。诊断开关测试前已经开启，导出后保持原状态。阶段 5 因此完成；米游社当天签到状态导致无法重做“未签到成功动作”，该业务样本继续归入阶段 0 长期复现集，不阻塞窗口恢复机制验收。

### 6.12 阶段 6 第一批统一动作执行器

新增统一 `ActionExecutor` 与 `ActionExecutionContext`，规则动作提交携带 displayId、windowId、rotation、windowBounds 和 visibleBounds。坐标必须同时位于窗口区域与屏幕可见交集；Root 输入显式指定 displayId，Root 失败转无障碍手势前重新执行同一 generation/前台/节点 guard，非默认显示屏缺少对应手势能力时拒绝回退。节点 API 明确返回 false 时最多尝试最近一个符合动作能力的可见父节点，不继续遍历更多可点击祖先；任何已接受或已完成动作均不重发。

动作结果新增 target、backend、窗口上下文和 retryCount，决策诊断可分别统计节点、父节点、Root 坐标、无障碍手势、全局动作和无动作。新增 3 项纯策略测试后 App 91/91、Selector 18/18、Release 与 Vital Lint 通过。APK 版本 `1.12.1-f5c03f8-dirty`，SHA-256 `1817E1F99DFCAFEC6CA5D001A3A9F9661DCC382FCC89519254C49AD8B5200848`，3,336,575 字节。

同一 Android 16 设备非清数据覆盖安装成功：主进程 PID `12288`，Root UserService PID `29736`、UID 0，StatusService 为前台。依次冷启动 B 站、滚动动态页面并恢复米游社 `HyperionMainActivity` 后两个 GKD 进程稳定，logcat 无新增 GKD `AndroidRuntime`。本轮仅证明安装、生命周期和动态事件链没有回退；需要真实业务动作才能验收父节点回退、显式 displayId 注入和动作诊断字段，因此阶段 6 仍为进行中。

### 6.13 阶段 6 第二批只观察动作验证

新增 `ActionVerificationStateMachine`，成功的 click/longClick/back 等动作以单调时钟最多观察 350ms。目标节点消失、窗口上下文改变或结构 generation 改变时，将原结果升级为 `Verified` 并记录对应 signal；稳定到期时记录 `Inconclusive`，仍保留原 `Accepted/Completed` 成功结果、规则计数和冷却，并明确禁止自动重发。`none`、`swipe` 和失败动作不增加观察等待。`ActionVerificationFailed` 不用于普通超时，避免把“没有证据”误写成“动作失败”。

状态机新增 8 项测试后 App 99/99、Selector 18/18、Release 与 Vital Lint 通过。APK 版本 `1.12.1-abfc983-dirty`，SHA-256 `52B32B7C8B06EABB999998556D1716275B6AC8FB61E43B1623ADA8E5429D3315`，3,336,567 字节。同一 Android 16 设备非清数据覆盖安装成功：本次安装后主进程 PID `26103`，Root UserService PID `31871`、UID 0，StatusService 为前台服务；B 站动态页面与米游社恢复过程无 GKD 崩溃。

导出的本轮诊断保存在本地忽略目录 `local-assets/diagnostics/root-runtime/phase6-verification-diagnostics.txt`。该缓存中 Action 记录为 0，因此不能作为 `Verified` 或 `Inconclusive` 真机证据；它只证明新版本在无动作动态事件链中没有回退。真机动作验证、父节点回退和显式 displayId 字段继续等待无业务副作用的自然场景，不为验收添加生产调试后门。

### 6.14 阶段 6 安全动作真机收口

使用 GKD 原生 HTTP 内存订阅加载一次性 B 站规则 `-1/app/9903/0`，选择器为 `@[vid="category_click_area"]`，动作上限为 1，目标仅打开“全部分区”页面，不产生关注、点赞、签到、购买或发送内容等业务副作用。首次探测发现新建内存订阅默认禁用，导出的诊断没有 Action，因此没有把 B 站自身 Compose 启动跳转误记为成功；随后在 GKD 订阅页显式启用该内存订阅后重新冷启动 B 站。

有效关联 ID `2799` 依次记录 `SelectorMatched`、`ActionSubmitted`、`ActionSucceeded` 和 `ActionVerified`。动作结果为 `action=clickNode, state=Verified, target=Node, backend=Node, display=0, window=2447, rotation=0, retries=0, verification=Verified, signal=GenerationChanged, verificationMs=104`，并完整携带 `windowBounds=(0,0)-(1079,2399)` 与同值 `visibleBounds`。B 站从 `MainActivityV2` 进入 `com.bilibili.lib.ui.GeneralActivity`，原 `category_click_area` 节点消失；同一时刻其他旧规则以 `StaleContext` 结束，没有再次提交动作。完整诊断保存在忽略目录 `local-assets/diagnostics/root-runtime/phase6-safe-action/decision-diagnostics-final.txt`。

测试后停止 HTTP 服务，由应用自身删除 `-1` 订阅及其动作日志；ADB 转发已移除，触发总数从测试后的 7728 恢复到测试前 7727。重新启动后主进程 PID `4014`，Root UserService PID `28853`、UID 0，StatusService `isForeground=true`，订阅页只保留测试前的本地/正式订阅，最近 10 分钟无新增 GKD `AndroidRuntime`。阶段 6 的真实动作、显式 displayId、窗口字段和只观察验证因此完成验收；父节点回退仍无自然真机样本，但已有纯策略测试覆盖，不增加生产调试后门。

## 7. 固定窗口场景

| 场景 | 测试步骤 | 当前前台判断 | 规则结果 | 证据路径 |
| --- | --- | --- | --- | --- |
| 普通全屏 Activity | 不清数据冷启动哔哩哔哩并保存 GKD 内部快照 | Task/UiAutomation/GKD 快照一致为 `tv.danmaku.bili/.MainActivityV2` | `g10/key0 → key50` 两步链执行，均返回 `clickNode=true` | `local-assets/diagnostics/root-runtime/bili-20260714_123300/` |
| 系统权限弹窗 | 临时探针请求相机权限，出现系统授权弹窗后采集并返回；随后卸载探针 | 顶部 Task 为 permissioncontroller，但 focused Accessibility Window 仍是底层探针；融合结果按 Task 识别 `PermissionController` | 默认阻断；最终 APK 已重放，探针已卸载 | `window-scenarios/20260715_112245_phase4_permission_dialog/` |
| 输入法显示/隐藏 | 打开系统设置搜索框，不输入文字；采集后返回隐藏 | Task/焦点应用窗口仍为 Settings；IME 独立存在但 `focused=false/active=false`，融合器按类型优先识别 `InputMethod` | 默认阻断；隐藏后恢复普通应用上下文 | `window-scenarios/20260715_111705_phase4_ime_visible/` |
| 通知栏/控制中心 | 桌面展开通知栏，采集后立即收回 | Task 始终为桌面；Accessibility 活动/焦点窗口从桌面 `id=5` 切至系统覆盖层 `id=6`；GKD 前台记录在 `com.miui.home` 与 `com.android.systemui` 间切换 | 无规则动作；收回后恢复桌面和 UiAutomation | `window-scenarios/20260714_124515_desktop/`、`20260714_124519_notification_shade/` |
| 小窗/画中画 | 自由窗保留旧样本；临时 PiP 探针调用系统 API 进入真实画中画并采集 | PiP Task 为 `mode=pinned`，窗口 `pictureInPicture=true` 但 `focused=false/active=false`；底层 GKD 仍 focused | 融合器按窗口 PiP 标志默认阻断；最终 APK 已重放，探针已卸载 | 自由窗：`20260714_125740_settings_freeform/`；PiP：`20260715_112617_phase4_pip_visible/` |
| 分屏 | 尝试以 `windowingMode=3/4` 启动 Settings/Bili | 参数进入两任务配置，但两者边界仍为全屏且 `mInSplitScreen=false`，不构成有效分屏 | ADB 方式未复现；临时任务已关闭，待手动进入系统分屏后重测 | `window-scenarios/20260714_130102_split_settings_bili/` |
| 横竖屏切换 | root 锁定 rotation 1 并采集，再恢复自动旋转和原方向 | 横屏时逻辑显示 `2400×1080`、orientation/rotation 1；恢复后 `1080×2400`、orientation 0 | UiAutomation 全程在线；系统状态已恢复 `accelerometer_rotation=1`、`user_rotation=0`、`wm=free` | `window-scenarios/20260714_125028_forced_landscape_ignore_orientation/` |
| 锁屏/解锁 | 重启后首次解锁前采集；正常解锁并观察自动化通道切换 | 解锁前无 resumed Activity；解锁后为桌面。GKD 普通进程和 Sui 守护进程开机恢复；root 用户服务连接曾超时，UiAutomation 最终恢复 | 解锁后系统无障碍短暂启动，约 2 分钟后按模式设置切回 UiAutomation，规则引擎恢复 | 锁屏：`local-assets/diagnostics/root-runtime/20260714_112858_<设备序列号>/`；解锁后状态见当日 GKD 日志 |
| 双开/工作资料 | 待填写 | 待填写 | 待填写 | 待填写 |

## 8. ADB 证据采集

连接并授权手机后，在复现动作刚完成时运行：

```powershell
.\scripts\capture-root-runtime-baseline.ps1
```

多设备连接时：

```powershell
.\scripts\capture-root-runtime-baseline.ps1 -Serial <adb-serial>
```

固定窗口矩阵使用轻量脚本，避免每个场景重复读取整份 logcat：

```powershell
.\scripts\capture-window-scenario.ps1 -Scenario notification_shade
```

默认不运行系统 `uiautomator dump`，因为 GKD 的 Automation 模式已经占用 UiAutomation 通道，外部 dump 在当前 Android 16 设备上会以 `already registered` 崩溃，并可能干扰待测运行时。只有在 GKD Automation 已停止，或需要专门复现该冲突时才显式启用：

```powershell
.\scripts\capture-root-runtime-baseline.ps1 -IncludeUiAutomatorDump
```

脚本输出到忽略目录 `local-assets/diagnostics/root-runtime/<时间_序列号>/`，采集内容包括：

- Git commit、设备序列号和 Root 身份；
- 系统属性、包信息、Activity/进程状态；
- Window、Accessibility、Display、输入法状态；
- 分辨率、密度和 `logcat`；外部节点树仅在显式启用时采集。

脚本不会读取 `/data/data/li.songe.gkd`，也不会代替 App 内的备份和日志导出。需要节点证据时，优先通过 GKD 自身的 `ExposeService --ei expose 0` 保存快照。节点树、日志和设备属性可能含隐私信息，只能保存在 `local-assets/`，分享前必须脱敏。

### 8.1 重启后首次解锁前样本

`2026-07-14 11:28:58` 在设备重启约 3 分钟、锁屏熄屏且尚未首次解锁时完成一次采集：

- `sys.boot_completed=1`，GKD 主进程已由系统恢复，身份仍为普通应用 UID 和 `untrusted_app` 域；
- KernelSU/Sui 守护进程已以 root 运行；
- `li.songe.gkd:shizuku-user-service` 尚未创建，Accessibility 中也没有 GKD 的 bound service 或 UiAutomation；
- ActivityTaskManager 没有 resumed Activity，WindowPolicy 显示 Keyguard 正在显示、屏幕关闭；
- 默认采集明确写入 `uiautomator-status.txt` 为 `Skipped by default`，未再制造 UiAutomation 通道冲突。

解锁后的日志表明：`12:10:49` 收到 `USER_PRESENT`；`12:10:51` 系统无障碍服务启动并使 Automation 局部关闭；`12:12:50` Automation 自动重新启动，`12:12:51` 系统无障碍局部关闭。随后 AccessibilityManager 明确显示 UiAutomation 在线，哔哩哔哩快照和规则匹配均成功。此次重启恢复流程因此通过“规则引擎最终可用”基线，但 GKD root 用户服务在开机时出现一次 3 秒连接超时，仍作为 root 桥接稳定性观察项保留。

## 9. 订阅兼容冒烟测试

| 用例 | 基线结果 | 后续阶段结果 |
| --- | --- | --- |
| 在线更新现有订阅 | 通过：解锁恢复后自动检查从“开始检测更新”正常结束，当前订阅 `667` 保持版本 541，无错误 | |
| 添加新订阅链接 | 待测试 | |
| 导入本地订阅 | 待测试 | |
| 导出完整备份 | 通过：ZIP 完整，配置和订阅数量符合基线 | |
| 将备份导回当前数据 | 通过：两次均记录“导入成功”，配置和文件哈希不变 | |
| 清空后恢复备份 | 未执行：属于破坏性测试，当前以原始快照和幂等导入覆盖安全基线 | |
| APK 原地升级后保留订阅文件 | 通过：3 个订阅文件 SHA-256 全部不变 | |
| APK 原地升级后保留订阅启停状态 | 通过：`store.json` 不变，`subs_item=3`、`subs_config=22` | |
| 保留应用/全局组局部配置 | 当前数据库 `app_config=0`、`category_config=0`，需增加非空样本后测试 | |
| 降级回基线 APK 后读取数据 | 待测试 | |

## 10. 阶段 0 验收

- [x] 当前 commit、版本、构建命令、APK 哈希和开发签名已固化。
- [x] 当前 HEAD 的基线 APK 构建成功。
- [x] 代表性规则、窗口场景和订阅兼容测试表已建立。
- [x] ADB/Root/Window/Accessibility 一键采集脚本已建立。
- [x] Release 构建和 App Debug 单元测试基线通过。
- [x] selector JVM 测试在修正旧断言后通过（18/18）。
- [x] Root 真机、系统、显示、用户、自动化通道和安装版本已记录。
- [x] 升级前 Root 原始快照、逐文件哈希和升级后对比已完成。
- [x] 当前 HEAD APK 已原地升级，订阅文件、设置和数据库计数保持一致。
- [x] 已从手机实际安装的应用和订阅中选出 10 条代表规则。
- [x] 已离线分析 7 天旧日志并登记 `B01-CANDIDATE-FOREGROUND-RACE`，未把相关性误写成因果结论。
- [x] App 自带备份已导出、拉取、计算 SHA-256 并验证 ZIP 内容完整。
- [x] 真实配置和订阅已通过 App 自带导出/导入验证可恢复；另有 Root 原始快照兜底。
- [ ] 至少 10 条真实规则完成基线统计。
- [x] B01“规则未执行”已复现并留存证据，见 6.2.1。
- [ ] B02“动作未生效但计为成功”已复现并留存证据。
- [ ] 固定窗口场景已完成基线记录。
- [ ] 订阅兼容冒烟测试已完成。

阶段 0 尚未全部完成时，用户因米游社当天已经签到而授权先实施“阶段 0.5 Root 桥前置诊断”。该授权仅覆盖能力状态、自检和有限重连，不包括修改选择器、规则时序、动作结果或重试策略。

当前结论：阶段 0 **进行中**，阶段 0.5 Root 桥已完成实现和真机验收。真机确认系统 Binder `8/8`、Root UserService UID `0`、UiAutomation 在线，手动重新检测不改变相关进程 PID；配置与 3 个订阅文件哈希保持基线一致。另修复了通知权限被拒绝时外部 `ExposeService` 跳过 `startForeground()` 的冷启动崩溃，并在同一设备上按“覆盖安装 → 强停 → ADB 拉起”路径复测通过。

截至 `2026-07-14` 当轮，普通全屏、通知栏、输入法、自由窗、旋转和锁屏/解锁已有真机证据；权限弹窗、真实分屏、画中画、双开、10 条规则重复统计和 B01/B02 尚未完成。当时米游社 B01 因已签到暂时跳过；其后 `2026-07-15` 的有效现场和兼容修复见 6.2.1。

## 11. 阶段 1 结构化决策诊断真机记录

2026-07-14 在同一 Xiaomi Android 16 / KernelSU + Sui 设备上覆盖安装阶段 1 首版：

- 高级设置新增“规则决策诊断”开关，初始值为关闭；开启后只使用进程内环形缓冲。
- 启动哔哩哔哩 `.MainActivityV2` 后，弹窗能按关联 ID 显示 `EventReceived → ForegroundConfirmed → RuleEligible/ForcedRuleSkipped`，规则标识包含真实订阅 `667/app/...`。
- 返回 GKD 高级设置后，最近未执行原因为 `NoApplicableRules`（中文“当前界面没有适用规则”），与当前界面事实一致。
- 弹窗支持最近 30 条预览、“复制全部”和“清空内存记录”。
- 首版 512 条容量在哔哩哔哩高频事件下约十秒填满；最终 2048 条 APK 已原地升级并完成同场景复测，仅使用 91/2048 条，没有发生环形淘汰。
- GKD 自身零规则现在记录为 `NoApplicableRules/Observed`，不再覆盖目标应用最近失败。最终最近失败显示“包名或 Activity 不匹配”，应用为 `com.miui.securitycenter`，详情为 `foreground=tv.danmaku.bili`，与应用启动过渡现场相符。
- 最终 App Debug 单元测试 8/8、Release 构建、Root UserService UID 0、StatusService 前台状态和零新增崩溃均通过；3 个订阅文件 SHA-256 保持阶段 0 基线值。
- 本阶段当时没有修改 `rule.trigger()`、规则状态计算、选择器、动作实现、延迟或查询唤醒算法；后续 B01 有效现场已于 6.2.1 完成诊断验收并单独加入窄范围兼容层。

## 12. 代码审查修复与待回归项

2026-07-14 对阶段 0.5/1 提交进行代码审查后完成以下收口：

- UserService 连接回调改为原子领取；超时或取消时撤销回调并解绑，迟到 wrapper 立即销毁。
- Root 重连在每轮开始、延迟后和连接返回后重新确认 Shizuku 仍启用且上下文未变化；已销毁上下文永久拒绝安装 wrapper。
- 无障碍事件关联 ID 改为在既有限流和连续事件合并后生成；事件队列不再误删非连续的同类事件。
- `ExposeService` 改用有界的 `shortService` 前台类型，不再依赖特殊用途前台服务 AppOp，并始终按前台服务契约调用 `startForeground()`。
- 诊断缓冲增加 revision，弹窗对成功记录、缓存数量和清空操作实时刷新。

本轮最终 `:app:testGkdDebugUnitTest` 13/13、`:selector:jvmTest` 18/18、`:app:assembleGkdRelease` 通过。13 项 App 测试包含 3 项连接回调竞态和 1 项连续事件合并回归。带日志目录导出的最终 APK SHA-256 为 `A606F46CBF74458FDEEB34223B60C27FF19BAC76472058C3A913BEC835D8E86A`，3,303,799 字节。

同日已在原 Xiaomi Android 16 设备完成非清数据覆盖安装和正常路径冒烟：安装成功，主进程启动，StatusService 保持前台，Root UserService PID `14641`、UID `0`；正常权限下外部 `ExposeService(expose=-1)` 启动后按预期自行结束，无新增 `AndroidRuntime`。`store.json` 与 `-2.json`、`666.json`、`667.json` 的 SHA-256 均与安装前一致。

特殊用途 AppOp 故障注入首轮发现：仅在 `onCreate()` 中检测失败并 `stopSelf()`，Android 16 仍抛出一次 `ForegroundServiceDidNotStartInTimeException`，且快照数量保持 2，证明请求未执行但前台契约仍未满足。最终将 ExposeService 改为 Manifest `shortService` 并重新覆盖安装；AppOp 明确保持 `ignore` 时再次调用 `expose=0`，快照数量由 2 增至 3，ExposeService 按预期结束，logcat 无新增崩溃。测试后 AppOp 已恢复为 `allow`。

两次使用 `SIGSTOP/SIGCONT` 可逆暂停 Sui 的尝试均未命中 UserService 内部三秒超时：应用会等待 Sui Binder 恢复后才进入 `buildServiceWrapper()`。随后改为仅暂停下一次 GKD Root UserService，成功命中真实三秒超时；系统级 Sui 全程不再受影响。3 项 JVM 测试继续覆盖“取消后迟到值必须释放、只能完成一次、取消后失败不得恢复”。

包含上述测试驱动重构的最终 APK 已再次非清数据覆盖安装：主进程 PID `4548`，Root UserService PID `15788`、UID `0`，StatusService 为前台，特殊用途 AppOp 为 `allow`；设置和 3 个订阅哈希继续保持不变。随后正常调用 `ExposeService(expose=-1)`，无新增 `AndroidRuntime`。

- [x] UserService 首次连接超时：PID `17466` 被暂停 4.5 秒，在 3.001 秒记录 `connection cancelled`；attempt `2/3` 成功，旧 PID 消失，无残留进程和崩溃。
- [x] 重连进行中关闭 Shizuku：连续暂停 PID `22856`、`23210`，第二次连接中关闭优化；恢复后没有第三次尝试、没有 Root UserService 残留，重新开启后 UID 0 服务恢复。
- [x] 哔哩哔哩高频诊断：导出 740 条记录、200 个 ID；31 个事件型 ID 中哔哩哔哩占 25 个，孤立 `EventReceived` 为 0；1 个 `coalesced>1` 的 ID 有后续记录。

后续复测可在弹窗点击“导出到日志目录”，拉取 `/sdcard/Android/data/li.songe.gkd/files/log/decision-diagnostics.txt` 后执行：

```powershell
.\scripts\analyze-decision-diagnostics.ps1 -Path .\decision-diagnostics.txt -TargetAppId tv.danmaku.bili
```

脚本发现任意只有 `EventReceived` 的关联 ID 时返回非零退出码。

## 13. 阶段 2 动作结果真机记录

2026-07-14 在同一 Xiaomi Android 16 / KernelSU + Sui 设备上非清数据覆盖安装阶段 2 工作版：

- Root UserService 进程 UID 为 `0`；通过 GKD 自身 HTTP 检查接口在哔哩哔哩节点树执行 `clickCenter`，返回 `{"action":"clickCenter","result":true,"shell":true,"state":"Completed"}`。
- 对 `story_bottom_pager` 执行 350ms `swipe`，返回 `{"action":"swipe","result":true,"shell":true,"state":"Completed"}`，真实界面发生滚动。
- `SafeInputManager.compatInjectInputEvent()` 现在只在隐藏输入 API 返回 `true` 时成功；DOWN 失败立即终止，MOVE 失败会保持整次失败但仍发送 UP 收尾，UP 失败同样判整次失败。
- 无障碍点击、长按和滑动改为等待 `GestureResultCallback`；只有 `onCompleted` 成功，`onCancelled`、提交拒绝和超时均失败。
- `A11yRuleEngine` 仍只在 `actionResult.result` 为真时调用 `rule.trigger()`；失败状态使用稳定的 `ActionRejected` 或 `ActionCancelled` 诊断原因，不增加次数、不进入冷却。
- 节点 `performAction()` 和全局返回键只能标记 `Accepted`，Root/输入序列和无障碍完成回调标记 `Completed`；`Verified` 仅为后续界面复核预留，本阶段没有把动作接受或完成等同于界面结果验证。
- 覆盖安装后 `store.json`、`-2.json`、`666.json`、`667.json` 的 SHA-256 与安装前完全一致。
- 最终 App Debug 单元测试 16/16、Selector JVM 测试 18/18、Release 构建和 lintVital 均通过；最终 APK SHA-256 为 `7841DEA509997085D350C867D38591AFF3220EA66FD31B73EB330A24AA3E5CE8`，3,320,183 字节。
- 最终 APK 再次原地安装后，主进程 PID `15075`，Root UserService PID `27651`、UID `0`，清空 logcat 后启动无新增 `AndroidRuntime`。

随后完成无障碍取消和规则消费故障注入：

- 测试前备份 `store.json`，临时设置 `automatorMode=1`、`enableShizuku=false`，由系统绑定 GKD `SelectToSpeakService`；此时不存在 Root UserService，动作只能经过无障碍手势路径。
- 同一进程向 HTTP 检查接口异步提交 5 秒与 250ms 两个重叠手势，第二个请求在 452ms 发出；首个结果为 `result=false, state=Cancelled`，第二个为 `result=true, state=Completed`。
- 启用临时内存订阅规则 `-1/app/9902/0` 后，用连续短手势取消其 5 秒动作。关联 ID `889` 在 383ms 内形成 `ActionSubmitted → ActionCancelled`，详情为 `action=swipe, state=Cancelled`。
- 此次规则级取消前后 `action_count.txt` 均为 `7705`；后续同一规则重新出现 `RuleEligible`，没有 `ActionMaximumReached` 或 `CooldownActive`，证明失败没有消费次数和冷却。
- 停止 HTTP 服务后 `-1.json` 自动删除；测试产生的两次成功对照动作已从动作计数中扣回，最终恢复为测试前的 `7703`。
- 最终恢复原 `store.json`、空的 enabled accessibility service 列表和 `accessibility_enabled=1`；Root UserService 重新上线且 UID 为 `0`。四个配置/订阅 SHA-256 与测试前一致，无新增 GKD `AndroidRuntime`。

阶段 2 验收完成。隐藏输入 API 的逐事件 `false` 聚合由 3 项 JVM 测试覆盖；真机采用系统真实 `onCancelled` 验证规则失败分支，未通过修改系统 `/system/bin/input` 或注入生产测试后门制造风险更高的权限故障。

### 13.1 阶段 2 审查收口复测

同日完成动作结果代码审查后的两项收口：`safeInvokeShizuku()` 将远端 `RemoteException` 转换为不可用结果，输入动作可继续走失败/回退路径；诊断文件改由 `Dispatchers.IO` 写入，写入异常交给 `launchTry` 记录和提示。新增 1 项 Binder 异常 JVM 回归后，App Debug 单元测试为 17/17，Selector JVM 测试为 18/18，Release 与 lintVital 通过。

最终 APK SHA-256 为 `B83171CD124BD7ED27043CCD221D3DDB622018FA7E8362947DBBE513F6E0A548`，3,320,183 字节。非清数据覆盖安装成功，主进程 PID `28892`，Root UserService PID `10058`、UID `0`；安装前后 `store.json`、`-2.json`、`666.json`、`667.json` 的 SHA-256 完全一致，`action_count.txt` 保持 `7703`，清空 logcat 后无新增 GKD `AndroidRuntime`。

## 14. 阶段 7 APK RootService 最小切片

2026-07-15 固定 `com.github.topjohnwu.libsu:service:6.0.0`，建立普通非 daemon RootService 的第一最小切片。AIDL 仅返回协议版本、远端 PID、远端 UID 和服务包名；每次 Binder 调用校验调用 UID、UID 包列表和签名一致性。客户端要求协议 1、PID 大于 0、UID 0、包名匹配，并处理 Binder 死亡、空 Binder、同步异常和 8 秒无回调超时。输入、任意 shell、路径、订阅和数据库均未暴露，也尚未接管现有动作后端。

静态与构建结果：

- App Debug 单元测试 115/115，其中调用方与远端身份策略 8 项、结构化输入参数 8 项；Selector JVM 18/18。
- `assembleGkdRelease`、R8 及 `lintVitalGkdRelease` 通过。
- Release APK：`app/build/outputs/apk/gkd/release/app-gkd-release.apk`，SHA-256 `49FD98F272AD33A0800B8688AEACB1EDAAEBD54A385A423758115238B6FB3505`，3,356,704 字节。
- `git diff --check` 通过，仅有既有 LF/CRLF 转换警告。

真机结果：ADB 流式安装被小米安装器以 `INSTALL_FAILED_USER_RESTRICTED` 拦截后，改用手机 Download + 系统安装器完成独立 `li.songe.gkd.debug` 安装，不覆盖正式版。首次打开“Root 与授权状态”并授予 KernelSU 权限后，弹窗显示 APK RootService PID `26957`、UID `0`、协议 `1`；Debug App UID 为 `10391`。

Binder 死亡首轮发现具体 `binder died` 会被随后到达的 `onServiceDisconnected` 覆盖为“未连接”。修复并覆盖更新后，设备侧 `base.apk` SHA-256 与本地 Debug APK 完全一致；新连接 PID `29808` 被强制结束后，Debug 主进程 PID `20830` 继续运行，无 GKD `AndroidRuntime`，弹窗稳定显示 `APK RootService：失败（binder died）`。点击“重新连接”生成 UID 0 新 PID `30430`。

非 daemon 清理验证：强停 `li.songe.gkd.debug` 后 PID `30430` 自动退出；覆盖更新会终止旧 App/root 进程；卸载 Debug 包后包名、App 进程和 root 子进程均不存在。手机 Download 中本轮 APK/XML/截图临时文件已删除；正式版主进程仍为 PID `4014`，StatusService `isForeground=true`。本地截图证据保存在忽略目录 `local-assets/diagnostics/root-runtime/phase7-rootservice/`。

第二切片在上述清理后实现，未重新安装测试包，也未触发真机输入。`RootInputRequest` 使用 Parcelable 数值字段和固定事务 5；服务端校验动作、displayId、有限坐标、半开窗口边界、0～10 秒 tap/1～10 秒 swipe 及 Tap 双端点一致性，之后才调用系统 `IInputManager` 逐事件路径。root AIDL/实现静态检查无 `execCommand`、`Runtime.exec`、命令字符串或文件路径入口。新增 8 项测试后 App 115/115、Selector 18/18、Release/R8/Vital Lint 通过；必须等显式特权桥接完成后用安全样本验证真实输入，不能把本次构建通过写成真机动作已完成。
