package com.lonetube.app.data

import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File

/** One WhatsApp status (image or video) found on the phone. */
data class StatusItem(
    val file: File,
    val isVideo: Boolean,
    val size: Long,
    val modified: Long
)

object Statuses {

    private const val FILE_PROVIDER = "com.lonetube.app.fileprovider"

    private val FOLDERS = listOf(
        "Android/media/com.whatsapp/WhatsApp/Media/.Statuses",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses",
        "Android/media/com.whatsapp/WhatsApp/Media/Statuses",
        "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/Statuses",
        "WhatsApp/Media/.Statuses",
        "WhatsApp Business/Media/.Statuses"
    )

    /** True when we are allowed to read WhatsApp's private status folder. */
    fun hasAccess(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            ctx.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    fun scan(ctx: Context): List<StatusItem> {
        val out = ArrayList<StatusItem>()
        val base = Environment.getExternalStorageDirectory()
        for (folder in FOLDERS) {
            val dir = File(base, folder)
            if (!dir.isDirectory) continue
            dir.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                val n = f.name.lowercase()
                val isVideo = n.endsWith(".mp4") || n.endsWith(".mkv") ||
                    n.endsWith(".3gp") || n.endsWith(".webm")
                val isImage = n.endsWith(".jpg") || n.endsWith(".jpeg") ||
                    n.endsWith(".png") || n.endsWith(".webp")
                if (isVideo || isImage) {
                    out.add(StatusItem(f, isVideo, f.length(), f.lastModified()))
                }
            }
        }
        return out.sortedByDescending { it.modified }
    }

    private fun mimeOf(item: StatusItem): String {
        val n = item.file.name.lowercase()
        return when {
            n.endsWith(".mp4") -> "video/mp4"
            n.endsWith(".mkv") -> "video/x-matroska"
            n.endsWith(".3gp") -> "video/3gpp"
            n.endsWith(".webm") -> "video/webm"
            n.endsWith(".png") -> "image/png"
            n.endsWith(".webp") -> "image/webp"
            else -> "image/jpeg"
        }
    }

    /** Copies a status into the phone's gallery so it stays saved. */
    fun saveToGallery(ctx: Context, item: StatusItem): String {
        val mime = mimeOf(item)
        val collection = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "LT_" + item.file.name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (item.isVideo) "Movies/LoneTube" else "Pictures/LoneTube"
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = ctx.contentResolver.insert(collection, values)
                ?: return "Could not save"
            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                item.file.inputStream().use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            ctx.contentResolver.update(uri, values, null, null)
            "Saved to gallery"
        } catch (e: Exception) {
            "Could not save: ${e.message}"
        }
    }

    fun shareUri(ctx: Context, item: StatusItem): Uri? = try {
        FileProvider.getUriForFile(ctx, FILE_PROVIDER, item.file)
    } catch (e: Exception) {
        null
    }
}
