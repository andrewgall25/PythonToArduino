package com.andrea.pythontoarduino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.andrea.pythontoarduino.R

val Utendo = FontFamily(
    Font(R.font.utendo_regular, FontWeight.Normal)
)

@Composable
fun FileNameTextField(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = ".py"

    // TextFieldValue ci permette di controllare il cursore
    var textFieldValue by remember {
        mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(fileName,
            selection = androidx.compose.ui.text.TextRange(fileName.length - extension.length)
        ))
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            val baseText = newValue.text.removeSuffix(extension)
            val safeText = "$baseText$extension"
            val cursorPosition = newValue.selection.end.coerceAtMost(baseText.length)

            textFieldValue = newValue.copy(
                text = safeText,
                selection = TextRange(cursorPosition)
            )

            onFileNameChange(safeText)
        },
        singleLine = true,
        modifier = modifier
            .background(Color.Transparent)
            .padding(vertical = 2.dp), // 🎯 Padding controllato!
        textStyle = TextStyle(
            fontSize = 20.sp,          // 🎯 Abbastanza grande, ma non enorme
            fontWeight = FontWeight.Thin,
            color = Color.White,
            fontFamily = Utendo,
            lineHeight = 22.sp         // 🎯 Fondamentale! Altezza riga controllata
        ),
        cursorBrush = SolidColor(Color.White),
        decorationBox = @Composable { innerTextField ->
            // Simula un campo senza bordi/indicatori
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp), // 🎯 Contenitore con altezza fissa minima
                contentAlignment = Alignment.CenterStart
            ) {
                innerTextField()
            }
        }
    )
}