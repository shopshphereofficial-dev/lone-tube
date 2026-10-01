package com.lonetube.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.lonetube.app.engine.Engine
import com.lonetube.app.service.DownloadService
import com.lonetube.app.ui.AppRoot
import com.lonetube.app.ui.theme.LoneTubeTheme

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        askNotifications()

        setContent {
            LoneTubeTheme {
                AppRoot(
                    onOpenBrowser = { url ->
                        startActivity(
                            Intent(this, BrowserActivity::class.java).putExtra("url", url)
                        )
                    },
                    onStartDownload = { url, quality -> startDownload(url, quality) }
                )
            }
        }
    }

    private fun startDownload(url: String, quality: Int) {
        askNotifications()
        DownloadService.start(this, url, quality)
        Toast.makeText(
            this,
            "Download started - " + Engine.qualityLabel(quality),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
