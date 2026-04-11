package com.andrea.pythontoarduino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle


/**
 * Composable per la sezione della console, che mostra l'output e permette l'input.
 * @param output L'output accumulato da visualizzare.
 * @param consoleInput L'input corrente nel campo di testo.
 * @param onConsoleInputChange Callback chiamato quando l'input della console cambia.
 * @param onSendConsoleInput Callback chiamato quando l'input della console viene inviato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConsoleSection(
    output: String,
    consoleInput: String,
    onConsoleInputChange: (String) -> Unit,
    onSendConsoleInput: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberLazyListState()
    LaunchedEffect(output) {
        scrollState.animateScrollToItem(output.lines().size)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D)) // nero opaco
            .padding(16.dp)
    ) {
        Text(
            text = "Console",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color(0xFF00FF00), // verde acceso terminale
            modifier = Modifier.padding(bottom = 12.dp)
        )

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF121212)), // sfondo leggermente più chiaro
            state = scrollState,
            contentPadding = PaddingValues(8.dp)
        ) {
            items(output.lines()) { line ->
                Text(
                    text = line,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color(0xFF00FF00),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF222222))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = consoleInput,
                onValueChange = onConsoleInputChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = Color(0xFF00FF00)
                ),
                placeholder = {
                    Text("Send a command", color = Color(0xFF555555))
                },
                singleLine = true,
                colors = TextFieldDefaults.textFieldColors(
                    cursorColor = Color(0xFF00FF00),
                    focusedIndicatorColor = Color(0xFF00FF00),
                    unfocusedIndicatorColor = Color(0xFF005500),
                    containerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Send,
                contentDescription = "Send",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00FF00))
                    .clickable { onSendConsoleInput(consoleInput) }
                    .padding(8.dp),
                tint = Color.Black
            )
        }
    }
}
