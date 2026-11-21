package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.PostItem
import com.example.ui.components.formatTimeClock
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerContainer
import com.example.ui.theme.DangerUndo
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.EmeraldContainer
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

@Composable
fun EditPostDialog(
    post: PostItem,
    onDismiss: () -> Unit,
    onSave: (postId: String, newContent: String, reason: String) -> Unit
) {
    var editedText by remember(post.id) { mutableStateOf(post.content) }
    var reason by remember { mutableStateOf("") }
    val hasChanged = editedText.trim().isNotEmpty() && editedText.trim() != post.content.trim()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, HairlineDivider),
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, null, tint = XBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Post", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
                Text(
                    text = "Unlocked Premium editing · Past versions are preserved in edit history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = editedText,
                    onValueChange = { editedText = it },
                    label = { Text("Post content") },
                    minLines = 4,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_post_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Edit note (optional)") },
                    placeholder = { Text("e.g. Fixed typo, added metric", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(post.id, editedText, reason) },
                        enabled = hasChanged,
                        colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("save_post_edit_button")
                    ) {
                        Text("Save Revision")
                    }
                }
            }
        }
    }
}

@Composable
fun EditHistoryDialog(
    post: PostItem,
    onDismiss: () -> Unit
) {
    // Build full chronological versions (latest first)
    val versions = remember(post) {
        val currentEntry = Triple(
            "Current Version",
            post.editedAtMillis ?: post.createdAtMillis,
            post.content
        )
        val pastEntries = post.revisions.reversed().mapIndexed { idx, rev ->
            Triple(
                "Revision ${post.revisions.size - idx} · ${rev.reason}",
                rev.editedAtMillis,
                rev.content
            )
        }
        listOf(currentEntry) + pastEntries
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, HairlineDivider),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.History, null, tint = XBlue, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit History & Diff", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close history", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    versions.forEachIndexed { idx, (label, timestamp, text) ->
                        val previousVersionText = versions.getOrNull(idx + 1)?.third
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (idx == 0) XBlue.copy(alpha = 0.5f) else BorderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (idx == 0) XBlue else TextPrimary
                                    )
                                    Text(
                                        text = formatTimeClock(timestamp),
                                        style = MonoTabularStyle.copy(fontSize = 10.5.sp),
                                        color = TextMuted
                                    )
                                }
                                HorizontalDivider(
                                    color = BorderSubtle,
                                    thickness = 0.8.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                                if (previousVersionText != null) {
                                    Text(
                                        text = buildWordDiff(previousVersionText, text),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                } else {
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary),
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

private fun buildWordDiff(oldText: String, newText: String) = buildAnnotatedString {
    val oldWords = oldText.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val newWords = newText.split(Regex("\\s+")).filter { it.isNotEmpty() }
    val oldSet = oldWords.toSet()
    val newSet = newWords.toSet()

    val removed = oldWords.filter { it !in newSet }
    if (removed.isNotEmpty()) {
        removed.forEach { word ->
            withStyle(
                SpanStyle(
                    color = DangerUndo,
                    background = DangerContainer,
                    textDecoration = TextDecoration.LineThrough
                )
            ) {
                append("-$word ")
            }
        }
        append("\n")
    }
    newWords.forEachIndexed { idx, word ->
        if (word !in oldSet) {
            withStyle(
                SpanStyle(
                    color = EmeraldAdShield,
                    background = EmeraldContainer,
                    fontWeight = FontWeight.SemiBold
                )
            ) {
                append(word)
            }
        } else {
            append(word)
        }
        if (idx < newWords.lastIndex) append(" ")
    }
}
