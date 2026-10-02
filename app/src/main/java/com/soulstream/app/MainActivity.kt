package com.soulstream.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.Engine
import com.soulstream.app.service.DownloadService
import com.soulstream.app.ui.AppRoot
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.theme.BgBottom
import com.soulstream.app.ui.theme.BgTop
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.SoulTheme

class MainActivity : ComponentActivity() {

    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        askNotifications()

        setContent {
            var accent by remember { mutableStateOf(Prefs.accent(this)) }
            var fatal by remember { mutableStateOf<String?>(null) }

            SoulTheme(accentIndex = accent) {
                val problem = fatal
                if (problem != null) {
                    FatalScreen(problem) { fatal = null }
                } else {
                    // Safety net: if anything in the UI blows up we show a readable
                    // screen instead of the app vanishing.
                    try {
                        AppRoot(
                            accentIndex = accent,
                            onAccentChange = { i ->
                                accent = i
                                Prefs.setAccent(this, i)
                            },
                            onOpenBrowser = { url ->
                                startActivity(
                                    Intent(this, BrowserActivity::class.java).putExtra("url", url)
                                )
                            },
                            onStartDownload = { url, quality -> startDownload(url, quality) }
                        )
                    } catch (t: Throwable) {
                        val msg = (t.message ?: t.javaClass.simpleName)
                        Prefs.setLastCrash(this, msg)
                        fatal = msg
                    }
                }
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

@Composable
private fun FatalScreen(message: String, onRetry: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            NeonCard(Modifier.fillMaxWidth(), accent = MaterialTheme.colorScheme.error) {
                Icon(
                    Icons.Rounded.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
                Text("Something went wrong", style = MaterialTheme.typography.titleLarge, color = OnDark)
                Spacer(Modifier.height(6.dp))
                Text(message, style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Spacer(Modifier.height(14.dp))
            GhostButton(
                text = "Try again",
                modifier = Modifier.fillMaxWidth(),
                accent = MaterialTheme.colorScheme.primary
            ) { onRetry() }
        }
    }
}
