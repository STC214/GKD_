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

- [ ] B01：规则应该执行但未执行，可稳定或统计性复现。
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

当前业务状态不具备再次签到条件，用户同意先跳过该现场并继续其他阶段 0 项目。B01 仍保持未验收；下次出现未签到页面时继续使用上述步骤，不因暂时跳过而把负样本记为通过。

### 6.3 哔哩哔哩冷启动：规则链成功样本

`2026-07-14 12:33:00` 对 `tv.danmaku.bili` 执行一次不清数据的冷启动。ActivityTaskManager 判断为 `tv.danmaku.bili/.MainActivityV2`，AccessibilityManager 显示 UiAutomation 正在监听窗口状态和内容变化；GKD 内部快照 `1784003590642` 保存成功，记录 `appId=tv.danmaku.bili`、`activityId=tv.danmaku.bili.MainActivityV2`、`231` 个节点和 `1080×2400` 屏幕。

订阅 `667` 的 `g10`“分段广告-首页推荐视频卡片广告”在本次启动中形成完整动作链：

| 时间 | 规则 | 目标 | 旧引擎结果 |
| --- | --- | --- | --- |
| `12:33:11.916` | `g10/r0/key0` | `tv.danmaku.bili:id/more` | `clickNode=true` |
| `12:33:12.262` | `g10/r2/key50` | “不感兴趣/相似内容过多”类可点击项 | `clickNode=true` |

第二步依赖 `preKeys=[0,1]`，能够紧随第一步命中，说明本次规则链实际推进，登记为哔哩哔哩成功对照样本，不计为 B01/B02。需要保留的观察点是：第一步动作日志中的节点为 `visibleToUser=false`，纵坐标超出当前屏幕且高度为负；这可能来自 RecyclerView 节点复用或快照/动作并发。现有 `performAction=true` 只能证明系统接受调用，后续阶段仍需通过动作后界面验证确认真实效果。

证据位于忽略目录 `local-assets/diagnostics/root-runtime/bili-20260714_123300/`，包含完整 GKD 当日日志、快照 JSON、最小 JSON、PNG 和原始归档。

## 7. 固定窗口场景

| 场景 | 测试步骤 | 当前前台判断 | 规则结果 | 证据路径 |
| --- | --- | --- | --- | --- |
| 普通全屏 Activity | 不清数据冷启动哔哩哔哩并保存 GKD 内部快照 | Task/UiAutomation/GKD 快照一致为 `tv.danmaku.bili/.MainActivityV2` | `g10/key0 → key50` 两步链执行，均返回 `clickNode=true` | `local-assets/diagnostics/root-runtime/bili-20260714_123300/` |
| 系统权限弹窗 | 待填写 | 待填写 | 待填写 | 待填写 |
| 输入法显示/隐藏 | 打开系统设置搜索框，不输入文字；采集后返回隐藏 | Task 仍为 `com.android.settings/.MiuiSettings`；IME 是独立 `TYPE_INPUT_METHOD` 窗口，`mInputShown=true`，隐藏后为 `false` | UiAutomation 全程在线；未记录规则动作 | `window-scenarios/20260714_125445_input_method_visible/` |
| 通知栏/控制中心 | 桌面展开通知栏，采集后立即收回 | Task 始终为桌面；Accessibility 活动/焦点窗口从桌面 `id=5` 切至系统覆盖层 `id=6`；GKD 前台记录在 `com.miui.home` 与 `com.android.systemui` 间切换 | 无规则动作；收回后恢复桌面和 UiAutomation | `window-scenarios/20260714_124515_desktop/`、`20260714_124519_notification_shade/` |
| 小窗/画中画 | 以 `windowingMode=5` 启动系统设置自由窗；画中画待测 | Settings 实际边界 `Rect(286,712–794,1792)`、`mWindowingMode=freeform`，Accessibility 焦点窗口 `id=109` | UiAutomation 在线；采集后 force-stop 临时 Settings 任务并回桌面 | `window-scenarios/20260714_125740_settings_freeform/` |
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
- [ ] B01“规则未执行”已复现并留存证据。
- [ ] B02“动作未生效但计为成功”已复现并留存证据。
- [ ] 固定窗口场景已完成基线记录。
- [ ] 订阅兼容冒烟测试已完成。

阶段 0 尚未全部完成时，用户因米游社当天已经签到而授权先实施“阶段 0.5 Root 桥前置诊断”。该授权仅覆盖能力状态、自检和有限重连，不包括修改选择器、规则时序、动作结果或重试策略。

当前结论：阶段 0 **进行中**，阶段 0.5 Root 桥已完成实现和真机验收。真机确认系统 Binder `8/8`、Root UserService UID `0`、UiAutomation 在线，手动重新检测不改变相关进程 PID；配置与 3 个订阅文件哈希保持基线一致。另修复了通知权限被拒绝时外部 `ExposeService` 跳过 `startForeground()` 的冷启动崩溃，并在同一设备上按“覆盖安装 → 强停 → ADB 拉起”路径复测通过。

普通全屏、通知栏、输入法、自由窗、旋转和锁屏/解锁已有真机证据；权限弹窗、真实分屏、画中画、双开、10 条规则重复统计和 B01/B02 仍未完成。米游社 B01 因当前已签到暂时跳过；仍不授权进入会改变规则执行逻辑的阶段。

## 11. 阶段 1 结构化决策诊断真机记录

2026-07-14 在同一 Xiaomi Android 16 / KernelSU + Sui 设备上覆盖安装阶段 1 首版：

- 高级设置新增“规则决策诊断”开关，初始值为关闭；开启后只使用进程内环形缓冲。
- 启动哔哩哔哩 `.MainActivityV2` 后，弹窗能按关联 ID 显示 `EventReceived → ForegroundConfirmed → RuleEligible/ForcedRuleSkipped`，规则标识包含真实订阅 `667/app/...`。
- 返回 GKD 高级设置后，最近未执行原因为 `NoApplicableRules`（中文“当前界面没有适用规则”），与当前界面事实一致。
- 弹窗支持最近 30 条预览、“复制全部”和“清空内存记录”。
- 首版 512 条容量在哔哩哔哩高频事件下约十秒填满；最终 2048 条 APK 已原地升级并完成同场景复测，仅使用 91/2048 条，没有发生环形淘汰。
- GKD 自身零规则现在记录为 `NoApplicableRules/Observed`，不再覆盖目标应用最近失败。最终最近失败显示“包名或 Activity 不匹配”，应用为 `com.miui.securitycenter`，详情为 `foreground=tv.danmaku.bili`，与应用启动过渡现场相符。
- 最终 App Debug 单元测试 8/8、Release 构建、Root UserService UID 0、StatusService 前台状态和零新增崩溃均通过；3 个订阅文件 SHA-256 保持阶段 0 基线值。
- 本阶段没有修改 `rule.trigger()`、规则状态计算、选择器、动作实现、延迟或查询唤醒算法；B01/B02 仍需有效现场才能验收“每次漏执行均有准确终止原因”。

## 12. 代码审查修复与待回归项

2026-07-14 对阶段 0.5/1 提交进行代码审查后完成以下收口：

- UserService 连接回调改为原子领取；超时或取消时撤销回调并解绑，迟到 wrapper 立即销毁。
- Root 重连在每轮开始、延迟后和连接返回后重新确认 Shizuku 仍启用且上下文未变化；已销毁上下文永久拒绝安装 wrapper。
- 无障碍事件关联 ID 改为在既有限流和连续事件合并后生成；事件队列不再误删非连续的同类事件。
- `ExposeService` 改用有界的 `shortService` 前台类型，不再依赖特殊用途前台服务 AppOp，并始终按前台服务契约调用 `startForeground()`。
- 诊断缓冲增加 revision，弹窗对成功记录、缓存数量和清空操作实时刷新。

本轮最终 `:app:testGkdDebugUnitTest` 13/13、`:selector:jvmTest` 18/18、`:app:assembleGkdRelease` 通过。13 项 App 测试包含 3 项连接回调竞态和 1 项连续事件合并回归。最终 APK SHA-256 为 `AC4D66E4FAD64606946C9E9251DCDAC8F215B66DC2CCA4C49E9CF62CDDB764B6`，3,303,799 字节。以下项目仍需真机故障注入，未完成前不得把本节写成全部通过：

同日已在原 Xiaomi Android 16 设备完成非清数据覆盖安装和正常路径冒烟：安装成功，主进程启动，StatusService 保持前台，Root UserService PID `14641`、UID `0`；正常权限下外部 `ExposeService(expose=-1)` 启动后按预期自行结束，无新增 `AndroidRuntime`。`store.json` 与 `-2.json`、`666.json`、`667.json` 的 SHA-256 均与安装前一致。

特殊用途 AppOp 故障注入首轮发现：仅在 `onCreate()` 中检测失败并 `stopSelf()`，Android 16 仍抛出一次 `ForegroundServiceDidNotStartInTimeException`，且快照数量保持 2，证明请求未执行但前台契约仍未满足。最终将 ExposeService 改为 Manifest `shortService` 并重新覆盖安装；AppOp 明确保持 `ignore` 时再次调用 `expose=0`，快照数量由 2 增至 3，ExposeService 按预期结束，logcat 无新增崩溃。测试后 AppOp 已恢复为 `allow`。

两次使用 `SIGSTOP/SIGCONT` 可逆暂停 Sui 的尝试均未命中 UserService 内部三秒超时：应用会等待 Sui Binder 恢复后才进入 `buildServiceWrapper()`。暂停期间和恢复后均无新增崩溃，Root UserService 可重新以 UID 0 建立。为避免继续破坏系统级 Sui，超时/迟到回调改由 3 项 JVM 测试直接覆盖“取消后迟到值必须释放、只能完成一次、取消后失败不得恢复”；真实三秒超时仍保留为待测，不把安全暂停结果冒充超时通过。

包含上述测试驱动重构的最终 APK 已再次非清数据覆盖安装：主进程 PID `4548`，Root UserService PID `15788`、UID `0`，StatusService 为前台，特殊用途 AppOp 为 `allow`；设置和 3 个订阅哈希继续保持不变。随后正常调用 `ExposeService(expose=-1)`，无新增 `AndroidRuntime`。

- 制造 UserService 首次连接超时，确认最多有限重连且无残留 `shizuku-user-service` 进程。
- 重连进行中关闭 Shizuku 优化，确认后续尝试立即终止。
- 高频打开哔哩哔哩并导出诊断，确认不存在只有 `EventReceived` 的限流/合并孤立 ID。
