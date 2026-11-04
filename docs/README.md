# 文档索引

同步日期：2026-11-04。当前正式版本为 **GKD Global Bridge 2.0.0 / 100**，模块2.0.0 / 2026110402。阅读现行说明，再按版本查看历史；后续纯文档提交不移动`v2.0.0`标签或替换其附件。

## 当前维护增量

[2.0.1 / 101 图标比例修正](icon-sizing-2.0.1.md)为本地验证版；公开Release仍是2.0.0。不会因修图标而修改2.0.0标签或附件。

## 当前使用与维护

| 文档 | 作用 |
| --- | --- |
| [仓库README](../README.md) | 下载、安装、功能、产物与边界 |
| [模块README](../ksu-sukisu-module/README.md) | Root/Zygisk前置、首装、配置、打包、卸载与回退 |
| [当前进度](current-progress.md) | 约定任务完成状态、已知覆盖范围与后续入口 |
| [现行架构契约](global-bridge-runtime.md) | 单引擎、身份/名单/租约、首次Root、动作真实性与维护验收 |
| [源码差异指南](upstream-code-delta-guide.md) | 2.0 CURRENT增量优先；下方旧快照仅历史 |
| [2.0验证矩阵](testing/global200-release.md) | v100直接验证、v99沿用证据、哈希及未覆盖项 |
| [2.0发布说明](releases/v2.0.0.md) | 正式发布结果与用户变化 |
| [更新记录](../CHANGELOG.md) | 最新变更在前，旧行为按日期理解 |

## 历史设计与验证

| 文档 | 版本/用途及现在的含义 |
| --- | --- |
| [单APK Root原始计划](root-runtime-refactor-plan.md) | 保留原阶段目标；禁止Hook的范围后来扩展，不是现行全局架构限制 |
| [阶段0测试底稿](testing/root-runtime-baseline.md) | Android16等早期样本，不等同2.0测试全部完成 |
| [97默认规则与首装](defaults97.md) | 旧LSP批量作用域已非必要；规则身份/保留原则继续有效 |
| [98真机续测](global98-device-followup-20261104.md) | Global试验、白名单、恢复和重启历史；未做样本不倒填 |
| [99首装收口](global99-fresh-bootstrap-20261104.md) | 真实首装、等待授权、窗口恢复与还原过程；后续真实广告/公告见2.0矩阵 |

## 历史发布说明（保持当时产物与验证身份）

- [2026-11-02 节点桥接](releases/20261102-nodebridge.md)
- [2026-11-02 SukiSU配套](releases/20261102-sukisu-nodebridge.md)
- [2026-11-02 XSpace](releases/20261102-xspace-nodebridge.md)
- [2026-11-02 批量作用域](releases/20261102-batch-scope.md)
- [2026-11-03 可可影视修复](releases/20261103-keke-bridge.md)
- [2026-11-03 审查94](releases/20261103-audit94.md)
- [2026-11-03 图标与审查96](releases/20261103-review96.md)

历史发布中“公告待验证”“模块卸载APK”“新应用需批准作用域”等描述只对应当时版本，不覆盖2.0说明。历史原始失败/缺样本保留，不把后续验证伪装成当时已通过。

## 证据与文档维护

当前Release已含公开验证摘要与源码差异；仓库文档描述行为与覆盖边界，不上传私人应用数据、原始日志或设备特定回退。源码/文档改动提交后核验相对链接、diff格式、版本与成品哈希，确认没有误改发布二进制或移动标签，再推送。
