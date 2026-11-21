package com.example.ui.modals

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.data.model.BookmarkFolder
import com.example.data.model.PostItem
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.DangerUndo
import com.example.ui.theme.HairlineDivider
import com.example.ui.theme.MonoTabularStyle
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.XBlue

fun resolveFolderIcon(iconKey: String): ImageVector {
    return when (iconKey) {
        "code" -> Icons.Filled.Code
        "palette" -> Icons.Filled.Palette
        "trending" -> Icons.Filled.TrendingUp
        "book" -> Icons.Filled.MenuBook
        else -> Icons.Filled.Folder
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveToFolderSheet(
    post: PostItem,
    folders: List<BookmarkFolder>,
    onSelectFolder: (folderId: String?) -> Unit,
    onCreateNewFolder: () -> Unit,
    onRemoveBookmark: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceElevated,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 26.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Save to Bookmark Folder", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text(
                        text = "Organize posts into categorized collections",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
                TextButton(onClick = onCreateNewFolder, modifier = Modifier.testTag("sheet_new_folder_btn")) {
                    Icon(Icons.Filled.Add, null, tint = XBlue)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("New Folder", color = XBlue)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            folders.forEach { folder ->
                val isSelected = post.isBookmarked && post.bookmarkFolderId == folder.id
                val accent = Color(folder.colorHex)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onSelectFolder(folder.id) }
                        .testTag("assign_folder_${folder.id}"),
                    color = if (isSelected) accent.copy(alpha = 0.14f) else SurfaceCard,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (isSelected) accent else BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accent.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = resolveFolderIcon(folder.iconKey),
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(folder.name, style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                                if (folder.description.isNotBlank()) {
                                    Text(folder.description, style = MaterialTheme.typography.bodySmall, color = TextMuted)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${folder.count}",
                                style = MonoTabularStyle,
                                color = TextMuted
                            )
                            if (isSelected) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Filled.Check, contentDescription = "Selected", tint = accent)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectFolder(null) },
                color = SurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Folder, null, tint = TextMuted, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Save to General Bookmarks (Unassigned)", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                }
            }

            if (post.isBookmarked) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = BorderSubtle, thickness = 0.8.dp)
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onRemoveBookmark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.BookmarkRemove, null, tint = DangerUndo)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Remove from Bookmarks", color = DangerUndo)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewBookmarkFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (name: String, description: String, colorHex: Long, iconKey: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF1D9BF0) }
    var selectedIcon by remember { mutableStateOf("folder") }

    val colors = listOf(0xFF1D9BF0L, 0xFFE2B714L, 0xFF00BA7CL, 0xFFA855F7L, 0xFFF91880L)
    val icons = listOf("folder", "code", "palette", "trending", "book")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, HairlineDivider),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("New Bookmark Folder", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Folder name") },
                    placeholder = { Text("e.g. AI Research, Rust Tricks", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = XBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_folder_name_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
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
                Text("Accent Color", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    colors.forEach { hex ->
                        val active = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (active) 2.5.dp else 0.dp,
                                    color = if (active) TextPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (active) {
                                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text("Icon", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { key ->
                        val active = selectedIcon == key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (active) Color(selectedColor).copy(alpha = 0.2f) else SurfaceCard)
                                .border(1.dp, if (active) Color(selectedColor) else BorderSubtle, RoundedCornerShape(10.dp))
                                .clickable { selectedIcon = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = resolveFolderIcon(key),
                                contentDescription = key,
                                tint = if (active) Color(selectedColor) else TextMuted,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextMuted)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onCreate(name, description, selectedColor, selectedIcon) },
                        enabled = name.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = XBlue, contentColor = TextPrimary),
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier.testTag("create_folder_confirm_btn")
                    ) {
                        Text("Create Folder")
                    }
                }
            }
        }
    }
}
