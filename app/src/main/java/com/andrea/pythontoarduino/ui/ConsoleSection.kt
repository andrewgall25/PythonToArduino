package com.andrea.pythontoarduino.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.pythontoarduino.ui.theme.HtmlColors
import com.andrea.pythontoarduino.ui.theme.JetBrainsMono

private val TimestampRegex = Regex("""^\[\d{2}:\d{2}:\d{2}]""")

private fun lineColor(line: String): Color {
    return when {
        TimestampRegex.matches(line) -> HtmlColors.TextSecondary
        line.startsWith("SYSTEM:") -> HtmlColors.SyntaxBlue
        line.startsWith("Arduino:") || line.contains("Connected", ignoreCase = true) -> HtmlColors.SyntaxGreen
        line.startsWith("> ") -> HtmlColors.SyntaxRed
        line.contains("Error", ignoreCase = true) || line.contains("Errore", ignoreCase = true) -> HtmlColors.SyntaxOrange
        line.startsWith("[") && line.contains("]") -> HtmlColors.TextDimmed
        else -> HtmlColors.OnSurface
    }
}

private fun lineStyle(line: String): FontWeight? {
    return when {
        line.startsWith("> ") -> FontWeight.SemiBold
        else -> null
    }
}

@Composable
fun ConsoleSection(
    output: String,
    consoleInput: String,
    onConsoleInputChange: (String) -> Unit,
    onSendConsoleInput: (String) -> Unit,
    onClearOutput: () -> Unit,
    onCopyOutput: () -> Unit,
    modifier: Modifier = Modifier,
    isPythonRunning: Boolean = false,
    onCancelPythonExecution: () -> Unit = {},
) {
    val scrollState = rememberLazyListState()
    val lines = remember(output) {
        output.lines().filter { it.isNotEmpty() }
    }
    val lineCount = lines.size

    LaunchedEffect(output) {
        if (lineCount > 0) {
            scrollState.animateScrollToItem(lineCount - 1)
        }
    }

    // Blinking cursor animation
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "cursorBlink"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HtmlColors.BgPrimary)
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, HtmlColors.BorderColor, RoundedCornerShape(8.dp))
    ) {
        // Terminal header — traffic lights + copy/clear
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HtmlColors.SurfaceContainer)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Traffic light dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(HtmlColors.StopRed.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(HtmlColors.FlashOrange.copy(alpha = 0.5f))
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(HtmlColors.SyntaxGreen.copy(alpha = 0.5f))
                )
            }

            // Copy + Clear buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCopyOutput,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Output",
                        tint = HtmlColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onClearOutput,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Console",
                        tint = HtmlColors.TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Log output area
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            state = scrollState,
            contentPadding = PaddingValues(bottom = 0.dp)
        ) {
            items(lines) { line ->
                Row(
                    modifier = Modifier.padding(bottom = 4.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    if (TimestampRegex.matches(line)) {
                        Text(
                            text = line,
                            fontFamily = JetBrainsMono,
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.3.sp,
                            color = HtmlColors.TextSecondary,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    } else {
                        Text(
                            text = line,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = lineColor(line),
                            fontWeight = lineStyle(line)
                        )
                    }
                }
            }

            // Blinking cursor at end
            item {
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = ">",
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = HtmlColors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .size(width = 8.dp, height = 16.dp)
                            .background(HtmlColors.Primary.copy(alpha = cursorAlpha))
                    )
                }
            }
        }

        // Input bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HtmlColors.SurfaceContainerLow)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, HtmlColors.BorderColor, RoundedCornerShape(24.dp))
                    .background(HtmlColors.Background)
                    .padding(start = 20.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = consoleInput,
                    onValueChange = onConsoleInputChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = HtmlColors.OnSurface
                    ),
                    decorationBox = { innerTextField ->
                        Box {
                            if (consoleInput.isEmpty()) {
                                Text(
                                    text = "Send a command",
                                    fontFamily = JetBrainsMono,
                                    fontSize = 13.sp,
                                    color = HtmlColors.TextSecondary
                                )
                            }
                            innerTextField()
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(HtmlColors.PrimaryContainer)
                        .clickable(enabled = consoleInput.isNotBlank()) { onSendConsoleInput(consoleInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = HtmlColors.OnPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
