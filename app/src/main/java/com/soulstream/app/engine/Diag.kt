package com.soulstream.app.engine

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.webkit.WebView
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Single source of truth for the version shown in the UI. */
object AppInfo {
    const val VERSION = "2.8"
    const val VERSION_CODE = 9
}

/**
 * On-device diagnostics.
 *
 * The app now records what the engine and the downloader actually did, so a
 * failure is never silent again: the user can open Settings -> Diagnostics,
 * copy the report and send it on.
 */
object Diag {

    private const val FILE = "soulstream_diag"
    private const val KEY_LOG = "log"
    private const val KEY_DL_ERROR = "last_download_error"
    private const val MAX = 16000

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    @Synchronized
    fun log(ctx: Context, tag: String, msg: String) {
        try {
            val t = SimpleDateFormat("dd/MM HH:mm:ss", Locale.US).format(Date())
            val line = "$t  [$tag]  $msg\n"
            val cur = sp(ctx).getString(KEY_LOG, "") ?: ""
            sp(ctx).edit().putString(KEY_LOG, (line + cur).take(MAX)).apply()
        } catch (e: Throwable) {
            // logging must never crash anything
        }
    }

    @Synchronized
    fun setDownloadError(ctx: Context, msg: String) {
        try {
            sp(ctx).edit().putString(KEY_DL_ERROR, msg.take(900)).apply()
        } catch (e: Throwable) {
            // ignore
        }
    }

    fun lastDownloadError(ctx: Context): String? =
        sp(ctx).getString(KEY_DL_ERROR, null)

    fun clearDownloadError(ctx: Context) {
        sp(ctx).edit().remove(KEY_DL_ERROR).apply()
    }

    fun rawLog(ctx: Context): String = sp(ctx).getString(KEY_LOG, "") ?: "(nothing logged yet)"

    fun clearLog(ctx: Context) {
        sp(ctx).edit().remove(KEY_LOG).apply()
    }

    /** Everything needed to explain a failure, in one copyable block. */
    fun snapshot(ctx: Context): String {
        val sb = StringBuilder()
        sb.append("=== SoulStream diagnostics ===\n")
        sb.append("app version : ").append(AppInfo.VERSION).append(" (").append(AppInfo.VERSION_CODE).append(")\n")
        sb.append("android     : ").append(Build.VERSION.RELEASE).append(" (api ").append(Build.VERSION.SDK_INT).append(")\n")
        sb.append("device      : ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n")
        sb.append("abi         : ").append(Build.SUPPORTED_ABIS.joinToString(",")).append("\n")
        sb.append("engineReady : ").append(com.soulstream.app.SoulStreamApp.engineReady).append("\n")
        sb.append("engineError : ").append(com.soulstream.app.SoulStreamApp.engineError ?: "-").append("\n")
        sb.append("engineNote  : ").append(com.soulstream.app.SoulStreamApp.engineNote ?: "-").append("\n")
        sb.append("yt-dlp ver  : ").append(ytdlpVersion(ctx)).append("\n")
        sb.append("webview     : ").append(webViewInfo()).append("\n")
        sb.append("storage     : ").append(storageInfo()).append("\n")
        sb.append("filesAccess : ").append(Tools.hasAccess(ctx)).append("\n")
        sb.append("lastCrash   : ").append(com.soulstream.app.data.Prefs.lastCrash(ctx) ?: "-").append("\n")
        sb.append("lastDlError : ").append(lastDownloadError(ctx) ?: "-").append("\n")
        sb.append("\n--- log (newest first) ---\n")
        sb.append(rawLog(ctx))
        return sb.toString()
    }

    private fun ytdlpVersion(ctx: Context): String {
        return try {
            val v = com.yausername.youtubedl_android.YoutubeDL.getInstance().version(ctx)
            if (v.isNullOrBlank()) "unknown" else v
        } catch (e: Throwable) {
            "unavailable"
        }
    }

    private fun webViewInfo(): String {
        return try {
            if (Build.VERSION.SDK_INT >= 26) {
                val p = WebView.getCurrentWebViewPackage()
                if (p == null) "missing" else p.packageName + " " + p.versionName
            } else {
                "unknown"
            }
        } catch (e: Throwable) {
            "unavailable"
        }
    }

    private fun storageInfo(): String {
        return try {
            val s = StatFs(Environment.getDataDirectory().path)
            val free = s.availableBytes / 1.0e9
            val total = s.totalBytes / 1.0e9
            String.format(Locale.US, "%.1f GB free / %.1f GB", free, total)
        } catch (e: Throwable) {
            "unavailable"
        }
    }

    /** Deletes the recorded log + crash so the next report is clean. */
    fun clearAll(ctx: Context) {
        clearLog(ctx)
        clearDownloadError(ctx)
        com.soulstream.app.data.Prefs.clearLastCrash(ctx)
    }

    /** Rough check that the engine unpacked its binaries. */
    fun engineFilesPresent(ctx: Context): Boolean {
        return try {
            val dir = File(ctx.filesDir, "youtubedl-android")
            dir.exists() && (dir.listFiles()?.isNotEmpty() == true)
        } catch (e: Throwable) {
            false
        }
    }
}
