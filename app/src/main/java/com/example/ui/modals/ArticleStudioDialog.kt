package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.UserProfile
import com.example.ui.components.MarkdownArticleBody
import com.example.ui.components.estimateReadingStats
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DmSansFontFamily
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer
import com.example.viewmodel.DraftToRestore

@Composable
fun ArticleStudioDialog(
    userProfile: UserProfile,
    restoredDraft: DraftToRestore?,
    onConsumeRestoredDraft: () -> Unit,
    onDismiss: () -> Unit,
    onPublish: (title: String, subtitle: String, body: String, mediaResKey: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var previewMode by remember { mutableStateOf(false) }
    var attachCover by remember { mutableStateOf(true) }

    LaunchedEffect(restoredDraft) {
        if (restoredDraft != null && restoredDraft.isLongForm) {
            title = restoredDraft.articleTitle.orEmpty()
            subtitle = restoredDraft.articleSubtitle.orEmpty()
            body = restoredDraft.content
            attachCover = restoredDraft.mediaResKey != null
            onConsumeRestoredDraft()
        }
    }

    val (readMins, words) = remember(body) { estimateReadingStats(body) }
    val canPublish = title.isNotBlank() && body.isNotBlank() && body.length <= 25_000

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime)),
            color = OledBlack
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close article studio", tint = TextPrimary)
                        }
                        Column {
                            Text("Article Studio", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                            Text(
                                text = "$words words · $readMins min read · Max 25K",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = XBlue
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { previewMode = !previewMode }) {
                            Icon(
                                imageVector = if (previewMode) Icons.Filled.Edit else Icons.Filled.Visibility,
                                contentDescription = if (previewMode) "Edit mode" else "Preview mode",
                                tint = if (previewMode) XBlue else TextMuted
                            )
                        }
                        Button(
                            onClick = {
                                onPublish(
                                    title,
                                    subtitle,
                                    body,
                                    if (attachCover) "article_cover_1" else null
                                )
                            },
                            enabled = canPublish,
                            colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("publish_article_btn")
                        ) {
                            Text("Publish")
                        }
                    }
                }
                HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)

                // Rich Markdown Formatting Toolbar
                if (!previewMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SurfaceCard)
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val insertBlock: (String) -> Unit = { snippet ->
                            body = if (body.isEmpty() || body.endsWith("\n")) "$body$snippet" else "$body\n$snippet"
                        }
                        FormatChip("H1") { insertBlock("# Heading\n") }
                        FormatChip("H2") { insertBlock("## Subheading\n") }
                        FormatChip("Bold") { body = "$body **bold text** " }
                        FormatChip("Italic") { body = "$body *italic text* " }
                        FormatChip("Code") { insertBlock("```kotlin\n// code block\n```\n") }
                        FormatChip("Quote") { insertBlock("> Key insight or quote\n") }
                        FormatChip("List") { insertBlock("- List item\n") }
                        TextButton(onClick = { attachCover = !attachCover }) {
                            Text(
                                text = if (attachCover) "Cover Art ✓" else "+ Cover Art",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (attachCover) XBlue else TextMuted
                            )
                        }
                    }
                    HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (previewMode) {
                        Text(
                            text = title.ifBlank { "Untitled Article" },
                            style = MaterialTheme.typography.displaySmall,
                            color = TextPrimary
                        )
                        if (subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(subtitle, style = MaterialTheme.typography.bodyLarge, color = TextMuted)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        MarkdownArticleBody(
                            markdown = body.ifBlank { "Start writing in Edit mode to preview rich formatting." },
                            fontFamily = DmSansFontFamily,
                            fontSizeSp = 17f
                        )
                    } else {
                        TextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = {
                                Text(
                                    "Article Title",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TextMuted
                                )
                            },
                            textStyle = MaterialTheme.typography.headlineSmall,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = XBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("article_title_input")
                        )
                        TextField(
                            value = subtitle,
                            onValueChange = { subtitle = it },
                            placeholder = { Text("Add an optional subtitle or summary…", color = TextMuted) },
                            textStyle = MaterialTheme.typography.bodyLarge,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = XBlue
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 8.dp))
                        TextField(
                            value = body,
                            onValueChange = { body = it },
                            placeholder = {
                                Text(
                                    "Write your long-form article using Markdown (# Headings, **bold**, > quotes, ```code```)…",
                                    color = TextMuted
                                )
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(lineHeight = 24.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                cursorColor = XBlue
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("article_body_input")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FormatChip(label: String, onClick: () -> Unit) {
    FilterChip(
        selected = false,
        onClick = onClick,
        label = { Text(label, style = MonoTabularStyle.copy(fontSize = 11.5.sp)) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceElevated,
            labelColor = TextPrimary
        ),
        border = BorderStroke(1.dp, BorderSubtle)
    )
}
