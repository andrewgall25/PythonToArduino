package com.andrea.pythontoarduino.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Build // Icona per Compile & Flash
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrea.pythontoarduino.R

/**
 * Composable per i pulsanti di azione in fondo all'interfaccia.
 * @param pythonCode Il codice Python attuale (usato per abilitare/disabilitare il pulsante Esegui).
 * @param isPythonRunning Indica se lo script Python è in esecuzione.
 * @param isUsbChecking Indica se il controllo USB è in corso.
 * @param onCheckUsb Callback per avviare il controllo USB.
 * @param onRunPython Callback per eseguire lo script Python.
 * @param onCancelPythonExecution Callback per interrompere l'esecuzione Python.
 * @param onCompileAndFlash Callback per compilare e flashare su Arduino.
 */
val CreatoDisplay2 = FontFamily(
    Font(R.font.creatodisplay_regular)
)

@Composable
fun ActionButtons(
    pythonCode: String,
    isPythonRunning: Boolean,
    isUsbChecking: Boolean,
    onCheckUsb: () -> Unit,
    onRunPython: (String) -> Unit,
    onCancelPythonExecution: () -> Unit,
    onCompileAndFlash: (String) -> Unit, // NUOVO PARAMETRO
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Controlla USB" Button
        val usbButtonBackground = if (isUsbChecking || isPythonRunning) {
            Modifier.background(Color.Gray.copy(alpha = 0.6f))
        } else {
            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF86D957), Color(0xFF249150))))
        }

        Surface(
            modifier = Modifier
                .size(50.dp)
                .clickable(enabled = !isUsbChecking && !isPythonRunning, indication = null, interactionSource = remember { MutableInteractionSource() }) {
                    onCheckUsb()
                }
                .shadow(if (isUsbChecking || isPythonRunning) 2.dp else 8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .then(usbButtonBackground)
                .animateContentSize(),
            color = Color.Transparent
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isUsbChecking) {
                    val rotation = remember { Animatable(0f) }
                    LaunchedEffect(isUsbChecking) {
                        if (isUsbChecking) {
                            rotation.animateTo(
                                targetValue = 360f * 3,
                                animationSpec = tween(durationMillis = 3000, easing = LinearEasing)
                            )
                        } else {
                            rotation.snapTo(0f)
                        }
                    }
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).rotate(rotation.value),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Usb,
                        contentDescription = "Check USB",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // "Compile & Flash" Button
        val compileFlashButtonBackground = if (isUsbChecking || isPythonRunning || pythonCode.isBlank()) {
            Modifier.background(Color.Gray.copy(alpha = 0.6f))
        } else {
            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFFA726), Color(0xFFF57C00))))
        }

        Surface(
            modifier = Modifier
                .size(50.dp)
                .clickable(
                    enabled = !isUsbChecking && !isPythonRunning && pythonCode.isNotBlank(),
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onCompileAndFlash(pythonCode)
                }
                .shadow(if (isUsbChecking || isPythonRunning || pythonCode.isBlank()) 2.dp else 8.dp, RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .then(compileFlashButtonBackground)
                .animateContentSize(),
            color = Color.Transparent
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = "Compile & Flash",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // "Play/Stop" Button
        val playStopButtonBackground = if (isUsbChecking) {
            Modifier.background(Color.Gray.copy(alpha = 0.6f))
        } else if (isPythonRunning) {
            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFEF5350), Color(0xFFD32F2F))))
        } else {
            Modifier.background(Brush.horizontalGradient(listOf(Color(0xFF86BBD8), Color(0xFF346295))))
        }

        Surface(
            modifier = Modifier
                .width(100.dp)
                .height(50.dp)
                .clickable(
                    enabled = !isUsbChecking && (if (isPythonRunning) true else pythonCode.isNotBlank()),
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    if (isPythonRunning) {
                        onCancelPythonExecution()
                    } else {
                        onRunPython(pythonCode)
                    }
                }
                .shadow(
                    if (isUsbChecking || (isPythonRunning || !pythonCode.isNotBlank())) 2.dp else 12.dp,
                    RoundedCornerShape(20.dp)
                )
                .clip(RoundedCornerShape(20.dp))
                .then(playStopButtonBackground)
                .animateContentSize()
                .padding(horizontal = 16.dp),
            color = Color.Transparent
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isPythonRunning) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop Execution",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Stop",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CreatoDisplay2
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Execute",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Run",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = CreatoDisplay2
                    )
                }
            }
        }
    }
}