package com.lonetube.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.lonetube.app.data.Prefs
import com.lonetube.app.engine.Engine
import com.lonetube.app.service.DownloadService
import com.lonetube.app.ui.components.AnimatedGradientBackground
import com.lonetube.app.ui.components.GradientButton
import com.lonetube.app.ui.components.QualitySheet
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.LoneTubeTheme
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Surface2

class BrowserActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val startUrl = intent?.getStringExtra("url") ?: "https://m.youtube.com"

        setContent {
            LoneTubeTheme {
                var webView by remember { mutableStateOf<WebView?>(null) }
                var currentUrl by remember { mutableStateOf(startUrl) }
                var progress by remember { mutableStateOf(0f) }
                var loading by remember { mutableStateOf(true) }
                var showSheet by remember { mutableStateOf(false) }
                var pendingUrl by remember { mutableStateOf("") }

                AnimatedGradientBackground(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = OnDark)
                            }
                            Text(
                                "Browse",
                                style = MaterialTheme.typography.titleMedium,
                                color = OnDark,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "Log in here for private videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = Cyan)
                            }
                        }

                        if (loading) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = Cyan,
                                trackColor = Surface2
                            )
                        }

                        AndroidView(
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.loadWithOverviewMode = true
                                    settings.useWideViewPort = true
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            loading = false
                                            url?.let { currentUrl = it }
                                        }

                                        override fun doUpdateVisitedHistory(
                                            view: WebView?,
                                            url: String?,
                                            isReload: Boolean
                                        ) {
                                            url?.let { currentUrl = it }
                                        }
                                    }
                                    webChromeClient = object : WebChromeClient() {
                                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                            progress = newProgress / 100f
                                            loading = newProgress < 100
                                        }
                                    }
                                    loadUrl(startUrl)
                                    webView = this
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        )

                        GradientButton(
                            text = "Download this video",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            icon = Icons.Rounded.Download,
                            enabled = !loading
                        ) {
                            val target = webView?.url ?: currentUrl
                            if (Prefs.askQuality(this@BrowserActivity)) {
                                pendingUrl = target
                                showSheet = true
                            } else {
                                startIt(target, Prefs.defaultQuality(this@BrowserActivity))
                            }
                        }
                    }

                    if (showSheet) {
                        QualitySheet(
                            onDismiss = { showSheet = false },
                            onPick = { q ->
                                showSheet = false
                                startIt(pendingUrl, q)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startIt(url: String, quality: Int) {
        Engine.askNotificationPermission(this)
        DownloadService.start(this, url, quality)
        Toast.makeText(
            this,
            "Download started - " + Engine.qualityLabel(quality),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }
}
