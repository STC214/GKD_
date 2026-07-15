# GKD Root 加强版与官方版代码差异及上游整合手册

## 1. 文档定位

本文档是本 fork 的长期维护手册，用于回答以下问题：

- 当前代码相对官方 GKD 到底改了什么。
- 哪些差异已经落地，哪些只是规划，哪些已经废弃。
- 每项差异改变了什么行为、涉及哪些符号、如何验证。
- 官方仓库更新后，哪些文件可以直接接收上游，哪些必须人工三方合并。
- 若官方重构或删除了原文件，应迁移“行为意图”还是保留旧实现。
- 每完成一个 Root 加强版开发阶段，应如何同步更新差异记录。

本文档不是发布日志。发布日志只说明用户能看到的变化；本文档必须保留代码级原因、边界、依赖和合并方法。

## 2. 状态术语

所有差异必须标记为以下状态之一：

| 状态 | 含义 | 合并时处理 |
| --- | --- | --- |
| `CURRENT` | 已存在于当前提交历史 | 必须确认保留、上游已吸收或明确删除 |
| `WORKTREE` | 已在工作区修改但尚未提交 | 合并上游前必须先提交、暂存到独立分支或明确排除 |
| `PLANNED` | 已批准但尚未实现 | 不得当成现有行为，不参与当前代码冲突处理 |
| `UPSTREAMED` | 官方已实现等价或更好的方案 | 优先采用上游，并删除本 fork 重复实现 |
| `DROPPED` | 已决定不再维护 | 合并时不再移植 |
| `BLOCKED` | 因兼容性、安全或设备问题暂停 | 保留证据，不继续扩大改动 |

禁止把计划性描述写成已经存在的代码。每次修改本文件时，都要同步状态和对应提交。

## 3. 当前基线快照

快照核对时间：`2026-07-15`（官方基线仍为上次已核对提交，本轮未联网刷新官方 HEAD）。

| 项目 | 值 |
| --- | --- |
| 官方仓库 | `https://github.com/gkd-kit/gkd.git` |
| 官方分支 | `main` |
| 官方 HEAD | `0b75375c0f40df62e93866e0e157d4e1ebc45c67` |
| 官方 HEAD 标题 | `fix: UserInfo name is null` |
| 当前 fork 仓库 | `https://github.com/STC214/GKD_.git` |
| 当前 fork 分支 | `main` |
| 当前 fork HEAD | `f2e60bd0e16fbaed416663b09d0c13aa6473c2df` |
| 共同祖先 | `0b75375c0f40df62e93866e0e157d4e1ebc45c67` |
| 分叉关系 | 相对上述官方基线，fork 领先 11 个提交；官方实时领先数本轮未刷新 |
| 当前应用版本 | `1.12.1` |
| 当前 Git remote | 只有 `origin`，尚未持久配置 `upstream` |

本次通过以下只读方式核对官方 HEAD，没有合并或重置工作区：

```powershell
git fetch --no-tags https://github.com/gkd-kit/gkd.git main
git rev-parse FETCH_HEAD
git merge-base HEAD FETCH_HEAD
git rev-list --left-right --count FETCH_HEAD...HEAD
```

注意：`FETCH_HEAD` 会在下一次 fetch 后变化，不能作为长期基线名称。正式整合前应配置 `upstream` remote 或把具体提交 ID 写入整合记录。

## 4. 三个版本层级必须分开理解

### 4.1 官方原版

官方版本已经包含：

- Compose UI、Room 数据库、订阅管理、备份和快照。
- `RawSubscription` 与 JSON/JSON5 解析。
- `selector` 选择器模块。
- 标准 `AccessibilityService` 执行通道。
- Shizuku/Sui 辅助的隐藏系统 API 和 `UiAutomation` 通道。
- `A11yRuleEngine`、规则状态机、动作执行和日志。

官方版不是“完全无特权”的简单应用；它已经具有 Shizuku/UiAutomation 能力。Root 加强版的价值不能仅定义成“能执行 root 命令”，而必须体现在更可靠的焦点判断、事件调度、动作结果和多用户处理上。

### 4.2 当前 fork 已落地版本

当前已提交版本在官方基线之上增加：

- KernelSU/SukiSU Ultra 安装、授权、后台启动和保活包装。
- 模块打包脚本、配置、执行页日志和卸载行为。
- 截图与 Bitmap/VirtualDisplay/ImageReader 生命周期修复。
- HTTP 服务销毁时主动停止服务器。
- 仓库根目录、Git 忽略和换行规则整理。
- Root UserService UID/命令真实性诊断、有限重连和回调竞态保护。
- 2048 条结构化规则决策诊断、导出与诊断分析脚本。
- Root/无障碍动作真实结果、手势完成/取消回调和失败不计次数语义。

当前已提交版本尚未包含：

- APK 内置 `RootService`。
- `ForegroundResolver`、`WindowProvider`、`ActionExecutor` 等新接口。
- focused Task 与 focused Accessibility Window 融合。
- 查询 pending/dirty 状态机。
- 动作后的界面结果验证。
- user 999/多显示屏完整运行时上下文。

### 4.3 规划中的 Root 加强版

规划版本将保留官方 UI、订阅、规则和数据库，仅重构运行时：

| 领域 | 官方/当前实现 | Root 加强版目标 | 订阅影响 |
| --- | --- | --- | --- |
| 前台应用 | `getTasks().firstOrNull()` 与无障碍事件修正 | focused Task + focused Window + root package + user/display 融合 | 无 |
| Activity | Task 顶部 Activity 与事件更新 | 只从 focused/visible Task 取 Activity，并附置信度 | 无 |
| 窗口 | `rootInActiveWindow` 为主 | 明确选择 `AccessibilityWindowInfo.isFocused` 窗口 | 无 |
| 调度 | `querying` 时直接返回 | conflated 唤醒或 dirty/pending 状态机 | 无 |
| 节点 | 事件节点、root 与秒级缓存 | generation、短时退避、强制刷新与失效策略 | 无 |
| 动作 | 节点动作、Shizuku 输入、无障碍手势 | 真实 Boolean、完成回调、displayId、动作后验证 | 无 |
| 特权 | Shizuku/Sui | APK 内置 root Binder 服务，Shizuku 和无障碍回退 | 无 |
| 多用户 | 部分包查询支持 userId | `userId + packageName` 作为运行时身份 | 第一轮不改订阅格式 |
| 诊断 | 成功动作日志为主 | 每轮判定的结构化失败原因 | 无 |

详细实施顺序见 `docs/root-runtime-refactor-plan.md`。

## 5. 当前 fork 提交清单

共同祖先之后共有 11 个 fork 提交：

| 顺序 | 提交 | 主要内容 | 未来处置 |
| ---: | --- | --- | --- |
| 1 | `eb1cf436` | 初始 KernelSU/SukiSU 模块、配置、脚本和忽略规则 | 模块仍存在时保留；RootService 稳定后重新评估 |
| 2 | `32a1a4ed` | README 模块使用说明 | 与实际模块行为保持同步 |
| 3 | `f8c9d965` | 快照资源释放修复、模块和打包增强 | App 资源修复独立保留；模块部分单独处理 |
| 4 | `5377cbc7` | 模块启动/配置调整 | 以模块实际脚本为准 |
| 5 | `aa3df106` | 卸载默认值和模块版本调整 | 若模块退役则删除对应差异 |
| 6 | `d4fbebe1` | 模块动作页状态和日志 | 若模块保留则继续维护 |
| 7 | `5ab462d4` | 文档、时间戳版本同步 | 文档必须与打包产物一致 |
| 8 | `35c5d67a` | 根目录整理、保活守护、仓库卫生 | `local-assets` 继续保持本地化 |
| 9 | `b829f66b` | Root 桥真实性、结构化诊断、基线与维护文档 | 保留诊断行为；按新上游控制流重新挂接旁路记录 |
| 10 | `d33249d1` | Root 回调竞态、外部服务契约与诊断收口 | 保留连接代际和服务启动边界，避免恢复迟到回调 |
| 11 | `f2e60bd0` | 动作结果可信化、诊断导出与当前进度入口 | 保留真实完成/取消结果和失败不触发规则语义 |

整合上游时可以使用：

```powershell
$base = git merge-base upstream/main HEAD
git log --oneline --reverse "$base..HEAD"
git range-diff "$base..upstream/main" "$base..HEAD"
```

不要仅根据提交标题决定取舍。部分时间戳标题提交同时包含 App 修复和模块修改，必须查看文件级差异。

## 6. 当前已提交文件级差异索引

相对上述官方基线的当前 HEAD 已提交差异共涉及 53 个文件。下表保留最早的模块、截图资源和仓库卫生差异；阶段 0.5、1、2 的新增运行时文件与符号由第 9 节逐项列出，不能再把本表的 19 项误读为完整文件清单。每次整合前使用以下命令生成实时全集：

```powershell
$base = git merge-base upstream/main HEAD
git diff --name-status "$base..HEAD"
```

| 文件 | 状态 | 类别 | 冲突风险 | 默认整合策略 |
| --- | --- | --- | --- | --- |
| `.gitattributes` | `CURRENT` 新增 | 仓库卫生 | 低 | 保留模块脚本 LF 规则 |
| `.gitignore` | `CURRENT` 修改 | 仓库卫生 | 中 | 合并上游新增项，再保留 fork 本地目录规则 |
| `CHANGELOG.md` | `CURRENT` 修改 | 文档 | 高 | 不整文件选 ours/theirs，按章节合并 |
| `README.md` | `CURRENT` 修改 | 文档入口 | 高 | 以新上游 README 为底，重新插入 fork 章节 |
| `HttpService.kt` | `CURRENT` 修改 | 生命周期修复 | 中 | 若上游未等价修复，保留显式 `stop()` |
| `SnapshotPage.kt` | `CURRENT` 修改 | Bitmap 释放 | 中 | 检查新上游所有权后迁移释放语义 |
| `ImageUtils.kt` | `CURRENT` 修改 | Bitmap 释放 | 高 | 重点检查 `recycle` 所有权，人工三方合并 |
| `ScreenshotUtil.kt` | `CURRENT` 修改 | 截图资源释放 | 高 | 按资源状态机迁移，禁止盲选整文件 |
| `SnapshotExt.kt` | `CURRENT` 修改 | Bitmap 释放 | 高 | 保留最终 Bitmap 单一所有权语义 |
| `ksu-sukisu-module/README.md` | `CURRENT` 新增 | 模块文档 | 低 | 与模块实际行为同步 |
| `module/action.sh` | `CURRENT` 新增 | 模块动作页 | 低 | 模块存在时独立维护 |
| `module/config.conf` | `CURRENT` 新增 | 模块配置 | 低 | 默认值与 `service.sh` 一致 |
| `module/customize.sh` | `CURRENT` 新增 | 模块安装 | 低 | 校验文件与权限 |
| `module/module.prop` | `CURRENT` 新增 | 模块元数据 | 低 | 打包时生成版本，不手工信任旧值 |
| `module/service.sh` | `CURRENT` 新增 | 模块启动/守护 | 中 | 真机和 ZIP 共同验证 |
| `module/skip_mount` | `CURRENT` 新增 | 模块标记 | 低 | 保留空标记文件 |
| `module/uninstall.sh` | `CURRENT` 新增 | 模块卸载 | 中 | 明确数据风险后再保留默认卸载 |
| `package-ksu-module.ps1` | `CURRENT` 新增 | 构建打包 | 中 | 跟随上游 APK 产物路径调整 |

以下工作区文档尚未属于上述已提交差异：

| 文件 | 状态 | 说明 |
| --- | --- | --- |
| `docs/root-runtime-refactor-plan.md` | `WORKTREE` | Root 加强版阶段计划 |
| `docs/upstream-code-delta-guide.md` | `WORKTREE` | 本文档 |
| `README.md` 开发路线段落 | `WORKTREE` | 两份开发文档入口 |

## 7. 已落地 App 代码差异详解

### 7.1 `HttpService.kt`

当前差异：

```kotlin
onDestroyed {
    if (storeFlow.value.autoClearMemorySubs) {
        deleteSubscription(LOCAL_HTTP_SUBS_ID)
    }
    httpServerFlow.value?.stop()
    httpServerFlow.value = null
}
```

目的：

- 在 Android Service 销毁时主动停止 Ktor/CIO server。
- 避免只清空 Flow 引用但底层监听端口或协程仍然存活。

行为不变量：

- `autoClearMemorySubs` 行为不变。
- `httpServerFlow` 最终必须为 `null`。
- `stop()` 必须在丢失最后引用之前调用。

上游更新时检查：

1. 搜索上游 `HttpService.onDestroyed`。
2. 若上游已通过生命周期、`close()`、`stop()` 或 scope cancel 等价释放 server，采用上游实现并将本差异标记为 `UPSTREAMED`。
3. 若上游仅改变 server 类型，查清新类型的关闭 API，不要机械保留旧 `stop()`。
4. 若仍只是置空引用，重新加入显式关闭。

最低测试：

- 启动 HTTP 服务并确认端口可访问。
- 停止服务后确认端口释放。
- 连续启动/停止三次，无端口占用提示。

### 7.2 `SnapshotPage.kt`

当前差异：替换截图时先计算尺寸是否一致，再回收旧、新 Bitmap。

目的：

- 防止替换截图操作长期持有两个完整屏幕 Bitmap。
- 保证尺寸比较完成后无论成功或失败都释放解码对象。

当前所有权：

```text
oldBitmap：仅用于读取 width/height，比较后立即 recycle。
newBitmap：仅用于读取 width/height，实际写入使用 newBytes，因此比较后立即 recycle。
newBytes：尺寸一致时写入截图文件。
```

上游更新时检查：

- 如果上游改成 `ImageDecoder`、流式尺寸读取或 `BitmapFactory.Options.inJustDecodeBounds`，优先采用不分配完整 Bitmap 的上游/新方案。
- 如果后续代码仍使用 `oldBitmap` 或 `newBitmap`，必须移动 `recycle()`，不能照搬当前位置。
- 不能在读取 width/height 前回收。

最低测试：

- 相同尺寸图片替换成功。
- 不同尺寸图片被拒绝。
- 两种路径都不出现 recycled bitmap 异常。

### 7.3 `ImageUtils.kt`

当前差异：将整个保存流程放入 `try/finally`，保证 `recycle=true` 时所有返回路径都回收源 Bitmap。

需要保留的语义：

```text
调用方传入 recycle=true：ImageUtils 获得 Bitmap 最终释放责任。
调用方传入 recycle=false：ImageUtils 不得回收 Bitmap。
Android Q 以下和 Q 以上所有成功/失败返回路径遵守同一所有权。
```

这是高风险合并点，因为 Bitmap 的所有权取决于调用方。如果上游改变调用方式，必须先搜索：

```powershell
rg -n "save2Album\(" app/src/main/kotlin
```

上游整合步骤：

1. 列出所有调用者及 `recycle` 参数。
2. 确认调用后是否继续访问 Bitmap。
3. 检查上游是否已经在调用方回收。
4. 只允许一个层级拥有最终释放责任。
5. 运行成功、权限拒绝、插入失败和压缩失败路径。

禁止事项：

- 不能因为看到上游增加 `bitmap.recycle()` 就同时保留本 fork `finally`。
- 不能在 `MediaStore` 写入完成前回收。
- 不能用 GC 会自动回收作为忽略 native 图像内存峰值的理由。

### 7.4 `ScreenshotUtil.kt`

当前差异引入 `releaseCapture()`，集中释放：

- `ImageReader` listener；
- `VirtualDisplay`；
- `ImageReader`；
- 对应成员引用。

同时处理：

- 新截图前释放旧 capture；
- 协程取消时释放本次 reader/display；
- 成功取得非透明截图后释放；
- 透明帧回收 Bitmap 并继续等待；
- 异常时释放；
- `destroy()` 时释放并停止 MediaProjection。

核心状态机：

```text
Idle
  -> ReaderCreated
  -> DisplayCreated
  -> WaitingFrame
      -> TransparentFrame -> WaitingFrame
      -> ValidFrame -> Released
      -> Exception -> Released
      -> CoroutineCancelled -> Released
  -> destroy -> Released + ProjectionStopped
```

上游更新时必须逐项回答：

- 新实现是否仍使用 `ImageReader` 和 `VirtualDisplay`。
- listener 是否会在 continuation 已完成后再次进入。
- 透明帧 Bitmap 是否回收。
- `Image` 是否在每一帧 finally 中关闭。
- stride 临时 Bitmap 是否回收。
- continuation 取消时是否释放 display/reader。
- `MediaProjection` 是否允许复用；若不允许，是否每次重建。

不能整文件选择 ours，因为截图 API 很可能被上游重构。应迁移上述资源状态机语义。

最低测试：

- 连续截图 20 次。
- 取消截图协程。
- 触发透明首帧后获得有效帧。
- 旋转屏幕后截图。
- 停止服务后再次申请截图。
- 检查 logcat 中 HardwareBuffer/ImageReader 资源警告。

### 7.5 `SnapshotExt.kt`

当前差异：

- 状态栏裁剪产生新 Bitmap 时，回收旧 `finalBitmap`。
- 快照文件和数据库写入结束后，在 `finally` 中回收最终 Bitmap。

需要保持的所有权链：

```text
rawPicture/fallback Bitmap
    -> finalBitmap
    -> 可能裁剪为 processedBitmap
    -> 写入 PNG
    -> recycle 最终 Bitmap
```

若 `processedBitmap !== finalBitmap`，旧 Bitmap 必须在切换所有权时释放；最终 Bitmap 必须在保存成功或失败后释放。

上游更新时检查：

- `cropBitmapStatusBar` 是否返回原对象、子 Bitmap 或新对象。
- `Bitmap.createBitmap` 在对应 Android 版本是否可能返回共享对象。
- 是否新增异步消费者在当前函数返回后继续使用 Bitmap。
- 通知或数据库是否仅使用文件，不持有 Bitmap。

最低测试：

- 开启和关闭隐藏状态栏选项各生成快照。
- 无截图权限、应用禁止截图和正常截图三种状态。
- 写文件失败时仍能恢复 `captureLoading=false` 且无 Bitmap 泄漏。

## 8. KernelSU/SukiSU 包装差异详解

### 8.1 定位

当前 `ksu-sukisu-module/` 是普通 APK 的安装和运行辅助，不替代规则引擎，不是 Root 加强版 App 的最终 root 运行时。

它当前负责：

- 可选安装内置 APK；
- 授予通知和安全设置权限；
- 配置后台 AppOps；
- 加入电池白名单；
- 可选开启无障碍；
- 后台启动 `ExposeService`；
- 低频检查 GKD 进程和常驻通知服务；
- 定期重授权；
- 在模块执行页展示状态和日志；
- 卸载模块时按配置卸载 App。

### 8.2 配置契约

以下变量在 `config.conf`、`service.sh` 默认值、`action.sh` 展示和 README 中必须一致：

| 变量 | 当前默认值 | 含义 |
| --- | ---: | --- |
| `GKD_PACKAGE` | `li.songe.gkd` | 目标包名 |
| `GKD_AUTO_INSTALL` | `1` | 自动安装 APK |
| `GKD_AUTO_GRANT` | `1` | 自动授权和配置 AppOps |
| `GKD_AUTO_ENABLE_A11Y` | `0` | 自动开启无障碍 |
| `GKD_AUTO_START` | `1` | 开机后台启动 |
| `GKD_KEEP_ALIVE` | `1` | 启用低频守护 |
| `GKD_KEEP_ALIVE_INTERVAL` | `300` | 守护间隔秒数 |
| `GKD_REGRANT_INTERVAL` | `1800` | 重授权间隔秒数 |
| `GKD_REQUIRE_BUNDLED_APK` | `0` | 内置 APK 安装失败是否停止 |
| `GKD_UNINSTALL_ON_REMOVE` | `1` | 移除模块时卸载 App |

修改任何默认值时，必须同时搜索：

```powershell
rg -n "GKD_[A-Z_]+" ksu-sukisu-module README.md CHANGELOG.md
```

### 8.3 打包契约

`package-ksu-module.ps1` 当前：

- 默认执行 `app:assembleGkdRelease`；
- 支持 `-ApkPath`；
- 支持 `-SkipBuild`；
- 从 release 输出中选择 APK；
- 使用分钟级时间戳写入模块版本；
- 将 APK 放入 `common/gkd.apk`；
- 输出 `ksu-sukisu-module/dist/gkd-ksu-sukisu-module.zip`。

官方更新后重点检查：

- Gradle task 名是否变化。
- product flavor 是否仍为 `gkd`。
- APK 输出目录和文件名是否变化。
- `versionCode` 约束是否变化。
- 签名配置是否仍兼容覆盖安装。
- Manifest 中 `ExposeService` 组件名和 exported 状态是否变化。

### 8.4 RootService 完成后的去留决策

RootService 稳定前，模块维持 `CURRENT`。

RootService 完成长测后，单独决定：

1. 保留模块作为自动安装/开机辅助；
2. 精简为只安装 APK；
3. 完全退役模块。

不得在 RootService 初次可运行时立即删除模块。必须先验证 APK 自启动、root 撤权、更新安装、卸载清理和 ROM 后台限制。

## 9. 规划中的 Root 加强版代码差异地图

本节全部为 `PLANNED`，实现后必须逐项改为 `CURRENT` 并补充实际提交、符号和测试结果。

### 9.0 阶段 0：基线工具和 selector 测试修正

状态：`CURRENT`（工作区，阶段 0 尚未完成，提交 ID 待定）。

新增：

```text
docs/testing/root-runtime-baseline.md
scripts/capture-root-runtime-baseline.ps1
scripts/capture-window-scenario.ps1
```

修改：

- `selector/src/jvmTest/kotlin/li/songe/selector/QueryUnitTest.kt`
  - 符号：`QueryUnitTest.example3`。
  - 官方/原断言把选择器中的 `<2` 路径期望写成 `<`，而输入选择器和 `formatConnectOffset` 的实际结果均明确包含偏移量 `2`。
  - 加强版只把期望修正为 `[163 <2 161, 161 -> 163, 160 + 161]`，不修改 parser、matcher、路径计算或运行时选择器语义。

不变量：

- 不修改规则 JSON/JSON5、选择器语法、订阅、数据库或备份。
- ADB 采集脚本只读取系统诊断信息，不读取 App 私有数据，不在 Git 跟踪目录保存设备输出。
- ADB 采集脚本默认不调用外部 `uiautomator dump`；`-IncludeUiAutomatorDump` 只用于 GKD Automation 已停止或专门复现 UiAutomation 通道冲突的场景。
- 阶段 0 工具完成不代表真机复现集验收完成。

上游迁移方法：

1. 先查看上游 `QueryUnitTest.example3` 是否已把期望改为 `<2`；若已修复，删除本 fork 的重复测试补丁。
2. 若上游调整 `formatConnectOffset` 语义，先确认 `<2` 的 DSL 含义，再按新语义更新测试，禁止为通过测试修改运行时结果。
3. `capture-root-runtime-baseline.ps1` 与 App 运行时代码无耦合，可独立保留；若包名变化，只更新脚本中的 `li.songe.gkd`。

验收：

- `app:assembleGkdRelease`：通过。
- `app:testGkdDebugUnitTest`：通过。
- `selector:jvmTest`：首次 18 项中 1 项因旧期望失败；修正测试断言后 18/18 通过。
- Xiaomi Android 16 / API 36、KernelSU 4.1.3 + Sui 真机：ADB 和 root 身份通过。
- 从 `1.12.1-5ab462d` 原地升级到 `1.12.1-35c5d67`：通过；`store.json` 和 3 个订阅文件哈希不变，数据库订阅/配置/日志计数保持一致。
- 已从设备实际存在的 6935 条订阅规则中选定 10 条能力样本；每条 10 次和窗口场景仍待执行。
- 7 天旧日志中，排除相同 Activity 的内部序号变化后，发现 149 次 TaskStack/A11y 在 250ms 内指向不同前台上下文、140 次 `fixAppId` 纠正；登记为 `B01-CANDIDATE-FOREGROUND-RACE`，因缺少查询关联 ID，暂不宣称已证明漏执行因果。
- App 自带备份导出/导入：通过；492,423 bytes、9 个 ZIP 条目、3 个订阅、22 条订阅配置，ZIP 完整性通过。两次导入日志均记录“导入成功”，设置和订阅文件哈希、订阅/配置计数均不变。用户未看到成功短 Toast，登记为提示可见性问题而不是恢复失败。
- 米游社签到页被确定为 B01 主复现场景，哔哩哔哩为对照。首次现场发生在当天已经签到之后：签到链规则 0 未命中，规则 1–3 保持前置规则未满足，无动作/Toast 属于正确负样本，不计为漏执行。外部 `uiautomator dump` 与 GKD UiAutomation 冲突，后续节点证据改由现有 `ExposeService(expose=0)` 生成。
- 重启后的锁屏/解锁基线已采集：GKD 普通前台服务和 Sui 守护进程先恢复；首次解锁后系统无障碍短暂接管，约 2 分钟后按 `automatorMode=2` 切回 UiAutomation。规则引擎最终可用，但 root 用户服务出现一次 3 秒连接超时，保留为桥接稳定性观察项。
- 哔哩哔哩冷启动成功样本已保存：Task、UiAutomation 和 GKD 快照一致为 `.MainActivityV2`，快照含 231 个节点；订阅 `667/g10` 的 `key0 → key50` 两步链均记录 `clickNode=true`。第一步节点同时出现 `visibleToUser=false`、越界和负高度，当前不误判为失败，留给阶段 2 的动作后验证机制复核。
- 窗口矩阵新增轻量采集脚本并完成通知栏、输入法、自由窗和真实横屏样本。通知栏证明 Task 前台仍为桌面时，Accessibility 活动窗口可切到 SystemUI；自由窗证明必须保留窗口边界/windowing mode；横屏测试证明普通 shell 无 `WRITE_SETTINGS`，root 可完成可逆系统状态测试。分屏的 `windowingMode=3/4` 参数未形成 `mInSplitScreen=true`，保持待手动验证。
- 截至 `2026-07-14` 首次负样本，米游社当天已签到，暂不具备 B01 有效复现条件；当时按用户决定先推进其他阶段 0 项目且不把 B01 标记为通过。`2026-07-15` 的有效现场、兼容实现和验收见 9.2.1。

### 9.0.1 阶段 1 前置：Root 桥真实性与有限重连

状态：`CURRENT`（工作区，Root 桥真机验收已完成；规则复现集仍属阶段 0）。

新增：

```text
app/src/main/kotlin/li/songe/gkd/shizuku/RootBridgeDiagnostics.kt
app/src/main/kotlin/li/songe/gkd/shizuku/ConnectionCallbackGate.kt
app/src/test/kotlin/li/songe/gkd/shizuku/RootBridgeDiagnosticsTest.kt
app/src/test/kotlin/li/songe/gkd/shizuku/ConnectionCallbackGateTest.kt
scripts/stop-next-root-user-service.sh
scripts/analyze-decision-diagnostics.ps1
```

修改：

- `shizuku/ShizukuApi.kt`
  - `ShizukuContext.serviceWrapper` 从构造期只读值改为线程可见、仅允许安装一次的可替换连接；其他系统 Binder 保持原实例。
  - `ShizukuContext.destroy()` 设置不可逆的 `destroyed` 标记；已经销毁的上下文拒绝安装迟到 wrapper。
  - 新增 `rootBridgeDiagnosticsFlow`、`refreshRootBridgeDiagnostics()`、`retryRootUserService()`。
  - 初次 UserService 失败后最多后台重试 2 次；每轮开始、重试延迟后和连接返回后均重新检查 Shizuku 开关及上下文身份，上下文已变化时销毁新建 wrapper，禁止旧连接串入新上下文。
  - 只有远端 `id` 成功且 UID 为 0 时才分类为 `Root`；系统 Binder 可用但 UserService 缺失时分类为 `Partial/Failed`。
- `shizuku/UserService.kt`
  - `buildServiceWrapper()` 返回 `UserServiceConnectionResult`，区分 `BindException`、`InvalidBinder` 和 `Timeout`。
  - ServiceConnection 回调使用原子领取；超时/取消立即撤销回调并只执行一次主动解绑，取消后才到达的有效 wrapper 必须立即 `destroy()`，不得恢复已取消 continuation 或泄漏 Root 进程。
- `ui/AdvancedPage.kt`
  - 原“授权状态”弹窗扩展为“Root 与授权状态”，显示系统 Binder 数量、UserService、远端 UID、Shell 自检、UiAutomation、连接尝试和失败原因。
  - 增加“重新连接/重新检测”按钮。
- `notif/Notif.kt`
  - 不再用 `POST_NOTIFICATIONS` 的结果阻止 `startForeground()`。Android 13+ 即使拒绝通知权限也允许前台服务运行；旧逻辑会让 shell 通过 `startForegroundService()` 拉起 `ExposeService` 时触发 `ForegroundServiceDidNotStartInTimeException`。
  - 仍保留特殊用途前台服务 AppOp 检查；它与通知展示权限是两项独立能力。
- `service/ExposeService.kt`
  - 该服务是有界命令桥，Manifest 前台类型由 `specialUse` 改为 `shortService`；调用 `notifyService(requireSpecialUse=false)`，因此不再依赖特殊用途 AppOp，并且仍会在系统时限内进入前台。
  - `notifyService()` 返回是否真正进入前台；无法进入前台时不执行请求，立即 `stopSelf()` 并返回 `START_NOT_STICKY`。

不变量：

- 不修改订阅格式、数据库、选择器、规则状态、匹配时序和动作选择。
- UserService 重连不重建 `ShizukuContext` 的 Package/Activity/Window/Input/Accessibility Binder，不主动关闭或重启 UiAutomation。
- 初次连接 1 次加后台重连 2 次，总次数存在硬上限；不会无限循环。
- 自检只执行 `id` 和只读前台查询，不注入点击、滑动或按键。

上游迁移方法：

1. 先确认上游是否已经把 UserService 与其余 Shizuku Binder 拆分状态；若已有等价能力，优先映射状态字段而不是保留两套重连器。
2. 若上游更改 `Shizuku.UserServiceArgs` 或 ServiceConnection 生命周期，保留 `UserServiceConnectionResult` 的失败分类、原子回调领取、取消解绑和迟到 wrapper 销毁，并重新验证延迟回调和取消语义。
3. 若上游新增 RootService，迁移时仍以远端 `id`/实际调用结果作为真实性判断，不能用授权开关代替能力检测。
4. 合并 `AdvancedPage` 时保留官方新增设置项，只在 Shizuku 状态弹窗内移植诊断字段和按钮。

验收：

- `RootBridgeDiagnosticsTest`：3/3 通过，覆盖 root/shell UID 解析以及 `Disconnected/Partial/Failed/Root/NonRoot` 分类。
- `app:testGkdDebugUnitTest`：通过；`app:assembleGkdRelease`：通过。
- Xiaomi Android 16 / KernelSU + Sui 原地升级：系统 Binder `8/8`，UserService 进程 UID/GID 为 `0`，Shell 自检 UID 为 `0`，UiAutomation 在线。
- 手动“重新检测”前后主进程 PID 和 Root UserService PID 均不变，未重建 UiAutomation。
- Root 明确撤销 `POST_NOTIFICATIONS` 并确认 `granted=false` 后，强停状态下由 ADB 冷启动 `ExposeService` 成功；5 秒内无新增 `AndroidRuntime`/前台服务超时，Root UserService 已上线。当前 Root 授权流程随后把通知权限恢复为 granted，测试结束也确认权限处于原有 granted 状态。
- `store.json`、`-2.json`、`666.json`、`667.json` 的 SHA-256 与阶段 0 Root 原始快照逐项一致。
- 本次 Release APK：SHA-256 `E42F0BBA837D6950935FE01EEB9A62EECBA9DDF2441C9009245F5B5B8E8C24DB`，3,303,851 字节；签名证书 SHA-256 仍为 `993B6D94D5AB2F34C95D95DFC6AB002E5BD939E6404BF36BC334998DBD8D9A9A`。

### 9.1 阶段 1：结构化诊断

状态：`CURRENT`（工作区，首轮真机链路通过）。

新增：

```text
runtime/diagnostics/DecisionTrace.kt
runtime/diagnostics/DecisionReason.kt
runtime/diagnostics/DecisionTraceBuffer.kt
app/src/test/kotlin/li/songe/gkd/runtime/diagnostics/DecisionTraceBufferTest.kt
app/src/test/kotlin/li/songe/gkd/a11y/A11yEventQueueTest.kt
```

修改：

- `A11yRuleEngine.kt`
  - 无障碍事件先经过既有限流与队列合并，再由最终被消费的事件创建关联 ID，并把该 ID 传入查询阶段；被限流或被合并的事件不再留下孤立 ID。定时、强制和延迟查询在没有事件 ID 时自行分配。
  - 队列只合并从队首开始连续且 `sameAs()` 的事件，禁止用全队列筛选数量删除队首，避免误删夹在同类事件之间的不同事件。
  - 旁路记录服务/匹配开关、前台确认、查询占用、窗口 root、规则状态、包名/Activity、选择器、动作提交和现有 Boolean 动作结果。
  - 捕获 `InterruptRuleMatchException` 时先记录 `StaleContext`，随后原样重新抛出，保持既有中断语义。
- `ResolvedRule.kt`
  - 新增只读 `diagnosticId`：`订阅/规则类型/组 key/规则 key 或 index`；不进入订阅序列化格式。
- `SettingsStore.kt`
  - 新增默认 `false` 的 `enableRuleDiagnostics`。旧 `store.json` 缺少字段时使用默认值；只有用户切换开关才产生一次正常设置写入。
- `AdvancedPage.kt`、`AdvancedVm.kt`
  - 日志区新增诊断开关和最近决策入口；弹窗通过缓冲 revision 响应每次追加和清空，实时显示缓存数量与最近 30 条，支持复制全部、清空内存，以及将完整文本导出到应用日志目录的 `decision-diagnostics.txt`。

诊断数据模型：

- `DecisionStage`：`Event/Foreground/Query/Window/Rule/Selector/Action`。
- `DecisionOutcome`：`Observed/Skipped/Submitted/Succeeded/Failed`。
- `DecisionReason` 使用稳定英文枚举名和中文展示文本，覆盖服务未连接、匹配关闭、前台未确认、窗口 root 为空、包名/Activity 不匹配、次数/前置/延迟/冷却/超时、选择器未命中以及动作拒绝/取消/验证失败。
- 环形缓冲硬上限 2048 条，只存在于应用进程内；弹窗最多渲染最近 30 条，导出为剪贴板纯文本。
- 关闭诊断时 `newCorrelationId()` 返回 `null`，所有逐规则记录函数在读取前台状态前立即返回，不写数据库、文件或 Android log。

真机与自动测试：

- `DecisionTraceBufferTest`：5 项，覆盖环形淘汰、关闭零记录、稳定枚举导出、正向观察不会覆盖最近未执行原因，以及每次追加/清空都会推进 UI revision。
- 哔哩哔哩 `.MainActivityV2`：已记录订阅 `667/app/...` 的规则状态与 `ForcedRuleSkipped`，关联 ID 能串起事件、前台和规则阶段。
- 返回 GKD 高级设置：最近原因显示 `NoApplicableRules`，与当前界面无适用规则的事实一致。
- 首轮 512 条缓冲在高频事件应用中约十秒填满，因此最终实现提高为 2048 条，并把 `RuleEligible` 移到强制窗口检查之后，避免为必然跳过的规则写两条记录。
- 最终 2048 条 APK 原地升级后，同一轮哔哩哔哩启动、等待和返回高级设置仅产生 91 条记录；没有发生环形淘汰。
- GKD 自身零规则使用 `NoApplicableRules/Observed`，不会覆盖目标应用的最近失败。最终最近失败为 `PackageActivityMismatch`，应用 `com.miui.securitycenter`，详情 `foreground=tv.danmaku.bili`，准确保留了过渡窗口与任务前台不一致的现场。
- 首版 App Debug 单元测试共 8/8 通过；最终审查修复后为 13/13，其中决策缓冲 5 项、连接回调竞态 3 项、连续事件合并 1 项、Root 桥分类 3 项、原有示例 1 项。Selector JVM 测试 18/18、Release 构建通过；带日志目录导出的最终 APK SHA-256 为 `A606F46CBF74458FDEEB34223B60C27FF19BAC76472058C3A913BEC835D8E86A`，3,303,799 字节。
- 最终阶段 1 APK：SHA-256 `32BDAB0954BD77B673C61805321E8F21691004C9EF2122B2FE6F2E7155BF2347`，3,303,851 字节；证书 SHA-256 为 `993B6D94D5AB2F34C95D95DFC6AB002E5BD939E6404BF36BC334998DBD8D9A9A`。
- 真机 Root UserService 仍为 UID 0，StatusService 保持前台，本轮 logcat 无 `AndroidRuntime` 崩溃。3 个订阅 JSON 的 SHA-256 均与阶段 0 基线一致；`store.json` 因用户开启新增的诊断开关而产生预期设置差异。
- 审查修复版在同一设备非清数据覆盖安装成功：Root UserService UID `0`，StatusService 前台状态正常，设置和 3 个订阅文件哈希均与安装前一致。首次特殊用途 AppOp 故障注入证明仅 `stopSelf()` 仍会被 Android 16 判定为未及时进入前台；改用 `shortService` 后在 AppOp 明确保持 `ignore` 时 `ExposeService(expose=0)` 正常生成快照、服务自行结束且零新增崩溃，随后 AppOp 已恢复为 `allow`。
- `stop-next-root-user-service.sh` 真机命中首次三秒超时：PID `17466` 暂停 4.5 秒，3.001 秒时记录 `connection cancelled`，随后 attempt `2/3` 连接成功；旧 PID 消失，只保留新的 UID 0 服务，零崩溃。
- 同一脚本连续暂停 PID `22856`、`23210`，在第二次重连进行中通过应用开关关闭 Shizuku；恢复进程后没有第三次尝试、没有残留 Root UserService，重新开启后 UID 0 服务恢复，零崩溃。
- 哔哩哔哩高频场景导出 740 条记录、200 个关联 ID；31 个事件型 ID 中包含 25 个哔哩哔哩 ID，`EventReceived` 孤立 ID 为 0；1 个合并事件 ID 同样拥有后续记录。

上游冲突策略：

- 如果上游重构日志系统，适配上游 logger，不保留平行日志框架。
- `DecisionReason` 必须保持本 fork 稳定，避免更新后诊断含义漂移。
- 诊断不可改变规则时序。
- 上游若改变 `A11yRuleEngine.startQueryJob/queryAction/consumeEvent` 的控制流，必须按新控制流重新放置旁路记录，禁止为了保留诊断而恢复旧查询算法。
- 阶段 2 接入真实手势完成/取消/验证结果时，复用已预留的 `ActionCancelled` 和 `ActionVerificationFailed`，不要另建含义重复的自由文本。

### 9.2 阶段 2：动作结果修复

状态：`CURRENT`（工作区，阶段 2 自动测试和真机验收完成）。

新增：

```text
app/src/main/kotlin/li/songe/gkd/shizuku/InputSequenceResult.kt
app/src/test/kotlin/li/songe/gkd/shizuku/InputSequenceResultTest.kt
app/src/test/kotlin/li/songe/gkd/shizuku/SafeInvokeShizukuTest.kt
```

修改：

- `GkdAction.kt`
  - 新增 `ActionResultState`：`Accepted/Completed/Verified/Cancelled/Rejected/TimedOut`。
  - `clickCenter`、`longClickCenter`、`swipe` 不再用 `dispatchGesture(...) != null` 判断成功；Root 成功标记 `Completed`，回退手势等待系统回调。
  - `Verified` 只预留给后续界面复核，禁止由 API 接受或手势完成自动推断。
- `A11yService.kt`
  - 新增 `dispatchGestureAwait()` 和 `GestureDispatchResult`，区分完成、取消、提交拒绝和有界超时。
- `InputManager.kt`、`ShizukuApi.kt`
  - `compatInjectInputEvent/tap/swipe/key` 逐级返回隐藏 API 的真实 Boolean；Shizuku 关闭、Binder 尚未收到或远端 Binder 异常时返回 `false`，删除 `Unit != null` 假成功。
- `InputShellCommand.kt`、`InputSequenceResult.kt`
  - tap、swipe、key 的 DOWN/MOVE/UP 逐步聚合；DOWN 失败立即停止，MOVE 失败后仍发送 UP 收尾但保持失败，UP 失败也返回失败；MotionEvent 注入后及时 recycle。
- `A11yRuleEngine.kt`
  - 动作诊断详情增加 `state`；取消/超时使用 `ActionCancelled`，其余失败使用 `ActionRejected`。
  - 返回键输入不再把非 null Boolean 当成功；只有 `true` 才停止回退。
- `AdvancedPage.kt`
  - 诊断文件通过 `Dispatchers.IO` 写入，写入异常由 `launchTry` 记录并提示，不再阻塞或直接冲击 Compose 点击回调。

必须保留：

- 现有动作字符串和默认动作选择。
- 只有真实成功才调用 `rule.trigger()`。
- 失败动作不消耗次数、不进入冷却。
- `ActionResult.result` 保持现有字段，订阅 JSON、动作字符串、默认动作和 HTTP 检查协议只增加向后兼容的 `state` 输出。

上游迁移方法：

1. 如果上游已为 `dispatchGesture` 增加 suspend 等待器，优先采用上游生命周期实现，但必须保留 completed/cancelled/rejected/timeout 四种结果。
2. 重新检查 `IInputManager.injectInputEvent*` 的返回类型；不得把 nullable wrapper 或 `Unit` 当成功。
3. 若上游重写输入序列，逐项确认 DOWN/MOVE/UP 失败是粘性的，并在中途 MOVE 失败后仍安全发送 UP。
4. `rule.trigger()` 必须继续位于可信成功分支内；合并冲突时禁止移动到动作提交之后。

当前验收：

- App Debug 单元测试新增 3 项输入序列测试，覆盖全成功、DOWN 失败及 MOVE 失败后成功 UP 不得覆盖失败；最终 App 测试 16/16、Selector JVM 测试 18/18、Release 构建和 lintVital 均通过。
- Xiaomi Android 16 / KernelSU + Sui：Root `clickCenter` 与 350ms `swipe` 均返回 `result=true, shell=true, state=Completed`。
- 非清数据覆盖安装后设置和 3 个订阅文件哈希保持不变。
- 最终 APK SHA-256 `7841DEA509997085D350C867D38591AFF3220EA66FD31B73EB330A24AA3E5CE8`，3,320,183 字节；最终覆盖安装后 Root UserService UID `0`，零新增 GKD `AndroidRuntime`。
- 临时关闭 Shizuku 输入并切换到系统无障碍服务后，两个重叠手势得到首个 `Cancelled`、第二个 `Completed`，证明 `GestureResultCallback` 分支来自系统真实回调。
- 临时内存规则 `-1/app/9902/0` 的关联 ID `889` 记录 `ActionSubmitted → ActionCancelled(state=Cancelled)`；动作计数前后不变，随后仍为 `RuleEligible`，没有消耗 `actionMaximum` 或 `actionCd`。
- 测试后内存订阅自动删除，动作计数扣回测试成功对照值，Root/自动化配置、系统无障碍设置和四个配置/订阅哈希全部恢复。
- 隐藏输入 API 的逐事件 false 聚合由 JVM 测试覆盖；真机没有修改 `/system/bin/input`，也没有为生产 APK增加故障注入后门。

上游合并时搜索：

```powershell
rg -n "dispatchGesture|performAction\(|injectInputEvent|rule\.trigger\(" app/src/main/kotlin
```

### 9.2.1 米游社第三方规则选择器兼容层

状态：`WORKTREE`（2026-07-15 原神/绝区零与星穹铁道两类旧选择器均已建立兼容；星铁未签到页已完成真机端到端验收）。

新增：

```text
app/src/main/kotlin/li/songe/gkd/data/RuleSelectorCompat.kt
app/src/test/kotlin/li/songe/gkd/data/RuleSelectorCompatTest.kt
```

修改：

- `ResolvedRule.kt`
  - 构造 `anyMatches` 前调用 `RuleSelectorCompat.resolveAnyMatchSources(g.appId, sources)`。
  - 通过 `resolveActionCd` 仅为星铁旧选择器设置 5 秒最短冷却；若订阅配置更长值则不覆盖。
  - `matches`、排除选择器、动作、次数、延迟、前置规则和订阅数据模型均不变。
- `RuleSelectorCompat.kt`
  - 仅识别包名 `com.mihoyo.hyperion`。
  - `childCount=11` 分支仍由同一 WebView 内的 `[text$="每日签到"]` 标题锚定，并保留奖励图标结构约束。
  - `childCount=10` 星铁分支不能依赖文本：真机快照可导出标题文本，但活动自动化查询中的文本条件全部失效。该分支使用 `View[childCount=6] + View[childCount=3] + TextView` 的奖励前缀结构定位第一个未签到日。
  - 若上游已经加入兜底则返回原列表，避免重复。
  - 若上游删除旧选择器则返回原列表，兼容逻辑自动失活。
- `RuleSelectorCompatTest.kt`
  - 12 项测试覆盖两类旧选择器的精确追加、其他 App 不修改、上游删除旧规则后自动停用、上游已加入兜底时不重复、正负节点树语义，以及 5 秒最短冷却和更长上游值保留。

为什么不修改订阅文件：

- `667.json` 属于远程订阅，直接本地改写会在更新时被覆盖，并使备份、哈希和订阅来源语义漂移。
- 本兼容层不绑定订阅 ID、版本或组 key；规则库升级后仍由上游内容决定是否触发。
- 不新增订阅字段，不修改 Room、备份、导入导出和在线更新流程。

安全边界：

- 兜底只去掉已失效的 WebView 自身聚合文本判断，改由同一 WebView 内可见的 `text$="每日签到"` 标题提供业务语义；同时保留米游社包名、原规则所属 Activity 约束、四层路径、`childCount=11`、可见三子节点容器及 `Image[index=0][text!=null]`。
- 现场已签到快照的对应图标为 `index=2`，未签到快照为 `index=0`，因此双快照静态验证不会在已签到页误点。
- `.MiHoYoWebActivity` 同时承载参量质变仪等普通网页；缩减节点树负样本确认即使结构和 `Image[index=0]` 相同，只要缺少“每日签到”标题就不会命中。
- 星铁现场已验证 `SelectorMatched → clickCenter(shell=true, Completed)`，累计签到从 13 天变为 14 天，并关闭签到成功弹窗；这部分可以登记为端到端成功。

上游迁移/删除方法：

1. 更新官方代码和远程订阅后，先搜索 `MIHOYO_SIGN_IN_STALE_SELECTOR` 对应原规则是否仍存在。
2. 若远程订阅已删除旧选择器或提供等价稳定选择器，删除 `RuleSelectorCompat` 中米游社条目及对应测试；`ResolvedRule` 若无其他兼容项则恢复直接解析。
3. 若上游重构 `ResolvedRule`，把“解析前、仅对 anyMatches 源字符串追加”的意图迁到新的规则解析边界，不得修改保存的 RawSubscription。
4. 若发现误触，优先撤销此兼容条目；不要通过增加订阅版本硬编码或修改用户订阅来掩盖。

#### 9.2.1.1 规则汇总晚于自动化连接时的补查

状态：`WORKTREE`，已由同一星铁静止页面完成真机验收。

- `A11yState.kt`：新增 `ActivityScene.RuleSummary`，使同一前台 Activity 在规则汇总变化时重建 `ActivityRule`，但不伪造屏幕点亮或 App 切换。
- `A11yFeat.kt`：`initRuleSummaryRefresh()` 监听 `ruleSummaryFlow.drop(1)`；仅当汇总对象确实变化时，在 `topActivityFlow` 锁内刷新当前规则，然后通知引擎补查。
- `A11yRuleEngine.kt`：`onRuleSummaryChanged()` 使用既有 `startQueryJob(byForced=true)`，继续服从服务状态、自动匹配开关、forcedTime、规则状态、前台校验和查询互斥。
- 故障证据：旧版启动日志先显示米游社前台，但 `activityRuleFlow` 只有 4 条全局规则；应用规则稍后进入 `ruleSummaryFlow` 后没有查询。修复版在应用列表更新后记录 `scene=RuleSummary`，立即加载 `667/app/8` 并完成签到。
- 上游迁移：若上游已在规则汇总变化时原子刷新当前 Activity 并补查，删除本监听和 `ActivityScene.RuleSummary`；不要保留两套 collector，以免重复 forced query。

最终 APK、测试和真机结果见本节末尾与第 18 节；未提交工作区必须使用带 `-dirty` 的版本名，不再沿用无法区分内容的纯提交后缀。

### 9.2.2 脏工作区 APK 可追溯版本

状态：`WORKTREE`。

修改：`app/build.gradle.kts` 的 `GitInfo` 新增 `isDirty`，由 `git status --porcelain --untracked-files=normal` 计算。版本后缀规则为：

| Git 状态 | 版本后缀 |
| --- | --- |
| 未打 tag、工作区干净 | `-<shortCommit>` |
| 未打 tag、工作区有修改 | `-<shortCommit>-dirty` |
| 精确 tag、工作区干净 | 无额外后缀 |
| 精确 tag、工作区有修改 | `-dirty` |

该差异只改变构建产物标识，不修改 `versionCode`、订阅格式、数据库或签名。正式提交后工作区恢复干净，APK 自动回到新提交短哈希；上游若已有等价 dirty 标识，采用上游实现并删除本差异。当前 APK manifest 已验证为 `versionCode=92`、`versionName=1.12.1-f2e60bd-dirty`。

### 9.3 阶段 3：查询唤醒状态机

状态：`WORKTREE`，已于 2026-07-15 完成代码、单元测试和真机事件风暴冒烟。

新增文件：

- `app/src/main/kotlin/li/songe/gkd/a11y/QueryWakeState.kt`
- `app/src/test/kotlin/li/songe/gkd/a11y/QueryWakeStateTest.kt`

修改文件：

- `A11yRuleEngine.kt`：用单 runner 加一个有界 pending 请求替代 `querying` 早退；查询结束时直接 handoff 给 pending 请求，不产生可丢事件的空闲间隙。
- `DecisionReason.kt`：新增 `QueryDeferred`，明确表示请求已合并等待，不再把这类情况记录为 `QueryAlreadyRunning` 丢弃。

`QueryWakeState` 的合并语义为：最新真实事件优先；普通唤醒高于 forced/delayed；delayed 高于 forced；同类请求保留最新一项。状态只保存当前所有权和一个 pending 请求，因此事件风暴不会建立无界队列。

`QueryEventBuffer` 进一步限制节点事件内存：连续同类事件最多保留最后两个，供既有增量节点查询使用；出现混合事件时只记录 `hadEvents=true, events=null`，让下一轮重新读取当前 root，不保留整批历史事件。

必须持续成立的不变量：

- 同时最多一轮主查询。
- 查询期间到达的新请求至少留下一个 pending 信号。
- 当前查询结束后在仍持有 runner 所有权时处理最新状态。
- 重复事件可以合并，但最后状态不能丢，缓冲空间必须保持常量级。
- 空闲时不轮询；forcedTime、delayRule 和既有 `interruptKey` 中断语义保持有效。

新增 10 项单元测试覆盖并发抢占、1000 次请求风暴、10000 次事件风暴、handoff、forced/normal/delay 优先级及混合事件降级；全量 App 测试为 39/39。真机在米游社已签到页连续执行 12 次交替滑动，前台、主进程、Root UserService 和动作计数均稳定，无误触或崩溃。

这是最高冲突风险文件。官方若改动 `A11yRuleEngine`，必须以新上游算法为底：若上游已有等价 pending/handoff 实现则采用上游并删除重复状态机；否则移植上述不变量和测试，禁止直接选 ours 或同时保留两套查询所有权状态。

### 9.4 阶段 4：焦点融合

状态：`WORKTREE`，第一批任务/窗口采样模型已于 2026-07-15 完成；尚未接管规则上下文。

已新增：

```text
runtime/foreground/ForegroundTask.kt
runtime/foreground/ForegroundSnapshot.kt
a11y/ForegroundSnapshotProvider.kt
```

已修改：

- `TaskInfoHidden.java`：映射 userId/taskId/effectiveUid/displayId/isFocused/isVisible/isRunning。
- `ShizukuApi.kt`：由 `getTasks(1).firstOrNull()` 改为查询 16 条候选，优先选择目标显示屏上 focused、visible、running 的任务；Android 9 保留旧顺序回退，Android 10/11 因系统尚无 focused/visible 字段而显式使用第一条任务作为兼容焦点，Android 12～15 不读取 Android 16 才加入的 effectiveUid。
- `TaskStackListener.kt`：taskId 回调使用选择后的任务，不再再次假设第一条正确。

`ForegroundSnapshotProvider` 从 focused Accessibility Window 采集窗口 ID、类型、层级、displayId、active 状态和 root 包名；同屏没有 focused 窗口时才回退 active 窗口。`ForegroundSnapshot` 在 Task 与 focused root 包一致时为 `Confirmed`，包冲突时为 `Conflict` 且 `canExecute=false`，只有单侧证据或 active 窗口时为 `Probable`。

新增 14 项测试覆盖任务优先级、跨显示屏过滤、窗口焦点/层级和快照置信度。全量 App 53/53、Selector 18/18、Release 与 Vital Lint 通过；Android 16 覆盖安装并在 GKD/米游社之间触发 TaskStack，未出现隐藏字段错误，Root UserService 仍为 UID 0。

下一批预计修改：

- 建立输入法、SystemUI、权限控制器、画中画和普通覆盖层分类。
- 对冲突快照做 50～300ms 有限确认，然后才接入 `A11yState.kt` 和规则动作门控。
- 结构化诊断附带 userId/displayId/taskId/windowId/置信度。

合并时必须区分：

- 包级规则使用焦点窗口 root 包名。
- Activity 级规则使用 focused Task topActivity。
- 两者冲突时暂缓或按明确的覆盖层策略处理。

上游迁移时若官方已有多任务或焦点窗口解析器，应复用其数据源，但必须保留“目标显示屏过滤、冲突默认禁止动作、旧 Android 明确回退、字段按平台版本门控”四个不变量；不要同时保留官方选择器和本 fork 的 `getForegroundTask()` 两套前台所有权。

### 9.5 阶段 5：窗口与节点 generation

预计新增或修改：

- 窗口 generation/transition 标识。
- 有限退避重试。
- `A11yContext` 缓存失效策略。
- 动作前节点和窗口二次验证。

合并时不能仅比较类名；必须核对上游是否已经改变节点过期、缓存或 `refresh()` 行为。

### 9.6 阶段 6：动作执行器

预计新增：

```text
runtime/action/ActionExecutor.kt
runtime/action/ActionRequest.kt
runtime/action/ActionOutcome.kt
runtime/action/A11yActionExecutor.kt
runtime/action/RootActionExecutor.kt
```

预计迁移：

- `GkdAction` 保留规则数据定义。
- 具体 `ActionPerformer` 逻辑逐步委托给执行器。
- displayId、window generation、验证策略进入结构化请求。

官方新增动作时，应先加入公共动作语义，再分别实现 A11y 和 Root 后端，不能只实现 Root 版本。

### 9.7 阶段 7：APK 内置 RootService

预计新增：

```text
root/ipc/*.aidl
root/service/GkdRootService.kt
root/system/RootActivityTaskManager.kt
root/system/RootInputManager.kt
root/system/RootWindowManager.kt
runtime/privilege/PrivilegedBridge.kt
runtime/privilege/RootPrivilegedBridge.kt
```

预计修改：

- `app/build.gradle.kts`：增加 root IPC 依赖和构建配置。
- `AndroidManifest.xml`：仅添加 RootService 所需声明；不得导出任意特权 Service。
- App 初始化和设置页：连接、授权、状态、回退。
- ProGuard/R8：保留 AIDL、root service 和反射/隐藏 API 必需符号。

安全不变量：

- 不提供任意 shell 字符串执行 AIDL。
- 输入、包管理和 AppOps 请求使用结构化参数及白名单。
- 校验 Binder 调用方。
- root 服务不下载、不解析、不保存订阅。
- root 服务不直接操作 Room 数据库。
- root 被拒绝或死亡时普通 App 不崩溃。

### 9.8 阶段 8：多用户和多显示屏

预计影响：

- `ForegroundSnapshot` 增加完整用户和 display 上下文。
- 包查询、任务查询、窗口和输入使用一致 userId/displayId。
- App 信息缓存不再仅按包名代表运行时实例。

第一轮不改变订阅格式中的 App ID。若未来需要按用户配置规则，必须单独设计兼容层，不能直接把订阅包名改成复合键。

## 10. 官方更新后的标准整合流程

### 10.1 前置检查

必须在仓库根目录执行：

```powershell
git status --short
git remote -v
git branch --show-current
git rev-parse HEAD
```

要求：

- 当前开发阶段已经完成或明确暂停。
- 工作区无不明修改。
- `docs/upstream-code-delta-guide.md` 已更新到当前代码。
- 用户配置和真机测试数据已备份。

不要用 `git reset --hard` 清理工作区。

### 10.2 配置官方 remote

仓库当前没有持久 `upstream` remote。首次正式整合时：

```powershell
git remote add upstream https://github.com/gkd-kit/gkd.git
git fetch upstream --tags
```

如果已经存在：

```powershell
git remote get-url upstream
git fetch upstream --tags
```

确认 URL 必须指向官方 `gkd-kit/gkd`，不能把 fork 的 `origin` 当成官方。

### 10.3 建立整合分支

不要直接在发布中的 `main` 上试冲突：

```powershell
$stamp = Get-Date -Format yyyyMMdd
git switch -c "integrate/upstream-$stamp"
```

记录三方基线：

```powershell
$fork = git rev-parse HEAD
$upstream = git rev-parse upstream/main
$base = git merge-base HEAD upstream/main

"BASE=$base"
"FORK=$fork"
"UPSTREAM=$upstream"
git rev-list --left-right --count upstream/main...HEAD
```

把这三个提交写入本文档新一轮整合记录。

### 10.4 合并前差异审计

官方新增内容：

```powershell
git log --oneline "$base..upstream/main"
git diff --name-status "$base..upstream/main"
```

fork 自己的内容：

```powershell
git log --oneline "$base..HEAD"
git diff --name-status "$base..HEAD"
```

双方共同修改的文件：

```powershell
$upFiles = git diff --name-only "$base..upstream/main"
$forkFiles = git diff --name-only "$base..HEAD"
Compare-Object $upFiles $forkFiles -IncludeEqual -ExcludeDifferent |
    ForEach-Object InputObject
```

重点检查：

```text
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/kotlin/li/songe/gkd/a11y/
app/src/main/kotlin/li/songe/gkd/data/GkdAction.kt
app/src/main/kotlin/li/songe/gkd/data/ResolvedRule.kt
app/src/main/kotlin/li/songe/gkd/shizuku/
app/src/main/kotlin/li/songe/gkd/util/SubsState.kt
app/src/main/kotlin/li/songe/gkd/data/RawSubscription.kt
hidden_api/
selector/
```

### 10.5 执行非破坏性合并

```powershell
git merge --no-commit --no-ff upstream/main
```

这样可以在提交前完成代码审查和测试。如果冲突或结果不符合预期，应逐文件解决；不要使用整仓 `ours` 或 `theirs`。

若决定放弃本次未提交合并，应先确认只是在整合分支且没有用户额外工作，再使用 Git 提供的合并中止流程。不要用硬重置代替冲突处理。

## 11. 冲突处理优先顺序

建议按以下顺序解决，前一层决定后一层如何适配：

1. Gradle、版本目录、SDK、Kotlin、AGP 和依赖。
2. `hidden_api` 声明及 Android 版本兼容。
3. 订阅模型、数据库和 selector，原则上优先保持上游。
4. `ForegroundResolver`、`WindowProvider`、`PrivilegedBridge` 接口。
5. `A11yRuleEngine` 调度状态机。
6. `GkdAction` 与 `ActionExecutor`。
7. Service 生命周期、Manifest 和初始化。
8. UI、设置、状态和诊断展示。
9. 模块打包、README、CHANGELOG 和维护文档。

原因：如果先解决 UI，后续底层接口变化会迫使重复修改；如果先盲目保留 fork 引擎，可能丢掉上游新的规则语义或 Android 兼容修复。

## 12. 单文件三方合并方法

对每个冲突热点文件，分别查看 base、官方和 fork：

```powershell
$base = git merge-base HEAD upstream/main
$path = "app/src/main/kotlin/li/songe/gkd/a11y/A11yRuleEngine.kt"

git diff "$base..upstream/main" -- $path
git diff "$base..HEAD" -- $path
git show "$base`:$path"
git show "upstream/main`:$path"
git show "HEAD`:$path"
```

判断顺序：

1. 官方是否已经实现本 fork 的行为意图。
2. 官方是否改变数据结构、线程模型或生命周期。
3. fork 差异是否仍是实际需求。
4. 能否把 fork 行为迁入官方新抽象，而不是保留旧文件结构。
5. 是否需要新增或更新测试证明行为。

典型情况：

### 情况 A：官方未改，fork 改了

保留 fork 修改，运行原验收。

### 情况 B：官方改了，fork 未改

直接接受官方修改，再运行 Root 加强版总回归。

### 情况 C：双方都改了同一函数

以官方最新函数结构为底，逐项迁移 fork 不变量。不要按行数多少决定选择哪边。

### 情况 D：官方删除或重命名文件

查找行为的新归属。迁移行为和测试，不要为了减少 diff 把旧类重新复制回来。

### 情况 E：官方实现等价能力

采用上游实现，删除 fork 重复代码，将差异状态改为 `UPSTREAMED`，记录替代提交。

## 13. 各核心域的整合红线

### 13.1 订阅和规则格式

默认优先接受上游。整合后必须保证：

- 旧订阅文件可加载。
- 订阅远程更新可用。
- 本地规则可编辑。
- 备份可恢复。
- `RawSubscription` 未被 Root IPC 版本绑架。

Root 增强代码不得要求远程规则增加 root 专用字段才能正常执行原动作。

### 13.2 规则状态机

必须保留上游对以下语义的修复：

- `preKeys`
- `matchDelay`
- `actionDelay`
- `matchTime`
- `actionCd`
- `actionMaximum`
- `resetMatch`
- 优先规则

本 fork 的调度重构只能改变“何时可靠地再次查询”，不能悄悄改变规则状态含义。

### 13.3 窗口和前台判断

整合后必须重新验证：

- focused Task 选择；
- focused Accessibility Window 选择；
- 输入法和 SystemUI 覆盖；
- Activity 与窗口包冲突；
- userId 和 displayId；
- 窗口切换稳定等待。

官方若新增更可靠的窗口 API，应优先纳入融合器，而不是无条件继续使用旧 Shizuku wrapper。

### 13.4 动作执行

动作成功必须是显式结果，禁止以下模式重新出现：

```kotlin
dispatchGesture(...) != null
inputManager.tap(...) != null // 当 tap 实际返回 Unit 时
```

整合后搜索：

```powershell
rg -n "dispatchGesture.*!= null|\.tap\(.*\) != null|injectInputEvent" app/src/main/kotlin
```

### 13.5 Root IPC

任何上游合并后都要重新检查：

- RootService 是否仍能从当前 APK 路径加载代码。
- AIDL parcel 类型是否兼容。
- R8 是否移除必要类。
- 包名或签名变化是否破坏调用方校验。
- Manifest 是否意外导出特权组件。
- 是否出现任意 shell 字符串接口。

### 13.6 资源生命周期

必须保留或由上游等价替代：

- HTTP server 显式关闭；
- Bitmap 单一所有者回收；
- Image 每帧关闭；
- ImageReader 和 VirtualDisplay 释放；
- 协程取消释放 capture；
- MediaProjection 停止和引用清理。

## 14. 构建和测试顺序

### 14.1 静态检查

```powershell
git diff --check
rg -n "<<<<<<<|=======|>>>>>>>" . -g '!**/build/**' -g '!**/.gradle/**'
```

检查新增文件、忽略状态和意外产物：

```powershell
git status --short
git status --ignored --short
git ls-files -o --exclude-standard
```

### 14.2 模块测试

按当前工程能力执行：

```powershell
.\gradlew.bat selector:jvmTest
.\gradlew.bat app:testGkdDebugUnitTest
.\gradlew.bat app:assembleGkdDebug
.\gradlew.bat app:assembleGkdRelease
```

若任务名称随上游变化，先用 Gradle tasks 核对，不要在文档和脚本里继续硬编码失效名称。

### 14.3 订阅兼容测试

- 使用阶段 0 保存的真实备份恢复。
- 更新至少一个远程订阅。
- 导入 JSON/JSON5 本地订阅。
- 启用、禁用和删除订阅。
- 验证规则数量、分类配置和排除配置。
- 验证动作日志关联的订阅 ID、版本、组 key 和规则 index。

### 14.4 窗口和动作真机测试

- 普通 Activity。
- Dialog、Popup 和系统权限弹窗。
- 输入法显示和隐藏。
- 通知栏和控制中心。
- 分屏、小窗和画中画。
- 旋转、锁屏和解锁。
- user 0、工作资料和小米双开/user 999。
- root 允许、拒绝、撤销和 root 进程死亡。
- Shizuku 回退和纯无障碍回退。

### 14.5 包装模块测试

仅在模块仍维护时执行：

```powershell
.\ksu-sukisu-module\scripts\package-ksu-module.ps1 -SkipBuild
```

必须重新打开 ZIP 检查实际内容，不只检查工作树：

```text
module.prop
customize.sh
service.sh
action.sh
config.conf
uninstall.sh
skip_mount
common/gkd.apk
```

## 15. 每个编码阶段必须附带的文档步骤

`docs/root-runtime-refactor-plan.md` 的每个阶段都必须把更新本文档作为完成条件。

每次阶段提交前：

- [ ] 更新状态：`PLANNED` -> `CURRENT`，或记录 `BLOCKED/DROPPED`。
- [ ] 写实际提交 ID。
- [ ] 列出新增、修改、删除和重命名文件。
- [ ] 写到类、接口、函数或字段级别。
- [ ] 说明行为变化和保持不变的行为。
- [ ] 说明订阅、数据库、备份和规则语义是否变化。
- [ ] 写上游合并策略：保留、迁移、优先上游或可删除。
- [ ] 写最低测试和已执行结果。
- [ ] 更新热点文件表和冲突风险。
- [ ] 如目录结构变化，更新 README 项目目录。

如果代码已提交而本文档没有更新，该阶段不能标记为“已完成”。

## 16. 单项差异记录模板

以后新增差异使用以下模板：

```markdown
### DELTA-XXX：简短标题

- 状态：CURRENT / WORKTREE / PLANNED / UPSTREAMED / DROPPED / BLOCKED
- 引入提交：<commit>
- 对应计划阶段：<phase>
- 官方基线：<upstream commit>
- 风险：低 / 中 / 高

#### 文件与符号

- `path/File.kt`
  - `ClassName.methodName`

#### 原版行为

...

#### 加强版行为

...

#### 为什么需要

...

#### 必须保持的不变量

- ...

#### 官方更新后的迁移方法

1. ...

#### 验收

- 命令：...
- 真机场景：...
- 结果：...
```

## 17. 上游整合记录模板

每次正式整合追加一节：

```markdown
### INTEGRATION-YYYYMMDD

- 整合分支：integrate/upstream-YYYYMMDD
- Base：<commit>
- Fork before：<commit>
- Upstream：<commit>
- Result：<commit>
- 官方新增提交数：<n>
- Fork 提交数：<n>

#### 共同修改文件

- ...

#### 冲突和决策

- 文件：...
- 官方变化：...
- Fork 变化：...
- 最终选择：...
- 原因：...

#### 被官方吸收的差异

- DELTA-XXX -> UPSTREAMED

#### 重新迁移的差异

- DELTA-XXX -> 新符号/新文件

#### 验证

- 构建：...
- 单元测试：...
- 真机：...
- 订阅兼容：...
- 已知问题：...
```

## 18. 当前工作区待办

截至 2026-07-15：

- 当前 HEAD 为 `f2e60bd0`；已提交阶段 0.5、1、2，米游社选择器兼容代码、测试和本轮文档仍在工作区，尚未提交。
- 阶段 0.5 Root 桥真实性与自恢复已完成；阶段 2 动作结果误判修复已完成。
- 阶段 1 已由米游社有效 B01 现场完成闭环：连续查询稳定终止于 `SelectorMiss`，窄范围兼容层随后完成真实未签到点击、弹窗关闭和累计签到天数变化验收。
- 当前 Release 为 `1.12.1-f2e60bd-dirty`，SHA-256 `8812A0E6AC2C977B2081548FCDB054AF23658FDFD9407D6C364B2BCA3E69A546`，3,320,191 字节；App 53/53、Selector 18/18、Release 和 Vital Lint 全部通过。APK 已非清数据覆盖安装：主进程 PID `1809`，Root UserService PID `1872`、UID 0，StatusService 为前台；GKD/米游社 TaskStack 往返后进程稳定，无隐藏字段错误或新增 GKD `AndroidRuntime`。
- 真机已恢复 Root/自动化原配置，Root UserService UID 0，临时 HTTP 服务和快照已清除。重启后的 KernelSU `ksu` SELinux 域不能直接读取 App 私有目录，因此本轮未重复宣称配置/订阅文件哈希验收，改用 App 首页实际加载结果作为非清数据证据。
- 阶段 3 已完成；阶段 4 第一批多任务选择、焦点窗口/root 包采集和统一置信度模型已完成，下一步实现覆盖层策略、冲突延迟确认和规则上下文接入。阶段 0 的 B02、规则重复统计和剩余窗口场景继续并行采样。

最新整体状态以 [`current-progress.md`](current-progress.md) 为入口；本文仍负责记录每项代码级差异和未来上游迁移方法。
