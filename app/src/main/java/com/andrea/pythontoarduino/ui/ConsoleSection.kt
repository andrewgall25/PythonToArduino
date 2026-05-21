package com.andrea.pythontoarduino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.andrea.pythontoarduino.R

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_regular)
)

// GitHub Dark Glow palette
private val BgPrimary = Color(0xFF0d1117)
private val BgCard = Color(0xFF161b22)
private val BorderColor = Color(0xFF30363d)
private val BorderSubtle = Color(0xFF21262d)
private val TextPrimary = Color(0xFFe6edf3)
private val TextSecondary = Color(0xFF8b949e)
private val TextDimmed = Color(0xFFC4CAD0)
private val AccentGreen = Color(0xFF238636)
private val SyntaxRed = Color(0xFFff7b72)
private val SyntaxBlue = Color(0xFFa5d6ff)
private val SyntaxPurple = Color(0xFFd2a8ff)
private val SyntaxGreen = Color(0xFF6a9955)
private val SyntaxOrange = Color(0xFFce9178)

private val TimestampRegex = Regex("""^\[\d{2}:\d{2}\]$""")

private fun lineColor(line: String): Color {
    return when {
        TimestampRegex.matches(line) -> TextDimmed
        line.startsWith("> ") -> SyntaxRed
        line.startsWith("Arduino:") -> SyntaxBlue
        line.startsWith("---") -> SyntaxGreen
        line.contains("Error", ignoreCase = true) || line.contains("Errore", ignoreCase = true) -> SyntaxOrange
        line.startsWith("[") && line.endsWith("]") -> SyntaxPurple
        else -> TextPrimary
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val lines = output.lines().filter { it.isNotEmpty() }
    val lineCount = lines.size

    LaunchedEffect(output) {
        if (lineCount > 0) {
            scrollState.animateScrollToItem(lineCount - 1)
        }
    }

    Box(
        modifier = modifier.fillMaxSize().background(BgPrimary)
    ) {
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header row: green dot + title + icon toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(AccentGreen)
                                .shadow(4.dp, CircleShape, ambientColor = AccentGreen.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Console",
                            fontFamily = JetBrainsMono,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = TextPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Row(
                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onCopyOutput() },
                            tint = TextSecondary
                        )
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onClearOutput() },
                            tint = TextSecondary
                        )
                    }
                }

                Divider(color = BorderSubtle, thickness = 1.dp)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    state = scrollState,
                ) {
                    items(lines) { line ->
                        if (TimestampRegex.matches(line)) {
                            Text(
                                text = line,
                                fontFamily = JetBrainsMono,
                                fontSize = 10.sp,
                                color = TextDimmed,
                                letterSpacing = 0.3.sp,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                            )
                        } else {
                            Text(
                                text = line,
                                fontFamily = JetBrainsMono,
                                fontSize = 13.sp,
                                color = lineColor(line),
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        if (isPythonRunning) {
            Button(
                onClick = onCancelPythonExecution,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-80).dp)
                    .padding(bottom = 0.dp)
            ) {
                Text("Stop")
            }
        }

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = BgCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = consoleInput,
                    onValueChange = onConsoleInputChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMono,
                        fontSize = 13.sp,
                        color = TextPrimary
                    ),
                    placeholder = {
                        Text(
                            "Send a command",
                            color = TextDimmed,
                            fontFamily = JetBrainsMono,
                            fontSize = 13.sp
                        )
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        cursorColor = AccentGreen,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        containerColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AccentGreen)
                        .shadow(4.dp, CircleShape, ambientColor = AccentGreen.copy(alpha = 0.3f))
                        .clickable { onSendConsoleInput(consoleInput) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                }
            }
        }
    }
}
