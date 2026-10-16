package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.example.data.model.AidaExtremeMode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NegativeRed
import com.example.ui.theme.PositiveGreen
import com.example.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showImportDialog by remember { mutableStateOf(false) }
    var importInputText by remember { mutableStateOf("") }
    var newCustomTaskText by remember { mutableStateOf("") }
    var editingTaskIndex by remember { mutableStateOf<Int?>(null) }
    var editingTaskText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Einstellungen",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status-Meldungen
            if (state.infoMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PositiveGreen.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = state.infoMessage ?: "",
                        color = PositiveGreen,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (state.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NegativeRed.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = state.errorMessage ?: "",
                        color = NegativeRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // 1. Standard-Bockrunden
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Standard Bockrunden",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Anzahl der Runden, die bei einer neuen Bockphase standardmäßig hinzugefügt werden:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(4, 5, 8).forEach { r ->
                            FilterChip(
                                selected = state.defaultBockRounds == r,
                                onClick = { viewModel.setDefaultBockRounds(r) },
                                label = { Text("$r Runden") }
                            )
                        }
                    }
                }
            }

            // 2. AIDA Extreme Mode
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AIDA Extreme Mode",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zufällige Aufgaben für aktive Spieler im Abstand von 5 bis 25 Minuten während der Partie.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.size(12.dp))
                        Switch(
                            checked = state.aidaExtremeModeEnabled,
                            onCheckedChange = { viewModel.setAidaExtremeModeEnabled(it) },
                            modifier = Modifier.testTag("aida_extreme_switch")
                        )
                    }
                }
            }

            // 3. Eigene Extreme-Aufgaben
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Eigene Extreme-Aufgaben",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Standardmäßig sind ${AidaExtremeMode.DEFAULT_TASKS.size} Standardaufgaben aktiv. Hier können weitere eigene Aufgaben hinzugefügt, bearbeitet oder gelöscht werden:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Neue Aufgabe eingeben
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newCustomTaskText,
                            onValueChange = { newCustomTaskText = it },
                            label = { Text("Neue Aufgabe") },
                            placeholder = { Text("z.B. Bestelle eine Runde Wasser") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("new_aida_task_input")
                        )
                        Button(
                            onClick = {
                                if (newCustomTaskText.trim().isNotEmpty()) {
                                    viewModel.addAidaCustomTask(newCustomTaskText.trim())
                                    newCustomTaskText = ""
                                }
                            },
                            enabled = newCustomTaskText.trim().isNotEmpty(),
                            modifier = Modifier.testTag("add_aida_task_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Hinzufügen")
                        }
                    }

                    // Liste vorhandener eigener Aufgaben
                    if (state.aidaCustomTasks.isEmpty()) {
                        Text(
                            text = "Noch keine eigenen Aufgaben gespeichert.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.aidaCustomTasks.forEachIndexed { index, task ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${index + 1}. $task",
                                        fontSize = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Row {
                                        IconButton(
                                            onClick = {
                                                editingTaskIndex = index
                                                editingTaskText = task
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Edit,
                                                contentDescription = "Bearbeiten",
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteAidaCustomTask(index) },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = "Löschen",
                                                modifier = Modifier.size(16.dp),
                                                tint = NegativeRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Datensicherung (JSON Export & Import)
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Datensicherheit & Backup",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vollständige Sicherung aller Spielerprofile, Partien und Runden als strukturierte JSON-Datei.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.exportDatabase() },
                            enabled = !state.isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_json_button")
                        ) {
                            if (state.isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("JSON Export")
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                importInputText = ""
                                showImportDialog = true
                            },
                            enabled = !state.isImporting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_json_button")
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("JSON Import")
                        }
                    }

                    // Exportierte Daten anzeigen
                    state.exportJsonString?.let { json ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sicherungsdaten:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                TextButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Doppelkopf Backup", json)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(context, "In die Zwischenablage kopiert!", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text("Kopieren", fontSize = 12.sp)
                                }
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = json.take(500) + if (json.length > 500) "\n... (${json.length} Zeichen)" else "",
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Über die App
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Doppelkopf Spielstandszähler",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Version 1.0 • Offline-fähig • Reine Punkteberechnung ohne automatische Kartenauswertung.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Import-Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Backup importieren", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Füge hier den zuvor exportierten JSON-Text ein. Die Daten werden transaktionssicher in die lokale Datenbank übernommen.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = importInputText,
                        onValueChange = { importInputText = it },
                        label = { Text("JSON-Daten einfügen") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        maxLines = 8
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importInputText.trim().isNotEmpty()) {
                            viewModel.importDatabase(importInputText.trim())
                            showImportDialog = false
                        }
                    },
                    enabled = importInputText.trim().isNotEmpty()
                ) {
                    Text("Importieren")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    // Bearbeiten-Dialog für eigene AIDA-Aufgabe
    editingTaskIndex?.let { index ->
        AlertDialog(
            onDismissRequest = { editingTaskIndex = null },
            title = { Text("Aufgabe bearbeiten", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = editingTaskText,
                    onValueChange = { editingTaskText = it },
                    label = { Text("Aufgabentext") },
                    singleLine = false,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editingTaskText.trim().isNotEmpty()) {
                            viewModel.updateAidaCustomTask(index, editingTaskText.trim())
                            editingTaskIndex = null
                        }
                    },
                    enabled = editingTaskText.trim().isNotEmpty()
                ) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTaskIndex = null }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}
