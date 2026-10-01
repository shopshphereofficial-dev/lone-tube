package com.lonetube.app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lonetube.app.engine.Engine
import com.lonetube.app.service.DownloadService
import com.lonetube.app.ui.components.GlassCard
import com.lonetube.app.ui.components.QualityRow
import com.lonetube.app.ui.theme.LoneTubeTheme
import com.lonetube.app.ui.theme.Muted
import com.lonetube.app.ui.theme.OnDark

/** The little popup shown when you tap Share -> LoneTube from another app. */
class ShareDialogActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)
        val url = Regex("https?://\\S+").find(text ?: "")?.value
        if (url == null) {
            Toast.makeText(this, "No link found in the shared text", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            LoneTubeTheme {
                var picked by remember { mutableStateOf(false) }

                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                        .systemBarsPadding(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    GlassCard(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Download with LoneTube",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = OnDark
                                )
                                Spacer(Modifier.height(3.dp))
                                Text(
                                    url,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Muted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = { finish() }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cancel", tint = Muted)
                            }
                        }
                        Spacer(Modifier.height(12.dp))

                        Column(
                            Modifier
                                .height(330.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Engine.QUALITIES.forEach { q ->
                                QualityRow(q) {
                                    if (!picked) {
                                        picked = true
                                        startIt(url, q.index)
                                    }
                                }
                            }
                        }
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
