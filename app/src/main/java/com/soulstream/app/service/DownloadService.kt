package com.soulstream.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.soulstream.app.MainActivity
import com.soulstream.app.SoulStreamApp
import com.soulstream.app.data.ActiveJob
import com.soulstream.app.data.History
import com.soulstream.app.data.LiveDownloads
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.Diag
import com.soulstream.app.engine.Engine
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {

    companion object {
        private const val CHANNEL_ID = "soulstream_downloads"

        fun start(context: Context, url: String, quality: Int) {
            val intent = Intent(context, DownloadService::class.java)
                .putExtra("url", url)
                .putExtra("quality", quality)
            ContextCompat.startForegroundService(context, intent)
        }
    }

    private val active = AtomicInteger(0)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val url = intent?.getStringExtra("url") ?: return START_NOT_STICKY
        val quality = intent.getIntExtra("quality", 0)
        val notifId = url.hashCode()
        val jobId = url + "#" + System.currentTimeMillis()

        val initial = buildNotification("Preparing download...", 0, true)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(notifId, initial, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(notifId, initial)
            }
        } catch (e: Throwable) {
            Diag.log(this, "dl", "startForeground refused: ${e.message}")
        }

        active.incrementAndGet()
        LiveDownloads.upsert(ActiveJob(jobId, "Preparing...", 0, ActiveJob.Status.PREPARING))
        Diag.log(this, "dl", "queued ${Engine.qualityLabel(quality)} $url")

        Thread {
            var lastLine = ""
            try {
                // 1) the engine must be alive - wait for it instead of failing
                updateNotification(notifId, "Warming up engine...", 0, true)
                try {
                    SoulStreamApp.awaitEngine(this@DownloadService, 60_000)
                } catch (e: Throwable) {
                    throw IllegalStateException(
                        "Engine could not start: " +
                            (SoulStreamApp.engineError ?: e.message ?: "unknown")
                    )
                }

                val cookieFile = Engine.writeCookieFile(this@DownloadService, url)

                val title = try {
                    val infoRequest = YoutubeDLRequest(url)
                    if (cookieFile != null) {
                        infoRequest.addOption("--cookies", cookieFile.absolutePath)
                    }
                    YoutubeDL.getInstance().getInfo(infoRequest).title ?: url
                } catch (e: Throwable) {
                    Diag.log(this, "dl", "title lookup failed: ${e.message}")
                    url
                }

                updateNotification(notifId, "Downloading: $title", 0, true)
                LiveDownloads.upsert(ActiveJob(jobId, title, 0, ActiveJob.Status.DOWNLOADING))

                val workDir = File(
                    File(getExternalFilesDir(null), "downloads"),
                    System.currentTimeMillis().toString()
                )
                workDir.mkdirs()

                // 2) two passes: the turbo aria2c engine first (when enabled),
                //    then yt-dlp's own downloader. aria2c is never the only way.
                val preferAria = Prefs.turbo(this@DownloadService)
                val passes = if (preferAria) listOf(true, false) else listOf(false)
                var finished = false
                var failure: Throwable? = null

                for (useAria in passes) {
                    workDir.listFiles()?.forEach { it.deleteRecursively() }
                    try {
                        val request = buildRequest(url, quality, workDir, cookieFile, useAria)
                        YoutubeDL.getInstance().execute(request) { progress, _, line ->
                            if (!line.isNullOrBlank()) lastLine = line
                            val p = progress.toInt().coerceIn(0, 100)
                            updateNotification(notifId, "Downloading: $title", p, true)
                            LiveDownloads.upsert(
                                ActiveJob(jobId, title, p, ActiveJob.Status.DOWNLOADING)
                            )
                        }
                        finished = true
                        break
                    } catch (e: Throwable) {
                        failure = e
                        Diag.log(
                            this,
                            "dl",
                            "pass aria=$useAria failed: ${e.message ?: e.javaClass.simpleName}"
                        )
                    }
                }

                if (!finished) {
                    throw (failure ?: IllegalStateException("the download failed"))
                }

                val files = workDir.listFiles()?.filter { it.isFile } ?: emptyList()
                if (files.isEmpty()) {
                    throw IllegalStateException("the engine finished but produced no file")
                }
                var totalBytes = 0L
                files.forEach { f ->
                    val mime = mimeFor(f)
                    val saved = saveResult(f, mime)
                    History.add(this@DownloadService, f.name, f.length(), saved, mime)
                    totalBytes += f.length()
                }
                workDir.deleteRecursively()
                val summary = if (files.size == 1) files[0].name else "${files.size} files"
                LiveDownloads.upsert(ActiveJob(jobId, summary, 100, ActiveJob.Status.DONE))
                Diag.clearDownloadError(this)
                Diag.log(this, "dl", "done $summary ${totalBytes / 1024} KB")
                finishWith(notifId, "Done: $summary", true)
            } catch (e: Throwable) {
                val msg = e.message ?: lastLine.ifBlank { "unknown error" }
                val detail = if (lastLine.isBlank()) msg else "$msg  |  ${lastLine.take(220)}"
                LiveDownloads.upsert(ActiveJob(jobId, msg, 0, ActiveJob.Status.FAILED))
                Diag.setDownloadError(this, detail)
                Diag.log(this, "dl", "FAILED: $detail")
                finishWith(notifId, "Failed: ${msg.take(120)}", false)
            } finally {
                if (active.decrementAndGet() == 0) stopSelf()
            }
        }.start()

        return START_NOT_STICKY
    }

    private fun buildRequest(
        url: String,
        quality: Int,
        workDir: File,
        cookieFile: File?,
        useAria: Boolean
    ): YoutubeDLRequest {
        return YoutubeDLRequest(url).apply {
            when (quality) {
                1 -> {
                    addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]")
                    addOption("--merge-output-format", "mp4")
                }
                2 -> {
                    addOption("-f", "bv*[height<=720]+ba/b[height<=720]")
                    addOption("--merge-output-format", "mp4")
                }
                3 -> {
                    addOption("-f", "bv*[height<=480]+ba/b[height<=480]")
                    addOption("--merge-output-format", "mp4")
                }
                4 -> {
                    addOption("-f", "bv*[height<=360]+ba/b[height<=360]")
                    addOption("--merge-output-format", "mp4")
                }
                5 -> {
                    addOption("-f", "ba/b")
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "320K")
                }
                6 -> {
                    addOption("-f", "ba/b")
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                    addOption("--audio-quality", "128K")
                }
                7 -> {
                    addOption("--skip-download")
                    addOption("--write-thumbnail")
                    addOption("--convert-thumbnails", "jpg")
                }
                8 -> {
                    addOption("-f", "bv*+ba/b")
                    addOption("--merge-output-format", "mp4")
                }
                else -> {
                    addOption("-f", "bv*+ba/b")
                    addOption("--merge-output-format", "mp4")
                }
            }
            addOption("-o", workDir.absolutePath + "/%(title)s.%(ext)s")
            if (quality == 8) addOption("--yes-playlist") else addOption("--no-playlist")
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("--no-update")
            if (useAria) {
                addOption("--downloader", "libaria2c.so")
            }
            if (cookieFile != null) {
                addOption("--cookies", cookieFile.absolutePath)
            }
        }
    }

    private fun contentIntent(): PendingIntent? {
        return try {
            val i = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            PendingIntent.getActivity(
                this,
                0,
                i,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } catch (e: Throwable) {
            null
        }
    }

    private fun buildNotification(
        text: String,
        progress: Int,
        ongoing: Boolean
    ): android.app.Notification {
        val b = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("SoulStream")
            .setContentText(text)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, progress <= 0)
        contentIntent()?.let { b.setContentIntent(it) }
        return b.build()
    }

    private fun updateNotification(notifId: Int, text: String, progress: Int, ongoing: Boolean) {
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(notifId, buildNotification(text, progress, ongoing))
        } catch (e: Throwable) {
            // a missing notification must never kill a download
        }
    }

    private fun finishWith(notifId: Int, text: String, ok: Boolean) {
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Throwable) {
            // ignore
        }
        try {
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            val b = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(
                    if (ok) android.R.drawable.stat_sys_download_done
                    else android.R.drawable.stat_notify_error
                )
                .setContentTitle("SoulStream")
                .setContentText(text)
                .setAutoCancel(true)
            contentIntent()?.let { b.setContentIntent(it) }
            nm.notify(notifId, b.build())
        } catch (e: Throwable) {
            // ignore
        }
    }

    private fun mimeFor(file: File): String {
        return when (file.extension.lowercase()) {
            "mp4", "m4v" -> "video/mp4"
            "webm" -> "video/webm"
            "mkv" -> "video/x-matroska"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            else -> "application/octet-stream"
        }
    }

    /**
     * Saves to the public Downloads folder. If MediaStore refuses (it happens),
     * the file still lands in the app's own Downloads folder - a finished
     * download is never reported as a failure.
     */
    private fun saveResult(file: File, mime: String): String {
        try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri: Uri? = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { it.copyTo(out) }
                }
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
                return uri.toString()
            }
        } catch (e: Throwable) {
            Diag.log(this, "dl", "MediaStore save failed: ${e.message}")
        }
        return try {
            val dir = File(getExternalFilesDir(null), "SoulStream")
            dir.mkdirs()
            val target = File(dir, file.name)
            file.copyTo(target, overwrite = true)
            target.absolutePath
        } catch (e: Throwable) {
            Diag.log(this, "dl", "fallback save failed: ${e.message}")
            file.absolutePath
        }
    }
}
