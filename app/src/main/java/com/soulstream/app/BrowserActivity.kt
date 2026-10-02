package com.soulstream.app

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.soulstream.app.ui.components.GlowButton
import com.soulstream.app.ui.components.NeonCard
import com.soulstream.app.ui.components.PopIn
import com.soulstream.app.ui.components.QualitySheet
import com.soulstream.app.ui.theme.Muted
import com.soulstream.app.ui.theme.OnDark
import com.soulstream.app.ui.theme.SoulTheme
import com.soulstream.app.ui.theme.Surface1
import com.soulstream.app.ui.theme.Surface2
import kotlinx.coroutines.Dispatchers
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
 * SnapTube-style browser with Aloha-style detection.
 *
 * LAYOUT NOTE: the WebView is an Android interop view and Compose draws over
 * interop views, so the page rectangle must stay free of Compose-painted
 * backgrounds. Only the top strip and bottom strip paint anything here.
 */
class BrowserActivity : ComponentActivity() {

    companion object {
        private const val HOME_URL = "https://www.google.com"

        val sniffedVideos: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet<String>())
        val sniffedImages: MutableSet<String> = Collections.synchronizedSet(LinkedHashSet<String>())

        private val MEDIA_HINTS = listOf(
            ".mp4", ".webm", ".m3u8", ".mpd", ".mov", ".m4v", ".mkv", ".ts",
            "videoplayback", "googlevideo.com", ".mp3", ".m4a"
        )
        private val IMAGE_HINTS = listOf(
            ".jpg", ".jpeg", ".png", ".webp", ".gif", ".bmp"
        )

        fun noteMedia(url: String?) {
            if (url.isNullOrBlank()) return
            val low = url.lowercase()
            if (!low.startsWith("http")) return
            if (low.startsWith("blob:") || low.startsWith("data:")) return
            if (MEDIA_HINTS.any { low.contains(it) }) sniffedVideos.add(url)
        }

        fun noteImage(url: String?) {
            if (url.isNullOrBlank()) return
            val low = url.lowercase()
            if (!low.startsWith("http")) return
            if (low.startsWith("data:")) return
            if (IMAGE_HINTS.any { low.substringBefore("?").endsWith(it) }) sniffedImages.add(url)
        }
    }

    private var adBlockOn = true

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
            if(u && im.naturalWidth >= 220 && im.naturalHeight >= 220) i.push(u);
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
        adBlockOn = Prefs.adBlock(this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        adBlockOn = Prefs.adBlock(this)
        val startUrl = intent?.getStringExtra("url") ?: HOME_URL

        setContent {
            SoulTheme(accentIndex = Prefs.accent(this)) {
                val ctx = LocalContext.current
                val scope = rememberCoroutineScope()
                var webView by remember { mutableStateOf<WebView?>(null) }
                var address by remember { mutableStateOf(startUrl) }
                var currentUrl by remember { mutableStateOf(startUrl) }
                var progress by remember { mutableStateOf(0f) }
                var loading by remember { mutableStateOf(true) }
                var showQuality by remember { mutableStateOf(false) }
                var showGrab by remember { mutableStateOf(false) }
                var pendingUrl by remember { mutableStateOf("") }
                var blocked by remember { mutableStateOf(adBlockOn) }
                var videos by remember { mutableStateOf<List<String>>(emptyList()) }
                var images by remember { mutableStateOf<List<String>>(emptyList()) }

                val accentA = MaterialTheme.colorScheme.primary
                val accentB = MaterialTheme.colorScheme.secondary

                LaunchedEffect(Unit) {
                    while (true) {
                        val v = sniffedVideos.toList()
                        val i = sniffedImages.toList()
                        if (v.size != videos.size) videos = v
                        if (i.size != images.size) images = i
                        delay(1100)
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .systemBarsPadding()
                ) {
                    // ---- top strip ----
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
                                    onGo = {
                                        val target = toUrl(address)
                                        address = target
                                        webView?.loadUrl(target)
                                    }
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
                                webView?.loadUrl(HOME_URL)
                            }) {
                                Icon(Icons.Rounded.Home, contentDescription = "Home", tint = Muted)
                            }
                            Spacer(Modifier.weight(1f))
                            AdChip(blocked, accentA) {
                                blocked = !blocked
                                Prefs.setAdBlock(ctx, blocked)
                                adBlockOn = blocked
                                Toast.makeText(
                                    ctx,
                                    if (blocked) "Ad blocking on" else "Ad blocking off",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        if (loading) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth(),
                                color = accentA,
                                trackColor = Surface2
                            )
                        }
                    }

                    // ---- page (kept free of Compose overlays) ----
                    AndroidView(
                        factory = { c ->
                            WebView(c).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                setBackgroundColor(AndroidColor.WHITE)
                                settings.javaScriptEnabled = true
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.setSupportZoom(true)
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                Session.prepare(this)

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: android.graphics.Bitmap?
                                    ) {
                                        sniffedVideos.clear()
                                        sniffedImages.clear()
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
                                        if (adBlockOn) {
                                            view?.evaluateJavascript(AdBlock.cssInjection()) { }
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
                                        if (!mainFrame && adBlockOn && AdBlock.isBlocked(u)) {
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

                    // ---- bottom strip ----
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(Surface1.copy(alpha = 0.98f))
                    ) {
                        if (videos.isNotEmpty() || images.isNotEmpty()) {
                            GrabPill(videos.size, images.size, accentA, accentB) { showGrab = true }
                        }
                        GlowButton(
                            text = "DOWNLOAD THIS PAGE",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            icon = Icons.Rounded.Download,
                            enabled = !loading,
                            accentA = accentA,
                            accentB = accentB
                        ) {
                            val target = webView?.url ?: currentUrl
                            if (Prefs.askQuality(ctx)) {
                                pendingUrl = target
                                showQuality = true
                            } else {
                                startIt(target, Prefs.defaultQuality(ctx))
                            }
                        }
                    }
                }

                if (showGrab) {
                    GrabSheet(
                        pageTitle = webView?.title ?: currentUrl,
                        pageUrl = webView?.url ?: currentUrl,
                        videos = videos,
                        images = images,
                        accentA = accentA,
                        accentB = accentB,
                        onDismiss = { showGrab = false },
                        onPickPage = { target ->
                            showGrab = false
                            if (Prefs.askQuality(ctx)) {
                                pendingUrl = target
                                showQuality = true
                            } else {
                                startIt(target, Prefs.defaultQuality(ctx))
                            }
                        },
                        onPickVideo = { target ->
                            showGrab = false
                            if (Prefs.askQuality(ctx)) {
                                pendingUrl = target
                                showQuality = true
                            } else {
                                startIt(target, Prefs.defaultQuality(ctx))
                            }
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
private fun GrabPill(videoCount: Int, imageCount: Int, accentA: Color, accentB: Color, onClick: () -> Unit) {
    val t = rememberInfiniteTransition(label = "pill")
    val pulse by t.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pillPulse"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp)
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(accentA.copy(alpha = 0.28f), accentB.copy(alpha = 0.24f))
                )
            )
            .border(1.dp, accentA.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(10.dp)
                .scale(pulse)
                .clip(RoundedCornerShape(50))
                .background(accentA)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            buildString {
                append("$videoCount video")
                if (videoCount != 1) append("s")
                if (imageCount > 0) {
                    append(" + $imageCount image")
                    if (imageCount != 1) append("s")
                }
                append(" found - tap to grab")
            },
            color = OnDark,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Icon(Icons.Rounded.Bolt, contentDescription = null, tint = accentA, modifier = Modifier.size(18.dp))
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
            Text(
                "GRAB FROM THIS PAGE",
                style = MaterialTheme.typography.titleLarge,
                color = OnDark
            )
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
                    title = "This page - best quality",
                    subtitle = pageTitle.ifBlank { pageUrl },
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
                images.take(8).forEachIndexed { i, u ->
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
