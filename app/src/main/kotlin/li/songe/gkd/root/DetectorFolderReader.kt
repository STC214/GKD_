package li.songe.gkd.root

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import java.io.File

/** Root process only. Read the launcher database; do not change launcher data. */
internal fun readDetectorFolderPackages(): Array<String>? {
    val directories = listOf(
        // RootService inherits app data isolation. The FBE mirror is a real mount path
        // and survives SQLite canonicalization without changing any mount or HMA setting.
        File("/data_mirror/data_de/null/0/com.miui.home/databases"),
        File("/data_mirror/data_ce/null/0/com.miui.home/databases"),
        File("/data/user_de/0/com.miui.home/databases"),
        File("/data/user/0/com.miui.home/databases"),
    )
    val packages = hashSetOf<String>()
    var found = false
    directories.flatMap { it.listFiles().orEmpty().toList() }
        .filter { it.name.matches(Regex("launcher[0-9]+x[0-9]+\\.db")) }
        .forEach { file ->
            runCatching {
                SQLiteDatabase.openDatabase(file.path, null,
                    SQLiteDatabase.OPEN_READONLY or SQLiteDatabase.NO_LOCALIZED_COLLATORS).use { db ->
                    val entries = arrayListOf<LauncherFolderEntry>()
                    db.rawQuery("SELECT _id, container, intent FROM favorites LIMIT 10000", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val intent = cursor.getString(2)
                            val packageName = intent?.let {
                                runCatching { Intent.parseUri(it, 0).component?.packageName }.getOrNull()
                            }
                            entries.add(LauncherFolderEntry(cursor.getLong(0), cursor.getLong(1), packageName))
                        }
                    }
                    val result = findDetectorFolderPackages(entries)
                    if (result.isNotEmpty()) { found = true; packages.addAll(result) }
                }
            }.onFailure { android.util.Log.w("GKD-Folder", "Launcher whitelist read failed: ${file.path}", it) }
        }
    return if (found) packages.sorted().toTypedArray() else null
}
