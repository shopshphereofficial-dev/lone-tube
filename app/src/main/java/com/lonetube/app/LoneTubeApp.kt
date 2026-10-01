package com.lonetube.app

import android.app.Application
import android.content.Context
import com.lonetube.app.data.Prefs
import com.yausername.aria2c.Aria2c
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class LoneTubeApp : Application() {

    override fun onCreate() {
        super.onCreate()
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                Aria2c.getInstance().init(this)
                engineReady = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
            updateEngineIfStale(this)
        }.start()
    }

    /**
     * Keeps the yt-dlp engine fresh (once a day) so site changes never break
     * downloads - YouTube changes its internals every few weeks.
     */
    private fun updateEngineIfStale(context: Context) {
        try {
            val last = Prefs.engineUpdatedAt(context)
            val now = System.currentTimeMillis()
            if (now - last < 24L * 60 * 60 * 1000) return
            YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE)
            Prefs.setEngineUpdatedAt(context, now)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        var engineReady = false
    }
}
