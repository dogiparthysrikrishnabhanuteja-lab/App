package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.CloudSyncState
import com.example.ui.AdviserViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupAuditScreen(
    viewModel: AdviserViewModel
) {
    val context = LocalContext.current
    val auditLogs by viewModel.auditLogs.collectAsState()
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()

    var showBackupDialog by remember { mutableStateOf(false) }
    var backupJsonText by remember { mutableStateOf("") }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var restoreInputJson by remember { mutableStateOf("") }

    val isPasscodeEnabled by viewModel.isPasscodeEnabled.collectAsState()

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup, Security & Audit Log", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // Real-Time Room to Firestore Cloud Sync Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Room to Firestore Cloud Backup", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                        Text(
                            "Real-time syncing for client contacts, policy renewals, custom groups, and approval message queues.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = when (cloudSyncState) {
                                is CloudSyncState.Success -> Color(0xFFE8F5E9)
                                is CloudSyncState.Syncing -> MaterialTheme.colorScheme.surfaceVariant
                                else -> MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    when (cloudSyncState) {
                                        is CloudSyncState.Syncing -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        is CloudSyncState.Success -> Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                                        else -> Icon(Icons.Default.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        when (val state = cloudSyncState) {
                                            is CloudSyncState.Syncing -> "Syncing to Firestore..."
                                            is CloudSyncState.Success -> "Synced: ${state.message} (${state.lastSyncTime})"
                                            is CloudSyncState.Offline -> "Offline Mode: ${state.reason}"
                                            is CloudSyncState.Idle -> "Cloud Backup Ready"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = when (cloudSyncState) {
                                            is CloudSyncState.Success -> Color(0xFF2E7D32)
                                            else -> MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                }

                                Button(
                                    onClick = { viewModel.triggerCloudSync() },
                                    modifier = Modifier.testTag("btn_sync_firestore_now"),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text("Sync Now", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Data Backup & Automatic Sync", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "Persists all 5,000+ contacts, policy records, custom groups, and approval history safely across device reinstallations.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.exportBackup { json ->
                                        backupJsonText = json
                                        showBackupDialog = true
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_export_backup")
                            ) {
                                Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON")
                            }

                            OutlinedButton(
                                onClick = { showRestoreDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_restore_backup")
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Data")
                            }
                        }
                    }
                }
            }

            // Passcode Security Toggle Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Passcode Lock Security", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Protect sensitive client financial data with PIN code.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = isPasscodeEnabled,
                            onCheckedChange = { viewModel.isPasscodeEnabled.value = it },
                            modifier = Modifier.testTag("toggle_passcode")
                        )
                    }
                }
            }

            // Compliance Audit Trail Header
            item {
                Text(
                    "Compliance Audit Log (${auditLogs.size} Records)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (auditLogs.isEmpty()) {
                item { Text("No audit records logged yet.") }
            } else {
                items(auditLogs, key = { it.id }) { log ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(log.actionType, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(dateFormat.format(Date(log.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("Client: ${log.clientName}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                                Text(log.details, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    // Export JSON Modal
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = { Text("Exported JSON Database Backup", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = backupJsonText,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    label = { Text("Backup Data Payload") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("AdviserSync Backup", backupJsonText)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                    showBackupDialog = false
                }) {
                    Text("Copy to Clipboard")
                }
            },
            dismissButton = { TextButton(onClick = { showBackupDialog = false }) { Text("Close") } }
        )
    }

    // Restore JSON Modal
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Restore Database from Backup", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = restoreInputJson,
                    onValueChange = { restoreInputJson = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    placeholder = { Text("Paste JSON backup payload here...") }
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.restoreBackup(restoreInputJson) { success ->
                        if (success) {
                            Toast.makeText(context, "Database restored successfully!", Toast.LENGTH_LONG).show()
                            showRestoreDialog = false
                        } else {
                            Toast.makeText(context, "Invalid JSON format.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }) {
                    Text("Restore Database")
                }
            },
            dismissButton = { TextButton(onClick = { showRestoreDialog = false }) { Text("Cancel") } }
        )
    }
}
