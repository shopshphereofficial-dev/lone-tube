package com.soulstream.app.engine

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import java.io.File
import java.util.Locale

/**
 * The "phone cleaner" toolbox: junk scan, large files, WhatsApp clean, trash
 * clean, photos clean, app list and battery info. Everything works on the real
 * filesystem, so it needs "All files access" (granted from the Vault/Tools tab).
 */
object Tools {

    data class Junk(val bytes: Long, val items: Int)
    data class AppEntry(val label: String, val pkg: String)
    data class Folder(val name: String, val bytes: Long)

    fun hasAccess(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ctx.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private val ROOTS = listOf(
        Environment.DIRECTORY_DOWNLOADS,
        Environment.DIRECTORY_DCIM,
        Environment.DIRECTORY_MOVIES,
        Environment.DIRECTORY_PICTURES,
        Environment.DIRECTORY_MUSIC,
        "WhatsApp/Media",
        "Android/media/com.whatsapp/WhatsApp/Media",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
    )

    private fun rootFiles(): List<File> {
        val base = Environment.getExternalStorageDirectory()
        return ROOTS.map { File(base, it) }.filter { it.exists() }
    }

    private fun sizeOf(file: File): Long {
        if (file.isFile) return file.length()
        var total = 0L
        file.listFiles()?.forEach { total += sizeOf(it) }
        return total
    }

    // ---------------- junk ----------------

    fun scanJunk(ctx: Context): Junk {
        var bytes = 0L
        var items = 0

        try {
            ctx.cacheDir?.let {
                bytes += sizeOf(it)
                items += (it.listFiles()?.size ?: 0)
            }
        } catch (e: Exception) {
            // ignore
        }

        for (root in rootFiles()) {
            walk(root, 6) { f ->
                if (f.isFile && f.name.startsWith(".trashed")) {
                    bytes += f.length()
                    items++
                }
            }
            walkDirs(root, 4) { d ->
                if (d.isDirectory && (d.listFiles()?.isEmpty() == true)) items++
            }
        }
        return Junk(bytes, items)
    }

    fun cleanJunk(ctx: Context): String {
        var freed = 0L
        try {
            ctx.cacheDir?.listFiles()?.forEach { freed += deleteRecursive(it) }
        } catch (e: Exception) {
            // ignore
        }
        var removedTrash = 0
        for (root in rootFiles()) {
            walk(root, 6) { f ->
                if (f.isFile && f.name.startsWith(".trashed")) {
                    freed += f.length()
                    if (f.delete()) removedTrash++
                }
            }
            walkDirs(root, 4) { d ->
                if (d.isDirectory && (d.listFiles()?.isEmpty() == true)) d.delete()
            }
        }
        return "Cleaned " + human(freed) + " (" + removedTrash + " trashed files)"
    }

    // ---------------- big / specific files ----------------

    fun scanLarge(ctx: Context, minMb: Int = 20, limit: Int = 40): List<File> {
        val min = minMb * 1024L * 1024L
        val out = ArrayList<File>()
        for (root in rootFiles()) {
            walk(root, 5) { f ->
                if (f.isFile && f.length() >= min && out.size < 400) out.add(f)
            }
        }
        return out.sortedByDescending { it.length() }.take(limit)
    }

    fun scanTrash(ctx: Context): List<File> {
        val out = ArrayList<File>()
        for (root in rootFiles()) {
            walk(root, 6) { f ->
                if (f.isFile && f.name.startsWith(".trashed")) out.add(f)
            }
        }
        return out.sortedByDescending { it.length() }
    }

    fun scanLargePhotos(ctx: Context, minMb: Int = 3, limit: Int = 30): List<File> {
        val min = minMb * 1024L * 1024L
        val out = ArrayList<File>()
        for (root in rootFiles()) {
            walk(root, 5) { f ->
                if (f.isFile && f.length() >= min && isImage(f.name)) out.add(f)
            }
        }
        return out.sortedByDescending { it.length() }.take(limit)
    }

    fun whatsappFolders(): List<Folder> {
        val out = ArrayList<Folder>()
        val bases = listOf(
            "WhatsApp/Media",
            "Android/media/com.whatsapp/WhatsApp/Media",
            "Android/media/com.whatsapp.w4b/WhatsApp Business/Media"
        )
        val base = Environment.getExternalStorageDirectory()
        for (b in bases) {
            val dir = File(base, b)
            if (!dir.isDirectory) continue
            dir.listFiles()?.forEach { child ->
                if (child.isDirectory) out.add(Folder(child.name, sizeOf(child)))
            }
        }
        return out.sortedByDescending { it.bytes }
    }

    fun deleteFiles(files: List<File>): String {
        var freed = 0L
        var n = 0
        files.forEach {
            val s = it.length()
            if (it.delete()) {
                freed += s
                n++
            }
        }
        return "Deleted $n files - " + human(freed) + " freed"
    }

    // ---------------- apps / device ----------------

    fun installedApps(ctx: Context): List<AppEntry> {
        val pm = ctx.packageManager
        val main = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(main, 0)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                AppEntry(info.loadLabel(pm).toString(), pkg)
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    fun uninstall(ctx: Context, pkg: String) {
        try {
            ctx.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // ignore
        }
    }

    fun batteryLine(ctx: Context): String {
        return try {
            val bm = ctx.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            "Battery $level%" + if (bm.isCharging) " - charging" else ""
        } catch (e: Exception) {
            "Battery info unavailable"
        }
    }

    fun storageLine(): String {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val free = stat.availableBytes / 1.0e9
            val total = stat.totalBytes / 1.0e9
            String.format(Locale.US, "%.1f GB free of %.1f GB", free, total)
        } catch (e: Exception) {
            "Storage info unavailable"
        }
    }

    fun ramLine(ctx: Context): String {
        return try {
            val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val mi = ActivityManager.MemoryInfo()
            am.getMemoryInfo(mi)
            String.format(
                Locale.US,
                "%.1f GB free of %.1f GB RAM",
                mi.availMem / 1.0e9,
                mi.totalMem / 1.0e9
            )
        } catch (e: Exception) {
            "RAM info unavailable"
        }
    }

    fun openBatterySettings(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            try {
                ctx.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (e2: Exception) {
                // ignore
            }
        }
    }

    fun openAppSettings(ctx: Context, pkg: String) {
        try {
            ctx.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:$pkg")
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            // ignore
        }
    }

    // ---------------- helpers ----------------

    fun human(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }

    private fun isImage(name: String): Boolean {
        val n = name.lowercase()
        return n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png") ||
            n.endsWith(".webp") || n.endsWith(".gif")
    }

    private fun walk(dir: File, depth: Int, action: (File) -> Unit) {
        if (depth < 0 || !dir.isDirectory) return
        val children = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        } ?: return
        for (f in children) {
            if (f.isDirectory) walk(f, depth - 1, action) else action(f)
        }
    }

    private fun walkDirs(dir: File, depth: Int, action: (File) -> Unit) {
        if (depth < 0 || !dir.isDirectory) return
        val children = try {
            dir.listFiles()
        } catch (e: Exception) {
            null
        } ?: return
        for (f in children) {
            if (f.isDirectory) {
                action(f)
                walkDirs(f, depth - 1, action)
            }
        }
    }

    private fun deleteRecursive(file: File): Long {
        var freed = 0L
        if (file.isDirectory) {
            file.listFiles()?.forEach { freed += deleteRecursive(it) }
            file.delete()
        } else {
            freed += file.length()
            file.delete()
        }
        return freed
    }
}
