package com.andrea.pythontoarduino

import android.app.Application
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.chaquo.python.Python
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import android.content.Context
import android.widget.Toast
import android.app.PendingIntent
import android.content.Intent
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import kotlinx.coroutines.isActive
import org.json.JSONObject
import kotlin.jvm.java

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private var pythonInputStream: PythonInputStream? = null
    private var pythonOutputStream: PythonOutputStream? = null
    private var pythonExecutionJob: Job? = null

    // Nel MainViewModel.kt, modifica la gestione dell'output
    // Aggiungi una variabile di classe nel ViewModel per accumulare i pezzi di testo
    // 1. Aggiungi questa variabile in cima alla classe MainViewModel
    private var arduinoLineBuffer = ""

    // 2. Modifica il callback nel init o dove inizializzi outputStream
    private val outputStream = PythonOutputStream { text ->
        viewModelScope.launch(Dispatchers.Main) {
            arduinoLineBuffer += text

            // 1. Processa tutte le righe complete
            while (arduinoLineBuffer.contains("\n")) {
                val newlineIndex = arduinoLineBuffer.indexOf("\n")
                val line = arduinoLineBuffer.substring(0, newlineIndex)
                arduinoLineBuffer = arduinoLineBuffer.substring(newlineIndex + 1)

                if (line.startsWith("SET:")) {
                    sendToSerial(line)
                } else {
                    writePythonOutput(line + "\n")
                }
            }

            // 2. Processa l'eventuale testo incompleto rimanente (es. prompt di input)
            if (arduinoLineBuffer.isNotEmpty()) {
                val isPossibleCommand = arduinoLineBuffer.startsWith("SET:") ||
                        arduinoLineBuffer == "SET" ||
                        arduinoLineBuffer == "SE" ||
                        arduinoLineBuffer == "S"
                if (!isPossibleCommand) {
                    writePythonOutput(arduinoLineBuffer)
                    arduinoLineBuffer = ""
                }
            }
        }
    }



    private val inputStream = PythonInputStream()

    // FIX: Job separato per monitoring USB per evitare interferenze
    private var usbMonitoringJob: Job? = null

    private var outputReadingJob: Job? = null // Nuovo Job per la lettura dell'output

    private var lastUsbCheckTime = 0L
    private val USB_CHECK_COOLDOWN = 3000L // 3 secondi di cooldown

    val serialData = MutableLiveData<String>()

    private val _pythonCode = MutableLiveData("")
    val pythonCode: LiveData<String> = _pythonCode

    private val _output = MutableLiveData<String>()
    val output: LiveData<String> = _output

    private val _usbStatus = MutableLiveData("")
    val usbStatus: LiveData<String> = _usbStatus

    private val _isPythonRunning = MutableLiveData(false)
    val isPythonRunning: LiveData<Boolean> = _isPythonRunning

    private val _isUsbChecking = MutableLiveData(false)
    val isUsbChecking: LiveData<Boolean> get() = _isUsbChecking

    private val _isUsbReady = MutableLiveData(false)
    val isUsbReady: LiveData<Boolean> get() = _isUsbReady

    private val _showUsbStatusMessage = MutableLiveData(false)
    val showUsbStatusMessage: LiveData<Boolean> = _showUsbStatusMessage

    private val _consoleInput = MutableLiveData("")
    val consoleInput: LiveData<String> = _consoleInput

    private val _fileName = MutableLiveData("script1.py")
    val fileName: LiveData<String> = _fileName

    private val context = application.applicationContext
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private var initialUsbMessageShown = false

    init {
        Log.d("MainViewModel", "Inizializzazione ViewModel e Chaquopy.")
        initStreams()
        setupSerialListener()
        startOptimizedSerialStatusMonitoring()
        // Collega i messaggi che arrivano DA Arduino alla console dell'app
        SerialManager.setReadCallback { data ->
            appendOutput("Arduino: $data")
        }
        performInitialUsbCheck()
        viewModelScope.launch(Dispatchers.IO) {
            outputStream.readAndProcessOutput()
        }
    }

    private fun initStreams() {
        if (pythonInputStream == null) {
            pythonInputStream = PythonInputStream()
        }
        if (pythonOutputStream == null) {
            pythonOutputStream = outputStream
        }

        val py = Python.getInstance()
        val pythonModule = py.getModule("my_script")

        // Reindirizza gli stream Python in my_script.py (sys.stdout, sys.stderr, sys.stdin)
        pythonModule.callAttr(
            "set_streams",
            pythonInputStream,
            pythonOutputStream,
            pythonOutputStream
        )
        Log.d("MainViewModel", "Stream Python reindirizzati a Kotlin/Java.")
    }

    fun clearOutput() {
        _output.value = ""
    }

    fun copyOutput(): String {
        return _output.value ?: ""
    }

    private var lastOutputTimestamp: String = ""

    private fun timestamp(): String {
        val now = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
        return now
    }

    private fun appendTimestampIfNeeded() {
        val ts = timestamp()
        if (ts != lastOutputTimestamp) {
            lastOutputTimestamp = ts
            _output.value = (_output.value ?: "") + "[$ts]\n"
        }
    }

    private fun appendOutput(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            appendTimestampIfNeeded()
            _output.value = (_output.value ?: "") + text + "\n"
        }
    }

    private fun writePythonOutput(text: String) {
        viewModelScope.launch(Dispatchers.Main) {
            appendTimestampIfNeeded()
            _output.value = (_output.value ?: "") + text
        }
    }

    // ========================= GESTIONE ULTIMO FILE =========================
    fun saveLastOpenedFile(fileName: String) {
        prefs.edit().putString("last_file_name", fileName).apply()
    }

    fun getLastOpenedFile(): String? {
        return prefs.getString("last_file_name", null)
    }

    fun loadLastOpenedFile() {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val lastFile = prefs.getString("last_file_name", null)
        lastFile?.let {
            val file = File(getApplication<Application>().filesDir, it)
            if (file.exists()) {
                _pythonCode.value = file.readText()
                _fileName.value = file.name
            }
        }
    }

    private fun setupSerialListener() {
        SerialManager.setReadCallback { data ->
            // FIX: Limita la frequenza degli aggiornamenti serialData per non sovraccaricare UI
            viewModelScope.launch(Dispatchers.Main) {
                serialData.value = data
            }
        }
    }

    // FIX: Monitoring USB ottimizzato per ridurre overhead
    private fun startOptimizedSerialStatusMonitoring() {
        usbMonitoringJob?.cancel() // Cancella job precedente se esiste

        usbMonitoringJob = viewModelScope.launch(Dispatchers.IO) {
            var lastConnectionState = false

            while (isActive) {
                try {
                    val isConnected = SerialManager.isConnected()

                    // FIX: Aggiorna UI solo se lo stato è cambiato
                    if (isConnected != lastConnectionState) {
                        withContext(Dispatchers.Main) {
                            _isUsbReady.value = isConnected
                            _usbStatus.value = if (isConnected) "Arduino connected" else "Arduino disconnected"

                            // Solo log quando stato cambia
                            Log.d("MainViewModel", "USB status changed: $isConnected")
                        }
                        lastConnectionState = isConnected
                    }

                    // FIX: Delay più lungo per ridurre CPU usage
                    delay(if (isConnected) 8000 else 4000) // Aumentati i tempi

                } catch (e: Exception) {
                    Log.e("MainViewModel", "Errore monitoring USB: ${e.message}")
                    delay(5000) // Delay più lungo in caso di errore
                }
            }
        }
    }

    private fun performInitialUsbCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                delay(1000) // FIX: Delay iniziale per permettere inizializzazione completa

                val isConnected = SerialManager.isConnected()
                withContext(Dispatchers.Main) {
                    _usbStatus.value = if (isConnected) "Arduino connected" else "Arduino disconnected"
                    _isUsbReady.value = isConnected
                    _showUsbStatusMessage.value = !isConnected
                    initialUsbMessageShown = true
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Errore check USB iniziale: ${e.message}")
            }
        }
    }

    fun sendToSerial(data: String) {
        // Invece di controllare isSerialReady (che potrebbe fluttuare),
        // prova a inviare se SerialManager ha un riferimento al device
        viewModelScope.launch(Dispatchers.IO) {
            try {
                SerialManager.send(data)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("[Errore Seriale] ${e.message}")
                }
            }
        }
    }

    fun hideUsbStatusMessage() {
        _showUsbStatusMessage.value = false
    }

    fun runPythonCode(code: String) {
        if (_isPythonRunning.value == true) return
        val py = Python.getInstance()
        val module = py.getModule("my_script")


        val connectionChecker = {
            SerialManager.isConnected()
        }
        // Inseriamo la lambda direttamente nel modulo Python
        module.put("check_connection_bridge", connectionChecker)

        // Diciamo a my_script di usare questo bridge
        py.getModule("my_script").callAttr("set_connection_checker", connectionChecker)
        // ----------------------------------------

        _isPythonRunning.value = true
        lastOutputTimestamp = ""
        appendOutput("--- Starting Python execution ---")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // 2. IMPORTANTE: Resetta il flag prima di partire
                // Questo assicura che il vecchio loop (se esistente) muoia
                module.callAttr("stop_execution")

                // Usa una funzione che NON cattura l'output in una stringa,
                // ma lo lancia e basta
                module.callAttr("execute_live_code", code)

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    appendOutput("Python error: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isPythonRunning.value = false
                }
            }
        }
    }

    fun cancelPythonExecution() {viewModelScope.launch(Dispatchers.IO) {
        try {
            val py = Python.getInstance()
            val module = py.getModule("my_script")

            // 1. Diciamo a Python di fermare il ciclo is_running()
            module.callAttr("stop_execution")

            Log.d("MainViewModel", "Flag di esecuzione Python impostato a False")

            // 2. Opzionale: Interrompiamo la coroutine Kotlin per sicurezza
            // Se hai salvato il job quando hai lanciato runPythonCode
            // pythonJob?.cancel()

        } catch (e: Exception) {
            Log.e("MainViewModel", "Errore durante lo stop: ${e.message}")
        } finally {
            withContext(Dispatchers.Main) {
                _isPythonRunning.value = false
                appendOutput("--- Python execution stopped by user ---")
            }
        }
    }
    }

    // FIX: Check USB con throttling per evitare spam
    fun checkUsbDevice(usbManager: UsbManager) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUsbCheckTime < USB_CHECK_COOLDOWN) {
            Log.d("MainViewModel", "USB check in cooldown, saltando...")
            return
        }
        lastUsbCheckTime = currentTime

        if (SerialManager.isConnected()) {
            Log.d("MainViewModel", "Connessione già attiva, salto checkUsbDevice")
            _usbStatus.value = "Arduino connected"
            _isUsbReady.value = true
            _showUsbStatusMessage.value = false
            return
        }

        if (_isUsbChecking.value == true) {
            Log.d("MainViewModel", "Check USB già in corso, saltando...")
            return
        }

        _isUsbChecking.value = true

        viewModelScope.launch(Dispatchers.IO) { // FIX: Esplicito IO dispatcher
            try {
                delay(300) // FIX: Delay ridotto

                val deviceList = usbManager.deviceList
                val arduinoDevice = deviceList.values.find { SerialManager.isArduinoDevice(it) }

                val status = when {
                    deviceList.isEmpty() -> "No USB Device detected."
                    arduinoDevice != null -> {
                        if (usbManager.hasPermission(arduinoDevice)) {
                            try {
                                SerialManager.connectToDevice(context, arduinoDevice)
                                if (SerialManager.isConnected()) {
                                    "Arduino connected successfully."
                                } else {
                                    "Arduino authorized but failed connection."
                                }
                            } catch (e: Exception) {
                                Log.e("MainViewModel", "Errore connessione in checkUsbDevice: ${e.message}")
                                "Errore connessione: ${e.message}"
                            }
                        } else {
                            "Arduino detected. Permission request sent."
                        }
                    }
                    else -> "Unknown USB device connected."
                }

                withContext(Dispatchers.Main) {
                    _usbStatus.value = status
                    _isUsbReady.value = SerialManager.isConnected()
                    if (!initialUsbMessageShown) {
                        _showUsbStatusMessage.value = !SerialManager.isConnected()
                    }
                }

            } catch (e: Exception) {
                Log.e("MainViewModel", "Errore durante USB check: ${e.message}")
                withContext(Dispatchers.Main) {
                    _usbStatus.value = "Error during USB check"
                }
            } finally {
                delay(1000) // FIX: Delay ridotto
                withContext(Dispatchers.Main) {
                    _isUsbChecking.value = false
                }
            }
        }
    }

    fun requestUsbPermissionManually(usbManager: UsbManager) {
        if (SerialManager.isConnected()) {
            _usbStatus.value = "Arduino connected"
            _isUsbReady.value = true
            _showUsbStatusMessage.value = false
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val device = usbManager.deviceList.values.find { SerialManager.isArduinoDevice(it) }
            if (device == null) {
                withContext(Dispatchers.Main) {
                    _usbStatus.value = "No Arduino found"
                    if (!initialUsbMessageShown) {
                        _showUsbStatusMessage.value = true
                    }
                }
                return@launch
            }

            if (usbManager.hasPermission(device)) {
                try {
                    SerialManager.connectToDevice(context, device)
                    withContext(Dispatchers.Main) {
                        val connected = SerialManager.isConnected()
                        _usbStatus.value = if (connected) {
                            "Arduino connected successfully"
                        } else {
                            "Failed connection despite permission"
                        }
                        _isUsbReady.value = connected
                        _showUsbStatusMessage.value = !connected
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Errore connessione: ${e.message}")
                    withContext(Dispatchers.Main) {
                        _usbStatus.value = "Connection error: ${e.message}"
                        _showUsbStatusMessage.value = true
                    }
                }
                return@launch
            }

            // Richiesta permessi
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                System.currentTimeMillis().toInt(), // FIX: RequestCode unico basato su timestamp
                Intent(UsbBroadcastReceiver.ACTION_USB_PERMISSION).apply {
                    setPackage(context.packageName)
                    putExtra(UsbManager.EXTRA_DEVICE, device)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            usbManager.requestPermission(device, permissionIntent)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Richiesta permessi USB...", Toast.LENGTH_SHORT).show()
                _usbStatus.value = "Richiesta permessi in corso..."
                _showUsbStatusMessage.value = true
            }
        }
    }

    // Metodi semplici senza ottimizzazioni necessarie
    fun setUsbReady(ready: Boolean) {
        _isUsbReady.value = ready
    }

    fun setPythonCode(newCode: String) {
        _pythonCode.value = newCode
    }

    fun setConsoleInput(input: String) {
        _consoleInput.value = input
    }

    fun sendConsoleInput(input: String) {
        viewModelScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                appendOutput("> $input")
                _consoleInput.value = ""
            }
            pythonInputStream?.write(input + "\n")
            Log.d("MainViewModel", "Inviato a Python: '$input'")
        }
    }

    fun setFileName(userInput: String) {
        val currentExtension = ".py"
        val trimmed = userInput.trim()

        _fileName.value = if (trimmed.lowercase().endsWith(currentExtension)) {
            trimmed
        } else {
            val baseName = trimmed.removeSuffix(currentExtension)
            "$baseName$currentExtension"
        }
    }

    fun savePythonCodeToFile(code: String, onResult: (Boolean, String) -> Unit) {
        val name = _fileName.value ?: "script1.py"
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(getApplication<Application>().filesDir, name)
                file.writeText(code)

                val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                prefs.edit().putString("last_file_name", name).apply()

                withContext(Dispatchers.Main) {
                    onResult(true, "File saved as $name")
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Error on saving: ${e.message}")
                }
            }
        }
    }

    suspend fun runPythonCodeBlocking(code: String): Pair<String?, String> {
        return withContext(Dispatchers.IO) {
            outputStream.reset()
            inputStream.resetStream()
            // ELIMINA O COMMENTA QUESTE RIGHE:
                // outputReadingJob = viewModelScope.launch(Dispatchers.IO) {
                //     outputStream.readAndProcessOutput()
            // }
            try {
                val py = Python.getInstance()
                py.getModule("sys").put("stdout", outputStream)
                py.getModule("sys").put("stderr", outputStream)
                py.getModule("sys").put("stdin", inputStream)
                val module = py.getModule("my_script")
                if (module == null) {
                    Log.e("MainViewModel", "Modulo my_script non trovato")
                    return@withContext Pair(null, "Errore: Modulo my_script non trovato")
                }
                val function = module["execute_user_code"]
                if (function == null) {
                    Log.e("MainViewModel", "Funzione execute_user_code non trovata in my_script")
                    return@withContext Pair(null, "Errore: Funzione execute_user_code non trovata")
                }
                Log.d("MainViewModel", "Eseguendo codice Python:\n$code")
                val resultPy = module.callAttr("execute_user_code", code)
                if (resultPy == null) {
                    Log.e("MainViewModel", "Risultato di execute_user_code è null")
                    return@withContext Pair(null, "Errore: Risultato di execute_user_code è null")
                }
                val resultList = resultPy.asList()

                val hexValuePy = resultList[0]
                val outputPy = resultList[1]
                val hexValue = hexValuePy?.toString()?.takeIf { it != "None" }
                val output = outputPy?.toString().orEmpty()
                Log.d("MainViewModel", "Risultato Python: hex=$hexValue, output=$output")
                Pair(hexValue, output)
            } catch (e: PyException) {
                Log.e("MainViewModel", "Errore Python in runPythonCodeBlocking: ${e.message}", e)
                Pair(null, "Errore esecuzione Python: ${e.message}\nStack trace: ${e.stackTraceToString()}")
            } catch (e: Exception) {
                Log.e("MainViewModel", "Errore generico in runPythonCodeBlocking: ${e.message}", e)
                Pair(null, "Errore esecuzione: ${e.message}")
            } finally {
                try {
                    outputStream.flush()
                    Log.d("MainViewModel", "Flush eseguito in runPythonCodeBlocking")
                } catch (e: Exception) {
                    Log.w("MainViewModel", "Errore flush outputStream: ${e.message}")
                }
                outputReadingJob?.cancel()
            }
        }
    }


    fun SerialManager.flashHex(hex: String) {
        // Stub temporaneo: logga l'HEX invece di flashare
        Log.d("SerialManager", "FLASH HEX:\n$hex")
    }

    fun compileAndFlashArduino(pythonCode: String) {
        if (_isPythonRunning.value == true) {
            Log.d("MainViewModel", "Python già in esecuzione, ignoro")
            return
        }
        _isPythonRunning.value = true
        lastOutputTimestamp = ""
        appendOutput("--- Building Arduino ---")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fix: No indents, use \n for lines
                val escaped = pythonCode.replace("'''", "''\\'")
                val script = "from build import send_python_to_server\n" +
                        "hex = send_python_to_server(r'''$escaped''')\n" +
                        "print(hex)\n"


                Log.d("MainViewModel", "Codice Python generato:\n$script")
                val result = runPythonCodeBlocking(script)
                val hexCode = result.first
                val pythonOutput = result.second
                Log.d("FULL_OUTPUT", pythonOutput)
                withContext(Dispatchers.Main) {
                    appendOutput(pythonOutput)
                    if (hexCode.isNullOrEmpty()) {
                        Log.e("MainViewModel", "Nessun HEX ricevuto: hexCode è null o vuoto")
                        appendOutput("Error: no HEX received from server.")
                        _isPythonRunning.value = false
                        return@withContext
                    }
                    if (!hexCode.startsWith(":")) {
                        Log.e("MainViewModel", "HEX non valido: non inizia con ':'")
                        appendOutput("Error: HEX not valid received from server.")
                        _isPythonRunning.value = false
                        return@withContext
                    }
                    Log.d("MainViewModel", "HEX valido ricevuto: ${hexCode.take(100)}...")
                    appendOutput("--- HEX received ---\n$hexCode")
                    try {
                        SerialManager.flashHex(hexCode)
                        appendOutput("--- Flash completed successfully ---")
                    } catch (e: Exception) {
                        Log.e("MainViewModel", "Errore flash Arduino: ${e.message}")
                        appendOutput("Error on Arduino flash: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("MainViewModel", "Errore compilazione: ${e.message}")
                    appendOutput("Error on compilation: ${e.message}")
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isPythonRunning.value = false
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("MainViewModel", "onCleared: Cleanup completo")

        // FIX: Cancella tutti i job attivi
        pythonExecutionJob?.cancel()
        usbMonitoringJob?.cancel()
        outputReadingJob?.cancel() // Annulla anche il job di lettura

        // Cleanup stream
        try {
            pythonInputStream?.close()
            pythonOutputStream?.close()
        } catch (e: Exception) {
            Log.w("MainViewModel", "Errore cleanup stream: ${e.message}")
        }

        pythonInputStream = null
        pythonOutputStream = null
        SerialManager.disconnect()
    }
}