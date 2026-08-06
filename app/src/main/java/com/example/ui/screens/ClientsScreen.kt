package com.example.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.ConsentStatus
import com.example.data.model.ContactImportResult
import com.example.data.model.DuplicateGroup
import com.example.service.WhatsAppHelper
import com.example.ui.AdviserViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ClientsScreen(
    viewModel: AdviserViewModel,
    onNavigateToDetail: (Long) -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.filteredClients.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val duplicateGroups by viewModel.duplicateGroups.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showRationaleDialog by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var importResultSummary by remember { mutableStateOf<ContactImportResult?>(null) }
    var consentFilter by remember { mutableStateOf<ConsentStatus?>(null) }

    // Accompanist Contacts Permission State
    val contactsPermissionState = rememberPermissionState(
        permission = Manifest.permission.READ_CONTACTS
    )

    fun startBulkContactImport() {
        when {
            contactsPermissionState.status.isGranted -> {
                viewModel.importPhoneContactsWithAutoMerge(context) { result ->
                    importResultSummary = result
                }
            }
            contactsPermissionState.status.shouldShowRationale -> {
                showRationaleDialog = true
            }
            else -> {
                contactsPermissionState.launchPermissionRequest()
            }
        }
    }

    val displayClients = remember(clients, consentFilter) {
        if (consentFilter == null) clients
        else clients.filter { it.consentStatus == consentFilter }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Client Contacts (${displayClients.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(
                        onClick = { startBulkContactImport() },
                        modifier = Modifier.testTag("btn_import_contacts")
                    ) {
                        Icon(Icons.Default.Contacts, contentDescription = "Bulk Import Phone Contacts")
                    }
                    IconButton(
                        onClick = {
                            viewModel.exportCsv { csvData ->
                                Toast.makeText(context, "CSV exported (${csvData.length} bytes)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.testTag("btn_export_csv")
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                modifier = Modifier.testTag("fab_add_client"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Client")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_search_clients"),
                placeholder = { Text("Search 5,000+ clients by name, phone, email...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bulk Import Banner powered by Accompanist Permissions
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (contactsPermissionState.status.isGranted)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("card_bulk_import_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Contacts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (contactsPermissionState.status.isGranted) "Phone Contacts Connected" else "Bulk Import Phone Contacts",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (contactsPermissionState.status.isGranted) "Tap to sync new phone contacts to CRM" else "Grant read permission to bulk import client contacts",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Button(
                        onClick = { startBulkContactImport() },
                        modifier = Modifier.testTag("btn_bulk_import_contacts_banner"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            if (contactsPermissionState.status.isGranted) "Import Now" else "Grant & Sync",
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Consent Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = consentFilter == null,
                        onClick = { consentFilter = null },
                        label = { Text("All (${clients.size})") }
                    )
                }
                item {
                    FilterChip(
                        selected = consentFilter == ConsentStatus.CONSENTED,
                        onClick = { consentFilter = ConsentStatus.CONSENTED },
                        label = { Text("Consented") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp)) }
                    )
                }
                item {
                    FilterChip(
                        selected = consentFilter == ConsentStatus.PENDING,
                        onClick = { consentFilter = ConsentStatus.PENDING },
                        label = { Text("Pending DND") }
                    )
                }
                item {
                    FilterChip(
                        selected = consentFilter == ConsentStatus.OPTED_OUT,
                        onClick = { consentFilter = ConsentStatus.OPTED_OUT },
                        label = { Text("Opted Out") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Duplicate warning banner
            if (duplicateGroups.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.MergeType, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Duplicate Contacts Detected (${duplicateGroups.size} group${if (duplicateGroups.size > 1) "s" else ""})",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "Matches based on Phone or Email. Prevent redundant communications.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            OutlinedButton(
                                onClick = { showDuplicateDialog = true },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_review_duplicates")
                            ) {
                                Text("Review Duplicates", fontSize = 11.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.autoMergeAllDuplicates { count ->
                                        Toast.makeText(context, "Auto-merged $count duplicate contact entries!", Toast.LENGTH_LONG).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("btn_auto_merge_all")
                            ) {
                                Text("Auto-Merge All (${duplicateGroups.sumOf { it.duplicateClients.size }})", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Contacts List
            if (displayClients.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.PersonSearch,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No clients found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(displayClients, key = { it.id }) { client ->
                        ClientItemCard(
                            client = client,
                            onClick = { onNavigateToDetail(client.id) },
                            onWhatsAppClick = {
                                WhatsAppHelper.sendWhatsAppMessage(context, client.phone, "Hello ${client.name}, greetings from your financial advisor!")
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Client Dialog
    if (showAddDialog) {
        AddClientDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, phone, email, dob, ann, consent, isProspect, notes ->
                viewModel.addClient(name, phone, email, dob, ann, consent, isProspect, notes)
                showAddDialog = false
            }
        )
    }

    // Accompanist Permission Rationale Dialog
    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            icon = { Icon(Icons.Default.Contacts, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Contacts Read Access Required", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "To enable the bulk import flow for your device phone contacts into Adviser CRM, please grant read access to your device's contact list."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRationaleDialog = false
                        contactsPermissionState.launchPermissionRequest()
                    },
                    modifier = Modifier.testTag("btn_grant_contacts_permission")
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRationaleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Post-Import Summary Dialog
    if (importResultSummary != null) {
        val res = importResultSummary!!
        AlertDialog(
            onDismissRequest = { importResultSummary = null },
            icon = { Icon(Icons.Default.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Contact Import & Auto-Merge Summary", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Phone contacts sync summary:")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Contacts Added: ${res.newContactsAdded}", fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MergeType, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Auto-Merged Duplicates: ${res.contactsAutoMerged}", fontWeight = FontWeight.SemiBold)
                    }
                    if (res.duplicateGroupsFlagged > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Duplicate Groups Flagged: ${res.duplicateGroupsFlagged}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Text("Redundant messaging queues & records have been consolidated automatically.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                if (res.duplicateGroupsFlagged > 0) {
                    Button(onClick = {
                        importResultSummary = null
                        showDuplicateDialog = true
                    }) {
                        Text("Review Flagged Groups")
                    }
                } else {
                    Button(onClick = { importResultSummary = null }) {
                        Text("Done")
                    }
                }
            },
            dismissButton = {
                if (res.duplicateGroupsFlagged > 0) {
                    TextButton(onClick = { importResultSummary = null }) {
                        Text("Dismiss")
                    }
                }
            }
        )
    }

    // Duplicate Resolution Modal Dialog
    if (showDuplicateDialog) {
        DuplicateResolutionDialog(
            duplicateGroups = duplicateGroups,
            onDismiss = { showDuplicateDialog = false },
            onMergeGroup = { primaryId, dupIds ->
                viewModel.mergeSelectedDuplicateGroup(primaryId, dupIds) {
                    Toast.makeText(context, "Merged duplicate contact group!", Toast.LENGTH_SHORT).show()
                }
            },
            onMergeAll = {
                viewModel.autoMergeAllDuplicates { count ->
                    Toast.makeText(context, "Auto-merged all $count duplicate entries!", Toast.LENGTH_LONG).show()
                    showDuplicateDialog = false
                }
            }
        )
    }
}

@Composable
fun DuplicateResolutionDialog(
    duplicateGroups: List<DuplicateGroup>,
    onDismiss: () -> Unit,
    onMergeGroup: (primaryId: Long, dupIds: List<Long>) -> Unit,
    onMergeAll: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.MergeType, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Duplicate Contacts Review", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                Text(
                    "Identified contacts sharing phone numbers or emails. Merging keeps primary details, consolidates notes, and re-links all policy reminders and documents.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (duplicateGroups.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("All duplicate contacts resolved! No duplicate groups remaining.", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(duplicateGroups, key = { it.id }) { group ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                "Match: ${group.matchType} (${group.matchKey})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                onMergeGroup(group.primaryClient.id, group.duplicateClients.map { it.id })
                                            },
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text("Merge Group", fontSize = 11.sp)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text("Primary Record:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                    Text("${group.primaryClient.name} • ${group.primaryClient.phone}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    if (group.primaryClient.email.isNotBlank()) {
                                        Text("Email: ${group.primaryClient.email}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Text("Duplicates (${group.duplicateClients.size}):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                                    group.duplicateClients.forEach { dup ->
                                        Text("• ${dup.name} (${dup.phone}) ${if (dup.email.isNotBlank()) "- " + dup.email else ""}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (duplicateGroups.isNotEmpty()) {
                Button(onClick = onMergeAll) {
                    Text("Auto-Merge All (${duplicateGroups.sumOf { it.duplicateClients.size }})")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ClientItemCard(
    client: Client,
    onClick: () -> Unit,
    onWhatsAppClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("client_card_${client.id}")
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    client.name.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        client.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (client.isProspect) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                "Prospect",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Text(
                    client.phone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (client.email.isNotBlank()) {
                    Text(
                        client.email,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            // Quick WhatsApp Button
            IconButton(
                onClick = onWhatsAppClick,
                modifier = Modifier.testTag("btn_client_wa_${client.id}")
            ) {
                Icon(
                    Icons.Default.Chat,
                    contentDescription = "WhatsApp Send",
                    tint = Color(0xFF25D366)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddClientDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String, String, ConsentStatus, Boolean, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var dob by remember { mutableStateOf("") }
    var anniversary by remember { mutableStateOf("") }
    var consentStatus by remember { mutableStateOf(ConsentStatus.CONSENTED) }
    var isProspect by remember { mutableStateOf(false) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Client Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_add_client_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number (+91...) *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_add_client_phone")
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = dob,
                        onValueChange = { dob = it },
                        label = { Text("DOB (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = anniversary,
                        onValueChange = { anniversary = it },
                        label = { Text("Anniversary") },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isProspect, onCheckedChange = { isProspect = it })
                    Text("Mark as Prospect/Lead", fontSize = 13.sp)
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes / Preferences") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && phone.isNotBlank()) {
                        onConfirm(name, phone, email, dob, anniversary, consentStatus, isProspect, notes)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_add_client")
            ) {
                Text("Save Client")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
