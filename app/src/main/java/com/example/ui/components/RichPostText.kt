package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.XBlue

fun buildHighlightedPostText(text: String): AnnotatedString {
    val tokenRegex = Regex("([#@$][A-Za-z0-9_]+|`[^`]+`|\\*\\*[^*]+\\*\\*)")
    return buildAnnotatedString {
        var lastIndex = 0
        for (match in tokenRegex.findAll(text)) {
            if (match.range.first > lastIndex) {
                append(text.substring(lastIndex, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("#") || token.startsWith("@") || token.startsWith("$") -> {
                    withStyle(SpanStyle(color = XBlue, fontWeight = FontWeight.Medium)) {
                        append(token)
                    }
                }
                token.startsWith("`") && token.endsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = XBlue,
                            background = SurfaceElevated
                        )
                    ) {
                        append(token.removeSurrounding("`"))
                    }
                }
                token.startsWith("**") && token.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(token.removeSurrounding("**"))
                    }
                }
                else -> append(token)
            }
            lastIndex = match.range.last + 1
        }
        if (lastIndex < text.length) {
            append(text.substring(lastIndex))
        }
    }
}

@Composable
fun RichPostText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = TextPrimary,
    maxLines: Int = Int.MAX_VALUE
) {
    Text(
        text = buildHighlightedPostText(text),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines
    )
}

@Composable
fun MarkdownArticleBody(
    markdown: String,
    fontFamily: FontFamily,
    fontSizeSp: Float,
    modifier: Modifier = Modifier
) {
    val lines = markdown.lines()
    Column(modifier = modifier.fillMaxWidth()) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val trimmed = line.trim()
            when {
                trimmed.startsWith("```") -> {
                    val codeLines = mutableListOf<String>()
                    i++
                    while (i < lines.size && !lines[i].trim().startsWith("```")) {
                        codeLines.add(lines[i])
                        i++
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceElevated)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = codeLines.joinToString("\n"),
                            style = TextStyle(
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = (fontSizeSp - 2.5f).coerceAtLeast(11f).sp,
                                lineHeight = (fontSizeSp + 4f).sp,
                                color = TextPrimary
                            )
                        )
                    }
                }
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# "),
                        style = TextStyle(
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = (fontSizeSp + 7f).sp,
                            lineHeight = (fontSizeSp + 14f).sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(top = 14.dp, bottom = 6.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## "),
                        style = TextStyle(
                            fontFamily = PlusJakartaSansFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (fontSizeSp + 3.5f).sp,
                            lineHeight = (fontSizeSp + 9f).sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("> ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(XBlue.copy(alpha = 0.06f))
                            .padding(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(42.dp)
                                .background(XBlue, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = buildInlineStyledSpan(trimmed.removePrefix("> ")),
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontStyle = FontStyle.Italic,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.5f).sp,
                                color = TextPrimary
                            )
                        )
                    }
                }
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp, horizontal = 4.dp)
                    ) {
                        Text(
                            text = "•",
                            color = XBlue,
                            fontSize = fontSizeSp.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 10.dp)
                        )
                        Text(
                            text = buildInlineStyledSpan(trimmed.drop(2)),
                            style = TextStyle(
                                fontFamily = fontFamily,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.48f).sp,
                                color = TextPrimary
                            )
                        )
                    }
                }
                trimmed.isEmpty() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                }
                else -> {
                    Text(
                        text = buildInlineStyledSpan(trimmed),
                        style = TextStyle(
                            fontFamily = fontFamily,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.54f).sp,
                            color = TextSecondary
                        ),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
            i++
        }
    }
}

private fun buildInlineStyledSpan(text: String): AnnotatedString {
    val pattern = Regex("(\\*\\*[^*]+\\*\\*|\\*[^*]+\\*|`[^`]+`)")
    return buildAnnotatedString {
        var idx = 0
        for (match in pattern.findAll(text)) {
            if (match.range.first > idx) {
                append(text.substring(idx, match.range.first))
            }
            val token = match.value
            when {
                token.startsWith("**") && token.endsWith("**") -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextPrimary)) {
                        append(token.removeSurrounding("**"))
                    }
                }
                token.startsWith("*") && token.endsWith("*") -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(token.removeSurrounding("*"))
                    }
                }
                token.startsWith("`") && token.endsWith("`") -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            color = XBlue,
                            background = SurfaceElevated
                        )
                    ) {
                        append(token.removeSurrounding("`"))
                    }
                }
                else -> append(token)
            }
            idx = match.range.last + 1
        }
        if (idx < text.length) {
            append(text.substring(idx))
        }
    }
}
