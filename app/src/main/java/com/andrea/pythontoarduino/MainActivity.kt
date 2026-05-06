package com.andrea.pythontoarduino

import android.app.AlertDialog
import android.app.Application
import android.os.Build
import android.app.PendingIntent
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.andrea.pythontoarduino.ui.PythonToArduinoUI
import com.andrea.pythontoarduino.ui.theme.PythonToArduinoTheme
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class MainActivity : ComponentActivity() {

    private lateinit var usbManager: UsbManager
    private lateinit var viewModel: MainViewModel
    private var usbReceiver: UsbBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Gestisce la transizione dal tema Splash al tema dell'app
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        usbManager = getSystemService(Context.USB_SERVICE) as UsbManager

        // 🔥 FONDAMENTALE: Registra il BroadcastReceiver
        setupUsbBroadcastReceiver()

        // Controlla se ci sono già dispositivi USB collegati
        checkExistingUsbDevices()

        for (device in usbManager.deviceList.values) {
            Log.d("USB_DEBUG", "Device trovato: name=${device.deviceName}, vendorId=${device.vendorId}, productId=${device.productId}")
        }

        setContent {
            PythonToArduinoTheme {
                val context = LocalContext.current
                val viewModel: MainViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return MainViewModel(context.applicationContext as Application) as T
                        }
                    }
                )

                val pythonCode by viewModel.pythonCode.observeAsState("")
                val output by viewModel.output.observeAsState("")
                val usbStatus by viewModel.usbStatus.observeAsState("")
                val isPythonRunning by viewModel.isPythonRunning.observeAsState(false)
                val isUsbChecking by viewModel.isUsbChecking.observeAsState(false)
                val serialData by viewModel.serialData.observeAsState("")
                val showUsbStatusMessage by viewModel.showUsbStatusMessage.observeAsState(false)
                val consoleInput by viewModel.consoleInput.observeAsState("")
                val fileName by viewModel.fileName.observeAsState("script1.py")

                // Al primo avvio carichiamo l’ultimo file salvato
                LaunchedEffect(Unit) {
                    viewModel.loadLastOpenedFile()
                }

                DisposableEffect(Unit) {
                    onDispose {
                        SerialManager.disconnect()
                    }
                }

                PythonToArduinoUI(
                    pythonCode = pythonCode,
                    output = output,
                    usbStatus = usbStatus,
                    serialData = serialData,
                    isPythonRunning = isPythonRunning,
                    isUsbChecking = isUsbChecking,
                    showUsbStatusMessage = showUsbStatusMessage,
                    consoleInput = consoleInput,
                    fileName = fileName,
                    onCodeChange = { code -> viewModel.setPythonCode(code) },
                    onRunPython = { code -> viewModel.runPythonCode(code) },
                    onCheckUsb = { viewModel.checkUsbDevice(usbManager) },
                    onHideUsbStatusMessage = { viewModel.hideUsbStatusMessage() },
                    onConsoleInputChange = { input -> viewModel.setConsoleInput(input) },
                    onSendConsoleInput = { input -> viewModel.sendConsoleInput(input) },
                    onCancelPythonExecution = { viewModel.cancelPythonExecution() },
                    onFileNameChange = { newName -> viewModel.setFileName(newName) },
                    onSaveFile = { code, _ ->
                        viewModel.savePythonCodeToFile(code) { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onCompileAndFlash = { code -> viewModel.compileAndFlashArduino(code) } // 👈 Aggiungi

                )
            }
        }
    }

    private fun setupUsbBroadcastReceiver() {
        Log.d("MainActivity", "Registrando UsbBroadcastReceiver...")

        usbReceiver = UsbBroadcastReceiver()

        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction(UsbBroadcastReceiver.ACTION_USB_PERMISSION)
        }

        // Registra il receiver con flag appropriati in base alla versione API
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // API 26 (Oreo) and above
            val receiverFlags = Context.RECEIVER_NOT_EXPORTED
            registerReceiver(usbReceiver, filter, receiverFlags)
        } else { // API 24 and 25 (Nougat)
            registerReceiver(usbReceiver, filter)
        }
        Log.d("MainActivity", "UsbBroadcastReceiver registrato con successo")
    }

    private fun checkExistingUsbDevices() {
        Log.d("MainActivity", "Controllando dispositivi USB già collegati...")

        val deviceList = usbManager.deviceList
        if (deviceList.isEmpty()) {
            Log.d("MainActivity", "Nessun dispositivo USB collegato")
            return
        }

        for (device in deviceList.values) {
            Log.d("MainActivity", "Dispositivo trovato: ${device.deviceName} (VID: ${device.vendorId}, PID: ${device.productId})")

            if (isArduinoDevice(device)) {
                if (usbManager.hasPermission(device)) {
                    Log.d("MainActivity", "Arduino trovato con permessi, connetto subito...")
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            SerialManager.connectToDevice(this@MainActivity, device)
                            if (SerialManager.isConnected()) {
                                Log.d("MainActivity", "Connessione automatica riuscita per ${device.deviceName}")
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(this@MainActivity, "Arduino connesso automaticamente", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Log.w("MainActivity", "Connessione automatica fallita per ${device.deviceName}")
                            }
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Errore connessione automatica: ${e.message}")
                            CoroutineScope(Dispatchers.Main).launch {
                                Toast.makeText(this@MainActivity, "Errore connessione automatica: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                } else {
                    Log.d("MainActivity", "Arduino trovato senza permessi, richiedo permesso...")
                    requestUsbPermission(device)
                }
            }
        }
    }

    private fun isArduinoDevice(device: UsbDevice): Boolean {
        val arduinoVendorIds = setOf(
            0x2341, // Arduino SA
            0x2A03, // Arduino LLC
            0x1A86, // CH340 (cloni cinesi)
            0x0403, // FTDI (alcuni Arduino)
            0x10C4, // Silicon Labs (ESP32, alcuni cloni)
            0x067B  // Prolific (alcuni adattatori)
        )
        return device.vendorId in arduinoVendorIds
    }

    private fun requestUsbPermission(device: UsbDevice) {
        Log.d("MainActivity", "Richiedendo permessi per ${device.deviceName}...")

        val permissionIntent = PendingIntent.getBroadcast(
            applicationContext,
            System.currentTimeMillis().toInt(),
            Intent(UsbBroadcastReceiver.ACTION_USB_PERMISSION).apply {
                setPackage(packageName)
                putExtra(UsbManager.EXTRA_DEVICE, device)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        usbManager.requestPermission(device, permissionIntent)
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(this@MainActivity, "Richiesta permessi per Arduino...", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()    // Invece di ricreare il PendingIntent qui (che causava l'errore),
        // usiamo la funzione che abbiamo già scritto per controllare i dispositivi.
        checkExistingUsbDevices()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy: Disconnettendo SerialManager e deregistrando receiver")

        SerialManager.disconnect()
        try {
            usbReceiver?.let {
                unregisterReceiver(it)
                Log.d("MainActivity", "UsbBroadcastReceiver deregistrato")
                usbReceiver = null
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Errore deregistrando receiver: ${e.message}")
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.d("MainActivity", "onNewIntent chiamato con action: ${intent?.action}")

        // Gestisce il caso in cui l'app venga aperta da un intent USB
        if (intent?.action == UsbManager.ACTION_USB_DEVICE_ATTACHED) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            Log.d("MainActivity", "Device attached via intent: ${device?.deviceName}")
        }
    }
}