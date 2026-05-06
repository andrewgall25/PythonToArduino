package com.andrea.pythontoarduino.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andrea.pythontoarduino.R

/**
 * Composable per la barra superiore dell'applicazione con logo, titolo e indicatore cursore.
 */

val CreatoDisplay = FontFamily(
    Font(R.font.creatodisplay_regular, FontWeight.Normal)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    fileName: String,
    onFileNameChange: (String) -> Unit,
    onSaveClick: () -> Unit,
    onPasteClick: () -> Unit,
    onCopyClick: () -> Unit,
    onClearClick: () -> Unit,
    cursorPosition: Pair<Int, Int>
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(55.dp) // 🎯 Altezza richiesta: 55.dp
            .shadow(8.dp)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF162F72),
                        Color(0xFF2A4D6E),
                        Color(0xFF2A4D6C)
                    )
                )
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp), // 📏 Ridotto padding verticale per guadagnare spazio
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(32.dp)
                    .shadow(4.dp, CircleShape)
                    .clip(CircleShape)
            )

            Spacer(modifier = Modifier.width(14.dp))

            // Colonna per nome file e indicatore cursore
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center, // 🎯 Cambiato da SpaceEvenly a Center
                horizontalAlignment = Alignment.Start
            ) {
                FileNameTextField(
                    fileName = fileName,
                    onFileNameChange = onFileNameChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(26.dp) // 🎯 Lascia 2.dp di margine sopra/sotto rispetto ai 24.dp interni
                )

                Text(
                    text = "Ln: ${cursorPosition.first}, Col: ${cursorPosition.second} • UTF-8",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = CreatoDisplay,
                    modifier = Modifier.padding(top = 1.dp) // 🎯 Piccolo spazio sopra per separazione
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Pulsante salva
            IconButton(onClick = onSaveClick) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Salva",
                    tint = Color.White
                )
            }

            // Pulsante menu dropdown
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
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
                }
            }
        }
    }
}

/**
 * Composable per la riga delle schede di navigazione.
 * @param selectedTabIndex L'indice della scheda attualmente selezionata.
 * @param tabs La lista dei titoli delle schede.
 * @param onTabSelected Callback chiamato quando una scheda viene selezionata.
 */
@Composable
fun AppTabRow(selectedTabIndex: Int, tabs: List<String>, onTabSelected: (Int) -> Unit) {
    val yellow = Color(0xFFFFCF58)
    val pink = Color(0xFFE91E63)

    TabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = Modifier
            .height(35.dp)
            .background(Color(0xFF1E1E1E)),
        indicator = { tabPositions ->
            TabRowDefaults.Indicator(
                modifier = Modifier
                    .tabIndicatorOffset(tabPositions[selectedTabIndex])
                    .height(4.dp),
                color = yellow
            )
        },
        divider = {}
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = selectedTabIndex == index
            Tab(
                selected = isSelected,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (isSelected) yellow else pink,
                        fontFamily = CreatoDisplay
                    )
                }
            )
        }
    }
}
