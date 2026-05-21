package com.andrea.pythontoarduino.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.andrea.pythontoarduino.server.AssetServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.IOException
import android.app.Activity
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.compose.ui.graphics.toArgb
import android.util.TypedValue // Import necessario per TypedValue
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * JSBridge class to facilitate communication between JavaScript in the WebView and Kotlin.
 * It provides methods that JavaScript can call.
 * This class is now defined here to be part of the persistent scope for the WebView.
 *
 * @param onEditorReadyCallback A callback to notify Kotlin when Monaco Editor is fully initialized.
 * @param onTextChangedNotificationCallback A callback to notify Kotlin when the text in the editor changes.
 * @param onCursorPositionChangedCallback A callback to notify Kotlin when the cursor position changes.
 */
class JSBridge(
    private val context: Context,
    private val webView: WebView,
    private val onEditorReadyCallback: () -> Unit,
    private val onTextChangedNotificationCallback: () -> Unit,
    private val onCursorPositionChangedCallback: (Int, Int) -> Unit
) {
    private val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    private var isScrolling = false

    fun setScrolling(value: Boolean) { isScrolling = value }

    @JavascriptInterface
    fun onEditorReady() {
        onEditorReadyCallback()
    }

    @JavascriptInterface
    fun onTextChangedNotification() {
        onTextChangedNotificationCallback()
    }

    @JavascriptInterface
    fun getClipboardText(): String {
        val clip = clipboardManager.primaryClip
        return if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(context).toString()
        } else {
            ""
        }
    }

    @JavascriptInterface
    fun onEditorTouched() {
        if (isScrolling) return
        (context as? Activity)?.runOnUiThread {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            webView.isFocusable = true
            webView.isFocusableInTouchMode = true
            webView.requestFocus()
            imm.showSoftInput(webView, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    @JavascriptInterface
    fun onCursorPositionChanged(lineNumber: Int, column: Int) {
        onCursorPositionChangedCallback(lineNumber, column)
    }

    @JavascriptInterface
    fun onEditorTouchedAt(x: Float, y: Float) {
        if (isScrolling) return
        (context as? Activity)?.runOnUiThread {
            showEditorPopupAt(x, y)
        }
    }

    @SuppressLint("InflateParams")
    fun showEditorPopupAt(x: Float, y: Float) {
        // Container verticale per le voci
        val popupView = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(12, 12, 12, 12)

            // 1. Il GradientDrawable gestisce lo sfondo e gli angoli
            background = GradientDrawable().apply {
                // Riferimento esplicito a android.graphics.Color
                setColor(android.graphics.Color.parseColor("#F9F9F9")) // colore di sfondo leggero
                cornerRadius = 16f // angoli arrotondati
                setStroke(1, android.graphics.Color.parseColor("#CCCCCC")) // bordo sottile
            }

            // L'ELEVATION VA SULLA VIEW (LinearLayout)
            elevation = 8f // Aggiunge l'ombra morbida (richiede API 21+)
        }

        // Creiamo il popup (DEVE ESSERE DEFINITO PRIMA DI ESSERE USATO)
        val popupWindow = PopupWindow(
            popupView,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            elevation = 12f // Elevazione aggiuntiva per la finestra stessa
            isOutsideTouchable = true
            // Riferimento esplicito a android.graphics.Color.TRANSPARENT
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            animationStyle = android.R.style.Animation_Dialog // animazione comparsa
        }

        // Funzione helper per aggiungere le voci con effetto al click
        fun addMenuItem(label: String, action: () -> Unit) {

            // --- INIZIO CORREZIONE DEL CRASH ---
            val outValue = TypedValue()
            context.theme.resolveAttribute(android.R.attr.selectableItemBackground, outValue, true)
            val selectableBackgroundResourceId = outValue.resourceId
            // --- FINE CORREZIONE DEL CRASH ---

            val textView = TextView(context).apply {
                text = label
                textSize = 16f
                setPadding(24, 18, 24, 18)
                // Riferimento esplicito a android.graphics.Color
                setTextColor(android.graphics.Color.parseColor("#333333"))

                // 3. Ora usiamo l'ID della risorsa Drawable effettiva che abbiamo risolto
                setBackgroundResource(selectableBackgroundResourceId)

                setOnClickListener {
                    action()
                    popupWindow.dismiss() // Ora 'popupWindow' è definito!
                }
            }
            popupView.addView(textView)

            // Divider leggero tra le voci
            val divider = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
                ).apply { setMargins(0, 0, 0, 0) }
                // Riferimento esplicito a android.graphics.Color
                setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
            }
            popupView.addView(divider)
        }

        // Aggiungi tutte le voci del menu
        addMenuItem("Copia") { copyToClipboard(webView, context) }
        addMenuItem("Incolla") { pasteFromClipboard(webView, context) }
        addMenuItem("Seleziona tutto") { webView.evaluateJavascript("editor.setSelection(editor.getModel().getFullModelRange());", null) }
        addMenuItem("Seleziona riga") {
            webView.evaluateJavascript("""
            const pos = editor.getPosition();
            editor.setSelection({
                startLineNumber: pos.lineNumber,
                startColumn: 1,
                endLineNumber: pos.lineNumber,
                endColumn: editor.getModel().getLineMaxColumn(pos.lineNumber)
            });
            """.trimIndent(), null)
        }
        addMenuItem("Commenta riga") { webView.evaluateJavascript("editor.getAction('editor.action.commentLine').run();", null) }
        addMenuItem("Formatta codice") { webView.evaluateJavascript("editor.getAction('editor.action.formatDocument').run();", null) }

        // Mostra il popup in posizione toccata
        popupWindow.showAtLocation(webView, Gravity.NO_GRAVITY, x.toInt(), y.toInt())
    }
}

fun pasteFromClipboard(webView: WebView, context: Context) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clipData = clipboard.primaryClip
    val textToPaste = clipData?.getItemAt(0)?.coerceToText(context)?.toString() ?: ""

    if (textToPaste.isNotEmpty()) {
        val jsCode = """
            (function() {
                const editor = window.editor;
                if (editor) {
                    const selection = editor.getSelection();
                    editor.executeEdits(null, [{
                        range: selection,
                        text: ${toJsString(textToPaste)},
                        forceMoveMarkers: true
                    }]);
                    editor.focus();
                }
            })();
        """.trimIndent()

        webView.post {
            webView.evaluateJavascript(jsCode, null)
        }
    }
}

fun copyToClipboard(webView: WebView, context: Context) {
    webView.evaluateJavascript("window.getCode();") { result ->
        val code = try {
            JSONArray("[$result]").getString(0)
        } catch (e: Exception) {
            ""
        }
        if (code.isNotEmpty()) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("code", code)
            clipboard.setPrimaryClip(clip)
        }
    }
}

fun clearEditor(webView: WebView) {
    val jsCode = "window.setCode('');"
    webView.post {
        webView.evaluateJavascript(jsCode, null)
    }
}

private fun toJsString(text: String): String {
    return buildString {
        append('"')
        for (char in text) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> {} // ignora carriage return
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
        append('"')
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PythonToArduinoUI(
    pythonCode: String,
    output: String,
    usbStatus: String,
    serialData: String,
    isPythonRunning: Boolean,
    isUsbChecking: Boolean,
    showUsbStatusMessage: Boolean,
    consoleInput: String,
    fileName: String,
    onCodeChange: (String) -> Unit,
    onRunPython: (String) -> Unit,
    onCheckUsb: () -> Unit,
    onHideUsbStatusMessage: () -> Unit,
    onConsoleInputChange: (String) -> Unit,
    onSendConsoleInput: (String) -> Unit,
    onCancelPythonExecution: () -> Unit,
    onFileNameChange: (String) -> Unit,
    onSaveFile: (String, String) -> Unit,
    onCompileAndFlash: (String) -> Unit = { _ -> }, // 👈 Aggiungi con default vuoto
    onClearOutput: () -> Unit = {},
    onCopyOutput: () -> Unit = {}
) {
    val context = LocalContext.current
    val persistentWebView = remember { WebView(context) }
    val serverPort = 8080
    val coroutineScope = rememberCoroutineScope()

    var editorReady by remember { mutableStateOf(false) }
    var textChangeSignal by remember { mutableIntStateOf(0) }
    val codeAlreadySet = remember { mutableStateOf(false) }
    var cursorPosition by remember { mutableStateOf(Pair(1, 1)) } // Stato per riga e colonna

    val editorAlpha by animateFloatAsState(targetValue = if (editorReady) 1f else 0f)

    // ✅ CREA jsBridge QUI, fuori dal DisposableEffect
    val jsBridge = remember {
        JSBridge(
            context,
            persistentWebView,
            onEditorReadyCallback = { editorReady = true },
            onTextChangedNotificationCallback = { textChangeSignal++ },
            onCursorPositionChangedCallback = { line, column ->
                cursorPosition = Pair(line, column)
                Log.d("CodeEditorSection", "Cursor position updated: Row $line, Col $column")
            }
        )
    }


    DisposableEffect(context) {
        var assetServer: AssetServer? = null
        val serverJob = coroutineScope.launch {
            assetServer = AssetServer(context, serverPort)
            try {
                withContext(Dispatchers.IO) { assetServer?.start() }
                Log.d("AssetServer", "Local HTTP server started on http://localhost:$serverPort")

                persistentWebView.apply {
                    setBackgroundColor(0)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
                    clipToPadding = false
                    setPadding(0, 0, 0, 0)
                    isFocusable = false
                    isFocusableInTouchMode = false
                    isClickable = true
                    isLongClickable = true
                    outlineProvider = null
                    clipToOutline = false
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        WebView.setWebContentsDebuggingEnabled(true)
                    }
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.allowContentAccess = true
                    settings.textZoom = 100
                    settings.defaultFontSize = 14
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.allowFileAccessFromFileURLs = true
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                            consoleMessage?.let {
                                Log.d("WebViewConsole", "${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                            }
                            return super.onConsoleMessage(consoleMessage)
                        }
                    }
                    webViewClient = WebViewClient()

                    addJavascriptInterface(jsBridge, "Android")

                    if (persistentWebView.url != "http://localhost:$serverPort/editor.html") {
                        persistentWebView.loadUrl("http://localhost:$serverPort/editor.html")
                    }
                }
            } catch (e: IOException) {
                Log.e("AssetServer", "Failed to start local HTTP server: ${e.message}")
            }
        }

        onDispose {
            serverJob.cancel()
            assetServer?.stop()
            Log.d("AssetServer", "Local HTTP server stopped.")
        }
    }

    LaunchedEffect(textChangeSignal) {
        if (editorReady && textChangeSignal > 0) {
            Log.d("CodeEditorSection", "Fetching code from WebView due to textChangeSignal.")
            persistentWebView.evaluateJavascript("window.getCode();", ValueCallback { result ->
                val cleanedResult = try {
                    JSONArray("[$result]").getString(0)
                } catch (e: Exception) {
                    Log.e("CodeEditorSection", "Errore parsing codice: ${e.message}")
                    ""
                }
                onCodeChange(cleanedResult)
                Log.d("CodeEditorSection", "Code retrieved: ${cleanedResult.take(50)}...")
            })
        }
    }

    LaunchedEffect(editorReady, pythonCode) {
        if (editorReady && !codeAlreadySet.value) {
            persistentWebView.evaluateJavascript("setCode(${toJsString(pythonCode)});", null)
            codeAlreadySet.value = true
            Log.d("CodeEditorSection", "Codice iniziale impostato per la prima volta.")
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("CODE", "CONSOLE", "DEBUG")

    Scaffold(
        topBar = {
            Column {
                AppTopBar(
                    fileName = fileName,
                    onFileNameChange = onFileNameChange,
                    onSaveClick = { onSaveFile(pythonCode, fileName) },
                    onPasteClick = { pasteFromClipboard(persistentWebView, context) },
                    onCopyClick = { copyToClipboard(persistentWebView, context) },
                    onClearClick = { clearEditor(persistentWebView) },
                    cursorPosition = cursorPosition
                )
                AppTabRow(selectedTabIndex = selectedTabIndex, tabs = tabs) { index ->
                    selectedTabIndex = index
                }
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF121921), Color(0xFF002B44)),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        ) {
            UsbStatusMessage(
                showUsbStatusMessage = showUsbStatusMessage,
                usbStatus = usbStatus,
                onHideUsbStatusMessage = onHideUsbStatusMessage,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTabIndex) {
                    0 -> CodeEditorSection(
                        webView = persistentWebView,
                        onCodeChange = onCodeChange,
                        jsBridge = jsBridge,
                        modifier = Modifier.fillMaxSize().alpha(editorAlpha),
                        editorReady = editorReady,
                        textChangeSignal = textChangeSignal,
                        pythonCode = pythonCode,
                        isPythonRunning = isPythonRunning,
                        isUsbChecking = isUsbChecking,
                        onCheckUsb = onCheckUsb,
                        onRunPython = onRunPython,
                        onCancelPythonExecution = onCancelPythonExecution,
                        onCompileAndFlash = onCompileAndFlash,
                    )

                    1 -> ConsoleSection(
                        output = output,
                        consoleInput = consoleInput,
                        onConsoleInputChange = onConsoleInputChange,
                        onSendConsoleInput = onSendConsoleInput,
                        onClearOutput = onClearOutput,
                        onCopyOutput = onCopyOutput,
                        modifier = Modifier.fillMaxSize(),
                        isPythonRunning = isPythonRunning,
                        onCancelPythonExecution = onCancelPythonExecution,
                    )

                    2 -> DebugInfoSection(
                        usbStatus = usbStatus,
                        isPythonRunning = isPythonRunning,
                        isUsbChecking = isUsbChecking,
                        serialData = serialData,
                        modifier = Modifier.fillMaxSize().padding(16.dp)
                    )
                }
            }
        }
    }
}