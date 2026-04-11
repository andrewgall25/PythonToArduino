package com.andrea.pythontoarduino.ui

import android.content.Context
import android.hardware.usb.UsbManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrea.pythontoarduino.MainViewModel
import com.andrea.pythontoarduino.SerialManager
import androidx.compose.ui.graphics.Color

@Composable
fun SerialTestScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val serialData by viewModel.serialData.observeAsState("")
    val usbStatus by viewModel.usbStatus.observeAsState("")
    val isReady by remember { mutableStateOf(SerialManager.isSerialReady) }
    val isUsbChecking by viewModel.isUsbChecking.observeAsState(false)
    val isUsbReady by viewModel.isUsbReady.observeAsState(false)
    val showUsbStatusMessage by viewModel.showUsbStatusMessage.observeAsState(false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Button(
            onClick = { viewModel.sendToSerial("blink") },
            enabled = isUsbReady
        ) {
            Text("Blink LED")
        }

        Text(text = "USB Status: $usbStatus")
        Text(text = "USB Ready: $isUsbReady")

        if (showUsbStatusMessage) {
            Text(text = "USB Status Message: $usbStatus", color = Color.White)
        }

        Button(
            onClick = {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                viewModel.requestUsbPermissionManually(usbManager)
            }
        ) {
            Text("Richiedi Permessi USB Manualmente")
        }

        Text(
            text = "Risposta Arduino:\n$serialData",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
