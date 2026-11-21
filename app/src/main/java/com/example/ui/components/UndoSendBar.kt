package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PendingPost
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerUndo
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import kotlin.math.ceil

@Composable
fun UndoSendBar(
    pendingPost: PendingPost?,
    onUndo: () -> Unit,
    onSendNow: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = pendingPost != null,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        if (pendingPost != null) {
            val secondsLeft = ceil(pendingPost.remainingMillis / 1000.0).toInt().coerceAtLeast(1)
            val progress = (pendingPost.remainingMillis.toFloat() / pendingPost.totalMillis.coerceAtLeast(1L).toFloat())
                .coerceIn(0f, 1f)

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = SurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, XBlue.copy(alpha = 0.45f)),
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (pendingPost.isLongForm) "Publishing article…" else "Sending post…",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary
                            )
                            Text(
                                text = pendingPost.articleTitle ?: pendingPost.content.take(48),
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = onSendNow,
                            modifier = Modifier.testTag("send_now_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = XBlue,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Send", color = XBlue)
                        }
                        Button(
                            onClick = onUndo,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DangerContainer,
                                contentColor = DangerUndo
                            ),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, DangerUndo.copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("undo_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Undo,
                                contentDescription = "Undo post",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Undo (${secondsLeft}s)",
                                style = MonoTabularStyle.copy(fontSize = 12.sp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = XBlue,
                        trackColor = BorderSubtle
                    )
                }
            }
        }
    }
}
