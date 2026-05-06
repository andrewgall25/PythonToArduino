package com.andrea.pythontoarduino

import android.content.Context
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

object SerialManager {
    private var serialDevice: UsbSerialDevice? = null
    private var usbConnection: UsbDeviceConnection? = null
    private val writeMutex = Mutex()
    private val connectionMutex = Mutex()

    var isSerialReady = false
        private set

    private var readCallback: ((String) -> Unit)? = null

    fun setReadCallback(callback: (String) -> Unit) {
        readCallback = callback
    }

    suspend fun connectToDevice(context: Context, device: UsbDevice) {
        connectionMutex.withLock {
            Log.d("SerialManager", "connectToDevice chiamato per ${device.deviceName}")
            // Disconnetti se già connesso
            if (isSerialReady) {
                Log.d("SerialManager", "Disconnettendo connessione esistente...")
                disconnect()
            }

            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

            // Verifica che sia un dispositivo Arduino
            if (!isArduinoDevice(device)) {
                Log.w("SerialManager", "Dispositivo non Arduino: VID=${device.vendorId}, PID=${device.productId}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Dispositivo non supportato", Toast.LENGTH_SHORT).show()
                }
                return
            }

            usbConnection = usbManager.openDevice(device)
            if (usbConnection == null) {
                Log.e("SerialManager", "Connection null per device: ${device.deviceName}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Connessione fallita. Hai dato i permessi?", Toast.LENGTH_SHORT).show()
                }
                return
            }

            serialDevice = UsbSerialDevice.createUsbSerialDevice(device, usbConnection!!)
            if (serialDevice == null) {
                Log.e("SerialManager", "Driver USB null per VID=${device.vendorId}, PID=${device.productId}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Driver USB non trovato per questo dispositivo", Toast.LENGTH_SHORT).show()
                }
                cleanup()
                return
            }

            if (!serialDevice!!.open()) {
                Log.e("SerialManager", "Fallita apertura porta seriale per ${device.deviceName}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Impossibile aprire la porta seriale", Toast.LENGTH_SHORT).show()
                }
                cleanup()
                return
            }

            // Configurazione seriale con retry
            try {
                configureSerialPort()
                setupReadCallback()
                isSerialReady = true

                Log.d("SerialManager", "Connessione riuscita per ${device.deviceName}")
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Arduino connesso!", Toast.LENGTH_SHORT).show()
                }
                Log.d("SerialManager", "Seriale aperta su ${device.deviceName} (VID=${device.vendorId}, PID=${device.productId})")

            } catch (e: Exception) {
                Log.e("SerialManager", "Errore configurazione seriale: ${e.message}", e)
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Errore configurazione seriale: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                cleanup()
            }
        }
    }

    fun isArduinoDevice(device: UsbDevice): Boolean {
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

    private fun configureSerialPort() {
        serialDevice?.apply {
            setBaudRate(9600)
            setDataBits(UsbSerialInterface.DATA_BITS_8)
            setStopBits(UsbSerialInterface.STOP_BITS_1)
            setParity(UsbSerialInterface.PARITY_NONE)
            setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
        }

        // Piccola pausa per stabilizzare la connessione
        Thread.sleep(500)
    }

    private fun setupReadCallback() {
        serialDevice?.read { data ->
            try {
                val message = String(data, Charsets.UTF_8).trim()
                if (message.isNotEmpty()) {
                    Log.d("SerialManager", "Ricevuto: $message")
                    CoroutineScope(Dispatchers.Main).launch {
                        readCallback?.invoke(message)
                    }
                }
            } catch (e: Exception) {
                Log.e("SerialManager", "Errore lettura dati: ${e.message}")
            }
        }
    }

    fun send(data: String) {
        if (!isSerialReady) {
            Log.w("SerialManager", "Seriale non pronta, impossibile inviare: $data")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            writeMutex.withLock {
                try {
                    val message = data.trim() + "\n"
                    val bytes = message.toByteArray(Charsets.UTF_8)

                    serialDevice?.write(bytes)
                    Log.d("SerialManager", "Inviato (${bytes.size} bytes): $data")
                } catch (e: Exception) {
                    Log.e("SerialManager", "Errore invio seriale: ${e.message}")
                    // Se c'è un errore, potrebbe essere che la connessione si è interrotta
                    if (e.message?.contains("device") == true) {
                        isSerialReady = false
                    }
                }
            }
        }
    }

    fun disconnect() {
        cleanup()
        Log.d("SerialManager", "Disconnesso")
    }

    fun disconnectFromDevice(device: UsbDevice) {
        // Metodo specifico per disconnettere un device particolare
        Log.d("SerialManager", "Richiesta disconnessione per ${device.deviceName}")
        disconnect()
    }

    private fun cleanup() {
        isSerialReady = false
        serialDevice?.close()
        serialDevice = null
        usbConnection?.close()
        usbConnection = null
    }

    // Metodo per verificare lo stato della connessione
    fun isConnected(): Boolean {
        return isSerialReady && serialDevice != null && usbConnection != null
    }

    // Metodo per ottenere info sul dispositivo connesso
    fun getConnectedDeviceInfo(): String? {
        return if (isSerialReady && serialDevice != null) {
            "Connesso su porta seriale"
        } else null
    }
}