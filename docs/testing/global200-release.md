# 2.0 发布验证矩阵

记录日期：2026-11-04。源码标签`v2.0.0` → `94998d38c7f745489ef267a4725544b038cc83cf`。[Release](https://github.com/STC214/GKD_/releases/tag/v2.0.0)／[文档索引](../README.md)。文档提交不移动标签或重建附件。

## v100直接验证

| 项目 | 输入/操作 | 字面结果与限制 |
| --- | --- | --- |
| 构建 | `:app:testGkdDebugUnitTest :app:lintGkdRelease :selector:jvmTest :app:assembleGkdRelease --offline --max-workers=2 --console=plain` | exit0；BUILD SUCCESSFUL；255tests、0failures/errors/skips；Lint0errors、58warnings |
| 主机生命周期 | `python ksu-sukisu-module/tests/test_global_bridge_lifecycle.py --java-home JAVA_HOME --repeat 30`（JAVA_HOME替换实际JDK目录） | 6场景×31轮通过；JVM Android桩，不代替系统私有API实机 |
| 安装器查找 | `python ksu-sukisu-module/tests/test_bootstrap_lookup.py` | 7场景通过，含缩进、元数据、缺失/错误包与暂不可见 |
| 包与签名 | 读取签名、ZIP CRC、精确12条目、内嵌APK与DEX/SO逐字节比较；Android `sh -n` | 签名等于99；脚本解析exit0；独立APK=内嵌APK；Native与99相同 |
| 真机安装/回退 | 99→100→99→100；不卸载、不清数据 | 每次安装Success；Existing configuration retained；UID10404不变；配置/白名单/规则/HMA哈希一致 |
| 两用户点击 | 初次100、回退99、最终100各冷启测试包user0/999，无手动跳过 | 6个组合均有目标click-handler-observed及引擎ActionSucceeded/state=Verified；临时999登记清理，无第二套GKD |
| 源码补丁 | 对上一公开96应用release.diff、比对、反向恢复 | 50路径通过；文本EOL归一化比较；不是修改真实工作区回退 |
| 推送发布 | main与v2.0.0同源码SHA；正式Release上传5附件后下载 | draft=false、prerelease=false、latest=true；5附件SHA256相等 |

所有实机数据来自Android17 ARM64。主机生命周期和查找检查是离线测试，不计入255项JVM总数。没有把Lint警告说成全消除。

## 沿用的v99功能证据（非v100重跑）

| 项目 | 已验证内容 | 证据边界 |
| --- | --- | --- |
| 首装与Root延迟 | 真实卸载重装复现安装器问题；修复后初始化，延迟授权同PID就绪；纯净默认无Shizuku路线节点点击 | 最后窗口修正是纯净配置上的覆盖更新，不是最后构建再次卸载首装 |
| 白名单即时生效 | 加入→移除→加入→移除，user0/999共8例；阻断节点0且无点击、移除恢复Verified | PID不变；Activity可重建，不声称同一实例 |
| 进程恢复 | SIGTERM后约2.17秒出现新引擎，无采样脚本启动命令 | 不等于force-stop；不单独归因模块守护 |
| 最终99重启 | 用户仅解锁，不打开GKD；引擎/Root/原配置Shizuku自动就绪；owner点击52ms、正式分身338节点 | 临时999登记未跨ROM重启保留，不将其计成功 |
| 真实开屏 | 可可影视owner及临时999均clickNode/Verified | 有些尝试没有广告，未算失败/成功；临时分身初始化仅手动选择服务器 |
| 自然公告 | 不改缓存，等3小时间隔自然触发“朕知道了”；本地-2规则2026110301；Verified/WindowChanged 56ms | 没有手动确认、没有伪造公告，没有将加载规则等同点击成功 |

2.0 Native DEX/两个SO与99字节相同，2.0只新增版本/分发相关变化并重新构建APK。没有为2.0额外重启、等待真实开屏/公告；此项明确是已验证功能基线，不冒充新包全场景实测。

## 发布成品及完整性

本机目录`dist/v2.0.0/`（Git忽略）；公网以[Release附件](https://github.com/STC214/GKD_/releases/tag/v2.0.0)为准。

| 文件 | SHA-256 |
| --- | --- |
| gkd-2.0.0.apk | `fb02307e96a36f5fd9cd45e374c6bf7021b0e79803d7a4c81b6e1e99516c7544` |
| gkd-ksu-2.0.0.zip | `fc8e741cc955f12d006ce4bc23348f7fbe566e5f6e945a7c24b2a9b17b05ed82` |
| release.diff | `0cfcde517321fd9285602f26ed86ce77927c69aaef8bf29384a80719d2474041` |
| verification-summary.md | `c7a99c572afcd5e89ed450057baec6782821275e5d32c0910b6a74df089e0747` |
| SHA256SUMS.txt | `250bdb7a6810a4bbbd2551e9a0c7ce4272be192f3a99d43279ea01c5969e0af4` |

完整私人命令/输入/字面输出/退出状态与执行过的回退器保留于`local-assets/release-20261104-v100/`的`verification.json`、`device-commands.json`、各测试phase和`rollback.py`。首装、名单与自然广告的原始证据在对应global-20261104及global99目录；私人备份、第三方APK、MMKV与原始设备日志不上传。不要把其他设备的rollback.py或数据包直接用于自己的设备。

## 非承诺范围

x86_64仅构建/ELF检查；32位、其他厂商/用户容器、多显示屏、无限时长压力、所有规则重复统计及Native彻底卸载后重启对照没有全面实机结果。Root宿主、节点API布局或目标绘制方式可能使节点不可用；规则未匹配不保证跳过。历史未做样本不因本次文档同步而自动标记通过。
