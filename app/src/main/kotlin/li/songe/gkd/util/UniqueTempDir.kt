package li.songe.gkd.util

import java.io.File
import java.nio.file.Files

/** mkdir by wall-clock milliseconds collides across simultaneous exports/screenshots. */
internal fun createUniqueTempDir(parent: File): File {
    if (!parent.isDirectory) parent.mkdirs()
    check(parent.isDirectory) { "Temporary parent directory is unavailable" }
    return Files.createTempDirectory(parent.toPath(), "gkd-").toFile()
}
