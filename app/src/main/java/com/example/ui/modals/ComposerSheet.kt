package com.example.ui.modals

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Poll
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.model.PostItem
import com.example.data.model.UserProfile
import com.example.ui.components.UserAvatar
import com.example.ui.components.VerificationBadgeIcon
import com.example.ui.components.resolveBundledMedia
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerUndo
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ComposerDialog(
    userProfile: UserProfile,
    replyTo: PostItem?,
    restoredDraft: DraftToRestore?,
    onConsumeRestoredDraft: () -> Unit,
    onDismiss: () -> Unit,
    onPublish: (
        content: String,
        mediaResKey: String?,
        mediaUri: String?,
        pollOptions: List<String>,
        audience: String,
        parentPostId: String?
    ) -> Unit
) {
    var content by remember { mutableStateOf("") }
    var mediaResKey by remember { mutableStateOf<String?>(null) }
    var mediaUri by remember { mutableStateOf<String?>(null) }
    var showPollBuilder by remember { mutableStateOf(false) }
    val pollOptions = remember { mutableStateListOf("", "") }
    var audience by remember { mutableStateOf("Everyone") }
    var audienceMenuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(restoredDraft) {
        if (restoredDraft != null && !restoredDraft.isLongForm) {
            content = restoredDraft.content
            mediaResKey = restoredDraft.mediaResKey
            mediaUri = restoredDraft.mediaUri
            audience = restoredDraft.audience
            if (restoredDraft.pollOptions.isNotEmpty()) {
                showPollBuilder = true
                pollOptions.clear()
                pollOptions.addAll(restoredDraft.pollOptions)
            }
            onConsumeRestoredDraft()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            mediaUri = uri.toString()
            mediaResKey = null
        }
    }

    val maxChars = if (userProfile.extendedPostsEnabled) 25_000 else 280
    val charProgress = (content.length.toFloat() / maxChars.toFloat()).coerceIn(0f, 1f)
    val canPost = content.isNotBlank() && content.length <= maxChars

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
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = onDismiss, modifier = Modifier.testTag("composer_close")) {
                        Icon(Icons.Filled.Close, contentDescription = "Close composer", tint = TextPrimary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (userProfile.undoDelaySeconds > 0) {
                            Text(
                                text = "Undo ${userProfile.undoDelaySeconds}s",
                                style = MonoTabularStyle.copy(fontSize = 11.sp),
                                color = XBlue,
                                modifier = Modifier.padding(end = 10.dp)
                            )
                        }
                        Button(
                            onClick = {
                                onPublish(
                                    content,
                                    mediaResKey,
                                    mediaUri,
                                    if (showPollBuilder) pollOptions.toList() else emptyList(),
                                    audience,
                                    replyTo?.id
                                )
                            },
                            enabled = canPost,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XBlue,
                                contentColor = TextPrimary,
                                disabledContainerColor = XBlue.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(999.dp),
                            modifier = Modifier.testTag("composer_post_button")
                        ) {
                            Text(
                                text = if (replyTo != null) "Reply" else "Post",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
                HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    if (replyTo != null) {
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Replying to ${replyTo.authorHandle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = XBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = replyTo.content,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.Top) {
                        UserAvatar(
                            name = userProfile.name,
                            isCurrentUser = true,
                            avatarColorHex = 0xFF1D9BF0,
                            size = 42.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = userProfile.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                VerificationBadgeIcon(badge = userProfile.badge, size = 15.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Box {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(999.dp))
                                            .clickable { audienceMenuExpanded = true },
                                        color = XBlueContainer,
                                        shape = RoundedCornerShape(999.dp),
                                        border = BorderStroke(1.dp, XBlue.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (audience == "Verified Only") Icons.Filled.Verified else Icons.Filled.Public,
                                                contentDescription = null,
                                                tint = XBlue,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = audience,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = XBlue
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = audienceMenuExpanded,
                                        onDismissRequest = { audienceMenuExpanded = false },
                                        containerColor = SurfaceElevated
                                    ) {
                                        listOf("Everyone", "Verified Only", "Followers").forEach { opt ->
                                            DropdownMenuItem(
                                                text = { Text(opt, color = TextPrimary) },
                                                onClick = {
                                                    audience = opt
                                                    audienceMenuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            TextField(
                                value = content,
                                onValueChange = { content = it },
                                placeholder = {
                                    Text(
                                        text = if (replyTo != null) "Post your reply…" else "What is happening?! (25K chars unlocked)",
                                        color = TextMuted
                                    )
                                },
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    cursorColor = XBlue
                                ),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("composer_input")
                            )
                        }
                    }

                    // Attached media preview
                    if (mediaResKey != null || mediaUri != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, HairlineDivider, RoundedCornerShape(16.dp))
                        ) {
                            val bundled = resolveBundledMedia(mediaResKey)
                            if (bundled != null) {
                                Image(
                                    painter = painterResource(id = bundled),
                                    contentDescription = "Selected attachment",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp),
                                    contentScale = ContentScale.Crop
                                )
                            } else if (mediaUri != null) {
                                AsyncImage(
                                    model = mediaUri,
                                    contentDescription = "Selected photo",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            IconButton(
                                onClick = {
                                    mediaUri = null
                                    mediaResKey = null
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .clip(CircleShape)
                                    .background(OledBlack.copy(alpha = 0.78f))
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove attachment", tint = TextPrimary)
                            }
                        }
                    }

                    // Poll builder
                    if (showPollBuilder) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = SurfaceCard,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, HairlineDivider),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Poll Choices", style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                    TextButton(onClick = { showPollBuilder = false }) {
                                        Text("Remove poll", color = DangerUndo)
                                    }
                                }
                                pollOptions.forEachIndexed { idx, value ->
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { pollOptions[idx] = it },
                                        label = { Text("Choice ${idx + 1}") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = XBlue,
                                            unfocusedBorderColor = BorderSubtle,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    )
                                }
                                if (pollOptions.size < 4) {
                                    TextButton(onClick = { pollOptions.add("") }) {
                                        Icon(Icons.Filled.Add, null, tint = XBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add option", color = XBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Toolbar
                HorizontalDivider(color = HairlineDivider, thickness = 0.8.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        IconButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = "Pick photo", tint = XBlue)
                        }
                        IconButton(onClick = { showPollBuilder = !showPollBuilder }) {
                            Icon(Icons.Filled.Poll, contentDescription = "Toggle poll", tint = XBlue)
                        }
                        TextButton(
                            onClick = {
                                mediaResKey = if (mediaResKey == "media_post_1") null else "media_post_1"
                                mediaUri = null
                            }
                        ) {
                            Text(
                                text = if (mediaResKey == "media_post_1") "Studio Photo ✓" else "+ Studio Photo",
                                style = MaterialTheme.typography.labelMedium,
                                color = XBlue
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${content.length} / $maxChars",
                            style = MonoTabularStyle.copy(fontSize = 11.5.sp),
                            color = if (content.length > maxChars) DangerUndo else TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        CircularProgressIndicator(
                            progress = { charProgress },
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = if (content.length > maxChars) DangerUndo else XBlue,
                            trackColor = BorderSubtle
                        )
                    }
                }
            }
        }
    }
}
