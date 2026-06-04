package com.andrea.pythontoarduino.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.pythontoarduino.ui.theme.HtmlColors
import com.andrea.pythontoarduino.ui.theme.JetBrainsMono
import com.andrea.pythontoarduino.ui.theme.Manrope

@Composable
fun AppTopBar(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onPasteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onClearClick: () -> Unit,
    cursorPosition: Pair<Int, Int>,
    onCheckUsb: () -> Unit = {},
    onCompileAndFlash: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(55.dp)
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Terminal icon + "PythonToArduino" title
            Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = "Terminal",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "PythonToArduino",
                fontFamily = Manrope,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            // Settings button
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Save") },
                    onClick = {
                        expanded = false
                        onSaveClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Paste here") },
                    onClick = {
                        expanded = false
                        onPasteClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Copy code") },
                    onClick = {
                        expanded = false
                        onCopyClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Delete all") },
                    onClick = {
                        expanded = false
                        onClearClick()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Check USB") },
                    onClick = {
                        expanded = false
                        onCheckUsb()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Compile & Flash") },
                    onClick = {
                        expanded = false
                        onCompileAndFlash()
                    }
                )
            }
        }
    }
}

data class BottomNavItem(val label: String, val icon: ImageVector)

@Composable
fun BottomNavBar(selectedTabIndex: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        BottomNavItem("CODE", Icons.Default.Code),
        BottomNavItem("CONSOLE", Icons.Default.Terminal),
        BottomNavItem("DEBUG", Icons.Default.BugReport),
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = HtmlColors.SurfaceContainerLow,
        tonalElevation = 0.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top indicator bar on active tab
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            ) {
                for (i in items.indices) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(
                                if (i == selectedTabIndex) HtmlColors.Tertiary
                                else Color.Transparent
                            )
                    )
                }
            }

            NavigationBar(
                modifier = Modifier.fillMaxWidth(),
                containerColor = Color.Transparent,
                tonalElevation = 0.dp
            ) {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { onTabSelected(index) },
                        icon = {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.label,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                lineHeight = 18.sp
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = HtmlColors.Tertiary,
                            selectedTextColor = HtmlColors.Tertiary,
                            unselectedIconColor = HtmlColors.OnSurfaceVariant,
                            unselectedTextColor = HtmlColors.OnSurfaceVariant,
                            indicatorColor = Color.Transparent
                        ),
                        alwaysShowLabel = true
                    )
                }
            }
        }
    }
}
