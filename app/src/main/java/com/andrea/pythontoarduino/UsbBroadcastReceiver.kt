package com.andrea.pythontoarduino

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class UsbBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_USB_PERMISSION = "com.andrea.pythontoarduino.USB_PERMISSION"
        private const val TAG = "UsbBroadcastReceiver"
        private const val MAX_RETRY_ATTEMPTS = 2
        private const val RETRY_TIMEOUT_MS = 5000L // 5 secondi
    }

    private val retryCounts = mutableMapOf<String, Int>()
    private val retryTimestamps = mutableMapOf<String, Long>()

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive chiamato!")

        val ctx = context ?: return
        val action = intent?.action ?: return

        Log.d(TAG, "Action ricevuta: $action")

        when (action) {
            UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                Log.d(TAG, "USB_DEVICE_ATTACHED ricevuto")
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                Log.d(TAG, "Device: ${device?.deviceName}, VID: ${device?.vendorId}, PID: ${device?.productId}")

                device?.let {
                    val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
                    Log.d(TAG, "UsbManager ottenuto: $usbManager")

                    if (SerialManager.isConnected()) {
                        Log.d(TAG, "Connessione già attiva, ignoro evento ATTACHED")
                        return
                    }

                    if (!usbManager.hasPermission(it)) {
                        Log.d(TAG, "Permesso non presente, richiedo permesso...")
                        val permissionIntent = PendingIntent.getBroadcast(
                            ctx.applicationContext,
                            System.currentTimeMillis().toInt(),
                            Intent(ACTION_USB_PERMISSION).apply {
                                setPackage(ctx.packageName)
                                putExtra(UsbManager.EXTRA_DEVICE, it)
                            },
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        usbManager.requestPermission(it, permissionIntent)
                        Log.d(TAG, "Richiesta permesso inviata per ${device.deviceName}, requestCode: ${System.currentTimeMillis().toInt()}")
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(ctx, "Richiesta permessi USB...", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.d(TAG, "Permesso già presente, connetto subito...")
                        connectToDeviceAsync(ctx, it)
                    }
                }
            }

            ACTION_USB_PERMISSION -> {
                Log.d(TAG, "ACTION_USB_PERMISSION ricevuto")
                val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                var device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)

                Log.d(TAG, "Permesso concesso: $granted")
                Log.d(TAG, "Device: ${device?.deviceName ?: "null"}")

                if (device == null) {
                    Log.w(TAG, "Device null in ACTION_USB_PERMISSION, tentando di trovare dispositivo...")
                    val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
                    device = usbManager.deviceList.values.find {
                        it.vendorId == 0x2341 && it.productId == 0x0043
                    }
                    Log.d(TAG, "Fallback device: ${device?.deviceName ?: "null"}")
                }

                val deviceKey = device?.deviceName ?: "unknown"
                val currentTime = System.currentTimeMillis()
                val lastRetryTime = retryTimestamps.getOrDefault(deviceKey, 0L)

                if (SerialManager.isConnected()) {
                    Log.d(TAG, "Connessione già attiva, ignoro ACTION_USB_PERMISSION")
                    retryCounts.remove(deviceKey)
                    retryTimestamps.remove(deviceKey)
                    return
                }

                if (granted && device != null) {
                    retryCounts.remove(deviceKey)
                    retryTimestamps.remove(deviceKey)
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(ctx, "Permesso USB concesso", Toast.LENGTH_SHORT).show()
                    }
                    Log.d(TAG, "Inizio connessione al device...")
                    connectToDeviceAsync(ctx, device)
                } else {
                    val currentAttempts = retryCounts.getOrDefault(deviceKey, 0)
                    if (currentAttempts < MAX_RETRY_ATTEMPTS && device != null && (currentTime - lastRetryTime) > RETRY_TIMEOUT_MS) {
                        retryCounts[deviceKey] = currentAttempts + 1
                        retryTimestamps[deviceKey] = currentTime
                        Log.d(TAG, "Riprovo a richiedere permesso per ${device.deviceName} (tentativo ${currentAttempts + 1}/$MAX_RETRY_ATTEMPTS)...")
                        CoroutineScope(Dispatchers.IO).launch {
                            val result = withTimeoutOrNull(RETRY_TIMEOUT_MS) {
                                val permissionIntent = PendingIntent.getBroadcast(
                                    ctx.applicationContext,
                                    System.currentTimeMillis().toInt(),
                                    Intent(ACTION_USB_PERMISSION).apply {
                                        setPackage(ctx.packageName)
                                        putExtra(UsbManager.EXTRA_DEVICE, device)
                                    },
                                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                                )
                                val usbManager = ctx.getSystemService(Context.USB_SERVICE) as UsbManager
                                usbManager.requestPermission(device, permissionIntent)
                                Log.d(TAG, "Richiesta permesso inviata per ${device.deviceName}, requestCode: ${System.currentTimeMillis().toInt()}")
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Riprovo richiesta permessi USB...", Toast.LENGTH_SHORT).show()
                                }
                                true // Indica che la richiesta è stata inviata
                            }
                            if (result == null) {
                                Log.d(TAG, "Timeout retry per ${device.deviceName}")
                                retryCounts.remove(deviceKey)
                                retryTimestamps.remove(deviceKey)
                                CoroutineScope(Dispatchers.Main).launch {
                                    Toast.makeText(ctx, "Timeout richiesta permessi USB", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    } else {
                        CoroutineScope(Dispatchers.Main).launch {
                            Toast.makeText(ctx, "Permesso USB negato o dispositivo non trovato. Tentativi esauriti.", Toast.LENGTH_LONG).show()
                        }
                        Log.w(TAG, "Permesso negato: $granted, Device: ${device?.deviceName ?: "null"}, Tentativi: $currentAttempts")
                        retryCounts.remove(deviceKey)
                        retryTimestamps.remove(deviceKey)
                    }
                }
            }

            UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                Log.d(TAG, "USB_DEVICE_DETACHED ricevuto")
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                device?.let {
                    Log.d(TAG, "Disconnettendo device: ${it.deviceName}")
                    SerialManager.disconnectFromDevice(it)
                    retryCounts.remove(it.deviceName)
                    retryTimestamps.remove(it.deviceName)
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(ctx, "Arduino disconnesso", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            else -> {
                Log.d(TAG, "Action non gestita: $action, intent: $intent")
            }
        }
    }

    private fun connectToDeviceAsync(context: Context, device: UsbDevice) {
        Log.d(TAG, "connectToDeviceAsync iniziato per device: ${device.deviceName}")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Chiamando SerialManager.connectToDevice...")
                SerialManager.connectToDevice(context, device)
                Log.d(TAG, "SerialManager.connectToDevice completato")
                Log.d(TAG, "SerialManager.isConnected(): ${SerialManager.isConnected()}")

                CoroutineScope(Dispatchers.Main).launch {
                    if (SerialManager.isConnected()) {
                        Toast.makeText(context, "Arduino connesso con successo!", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Toast di successo mostrato")
                    } else {
                        Toast.makeText(context, "Connessione fallita", Toast.LENGTH_SHORT).show()
                        Log.w(TAG, "Connessione fallita - SerialManager.isConnected() = false")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Errore durante connessione: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Errore connessione: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}