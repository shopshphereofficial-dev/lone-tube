package com.soulstream.app.engine

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.webkit.CookieManager
import java.net.HttpURLConnection
import java.net.URL

/**
 * Saves a web image straight into the gallery (Pictures/SoulStream), reusing
 * the browser's cookies so logged-in images work too. Run this off the main
 * thread.
 */
object ImageSaver {

    private const val UA =
        "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120 Mobile Safari/537.36"

    fun save(ctx: Context, url: String, suggestedName: String? = null): String {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 25000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Referer", url)
                val cookie = try {
                    CookieManager.getInstance().getCookie(url)
                } catch (e: Exception) {
                    null
                }
                if (!cookie.isNullOrBlank()) setRequestProperty("Cookie", cookie)
            }
            conn.connect()
            val code = conn.responseCode
            if (code !in 200..299) return "Image download failed (HTTP $code)"

            val mime = conn.contentType?.substringBefore(";")?.trim()?.lowercase()
                ?: "image/jpeg"
            if (!mime.startsWith("image")) return "That link is not an image"

            val ext = when {
                mime.contains("png") -> "png"
                mime.contains("webp") -> "webp"
                mime.contains("gif") -> "gif"
                mime.contains("bmp") -> "bmp"
                else -> "jpg"
            }
            val base = suggestedName?.takeIf { it.isNotBlank() }
                ?.substringBeforeLast(".")
                ?.take(40)
                ?.replace(Regex("[^A-Za-z0-9 _-]"), "")
                ?.trim()
                ?.ifBlank { null }
                ?: "image"
            val name = "SS_${base}_${System.currentTimeMillis()}.$ext"

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                put(MediaStore.MediaColumns.MIME_TYPE, mime)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/SoulStream")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri: Uri = ctx.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
            ) ?: return "Could not create the image file"

            ctx.contentResolver.openOutputStream(uri)?.use { out ->
                conn.inputStream.use { it.copyTo(out) }
            }
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            ctx.contentResolver.update(uri, values, null, null)

            try {
                com.soulstream.app.data.History.add(ctx, name, 0L, uri.toString(), mime)
            } catch (e: Exception) {
                // history is a nice-to-have
            }
            "Image saved to Pictures/SoulStream"
        } catch (e: Exception) {
            "Image download failed: ${e.message}"
        } finally {
            try {
                conn?.disconnect()
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
