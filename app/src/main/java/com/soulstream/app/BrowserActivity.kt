package com.soulstream.app

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.soulstream.app.data.Prefs
import com.soulstream.app.engine.AdBlock
import com.soulstream.app.engine.Engine
import com.soulstream.app.engine.ImageSaver
import com.soulstream.app.engine.Session
import com.soulstream.app.service.DownloadService
import com.soulstream.app.ui.components.GhostButton
import com.soulstream.app.ui.components.GlowButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.QualitySheet
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.NeonAmber
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.SoulTheme
import com.soulstream.app.ui.theme.Surface1
import com.soulstream.app.ui.theme.Surface2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.ByteArrayInputStream
import java.util.Collections
import java.util.LinkedHashSet

/**
 * SnapTube-style browser with Aloha-style media detection.
 *
 * The WebView is hosted with Compose's AndroidView inside a normal Column
 * (toolbar / page / bottom bar) - it is NOT covered by a full-screen Compose
 * overlay any more. Overlapping a hardware-accelerated WebView with a second
 * surface is what makes pages paint blank on many devices, so the page now has
 * its own slot and always renders.
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        private const val HOME_URL = "https://www.google.com"

        // A real Chrome-mobile UA. Many sites serve a broken/blank page to the
        // default WebView UA (the one with "; wv)"), which is another cause of
        // "page opens but nothing shows".
        private const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val sniffedVideos: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet<String>())
        val sniffedImages: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet<String>())

        private val MEDIA_HINTS = listOf(
            ".mp4", ".webm", ".m3u8", ".mpd", ".mov", ".m4v", ".mkv", ".ts",
            "videoplayback", "googlevideo.com", ".mp3", ".m4a"
        )
        private val IMAGE_HINTS = listOf(".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp", ".avif")

        fun noteMedia(url: String?) {
            if (url.isNullOrBlank()) return
            val low = url.lowercase()
            if (!low.startsWith("http")) return
            if (MEDIA_HINTS.any { low.contains(it) }) sniffedVideos.add(url)
        }

        fun noteImage(url: String?) {
            if (url.isNullOrBlank()) return
            val low = url.lowercase()
            if (!low.startsWith("http")) return
            if (IMAGE_HINTS.any { low.substringBefore("?").endsWith(it) }) sniffedImages.add(url)
        }
    }

    private var address by mutableStateOf(HOME_URL)
    private var pageTitle by mutableStateOf("")
    private var pageUrl by mutableStateOf(HOME_URL)
    private var loading by mutableStateOf(true)
    private var pageProgress by mutableStateOf(0f)
    private var blocked by mutableStateOf(true)
    private var videos by mutableStateOf<List<String>>(emptyList())
    private var images by mutableStateOf<List<String>>(emptyList())
    private var showQuality by mutableStateOf(false)
    private var showGrab by mutableStateOf(false)
    private var pendingUrl by mutableStateOf("")
    private var pageBlank by mutableStateOf(false)
    private var softwareMode by mutableStateOf(false)
    private var mediaDismissed by mutableStateOf(false)

    private var webView: WebView? = null
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val scanJs = """
        (function(){
          var v=[], i=[];
          function addV(u){ if(u && u.indexOf('blob:')!==0 && u.indexOf('data:')!==0) v.push(u); }
          document.querySelectorAll('video').forEach(function(el){
            addV(el.src); addV(el.currentSrc);
            var ss=el.querySelectorAll('source');
            for(var k=0;k<ss.length;k++){ addV(ss[k].src); }
          });
          document.querySelectorAll('meta[property="og:video"],meta[property="og:video:url"],meta[property="og:video:secure_url"],meta[name="twitter:player:stream"]').forEach(function(m){ addV(m.content); });
          document.querySelectorAll('iframe').forEach(function(f){ addV(f.src); });
          document.querySelectorAll('img').forEach(function(im){
            var u = im.currentSrc || im.src;
            if(u && im.naturalWidth >= 200 && im.naturalHeight >= 200) i.push(u);
          });
          document.querySelectorAll('meta[property="og:image"],meta[name="twitter:image"]').forEach(function(m){ if(m.content) i.push(m.content); });
          return JSON.stringify({v:v, i:i});
        })()
    """.trimIndent()

    override fun onPause() {
        super.onPause()
        Session.flush()
    }

    override fun onResume() {
        super.onResume()
        blocked = Prefs.adBlock(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val startUrl = intent?.getStringExtra("url") ?: HOME_URL
        address = startUrl
        pageUrl = startUrl
        blocked = Prefs.adBlock(this)

        setContent {
            SoulTheme(accentIndex = Prefs.accent(this)) {
                BrowserScreen(startUrl)
            }
        }
    }

    override fun onDestroy() {
        uiScope.cancel()
        try {
            webView?.stopLoading()
            webView?.destroy()
        } catch (e: Throwable) {
            // ignore
        }
        webView = null
        super.onDestroy()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun buildWebView(): WebView {
        val wv = WebView(this)
        wv.setBackgroundColor(AndroidColor.WHITE)
        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            mediaPlaybackRequiresUserGesture = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            userAgentString = MOBILE_UA
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        }
        Session.prepare(wv)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                loading = true
                sniffedVideos.clear()
                sniffedImages.clear()
                url?.let {
                    pageUrl = it
                    address = it
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loading = false
                url?.let {
                    pageUrl = it
                    address = it
                }
                pageTitle = view?.title ?: ""
                view?.evaluateJavascript(
                    "(document.body?document.body.scrollHeight:0)"
                ) { h ->
                    val v = h?.trim('"')?.toFloatOrNull() ?: 0f
                    pageBlank = v < 60f
                }
                if (blocked) {
                    view?.evaluateJavascript(AdBlock.cssInjection()) { }
                    view?.evaluateJavascript(AdBlock.domGuardJs()) { }
                }
                if (url != null && AdBlock.isYoutube(url)) {
                    view?.evaluateJavascript(AdBlock.youtubeAdFreeJs()) { }
                }
                view?.evaluateJavascript(scanJs) { result ->
                    try {
                        val raw = JSONTokener(result).nextValue()
                        val json = if (raw is String) raw else result
                        val obj = JSONObject(json)
                        val vArr: JSONArray = obj.optJSONArray("v") ?: JSONArray()
                        val iArr: JSONArray = obj.optJSONArray("i") ?: JSONArray()
                        for (i in 0 until vArr.length()) noteMedia(vArr.optString(i))
                        for (i in 0 until iArr.length()) noteImage(iArr.optString(i))
                    } catch (e: Exception) {
                        // nothing to grab on this page
                    }
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val u = request?.url?.toString() ?: return null
                val mainFrame = request?.isForMainFrame == true
                if (!mainFrame && blocked && AdBlock.isBlocked(u)) {
                    return WebResourceResponse(
                        "text/plain",
                        "utf-8",
                        ByteArrayInputStream(ByteArray(0))
                    )
                }
                noteMedia(u)
                noteImage(u)
                return null
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                pageProgress = newProgress / 100f
                loading = newProgress < 100
            }

            // Block pop-ups / pop-unders outright.
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean = false
        }

        return wv
    }

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

    private fun load(target: String) {
        address = target
        webView?.loadUrl(target)
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

    /**
     * Smart pick: an image link is saved straight to the gallery, everything
     * else goes through the quality picker. This is the fix for "it shows video
     * and audio formats for an image and then fails".
     */
    private fun pick(url: String) {
        uiScope.launch {
            val isImage = withContext(Dispatchers.IO) {
                Engine.looksLikeImageUrl(url) ||
                    Engine.probeIsImage(url, CookieManager.getInstance().getCookie(url))
            }
            if (isImage) {
                val msg = withContext(Dispatchers.IO) { ImageSaver.save(this@BrowserActivity, url) }
                Toast.makeText(this@BrowserActivity, msg, Toast.LENGTH_SHORT).show()
                return@launch
            }
            if (Prefs.askQuality(this@BrowserActivity)) {
                pendingUrl = url
                showQuality = true
            } else {
                startIt(url, Prefs.defaultQuality(this@BrowserActivity))
            }
        }
    }

    /** Turns the current YouTube page into its ad-free mirror twin. */
    private fun youtubeMirror(url: String): String {
        val id = Regex("(?:v=|/shorts/|/embed/|youtu\\.be/)([A-Za-z0-9_-]{6,})")
            .find(url)?.groupValues?.getOrNull(1)
        return if (id != null) {
            AdBlock.AD_FREE_YOUTUBE.first() + "/watch?v=" + id
        } else {
            AdBlock.AD_FREE_YOUTUBE.first()
        }
    }

    @Composable
    private fun BrowserScreen(startUrl: String) {
        val ctx = LocalContext.current
        val accentA = MaterialTheme.colorScheme.primary
        val accentB = MaterialTheme.colorScheme.secondary
        val scope = androidx.compose.runtime.rememberCoroutineScope()

        LaunchedEffect(Unit) {
            while (true) {
                val v = sniffedVideos.toList()
                val i = sniffedImages.toList()
                if (v.size != videos.size) videos = v
                if (i.size != images.size) images = i
                delay(1100)
            }
        }

        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // ---------------- top toolbar ----------------
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Surface1.copy(alpha = 0.98f))
                ) {
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
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = accentA)
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = ImeAction.Go
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onGo = { load(toUrl(address)) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentA,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                focusedContainerColor = Surface2.copy(alpha = 0.5f),
                                unfocusedContainerColor = Surface2.copy(alpha = 0.5f),
                                focusedTextColor = OnDark,
                                unfocusedTextColor = OnDark,
                                cursorColor = accentA
                            )
                        )
                        IconButton(onClick = { webView?.reload() }) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Reload", tint = accentA)
                        }
                    }

                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }) {
                            Icon(Icons.Rounded.ArrowBack, contentDescription = "Back", tint = Muted)
                        }
                        IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }) {
                            Icon(Icons.Rounded.ArrowForward, contentDescription = "Forward", tint = Muted)
                        }
                        IconButton(onClick = {
                            sniffedVideos.clear()
                            sniffedImages.clear()
                            videos = emptyList()
                            images = emptyList()
                            load(HOME_URL)
                        }) {
                            Icon(Icons.Rounded.Home, contentDescription = "Home", tint = Muted)
                        }
                        RenderChip(softwareMode, accentA) {
                            softwareMode = !softwareMode
                            webView?.setLayerType(
                                if (softwareMode) View.LAYER_TYPE_SOFTWARE
                                else View.LAYER_TYPE_HARDWARE,
                                null
                            )
                            webView?.reload()
                        }
                        Spacer(Modifier.weight(1f))
                        if (AdBlock.isYoutube(pageUrl)) {
                            YtChip(accentA) { load(youtubeMirror(pageUrl)) }
                            Spacer(Modifier.width(6.dp))
                        }
                        AdChip(blocked, accentA) {
                            blocked = !blocked
                            Prefs.setAdBlock(ctx, blocked)
                            Toast.makeText(
                                ctx,
                                if (blocked) "Ad blocking on" else "Ad blocking off",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    if (loading) {
                        LinearProgressIndicator(
                            progress = { pageProgress },
                            modifier = Modifier.fillMaxWidth(),
                            color = accentA,
                            trackColor = Surface2
                        )
                    }
                }

                // ---------------- the page (its own slot) ----------------
                AndroidView(
                    factory = {
                        val wv = buildWebView()
                        webView = wv
                        wv.loadUrl(startUrl)
                        wv
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                )

                if (pageBlank) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(NeonAmber.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Page blank? tap Fix for software rendering",
                            color = NeonAmber,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f)
                        )
                        GhostButton(text = "Fix", accent = accentA) {
                            softwareMode = true
                            webView?.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            pageBlank = false
                            webView?.reload()
                        }
                    }
                }
            }

            // Floating controls - small, so the page is never covered by a big
            // "download" panel.
            if ((videos.isNotEmpty() || images.isNotEmpty()) && !mediaDismissed) {
                MediaChip(
                    videoCount = videos.size,
                    imageCount = images.size,
                    accentA = accentA,
                    accentB = accentB,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 12.dp, bottom = 84.dp),
                    onOpen = { showGrab = true },
                    onClose = { mediaDismissed = true }
                )
            }
            DownloadFab(
                accentA = accentA,
                accentB = accentB,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            ) { pick(pageUrl) }

            if (showGrab) {
                GrabSheet(
                    pageTitle = pageTitle.ifBlank { pageUrl },
                    pageUrl = pageUrl,
                    videos = videos,
                    images = images,
                    accentA = accentA,
                    accentB = accentB,
                    onDismiss = { showGrab = false },
                    onPickPage = { t ->
                        showGrab = false
                        pick(t)
                    },
                    onPickVideo = { t ->
                        showGrab = false
                        pick(t)
                    },
                    onPickImage = { img ->
                        showGrab = false
                        scope.launch {
                            val msg = withContext(Dispatchers.IO) { ImageSaver.save(ctx, img) }
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
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

@Composable
private fun YtChip(accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text("AD-FREE", color = accent, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AdChip(on: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) accent.copy(alpha = 0.16f) else Surface2.copy(alpha = 0.5f))
            .border(
                1.dp,
                if (on) accent.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline,
                RoundedCornerShape(50)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Security,
            contentDescription = null,
            tint = if (on) accent else Muted,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            if (on) "ADS OFF" else "ADS ON",
            color = if (on) accent else Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

@Composable
private fun MediaChip(
    videoCount: Int,
    imageCount: Int,
    accentA: Color,
    accentB: Color,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentA.copy(alpha = 0.94f), accentB.copy(alpha = 0.94f))
                )
            )
            .clickable(onClick = onOpen)
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color(0xFF03060E), modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            buildString {
                append("$videoCount v")
                if (imageCount > 0) append(" + $imageCount img")
                append(" - grab")
            },
            color = Color(0xFF03060E),
            style = MaterialTheme.typography.labelSmall
        )
        IconButton(onClick = onClose, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Hide", tint = Color(0xFF03060E), modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun DownloadFab(
    accentA: Color,
    accentB: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(accentA, accentB)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Download, contentDescription = null, tint = Color(0xFF03060E), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Download", color = Color(0xFF03060E), style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun RenderChip(software: Boolean, accent: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Surface2.copy(alpha = 0.5f))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(if (software) "SW" else "HW", color = accent, style = MaterialTheme.typography.labelSmall)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GrabSheet(
    pageTitle: String,
    pageUrl: String,
    videos: List<String>,
    images: List<String>,
    accentA: Color,
    accentB: Color,
    onDismiss: () -> Unit,
    onPickPage: (String) -> Unit,
    onPickVideo: (String) -> Unit,
    onPickImage: (String) -> Unit
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
            Text("GRAB FROM THIS PAGE", style = MaterialTheme.typography.titleLarge, color = OnDark)
            Spacer(Modifier.height(4.dp))
            Text(
                "Page grab is the most reliable. Videos download; images save straight to your gallery.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted
            )
            Spacer(Modifier.height(14.dp))

            PopIn {
                GrabRow(
                    icon = Icons.Rounded.Language,
                    title = "This page - auto detect",
                    subtitle = pageTitle,
                    accent = accentA
                ) { onPickPage(pageUrl) }
            }

            videos.take(8).forEachIndexed { i, u ->
                PopIn(delayMillis = 40 * (i + 1)) {
                    GrabRow(
                        icon = Icons.Rounded.Movie,
                        title = "Video file",
                        subtitle = u,
                        accent = accentA
                    ) { onPickVideo(u) }
                }
            }

            if (images.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text("IMAGES", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(8.dp))
                images.take(12).forEachIndexed { i, u ->
                    PopIn(delayMillis = 40 * (i + 1)) {
                        GrabRow(
                            icon = Icons.Rounded.Image,
                            title = "Save image to gallery",
                            subtitle = u,
                            accent = accentB
                        ) { onPickImage(u) }
                    }
                }
            }
        }
    }
}

@Composable
private fun GrabRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    NeonCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        accent = accent
    ) {
        Row(
            Modifier.clickable(onClick = onClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
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
            Icon(Icons.Rounded.Download, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
    }
}
