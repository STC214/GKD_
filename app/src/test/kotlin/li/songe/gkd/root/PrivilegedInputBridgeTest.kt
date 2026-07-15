package li.songe.gkd.root

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PrivilegedInputBridgeTest {
    private fun result(
        backend: PrivilegedInputBackend,
        outcome: PrivilegedInputOutcome,
    ) = PrivilegedInputResult(backend, outcome)

    @Test
    fun rootCompletionStopsFallback() = runBlocking {
        var shizukuCalls = 0
        val actual = runPrivilegedInputFallbackChain(
            rootCall = { result(PrivilegedInputBackend.ApkRoot, PrivilegedInputOutcome.Completed) },
            isCurrent = { true },
            shizukuCall = {
                shizukuCalls++
                result(PrivilegedInputBackend.Shizuku, PrivilegedInputOutcome.Completed)
            },
        )

        assertEquals(PrivilegedInputBackend.ApkRoot, actual.backend)
        assertEquals(PrivilegedInputOutcome.Completed, actual.outcome)
        assertEquals(0, shizukuCalls)
    }

    @Test
    fun rootUnavailableFallsBackToShizuku() = runBlocking {
        val actual = runPrivilegedInputFallbackChain(
            rootCall = { result(PrivilegedInputBackend.ApkRoot, PrivilegedInputOutcome.Unavailable) },
            isCurrent = { true },
            shizukuCall = {
                result(PrivilegedInputBackend.Shizuku, PrivilegedInputOutcome.Completed)
            },
        )

        assertEquals(PrivilegedInputBackend.Shizuku, actual.backend)
        assertEquals(PrivilegedInputOutcome.Completed, actual.outcome)
    }

    @Test
    fun staleContextStopsBetweenBackends() = runBlocking {
        var shizukuCalls = 0
        val actual = runPrivilegedInputFallbackChain(
            rootCall = { result(PrivilegedInputBackend.ApkRoot, PrivilegedInputOutcome.Rejected) },
            isCurrent = { false },
            shizukuCall = {
                shizukuCalls++
                result(PrivilegedInputBackend.Shizuku, PrivilegedInputOutcome.Completed)
            },
        )

        assertEquals(PrivilegedInputBackend.None, actual.backend)
        assertEquals(PrivilegedInputOutcome.StaleContext, actual.outcome)
        assertEquals(0, shizukuCalls)
    }

    @Test
    fun ambiguousRootFailureNeverRetries() = runBlocking {
        var shizukuCalls = 0
        val actual = runPrivilegedInputFallbackChain(
            rootCall = { result(PrivilegedInputBackend.ApkRoot, PrivilegedInputOutcome.Failed) },
            isCurrent = { true },
            shizukuCall = {
                shizukuCalls++
                result(PrivilegedInputBackend.Shizuku, PrivilegedInputOutcome.Completed)
            },
        )

        assertEquals(PrivilegedInputBackend.ApkRoot, actual.backend)
        assertEquals(PrivilegedInputOutcome.Failed, actual.outcome)
        assertEquals(0, shizukuCalls)
    }

    @Test
    fun shizukuFailureRemainsTerminalForAccessibilityCaller() = runBlocking {
        val actual = runPrivilegedInputFallbackChain(
            rootCall = { result(PrivilegedInputBackend.ApkRoot, PrivilegedInputOutcome.Unavailable) },
            isCurrent = { true },
            shizukuCall = { result(PrivilegedInputBackend.Shizuku, PrivilegedInputOutcome.Failed) },
        )

        assertEquals(PrivilegedInputBackend.Shizuku, actual.backend)
        assertEquals(PrivilegedInputOutcome.Failed, actual.outcome)
        assertEquals(false, actual.canFallback)
    }
}
