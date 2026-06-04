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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.pythontoarduino.ui.theme.HtmlColors
import com.andrea.pythontoarduino.ui.theme.JetBrainsMono
import com.andrea.pythontoarduino.ui.theme.Manrope

@Composable
fun DebugInfoSection(
    usbStatus: String,
    isPythonRunning: Boolean,
    isUsbChecking: Boolean,
    serialData: String,
    modifier: Modifier = Modifier,
    onCancelPythonExecution: () -> Unit = {},
) {
    val isUsbConnected = usbStatus.contains("Connected", ignoreCase = true)
    val scrollState = rememberLazyListState()
    val lines = remember(serialData) {
        serialData.lines().filter { it.isNotEmpty() }
    }

    LaunchedEffect(serialData) {
        if (lines.isNotEmpty()) {
            scrollState.animateScrollToItem(lines.size - 1)
        }
    }

    // Pulse animation for serial cursor
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "pulseAnim"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(HtmlColors.BgPrimary)
            .padding(horizontal = 16.dp)
    ) {
        // USB Status Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = HtmlColors.BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, HtmlColors.BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "USB Status",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HtmlColors.SyntaxBlue
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isUsbConnected) HtmlColors.Primary else HtmlColors.StopRed)
                        )
                        Text(
                            text = if (isUsbConnected) "Live" else "Offline",
                            fontFamily = Manrope,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = if (isUsbConnected) HtmlColors.Primary else HtmlColors.StopRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HtmlColors.SurfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "USB",
                        tint = HtmlColors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = usbStatus.ifBlank { "No device" },
                            fontFamily = Manrope,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            lineHeight = 20.sp,
                            color = HtmlColors.TextPrimary
                        )
                        Text(
                            text = "Port: /dev/tty.usbmodem14101",
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = HtmlColors.TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Execution Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = HtmlColors.BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, HtmlColors.BorderColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Execution",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HtmlColors.SyntaxPurple
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HtmlColors.SurfaceContainerLow, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = "Python",
                            tint = HtmlColors.RunBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Python: ${if (isPythonRunning) "Active" else "Inactive"}",
                                fontFamily = Manrope,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                lineHeight = 20.sp,
                                color = HtmlColors.TextPrimary
                            )
                            Text(
                                text = if (isPythonRunning) "script.py running..." else "Idle",
                                fontFamily = JetBrainsMono,
                                fontSize = 11.sp,
                                lineHeight = 14.sp,
                                color = HtmlColors.SyntaxGreen
                            )
                        }
                    }

                    if (isPythonRunning) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, HtmlColors.StopRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                                .background(HtmlColors.StopRed.copy(alpha = 0.2f))
                                .clickable { onCancelPythonExecution() }
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "STOP",
                                fontFamily = Manrope,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HtmlColors.StopRed
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Serial Monitor Card (takes remaining space)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = HtmlColors.BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, HtmlColors.BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Serial Monitor",
                        fontFamily = Manrope,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = HtmlColors.SyntaxOrange
                    )
                    Text(
                        text = "9600 BAUD",
                        fontFamily = JetBrainsMono,
                        fontSize = 12.sp,
                        color = HtmlColors.TextSecondary
                    )
                }

                // Divider
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(HtmlColors.BorderSubtle)
                )

                // Scrollable serial output
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(8.dp),
                    state = scrollState,
                    contentPadding = PaddingValues(bottom = 0.dp)
                ) {
                    items(lines) { line ->
                        val isOut = line.startsWith(">")
                        val isIn = line.startsWith("<")
                        val color = when {
                            isOut -> HtmlColors.OnSurface
                            isIn -> HtmlColors.SyntaxBlue
                            line.contains("INIT", ignoreCase = true) -> HtmlColors.SyntaxGreen
                            else -> HtmlColors.OnSurface
                        }
                        Text(
                            text = line,
                            fontFamily = JetBrainsMono,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = color,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }

                // Input area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(HtmlColors.SurfaceContainerLowest)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = "Send command...",
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = HtmlColors.TextSecondary
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(width = 4.dp, height = 20.dp)
                            .background(HtmlColors.Primary.copy(alpha = pulseAlpha))
                    )
                }
            }
        }
    }
}
