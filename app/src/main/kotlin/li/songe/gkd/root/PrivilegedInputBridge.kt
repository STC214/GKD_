package li.songe.gkd.root

import li.songe.gkd.data.ActionExecutionContext
import li.songe.gkd.data.ActionExecutionGuard
import li.songe.gkd.shizuku.shizukuContextFlow

enum class PrivilegedInputBackend {
    None,
    ApkRoot,
    Shizuku,
}

enum class PrivilegedInputOutcome {
    Completed,
    Rejected,
    Unavailable,
    Failed,
    StaleContext,
}

data class PrivilegedInputResult(
    val backend: PrivilegedInputBackend,
    val outcome: PrivilegedInputOutcome,
) {
    val canFallback: Boolean
        get() = outcome == PrivilegedInputOutcome.Rejected ||
            outcome == PrivilegedInputOutcome.Unavailable
}

private fun Int.toRootInputResult() = PrivilegedInputResult(
    backend = PrivilegedInputBackend.ApkRoot,
    outcome = when (this) {
        ROOT_INPUT_RESULT_COMPLETED -> PrivilegedInputOutcome.Completed
        ROOT_INPUT_RESULT_REJECTED -> PrivilegedInputOutcome.Rejected
        ROOT_INPUT_RESULT_UNAVAILABLE -> PrivilegedInputOutcome.Unavailable
        else -> PrivilegedInputOutcome.Failed
    },
)

internal suspend fun runPrivilegedInputFallbackChain(
    rootCall: () -> PrivilegedInputResult,
    isCurrent: suspend () -> Boolean,
    shizukuCall: () -> PrivilegedInputResult,
): PrivilegedInputResult {
    val rootResult = rootCall()
    if (rootResult.outcome != PrivilegedInputOutcome.Rejected &&
        rootResult.outcome != PrivilegedInputOutcome.Unavailable
    ) {
        return rootResult
    }
    if (!isCurrent()) {
        return PrivilegedInputResult(
            backend = PrivilegedInputBackend.None,
            outcome = PrivilegedInputOutcome.StaleContext,
        )
    }
    return shizukuCall()
}

/**
 * Structured coordinate-input bridge. It retries only after a backend proves that no input was
 * accepted. A failed Binder transaction or a partially injected event sequence is deliberately
 * terminal, because repeating it through another backend could duplicate a side effect.
 */
object PrivilegedInputBridge {
    suspend fun tap(
        x: Float,
        y: Float,
        duration: Long,
        context: ActionExecutionContext,
        guard: ActionExecutionGuard,
    ): PrivilegedInputResult = perform(
        request = request(
            action = ROOT_INPUT_ACTION_TAP,
            x1 = x,
            y1 = y,
            x2 = x,
            y2 = y,
            duration = duration,
            context = context,
        ) ?: return rejected(),
        guard = guard,
        shizukuCall = {
            val shizuku = shizukuContextFlow.value
            if (shizuku.serviceWrapper == null && shizuku.inputManager == null) {
                unavailable()
            } else if (shizuku.tap(x, y, duration, context.displayId)) {
                completed(PrivilegedInputBackend.Shizuku)
            } else {
                failed(PrivilegedInputBackend.Shizuku)
            }
        },
    )

    suspend fun swipe(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        context: ActionExecutionContext,
        guard: ActionExecutionGuard,
    ): PrivilegedInputResult = perform(
        request = request(
            action = ROOT_INPUT_ACTION_SWIPE,
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
            duration = duration,
            context = context,
        ) ?: return rejected(),
        guard = guard,
        shizukuCall = {
            val shizuku = shizukuContextFlow.value
            if (shizuku.serviceWrapper == null && shizuku.inputManager == null) {
                unavailable()
            } else if (shizuku.swipe(x1, y1, x2, y2, duration, context.displayId)) {
                completed(PrivilegedInputBackend.Shizuku)
            } else {
                failed(PrivilegedInputBackend.Shizuku)
            }
        },
    )

    private suspend fun perform(
        request: RootInputRequest,
        guard: ActionExecutionGuard,
        shizukuCall: () -> PrivilegedInputResult,
    ) = runPrivilegedInputFallbackChain(
        rootCall = { RootServiceClient.performInput(request).toRootInputResult() },
        isCurrent = guard::isCurrent,
        shizukuCall = shizukuCall,
    )

    private fun request(
        action: Int,
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        duration: Long,
        context: ActionExecutionContext,
    ): RootInputRequest? {
        val bounds = context.windowBounds.intersect(context.visibleBounds) ?: return null
        return RootInputRequest(
            action = action,
            displayId = context.displayId,
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
            durationMs = duration,
            boundLeft = bounds.left,
            boundTop = bounds.top,
            boundRight = bounds.right,
            boundBottom = bounds.bottom,
        ).takeIf { validateRootInputRequest(it) == null }
    }

    private fun completed(backend: PrivilegedInputBackend) =
        PrivilegedInputResult(backend, PrivilegedInputOutcome.Completed)

    private fun failed(backend: PrivilegedInputBackend) =
        PrivilegedInputResult(backend, PrivilegedInputOutcome.Failed)

    private fun unavailable() =
        PrivilegedInputResult(PrivilegedInputBackend.None, PrivilegedInputOutcome.Unavailable)

    private fun rejected() =
        PrivilegedInputResult(PrivilegedInputBackend.None, PrivilegedInputOutcome.Rejected)
}
