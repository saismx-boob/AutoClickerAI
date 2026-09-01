package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.localization.Strings
import com.example.model.BackupSnapshot
import com.example.model.LocalBackupFile
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun BackupScreen(
    backups: List<BackupSnapshot>,
    localBackupFiles: List<LocalBackupFile>,
    strings: Strings,
    onCreateBackup: () -> Unit,
    onCreateLocalBackup: (String) -> Unit,
    onRestoreLocalBackup: (String) -> Unit,
    onDeleteLocalBackup: (String) -> Unit,
    onRefreshLocalBackups: () -> Unit,
    onRestoreBackup: (BackupSnapshot) -> Unit,
    onDeleteBackup: (BackupSnapshot) -> Unit,
    onExportJson: () -> String,
    onImportJson: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    LaunchedEffect(Unit) {
        onRefreshLocalBackups()
    }

    var showImportDialog by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportContent by remember { mutableStateOf("") }
    var showLocalBackupDialog by remember { mutableStateOf(false) }
    var localBackupNote by remember { mutableStateOf("Sauvegarde locale de mes automatisations") }

    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy à HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("backup_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Section 1: Local Phone Storage Backups
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalSageLight),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SdStorage, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.localPhoneBackup,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary
                                )
                                Text(
                                    text = "Stockage interne du téléphone • 100% hors ligne",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalForestGreen
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.8f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                        ) {
                            Text(
                                text = "${localBackupFiles.size} fichier(s)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = NaturalForestGreen,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = strings.localPhoneBackupDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { showLocalBackupDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NaturalForestGreen,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_local_backup_button")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(strings.saveToPhoneBtn, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                        }

                        OutlinedButton(
                            onClick = { onRefreshLocalBackups() },
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NaturalSageBorder)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualiser", tint = NaturalForestGreen, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }

        // Local files list
        if (localBackupFiles.isNotEmpty()) {
            item {
                Text(
                    text = "${strings.localFilesTitle} (${localBackupFiles.size})",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = NaturalTextPrimary
                )
            }

            items(localBackupFiles, key = { it.filePath }) { localFile ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = localFile.fileName,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary
                                )
                                Text(
                                    text = "${dateFormat.format(Date(localFile.timestamp))} • ${localFile.scriptCount} scripts • ${localFile.sizeBytes / 1024} Ko",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextSecondary
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        onRestoreLocalBackup(localFile.filePath)
                                        Toast.makeText(context, "Configurations restaurées depuis le fichier local !", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restaurer", tint = NaturalForestGreen)
                                }
                                IconButton(
                                    onClick = {
                                        onDeleteLocalBackup(localFile.filePath)
                                        Toast.makeText(context, "Fichier de sauvegarde supprimé", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Supprimer", tint = NaturalRose)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Cloud Auto-Backup Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(NaturalSageLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = strings.cloudBackupTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary
                                )
                                Text(
                                    text = "Synchronisation automatique dans le Cloud",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = strings.cloudBackupDesc,
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onCreateBackup()
                            Toast.makeText(context, "Sauvegarde Cloud créée avec succès !", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NaturalForestGreen,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_backup_button")
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(strings.createBackupNow, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Export / Import Manual Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        exportContent = onExportJson()
                        showExportDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = NaturalForestGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.exportConfig, color = NaturalForestGreen, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        importText = ""
                        showImportDialog = true
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, tint = NaturalAmberWarm, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.importConfig, color = NaturalAmberWarm, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Snapshots History Header
        item {
            Text(
                text = "Historique des Snapshots (${backups.size})",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = NaturalTextPrimary
            )
        }

        if (backups.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.CloudQueue, contentDescription = null, tint = NaturalTextMuted, modifier = Modifier.size(40.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Aucune sauvegarde enregistrée", style = MaterialTheme.typography.bodyMedium, color = NaturalTextSecondary)
                    }
                }
            }
        } else {
            items(backups, key = { it.id }) { snapshot ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("backup_snapshot_${snapshot.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = NaturalCardSurface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, NaturalCardBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = dateFormat.format(Date(snapshot.timestamp)),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = NaturalTextPrimary
                                )
                                Text(
                                    text = "${snapshot.scriptCount} automatisations • ${snapshot.totalRuns} clics archivés",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NaturalTextSecondary
                                )
                                if (snapshot.note.isNotBlank()) {
                                    Text(
                                        text = snapshot.note,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = NaturalForestGreen
                                    )
                                }
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        onRestoreBackup(snapshot)
                                        Toast.makeText(context, "Configurations restaurées !", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = "Restaurer", tint = NaturalForestGreen)
                                }
                                IconButton(onClick = { onDeleteBackup(snapshot) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Supprimer", tint = NaturalRose)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Local Backup creation dialog
    if (showLocalBackupDialog) {
        AlertDialog(
            onDismissRequest = { showLocalBackupDialog = false },
            title = { Text("Sauvegarde sur le Téléphone", fontWeight = FontWeight.Bold, color = NaturalTextPrimary) },
            text = {
                Column {
                    Text(
                        "Ce fichier sera enregistré sur la mémoire de l'appareil (format .json). Vous pourrez le restaurer à tout moment même hors-ligne.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NaturalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = localBackupNote,
                        onValueChange = { localBackupNote = it },
                        label = { Text("Note / Description") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalForestGreen,
                            unfocusedBorderColor = NaturalOliveBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateLocalBackup(localBackupNote)
                        Toast.makeText(context, "Sauvegarde enregistrée sur le téléphone !", Toast.LENGTH_SHORT).show()
                        showLocalBackupDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Enregistrer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocalBackupDialog = false }) {
                    Text("Annuler", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Configuration JSON", fontWeight = FontWeight.Bold, color = NaturalTextPrimary) },
            text = {
                Column {
                    Text("Copiez ce code pour sauvegarder ou partager vos scripts :", style = MaterialTheme.typography.bodySmall, color = NaturalTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = NaturalOliveLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NaturalOliveBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        LazyColumn(modifier = Modifier.padding(10.dp)) {
                            item {
                                Text(text = exportContent, style = MaterialTheme.typography.bodySmall, color = NaturalTextPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportContent))
                        Toast.makeText(context, "JSON copié dans le presse-papier !", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Copier", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Fermer", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }

    // Import Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Importer une Configuration JSON", fontWeight = FontWeight.Bold, color = NaturalTextPrimary) },
            text = {
                Column {
                    Text("Collez le contenu JSON exporté :", style = MaterialTheme.typography.bodySmall, color = NaturalTextSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importText,
                        onValueChange = { importText = it },
                        minLines = 6,
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NaturalForestGreen,
                            unfocusedBorderColor = NaturalOliveBorder
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importText.isNotBlank()) {
                            onImportJson(importText)
                            Toast.makeText(context, "Importation réussie !", Toast.LENGTH_SHORT).show()
                            showImportDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NaturalForestGreen, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Importer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Annuler", color = NaturalTextSecondary)
                }
            },
            containerColor = NaturalSurface
        )
    }
}

