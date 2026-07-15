package li.songe.gkd.data

import android.view.Display
import android.view.accessibility.AccessibilityNodeInfo
import li.songe.gkd.runtime.foreground.WindowContextToken
import li.songe.gkd.util.AndroidTarget
import li.songe.gkd.util.ScreenUtils

@kotlinx.serialization.Serializable
data class ActionBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    fun contains(x: Float, y: Float): Boolean =
        x >= left && y >= top && x < right && y < bottom

    fun intersect(other: ActionBounds): ActionBounds? {
        val result = ActionBounds(
            left = maxOf(left, other.left),
            top = maxOf(top, other.top),
            right = minOf(right, other.right),
            bottom = minOf(bottom, other.bottom),
        )
        return result.takeIf { it.left < it.right && it.top < it.bottom }
    }
}

data class ActionExecutionContext(
    val displayId: Int,
    val rotation: Int,
    val windowId: Int?,
    val windowBounds: ActionBounds,
    val visibleBounds: ActionBounds,
) {
    fun allowsPoint(x: Float, y: Float): Boolean =
        windowBounds.contains(x, y) && visibleBounds.contains(x, y)

    val supportsAccessibilityGesture: Boolean
        get() = displayId == Display.DEFAULT_DISPLAY

    companion object {
        fun fromNode(
            node: AccessibilityNodeInfo,
            token: WindowContextToken? = null,
        ): ActionExecutionContext {
            val screen = ScreenUtils.getScreenSize().let {
                ActionBounds(0, 0, it.width, it.height)
            }
            val windowBounds = runCatching {
                android.graphics.Rect().also { node.window?.getBoundsInScreen(it) }
            }.getOrNull()?.takeIf { !it.isEmpty }?.let {
                ActionBounds(it.left, it.top, it.right, it.bottom)
            } ?: screen
            return ActionExecutionContext(
                displayId = token?.displayId ?: if (AndroidTarget.TIRAMISU) {
                    runCatching { node.window?.displayId }.getOrNull()
                        ?: Display.DEFAULT_DISPLAY
                } else {
                    Display.DEFAULT_DISPLAY
                },
                rotation = token?.rotation ?: 0,
                windowId = token?.windowId ?: node.windowId,
                windowBounds = windowBounds,
                visibleBounds = windowBounds.intersect(screen) ?: ActionBounds(0, 0, 0, 0),
            )
        }
    }
}

fun interface ActionExecutionGuard {
    suspend fun isCurrent(): Boolean
}

/**
 * Owns action fallback policy. A second attempt is only allowed after an API explicitly rejects
 * the first one, so an unknown side-effect action is never repeated after acceptance/completion.
 */
object ActionExecutor {
    private const val MAX_PARENT_DEPTH = 8

    suspend fun execute(
        performer: ActionPerformer,
        node: AccessibilityNodeInfo,
        locationProps: RawSubscription.LocationProps,
        context: ActionExecutionContext = ActionExecutionContext.fromNode(node),
        guard: ActionExecutionGuard = ActionExecutionGuard { true },
    ): ActionResult {
        return executeInternal(performer, node, locationProps, context, guard).copy(
            displayId = context.displayId,
            windowId = context.windowId,
            rotation = context.rotation,
            windowBounds = context.windowBounds,
            visibleBounds = context.visibleBounds,
        )
    }

    private suspend fun executeInternal(
        performer: ActionPerformer,
        node: AccessibilityNodeInfo,
        locationProps: RawSubscription.LocationProps,
        context: ActionExecutionContext,
        guard: ActionExecutionGuard,
    ): ActionResult {
        if (!guard.isCurrent()) return staleResult(performer, context)
        return when (performer) {
            ActionPerformer.ClickNode -> performNodeActionWithParent(
                performer = performer,
                node = node,
                locationProps = locationProps,
                context = context,
                guard = guard,
            )

            ActionPerformer.Click -> {
                var rejectedAttempts = 0
                if (node.isClickable) {
                    val nodeResult = performNodeActionWithParent(
                        performer = ActionPerformer.ClickNode,
                        node = node,
                        locationProps = locationProps,
                        context = context,
                        guard = guard,
                    )
                    if (nodeResult.result || nodeResult.state == ActionResultState.StaleContext) {
                        return nodeResult
                    }
                    rejectedAttempts = 1 + nodeResult.retryCount
                }
                if (!guard.isCurrent()) return staleResult(performer, context)
                ActionPerformer.ClickCenter.perform(
                    node,
                    locationProps,
                    context,
                    guard,
                ).copy(retryCount = rejectedAttempts)
            }

            ActionPerformer.LongClickNode -> performNodeActionWithParent(
                performer = performer,
                node = node,
                locationProps = locationProps,
                context = context,
                guard = guard,
            )

            ActionPerformer.LongClick -> {
                var rejectedAttempts = 0
                if (node.isLongClickable) {
                    val nodeResult = performNodeActionWithParent(
                        performer = ActionPerformer.LongClickNode,
                        node = node,
                        locationProps = locationProps,
                        context = context,
                        guard = guard,
                    )
                    if (nodeResult.result || nodeResult.state == ActionResultState.StaleContext) {
                        return nodeResult
                    }
                    rejectedAttempts = 1 + nodeResult.retryCount
                }
                if (!guard.isCurrent()) return staleResult(performer, context)
                ActionPerformer.LongClickCenter.perform(
                    node,
                    locationProps,
                    context,
                    guard,
                ).copy(retryCount = rejectedAttempts)
            }

            else -> performer.perform(node, locationProps, context, guard)
        }
    }

    private suspend fun performNodeActionWithParent(
        performer: ActionPerformer,
        node: AccessibilityNodeInfo,
        locationProps: RawSubscription.LocationProps,
        context: ActionExecutionContext,
        guard: ActionExecutionGuard,
    ): ActionResult {
        val first = performer.perform(node, locationProps, context, guard)
        if (first.result) return first
        var parent = runCatching { node.parent }.getOrNull()
        var depth = 1
        while (parent != null && depth <= MAX_PARENT_DEPTH) {
            val eligible = if (performer == ActionPerformer.LongClickNode) {
                parent.isLongClickable
            } else {
                parent.isClickable
            }
            if (eligible && parent.isVisibleToUser) {
                if (!guard.isCurrent()) return staleResult(performer, context)
                val result = performer.perform(parent, locationProps, context, guard)
                if (result.result) {
                    return result.copy(
                        target = ActionTarget.ClickableParent,
                        retryCount = 1,
                    )
                }
                // Only the nearest eligible parent may be tried. Trying more ancestors could
                // change the semantic target and duplicate an unknown side effect.
                return result.copy(target = ActionTarget.ClickableParent, retryCount = 1)
            }
            parent = runCatching { parent.parent }.getOrNull()
            depth++
        }
        return first
    }

    internal fun staleResult(
        performer: ActionPerformer,
        context: ActionExecutionContext,
    ) = ActionResult(
        action = performer.action,
        result = false,
        state = ActionResultState.StaleContext,
        displayId = context.displayId,
        windowId = context.windowId,
        rotation = context.rotation,
    )
}
