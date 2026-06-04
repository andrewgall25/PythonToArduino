package com.andrea.pythontoarduino.ui

import android.annotation.SuppressLint
import android.util.Log
import android.view.GestureDetector
import android.view.MotionEvent
import android.webkit.ValueCallback
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable function to display and manage the Monaco Editor within a persistent Android WebView.
 * This component now receives the WebView instance and state from a higher-level parent,
 * ensuring the WebView is not reloaded on every tab change.
 *
 * @param webView The persistent WebView instance to be displayed.
 * @param onCodeChange Callback function to be invoked when the code in the editor changes.
 * @param modifier Modifier to be applied to the underlying AndroidView.
 * @param editorReady A state variable from the parent that indicates when the Monaco editor is ready.
 * @param textChangeSignal A state variable from the parent that signals a text change in the editor.
 */
@OptIn(ExperimentalComposeUiApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CodeEditorSection(
    webView: WebView,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    editorReady: Boolean,
    textChangeSignal: Int,
    jsBridge: JSBridge,
    pythonCode: String,
    isPythonRunning: Boolean,
    isUsbChecking: Boolean,
    onCheckUsb: () -> Unit,
    onRunPython: (String) -> Unit,
    onCancelPythonExecution: () -> Unit,
    onCompileAndFlash: (String) -> Unit,
) {
    val currentOnCodeChange = rememberUpdatedState(onCodeChange)

    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                webView.apply {
                    val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                        override fun onLongPress(e: MotionEvent) {
                            jsBridge.onEditorTouchedAt(e.rawX, e.rawY)
                        }

                        override fun onDoubleTap(e: MotionEvent): Boolean {
                            jsBridge.onEditorTouchedAt(e.rawX, e.rawY)
                            return true
                        }

                        override fun onDown(e: MotionEvent): Boolean {
                            return true
                        }
                    })

                    setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)
                        false
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { /* nessun aggiornamento necessario qui */ }
        )
    }

    LaunchedEffect(textChangeSignal) {
        if (editorReady && textChangeSignal > 0) {
            Log.d("CodeEditorSection", "Fetching code from WebView due to textChangeSignal.")
            webView.evaluateJavascript("window.getCode();") { result ->
                val cleanedResult = try {
                    org.json.JSONArray("[$result]").getString(0)
                } catch (e: Exception) {
                    Log.e("CodeEditorSection", "Parsing failed: ${e.message}")
                    ""
                }
                currentOnCodeChange.value(cleanedResult)
                Log.d("CodeEditorSection", "Code retrieved: ${cleanedResult.take(50)}...")
            }
        }
    }
}
