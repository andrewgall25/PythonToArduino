package com.andrea.pythontoarduino.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Composable per la sezione delle informazioni di debug.
 * @param usbStatus Lo stato attuale della connessione USB.
 * @param isPythonRunning Indica se lo script Python è in esecuzione.
 * @param isUsbChecking Indica se il controllo USB è in corso.
 * @param serialData I dati ricevuti dalla seriale.
 */
@Composable
fun DebugInfoSection(
    usbStatus: String,
    isPythonRunning: Boolean,
    isUsbChecking: Boolean,
    serialData: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Debug Info",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE0E6ED), // grigio chiaro tema dark
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF002B44)) // blu oceano scuro
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val textColor = Color(0xFFE0E6ED)
                Text(
                    text = "USB Status: $usbStatus",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Python Execution: ${if (isPythonRunning) "Running..." else "Inactive"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "USB Check: ${if (isUsbChecking) "Running..." else "Inactive"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Serial Data: ${serialData.ifBlank { "No data." }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
        }
    }
}
