package com.lonetube.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.lonetube.app.data.Prefs
import com.lonetube.app.engine.Engine
import com.lonetube.app.service.DownloadService
import com.lonetube.app.ui.components.AnimatedGradientBackground
import com.lonetube.app.ui.components.GlassCard
import com.lonetube.app.ui.components.GradientButton
import com.lonetube.app.ui.components.QualitySheet
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Hairline
import com.lonetube.app.ui.theme.LoneTubeTheme
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Surface1
import com.lonetube.app.ui.theme.Surface2
import com.lonetube.app.ui.theme.Violet
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONTokener
import java.util.Collections
import java.util.LinkedHashSet

/**
 * A SnapTube-style browser: Google search bar, back/forward/home, and
 * Aloha-style video detection - whenever a page plays a video, a download
 * pill appears so it can be grabbed in one tap.
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        private const val HOME_URL = "https://www.google.com"

        /** Media URLs seen either in the page or on the network. */
        val sniffed: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet<String>())

        private val MEDIA_HINTS = listOf(
            ".mp4", ".webm", ".m3u8", ".mpd", ".mov", ".m4v", ".mkv", ".ts",
            "videoplayback", "googlevideo.com", ".mp3", ".m4a"
        )

        fun note(url: String?) {
            if (url.isNullOrBlank()) return
            val low = url.lowercase()
            if (low.startsWith("blob:") || low.startsWith("data:")) return
            if (!low.startsWith("http")) return
            if (MEDIA_HINTS.any { low.contains(it) }) {
                sniffed.add(url)
            }
        }
    }

    private val scanJs = """
        (function(){
          var out=[];
          function add(u){ if(u && u.indexOf('blob:')!==0 && u.indexOf('data:')!==0) out.push(u); }
          document.querySelectorAll('video').forEach(function(v){
            add(v.src); add(v.currentSrc);
            var ss=v.querySelectorAll('source');
            for(var i=0;i<ss.length;i++){ add(ss[i].src); }
          });
          document.querySelectorAll('meta[property="og:video"],meta[property="og:video:url"],meta[property="og:video:secure_url"],meta[name="twitter:player:stream"]').forEach(function(m){ add(m.content); });
          document.querySelectorAll('iframe').forEach(function(f){ add(f.src); });
          return JSON.stringify(out);
        })()
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val startUrl = intent?.getStringExtra("url") ?: HOME_URL

        setContent {
            LoneTubeTheme {
                var webView by remember { mutableStateOf<WebView?>(null) }
                var address by remember { mutableStateOf(startUrl) }
                var currentUrl by remember { mutableStateOf(startUrl) }
                var progress by remember { mutableStateOf(0f) }
                var loading by remember { mutableStateOf(true) }
                var showQuality by remember { mutableStateOf(false) }
                var showMedia by remember { mutableStateOf(false) }
                var pendingUrl by remember { mutableStateOf("") }
                var detected by remember { mutableStateOf<List<String>>(emptyList()) }

                // Poll the sniffer so network-caught videos appear while browsing
                LaunchedEffect(Unit) {
                    while (true) {
                        val snap = sniffed.toList()
                        if (snap.size != detected.size) detected = snap
                        delay(1200)
                    }
                }

                AnimatedGradientBackground(Modifier.fillMaxSize()) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                    ) {
                        // ---- address / search bar ----
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = OnDark)
                            }
                            OutlinedTextField(
                                value = address,
                                onValueChange = { address = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(20.dp),
                                placeholder = { Text("Search Google or type a link", color = Muted) },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Cyan)
                                },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = ImeAction.Go
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onGo = {
                                        val target = toUrl(address)
                                        address = target
                                        webView?.loadUrl(target)
                                    }
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Cyan,
                                    unfocusedBorderColor = Hairline,
                                    focusedContainerColor = Surface2.copy(alpha = 0.4f),
                                    unfocusedContainerColor = Surface2.copy(alpha = 0.4f),
                                    focusedTextColor = OnDark,
                                    unfocusedTextColor = OnDark,
                                    cursorColor = Cyan
                                )
                            )
                            IconButton(onClick = { webView?.reload() }) {
                                Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = Cyan)
                            }
                        }

                        // ---- nav row ----
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }) {
                                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Muted)
                            }
                            IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }) {
                                Icon(Icons.Rounded.ArrowForward, contentDescription = "Forward", tint = Muted)
                            }
                            IconButton(onClick = {
                                sniffed.clear()
                                detected = emptyList()
                                webView?.loadUrl(HOME_URL)
                            }) {
                                Icon(Icons.Rounded.Home, contentDescription = "Home", tint = Muted)
                            }
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Log in here for private videos",
                                style = MaterialTheme.typography.bodySmall,
                                color = Muted
                            )
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
                                    settings.mediaPlaybackRequiresUserGesture = false

                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: android.graphics.Bitmap?
                                        ) {
                                            sniffed.clear()
                                            url?.let {
                                                currentUrl = it
                                                address = it
                                            }
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            loading = false
                                            url?.let {
                                                currentUrl = it
                                                address = it
                                            }
                                            view?.evaluateJavascript(scanJs) { result ->
                                                try {
                                                    val raw = JSONTokener(result).nextValue()
                                                    val json = if (raw is String) raw else result
                                                    val arr = JSONArray(json)
                                                    for (i in 0 until arr.length()) {
                                                        note(arr.optString(i))
                                                    }
                                                } catch (e: Exception) {
                                                    // page had nothing to offer
                                                }
                                            }
                                        }

                                        override fun shouldInterceptRequest(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): WebResourceResponse? {
                                            note(request?.url?.toString())
                                            return null
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

                        // ---- video detection pill ----
                        if (detected.isNotEmpty()) {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Violet.copy(alpha = 0.22f))
                                    .clickable { showMedia = true }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Movie, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    "${detected.size} video${if (detected.size == 1) "" else "s"} found on this page - tap to download",
                                    color = OnDark,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Rounded.Download, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
                            }
                        }

                        GradientButton(
                            text = "Download this page",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            icon = Icons.Rounded.Download,
                            enabled = !loading
                        ) {
                            val target = webView?.url ?: currentUrl
                            if (Prefs.askQuality(this@BrowserActivity)) {
                                pendingUrl = target
                                showQuality = true
                            } else {
                                startIt(target, Prefs.defaultQuality(this@BrowserActivity))
                            }
                        }
                    }

                    if (showMedia) {
                        MediaSheet(
                            pageTitle = webView?.title ?: currentUrl,
                            pageUrl = webView?.url ?: currentUrl,
                            detected = detected,
                            onDismiss = { showMedia = false },
                            onPick = { target ->
                                showMedia = false
                                if (Prefs.askQuality(this@BrowserActivity)) {
                                    pendingUrl = target
                                    showQuality = true
                                } else {
                                    startIt(target, Prefs.defaultQuality(this@BrowserActivity))
                                }
                            }
                        )
                    }

                    if (showQuality) {
                        QualitySheet(
                            onDismiss = { showQuality = false },
                            onPick = { q ->
                                showQuality = false
                                startIt(pendingUrl, q)
                            }
                        )
                    }
                }
            }
        }
    }

    /** Turns whatever is typed into a URL (search terms go to Google). */
    private fun toUrl(input: String): String {
        val t = input.trim()
        return when {
            t.isEmpty() -> HOME_URL
            t.startsWith("http://") || t.startsWith("https://") -> t
            t.contains(" ") || !t.contains(".") ->
                "https://www.google.com/search?q=" + android.net.Uri.encode(t)
            else -> "https://$t"
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
    }
}

/** Lets the user pick which of the detected videos (or the page) to download. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MediaSheet(
    pageTitle: String,
    pageUrl: String,
    detected: List<String>,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface1
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 26.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Download from this page", style = MaterialTheme.typography.titleLarge, color = OnDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Pick the whole page (most reliable) or a specific video file.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(14.dp))

            MediaRow(
                title = "This page - best quality",
                subtitle = pageTitle.ifBlank { pageUrl },
                recommended = true
            ) { onPick(pageUrl) }

            detected.take(8).forEach { u ->
                MediaRow(
                    title = "Video file",
                    subtitle = u,
                    recommended = false
                ) { onPick(u) }
            }
        }
    }
}

@Composable
private fun MediaRow(
    title: String,
    subtitle: String,
    recommended: Boolean,
    onClick: () -> Unit
) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        Row(
            Modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (recommended) Icons.Rounded.Language else Icons.Rounded.Movie,
                contentDescription = null,
                tint = if (recommended) Cyan else Violet,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = OnDark, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Rounded.Download, contentDescription = null, tint = Cyan, modifier = Modifier.size(18.dp))
        }
    }
}
