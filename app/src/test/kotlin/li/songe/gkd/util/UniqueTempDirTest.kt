package li.songe.gkd.util

import java.nio.file.Files
import java.io.File
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import org.junit.Assert.*
import org.junit.Test

class UniqueTempDirTest {
    @Test fun `parallel exports never share a directory`() {
        val root = Files.createTempDirectory("gkd-test-").toFile()
        val pool = Executors.newFixedThreadPool(8)
        try {
            val dirs = pool.invokeAll((1..128).map { Callable { createUniqueTempDir(root) } }).map { it.get() }
            assertEquals(128, dirs.map { it.canonicalPath }.toSet().size)
            assertTrue(dirs.all { it.isDirectory && it.canonicalFile.parentFile == root.canonicalFile })
            val missingParent = root.resolve("not-created-yet/nested")
            val nested = pool.invokeAll((1..128).map { Callable { createUniqueTempDir(missingParent) } }).map { it.get() }
            assertEquals(128, nested.map { it.canonicalPath }.toSet().size)
        } finally {
            pool.shutdownNow()
            check(root.canonicalFile.parentFile == File(System.getProperty("java.io.tmpdir")).canonicalFile)
            check(root.name.startsWith("gkd-test-"))
            root.deleteRecursively()
        }
    }
}
