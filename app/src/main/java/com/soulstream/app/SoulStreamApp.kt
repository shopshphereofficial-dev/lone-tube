package com.soulstream.app

import android.app.Application
import android.content.Context
import com.soulstream.app.data.Prefs
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class SoulStreamApp : Application() {

    override fun onCreate() {
        super.onCreate()
        installCrashRecorder()
        // Never let startup work take the app down: everything is guarded.
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                Aria2c.getInstance().init(this)
                engineReady = true
            } catch (e: Throwable) {
                startupError = e.message ?: e.javaClass.simpleName
            }
            try {
                updateEngineIfStale(this)
            } catch (e: Throwable) {
                // offline is fine - it will retry next launch
            }
        }.start()
    }

    /**
     * Records the last uncaught error so the app can show it on the next
     * launch instead of just vanishing. The original handler still runs.
     */
    private fun installCrashRecorder() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val where = throwable.stackTrace.firstOrNull()
                val msg = buildString {
                    append(throwable.javaClass.name)
                    append(": ")
                    append(throwable.message ?: "")
                    if (where != null) {
                        append("  @ ")
                        append(where.fileName ?: "?")
                        append(":")
                        append(where.lineNumber)
                    }
                }
                Prefs.setLastCrash(this, msg)
            } catch (e: Throwable) {
                // nothing we can do here
            }
            if (previous != null) {
                previous.uncaughtException(thread, throwable)
            }
        }
    }

    /** Keeps the yt-dlp engine fresh (once a day) so sites keep working. */
    private fun updateEngineIfStale(context: Context) {
        val last = Prefs.engineUpdatedAt(context)
        val now = System.currentTimeMillis()
        if (now - last < 24L * 60 * 60 * 1000) return
        YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
        Prefs.setEngineUpdatedAt(context, now)
    }

    companion object {
        @Volatile
        var engineReady = false

        @Volatile
        var startupError: String? = null
    }
}
