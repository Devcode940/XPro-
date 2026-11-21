package com.example.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonoTabularStyle

@Composable
fun DrawerMenuItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    badgeText: String? = null,
    tintOverride: Color? = null,
    testTagId: String? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = if (selected) colorScheme.primaryContainer else Color.Transparent
    val iconColor = tintOverride ?: if (selected) colorScheme.primary else colorScheme.onSurfaceVariant
    val labelColor = tintOverride ?: if (selected) colorScheme.primary else colorScheme.onSurface

    val baseMod = modifier
        .fillMaxWidth()
        .padding(horizontal = 10.dp, vertical = 2.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(containerColor)
        .clickable(onClick = onClick)
        .defaultMinSize(minHeight = 46.dp)
        .padding(horizontal = 12.dp, vertical = 10.dp)

    Row(
        modifier = if (testTagId != null) baseMod.testTag(testTagId) else baseMod,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            ),
            color = labelColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!badgeText.isNullOrBlank()) {
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = if (selected) colorScheme.primary else colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(999.dp)
            ) {
                Box(modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)) {
                    Text(
                        text = badgeText,
                        style = MonoTabularStyle.copy(fontSize = 10.5.sp),
                        color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
