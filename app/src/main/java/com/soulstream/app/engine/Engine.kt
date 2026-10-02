package com.soulstream.app.engine

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.WebStorage
import androidx.core.app.ActivityCompat
import java.io.File

/** One downloadable format shown in the quality picker. */
data class QualityOption(
    val index: Int,
    val title: String,
    val subtitle: String,
    val isAudio: Boolean
)

object Engine {

    val QUALITIES = listOf(
        QualityOption(0, "Best available", "Highest quality video + audio", false),
        QualityOption(1, "1080p Full HD", "Sharp and smooth", false),
        QualityOption(2, "720p HD", "Great balance of size and quality", false),
        QualityOption(3, "480p", "Lighter file", false),
        QualityOption(4, "360p", "Smallest video", false),
        QualityOption(5, "MP3 - 320 kbps", "Best audio quality", true),
        QualityOption(6, "MP3 - 128 kbps", "Small audio file", true)
    )

    fun qualityLabel(index: Int): String =
        QUALITIES.firstOrNull { it.index == index }?.title ?: QUALITIES[0].title

    /**
     * Exports WebView cookies (from logins made in the in-app browser) into a
     * Netscape cookie file that yt-dlp reads via --cookies. This is what makes
     * account-only / private content downloadable.
     */
    fun writeCookieFile(context: Context, url: String): File? {
        return try {
            CookieManager.getInstance().flush()
            val host = Uri.parse(url).host ?: return null
            val domain = host.removePrefix("www.").removePrefix("m.")
            val cookieStr = CookieManager.getInstance().getCookie("https://$domain")
                ?: CookieManager.getInstance().getCookie("https://$host")
                ?: return null
            val sb = StringBuilder("# Netscape HTTP Cookie File\n")
            val expiry = System.currentTimeMillis() / 1000 + 60L * 60 * 24 * 365
            for (pair in cookieStr.split(";")) {
                val kv = pair.trim().split("=", limit = 2)
                if (kv.size != 2) continue
                sb.append(".").append(domain).append("\tTRUE\t/\tTRUE\t")
                    .append(expiry).append("\t").append(kv[0]).append("\t")
                    .append(kv[1]).append("\n")
            }
            if (sb.toString().lines().size <= 1) return null
            val f = File(context.cacheDir, "cookies.txt")
            f.writeText(sb.toString())
            f
        } catch (e: Exception) {
            null
        }
    }

    fun askNotificationPermission(activity: Activity) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    /** Wipes every login/cookie. Used by "Log out everywhere" and browser reset. */
    fun resetBrowserData(context: Context) {
        try {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null)
            cm.flush()
        } catch (e: Throwable) {
            // ignore
        }
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (e: Throwable) {
            // ignore
        }
    }

    /** Opens a link in the phone's normal browser (escape hatch). */
    fun openInSystemBrowser(context: Context, url: String) {
        try {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Throwable) {
            // ignore
        }
    }
}
