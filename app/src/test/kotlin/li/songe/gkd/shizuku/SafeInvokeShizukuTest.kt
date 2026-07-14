package li.songe.gkd.shizuku

import android.os.RemoteException
import org.junit.Assert.assertNull
import org.junit.Test

class SafeInvokeShizukuTest {
    @Test
    fun `remote binder failure becomes unavailable result`() {
        val result = safeInvokeShizuku<Unit> {
            throw RemoteException("binder died")
        }

        assertNull(result)
    }
}
