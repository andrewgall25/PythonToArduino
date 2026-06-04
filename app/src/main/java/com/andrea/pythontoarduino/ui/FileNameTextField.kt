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
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.andrea.pythontoarduino.R

val Manrope4 = FontFamily(
    Font(R.font.base_neue)
)

@Composable
fun FileNameTextField(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val extension = ".py"

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
            .padding(vertical = 2.dp),
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Thin,
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = Manrope4,
            lineHeight = 22.sp
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        decorationBox = @Composable { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                innerTextField()
            }
        }
    )
}
