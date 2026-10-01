package com.lonetube.app.ui

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lonetube.app.LoneTubeApp
import com.lonetube.app.data.ActiveJob
import com.lonetube.app.data.LiveDownloads
import com.lonetube.app.data.Prefs
import com.lonetube.app.engine.Engine
import com.lonetube.app.ui.components.GlassCard
import com.lonetube.app.ui.components.GradientButton
import com.lonetube.app.ui.components.PulseDot
import com.lonetube.app.ui.components.QualitySheet
import com.lonetube.app.ui.components.RingProgress
import com.lonetube.app.ui.components.SectionLabel
import com.lonetube.app.ui.components.SoftButton
import com.lonetube.app.ui.theme.Amber
import com.lonetube.app.ui.theme.Cyan
import com.lonetube.app.ui.theme.Hairline
import com.lonetube.app.ui.theme.Lime
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark
import com.lonetube.app.ui.theme.Pink
import com.lonetube.app.ui.theme.Surface1
import com.lonetube.app.ui.theme.Surface2
import com.lonetube.app.ui.theme.Violet
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

private data class Site(val name: String, val glyph: String, val url: String)

private val SITES = listOf(
    Site("YouTube", "▶", "https://m.youtube.com"),
    Site("Instagram", "◎", "https://www.instagram.com"),
    Site("Facebook", "f", "https://m.facebook.com"),
    Site("TikTok", "♪", "https://www.tiktok.com"),
    Site("X", "✕", "https://x.com"),
    Site("Pinterest", "P", "https://www.pinterest.com"),
    Site("Reddit", "R", "https://www.reddit.com"),
    Site("Snapchat", "◕", "https://www.snapchat.com")
)

@Composable
fun HomeScreen(
    onOpenBrowser: (String) -> Unit,
    onStartDownload: (String, Int) -> Unit
) {
    val ctx = LocalContext.current
    var url by remember { mutableStateOf("") }
    var engineReady by remember { mutableStateOf(LoneTubeApp.engineReady) }
    var showSheet by remember { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    var jobs by remember { mutableStateOf<List<ActiveJob>>(emptyList()) }
    val appear = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
        while (!engineReady) {
            engineReady = LoneTubeApp.engineReady
            if (!engineReady) delay(400)
        }
    }
    LaunchedEffect(Unit) {
        LiveDownloads.jobs.collectLatest { jobs = it }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp)
                .padding(top = 16.dp, bottom = 24.dp)
                .graphicsLayer {
                    alpha = appear.value
                    translationY = (1f - appear.value) * 34f
                }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Brush.linearGradient(listOf(Violet, Cyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "LoneTube",
                        style = MaterialTheme.typography.headlineMedium,
                        color = OnDark
                    )
                    Text(
                        "Download anything. Beautifully.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Muted
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            EngineChip(engineReady)
            Spacer(Modifier.height(18.dp))

            GlassCard(Modifier.fillMaxWidth()) {
                Text("PASTE A LINK", style = MaterialTheme.typography.labelSmall, color = Muted)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text("https://youtube.com/...  or type to search", color = Muted)
                    },
                    maxLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Cyan,
                        unfocusedBorderColor = Hairline,
                        focusedContainerColor = Surface2.copy(alpha = 0.35f),
                        unfocusedContainerColor = Surface2.copy(alpha = 0.35f),
                        focusedTextColor = OnDark,
                        unfocusedTextColor = OnDark,
                        cursorColor = Cyan
                    ),
                    trailingIcon = {
                        IconButton(onClick = { pasteClipboard(ctx) { url = it } }) {
                            Icon(Icons.Rounded.ContentPaste, contentDescription = "Paste", tint = Cyan)
                        }
                    }
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton(
                        text = "Paste",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.ContentPaste
                    ) { pasteClipboard(ctx) { url = it } }
                    SoftButton(
                        text = "Browse",
                        modifier = Modifier.weight(1f),
                        icon = Icons.Rounded.Language
                    ) { onOpenBrowser("https://m.youtube.com") }
                }
                Spacer(Modifier.height(10.dp))
                GradientButton(
                    text = "Download",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.Download,
                    enabled = engineReady
                ) {
                    val input = url.trim()
                    when {
                        input.isEmpty() -> toast(ctx, "Paste a link first")
                        input.startsWith("http") -> {
                            if (Prefs.askQuality(ctx)) {
                                pendingUrl = input
                                showSheet = true
                            } else {
                                onStartDownload(input, Prefs.defaultQuality(ctx))
                            }
                        }
                        else -> onOpenBrowser(
                            "https://www.youtube.com/results?search_query=" + Uri.encode(input)
                        )
                    }
                }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Quick picks")
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Engine.QUALITIES.forEach { q ->
                    QuickChip(q.title, q.isAudio) {
                        val input = url.trim()
                        if (input.startsWith("http")) onStartDownload(input, q.index)
                        else toast(ctx, "Paste a link first")
                    }
                }
            }

            val running = jobs.filter { it.isRunning }
            if (running.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                SectionLabel("Downloading now")
                running.forEach { job -> LiveJobCard(job) }
            }
            val finished = jobs.filter { !it.isRunning }
            if (finished.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                finished.forEach { job -> FinishedJobRow(job) }
                Spacer(Modifier.height(6.dp))
                SoftButton(
                    text = "Clear finished",
                    modifier = Modifier.fillMaxWidth(),
                    icon = Icons.Rounded.ClearAll
                ) { LiveDownloads.clearFinished() }
            }

            Spacer(Modifier.height(22.dp))
            SectionLabel("Quick open")
            SITES.chunked(4).forEach { row ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { site ->
                        SiteTile(site, Modifier.weight(1f)) { onOpenBrowser(site.url) }
                    }
                    repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(10.dp))
            }
        }

        if (showSheet) {
            QualitySheet(
                onDismiss = { showSheet = false },
                onPick = { q ->
                    showSheet = false
                    onStartDownload(pendingUrl, q)
                }
            )
        }
    }
}

@Composable
private fun EngineChip(ready: Boolean) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Surface1.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PulseDot(if (ready) Lime else Amber)
        Spacer(Modifier.width(8.dp))
        Text(
            if (ready) "Engine ready - turbo downloads on" else "Waking up the engine...",
            color = if (ready) OnDark else Muted,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun QuickChip(text: String, audio: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "chip"
    )
    Row(
        Modifier
            .scale(scale)
            .clip(RoundedCornerShape(50))
            .background(if (audio) Pink.copy(alpha = 0.16f) else Violet.copy(alpha = 0.16f))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (audio) Icons.Rounded.MusicNote else Icons.Rounded.HighQuality,
            contentDescription = null,
            tint = if (audio) Pink else Violet,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(text, color = OnDark, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun LiveJobCard(job: ActiveJob) {
    GlassCard(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RingProgress(progress = job.progress / 100f)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    job.title,
                    color = OnDark,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    if (job.status == ActiveJob.Status.PREPARING) "Fetching details..."
                    else "${job.progress}% - downloading",
                    color = Cyan,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun FinishedJobRow(job: ActiveJob) {
    val ok = job.status == ActiveJob.Status.DONE
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (ok) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
            contentDescription = null,
            tint = if (ok) Lime else MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            job.title,
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { LiveDownloads.remove(job.id) }) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Dismiss",
                tint = Muted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SiteTile(site: Site, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
        label = "tile"
    )
    Column(
        modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface1.copy(alpha = 0.65f))
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(site.glyph, fontSize = 20.sp, color = Cyan, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            site.name,
            color = OnDark,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun pasteClipboard(ctx: Context, onText: (String) -> Unit) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val text = cm.primaryClip?.getItemAt(0)?.text?.toString()
    if (!text.isNullOrBlank()) onText(text.trim()) else toast(ctx, "Clipboard is empty")
}

private fun toast(ctx: Context, msg: String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}
