package li.songe.gkd.runtime.diagnostics

enum class DecisionStage(val label: String) {
    Event("事件"),
    Foreground("前台"),
    Query("查询"),
    Window("窗口"),
    Rule("规则"),
    Selector("选择器"),
    Action("动作"),
}

enum class DecisionOutcome(val label: String) {
    Observed("已观察"),
    Skipped("未执行"),
    Submitted("已提交"),
    Succeeded("成功"),
    Failed("失败"),
}

/**
 * 稳定的规则决策原因。枚举名会出现在导出文本中，上游合并时不可随意改名。
 */
enum class DecisionReason(val label: String) {
    EventReceived("收到无障碍事件"),
    ServiceDisconnected("自动化服务未连接"),
    ServiceStarting("自动化服务仍在启动"),
    AutoMatchDisabled("自动匹配已关闭"),
    ForegroundConfirmed("前台应用已确认"),
    ForegroundUnconfirmed("无法确认前台应用"),
    WindowRootAvailable("活动窗口根节点可用"),
    WindowRootUnavailable("活动窗口根节点为空"),
    PackageActivityMismatch("包名或 Activity 不匹配"),
    NoApplicableRules("当前界面没有适用规则"),
    MatchingPaused("当前规则均不可继续匹配"),
    ActionMaximumReached("规则达到执行次数上限"),
    PrerequisiteUnsatisfied("前置规则未满足"),
    MatchDelayActive("处于匹配延迟"),
    ActionDelayActive("处于动作延迟"),
    CooldownActive("处于动作冷却"),
    MatchTimeout("超出匹配时间"),
    SelectorMiss("选择器未命中"),
    ActionRejected("动作返回失败"),
    ActionCancelled("动作被取消"),
    ActionVerificationFailed("动作结果验证失败"),
    QueryAlreadyRunning("已有查询正在执行"),
    EventQueueEmpty("事件队列已被消费"),
    StaleContext("前台或事件上下文已变化"),
    ForcedRuleSkipped("规则不在强制查询窗口"),
    QueryStarted("开始规则查询"),
    RuleEligible("规则状态允许查询"),
    SelectorMatched("选择器已命中"),
    ActionSubmitted("动作已提交"),
    ActionSucceeded("动作返回成功"),
}
