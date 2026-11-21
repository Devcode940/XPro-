package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserProfile
import com.example.ui.components.GrokLogoMark
import com.example.ui.components.TopRightMenuButton
import com.example.ui.components.UserAvatar
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerUndo
import com.example.ui.theme.EmeraldAdShield
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue
import com.example.ui.theme.XBlueContainer
import com.example.viewmodel.GrokMessage
import com.example.viewmodel.GrokMode
import com.example.viewmodel.GrokRole
import com.example.viewmodel.GrokUiState

private val GrokQuickPrompts = listOf(
    "Summarize top X timeline posts" to "Give me a sharp briefing of the top posts in my XPro feed right now.",
    "Explain today's trends" to "Break down why today's trending topics on XPro matter.",
    "Draft an X thread" to "Help me write an engaging 4-post X thread about shipping fast on Android.",
    "Fact-check & analyze" to "What are the key takeaways and open questions from the latest engineering threads?"
)

@Composable
fun GrokScreen(
    grokState: GrokUiState,
    userProfile: UserProfile,
    onSendMessage: (String) -> Unit,
    onSelectMode: (GrokMode) -> Unit,
    onToggleFeedContext: (Boolean) -> Unit,
    onRetryLast: () -> Unit,
    onDismissError: () -> Unit,
    onClearChat: () -> Unit,
    onCopiedToast: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenDrawer: () -> Unit = onOpenProfile,
    onOpenSettings: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenArticles: () -> Unit,
    onOpenMediaDownloader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val clipboard = LocalClipboardManager.current

    LaunchedEffect(grokState.messages.size, grokState.messages.lastOrNull()?.text?.length) {
        if (grokState.messages.isNotEmpty()) {
            listState.animateScrollToItem(grokState.messages.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("grok_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .testTag("grok_top_bar"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier.testTag("grok_profile_avatar")
                ) {
                    UserAvatar(
                        name = userProfile.name,
                        isCurrentUser = true,
                        avatarColorHex = 0xFF1D9BF0,
                        size = 32.dp
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                GrokLogoMark(size = 22.dp, tint = colorScheme.onSurface)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Grok",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            color = XBlueContainer,
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, XBlue.copy(alpha = 0.45f))
                        ) {
                            Text(
                                text = "Powered by X",
                                style = MonoTabularStyle.copy(fontSize = 10.sp),
                                color = XBlue,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Text(
                        text = grokState.mode.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (grokState.messages.isNotEmpty()) {
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier.testTag("grok_clear_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "Clear conversation",
                            tint = TextMuted
                        )
                    }
                }
                TopRightMenuButton(
                    onOpenSettings = onOpenSettings,
                    onOpenProfile = onOpenProfile,
                    onOpenBookmarks = onOpenBookmarks,
                    onOpenArticles = onOpenArticles,
                    onOpenMediaDownloader = onOpenMediaDownloader
                )
            }
        }

        // Mode & Context Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 14.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GrokMode.entries.forEach { mode ->
                val selected = grokState.mode == mode
                FilterChip(
                    selected = selected,
                    onClick = { onSelectMode(mode) },
                    label = {
                        Text(
                            text = mode.label,
                            style = MaterialTheme.typography.labelMedium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = XBlueContainer,
                        selectedLabelColor = XBlue,
                        containerColor = SurfaceCard,
                        labelColor = TextMuted
                    ),
                    border = BorderStroke(1.dp, if (selected) XBlue else BorderSubtle),
                    modifier = Modifier.testTag("grok_mode_${mode.name}")
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            FilterChip(
                selected = grokState.attachFeedContext,
                onClick = { onToggleFeedContext(!grokState.attachFeedContext) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = if (grokState.attachFeedContext) EmeraldAdShield else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                },
                label = {
                    Text(
                        text = "X Context",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = EmeraldAdShield.copy(alpha = 0.14f),
                    selectedLabelColor = EmeraldAdShield,
                    containerColor = SurfaceCard,
                    labelColor = TextMuted
                ),
                border = BorderStroke(
                    1.dp,
                    if (grokState.attachFeedContext) EmeraldAdShield.copy(alpha = 0.5f) else BorderSubtle
                ),
                modifier = Modifier.testTag("grok_context_chip")
            )
        }
        HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)

        // Error Banner (surfaces real Firebase AI / App Check / timeout failures)
        grokState.errorMessage?.let { errorMsg ->
            Surface(
                color = DangerUndo.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, DangerUndo.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .testTag("grok_error_card")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = "Grok error",
                        tint = DangerUndo,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Grok request failed",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextPrimary
                        )
                        Text(
                            text = errorMsg,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                    if (grokState.lastFailedPrompt != null) {
                        TextButton(
                            onClick = onRetryLast,
                            modifier = Modifier.testTag("grok_retry_btn")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, tint = XBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Retry", color = XBlue)
                        }
                    }
                    IconButton(onClick = onDismissError) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss error", tint = TextMuted)
                    }
                }
            }
        }

        // Main Conversation / Starter Body
        if (grokState.messages.isEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = SurfaceCard,
                            shape = CircleShape,
                            border = BorderStroke(1.dp, HairlineDivider),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                GrokLogoMark(size = 30.dp)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Ask Grok, Powered by X",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Analyze your live XPro timeline, unpack trending discussions, or draft threads directly inside XPro.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    }
                }

                items(GrokQuickPrompts) { (title, prompt) ->
                    Surface(
                        color = SurfaceCard,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(enabled = !grokState.isGenerating) {
                                onSendMessage(prompt)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = XBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = prompt,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("grok_message_list"),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(grokState.messages, key = { it.id }) { msg ->
                    GrokMessageBubble(
                        message = msg,
                        onCopy = {
                            clipboard.setText(AnnotatedString(msg.text))
                            onCopiedToast()
                        }
                    )
                }
            }
        }

        // Composer Input Bar
        HorizontalDivider(color = colorScheme.outline, thickness = 0.8.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorScheme.background)
                .windowInsetsPadding(
                    WindowInsets.navigationBars
                        .union(WindowInsets.ime)
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = if (grokState.mode == GrokMode.FUN) {
                            "Ask Grok anything (Fun Mode)…"
                        } else {
                            "Ask Grok anything…"
                        },
                        color = colorScheme.onSurfaceVariant
                    )
                },
                maxLines = 4,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        val trimmed = inputText.trim()
                        if (trimmed.isNotEmpty() && !grokState.isGenerating) {
                            inputText = ""
                            onSendMessage(trimmed)
                        }
                    }
                ),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = colorScheme.surfaceContainerHigh,
                    focusedBorderColor = colorScheme.primary,
                    unfocusedBorderColor = colorScheme.outlineVariant,
                    focusedTextColor = colorScheme.onSurface,
                    unfocusedTextColor = colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("grok_prompt_input")
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val trimmed = inputText.trim()
                    if (trimmed.isNotEmpty() && !grokState.isGenerating) {
                        inputText = ""
                        onSendMessage(trimmed)
                    }
                },
                enabled = inputText.isNotBlank() && !grokState.isGenerating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = XBlue,
                    contentColor = TextPrimary,
                    disabledContainerColor = SurfaceElevated,
                    disabledContentColor = TextMuted
                ),
                shape = CircleShape,
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .size(48.dp)
                    .testTag("grok_send_btn")
            ) {
                if (grokState.isGenerating) {
                    CircularProgressIndicator(
                        color = XBlue,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send to Grok",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun GrokMessageBubble(
    message: GrokMessage,
    onCopy: () -> Unit
) {
    val isUser = message.role == GrokRole.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Surface(
                color = XBlueContainer,
                shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
                border = BorderStroke(1.dp, XBlue.copy(alpha = 0.45f)),
                modifier = Modifier.widthIn(max = 310.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        } else {
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
                border = BorderStroke(1.dp, HairlineDivider),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GrokLogoMark(size = 16.dp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Grok · ${message.mode.label}",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = XBlue
                            )
                        }
                        if (!message.isStreaming && message.text.isNotBlank()) {
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = "Copy response",
                                    tint = TextMuted,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else if (message.isStreaming) {
                            CircularProgressIndicator(
                                color = XBlue,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = message.text.ifBlank { "Thinking…" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (message.text.isBlank()) TextMuted else TextPrimary
                    )
                }
            }
        }
    }
}
