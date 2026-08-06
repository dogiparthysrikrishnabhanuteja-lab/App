package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Client
import com.example.data.model.CustomGroup
import com.example.service.TeluguTranslator
import com.example.ui.AdviserViewModel
import com.example.ui.components.AttachmentAndLanguageBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    viewModel: AdviserViewModel,
    onNavigateToApprovals: () -> Unit
) {
    val context = LocalContext.current
    val groups by viewModel.groups.collectAsState()
    val clients by viewModel.clients.collectAsState()

    var showAddGroupDialog by remember { mutableStateOf(false) }
    var showBulkComposerDialog by remember { mutableStateOf(false) }
    var showManageMembersGroup by remember { mutableStateOf<CustomGroup?>(null) }
    var showAllContactsSelectorDialog by remember { mutableStateOf(false) }

    var campaignTargetTitle by remember { mutableStateOf("All Contacts") }
    var campaignInitialClients by remember { mutableStateOf<List<Client>>(emptyList()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Groups & Contact Segments", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddGroupDialog = true },
                modifier = Modifier.testTag("fab_add_group"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.GroupAdd, contentDescription = "Create Group")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "Manage groups, select/deselect contacts, and broadcast tailored campaign messages.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                // 1. ALL CONTACTS SYSTEM GROUP CARD
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_all_contacts_group"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.People,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                "All Contacts",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 17.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    "System Group",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "Default segment containing all contacts in your CRM",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                "${clients.size} Total Contacts Registered",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showAllContactsSelectorDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_select_all_contacts"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select / Deselect", fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        campaignTargetTitle = "All Contacts"
                                        campaignInitialClients = clients
                                        showBulkComposerDialog = true
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_broadcast_all_contacts"),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Broadcast", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Header for Custom Segments
                item {
                    Text(
                        "CUSTOM SEGMENTS (${groups.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                if (groups.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No custom groups created yet. Tap '+' to create a group and select contacts.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(groups, key = { it.id }) { group ->
                        val groupClientsFlow = remember(group.id) { viewModel.repository.getClientsForGroup(group.id) }
                        val groupClients by groupClientsFlow.collectAsState(initial = emptyList())

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("group_card_${group.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    try { Color(android.graphics.Color.parseColor(group.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(group.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteGroup(group) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                if (group.description.isNotBlank()) {
                                    Text(group.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("${groupClients.size} Members in Segment", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showManageMembersGroup = group },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_manage_members_${group.id}"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Select Members", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = {
                                            campaignTargetTitle = group.name
                                            campaignInitialClients = groupClients
                                            showBulkComposerDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_broadcast_${group.id}"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Broadcast", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE NEW GROUP DIALOG WITH CONTACT SELECTOR
    if (showAddGroupDialog) {
        AddGroupWithContactSelectorDialog(
            allClients = clients,
            onDismiss = { showAddGroupDialog = false },
            onConfirm = { name, desc, colorHex, selectedClientIds ->
                viewModel.addGroupWithMembers(name, desc, colorHex, selectedClientIds)
                Toast.makeText(context, "Created group '$name' with ${selectedClientIds.size} contacts!", Toast.LENGTH_SHORT).show()
                showAddGroupDialog = false
            }
        )
    }

    // MANAGE MEMBERS FOR A CUSTOM GROUP
    if (showManageMembersGroup != null) {
        val group = showManageMembersGroup!!
        val groupClientsFlow = remember(group.id) { viewModel.repository.getClientsForGroup(group.id) }
        val currentGroupClients by groupClientsFlow.collectAsState(initial = emptyList())
        val currentMemberIds = remember(currentGroupClients) { currentGroupClients.map { it.id }.toSet() }

        ManageGroupMembersDialog(
            groupName = group.name,
            allClients = clients,
            initialMemberIds = currentMemberIds,
            onDismiss = { showManageMembersGroup = null },
            onSave = { newMemberIds ->
                viewModel.updateGroupMembers(group.id, newMemberIds, currentMemberIds)
                Toast.makeText(context, "Updated members for '${group.name}' (${newMemberIds.size} selected)", Toast.LENGTH_SHORT).show()
                showManageMembersGroup = null
            }
        )
    }

    // INSPECT / SELECT ALL CONTACTS DIALOG
    if (showAllContactsSelectorDialog) {
        SelectableContactListModal(
            title = "All Contacts - Select / Deselect",
            allClients = clients,
            initialSelectedIds = remember(clients) { clients.map { it.id }.toSet() },
            onDismiss = { showAllContactsSelectorDialog = false },
            onConfirm = { selectedIds ->
                Toast.makeText(context, "Selected ${selectedIds.size} contacts from All Contacts", Toast.LENGTH_SHORT).show()
                showAllContactsSelectorDialog = false
            }
        )
    }

    // RICH BULK BROADCAST COMPOSER
    if (showBulkComposerDialog) {
        BulkComposerDialog(
            groupName = campaignTargetTitle,
            initialClients = campaignInitialClients,
            allAvailableClients = clients,
            onDismiss = { showBulkComposerDialog = false },
            onSendBulk = { selectedClients, messageText, audioUrl, videoUrl ->
                val fullContent = buildString {
                    append(messageText)
                    if (audioUrl.isNotBlank()) append("\n\n🎙️ Audio Note: $audioUrl")
                    if (videoUrl.isNotBlank()) append("\n\n🎥 Video Link: $videoUrl")
                }

                viewModel.queueBulkCampaign(selectedClients, fullContent) {
                    Toast.makeText(context, "Queued ${selectedClients.size} campaign messages for approval!", Toast.LENGTH_LONG).show()
                    showBulkComposerDialog = false
                    onNavigateToApprovals()
                }
            }
        )
    }
}

@Composable
fun AddGroupWithContactSelectorDialog(
    allClients: List<Client>,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, Set<Long>) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }

    var selectedIds by remember { mutableStateOf(allClients.map { it.id }.toSet()) }

    val filteredClients = remember(allClients, searchQuery) {
        if (searchQuery.isBlank()) allClients
        else allClients.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Group", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Group Name (e.g. HNI Mutual Fund Clients)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_group_name")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select Contacts (${selectedIds.size}/${allClients.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { selectedIds = allClients.map { it.id }.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Select All", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Deselect All", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val isChecked = selectedIds.contains(client.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isChecked) selectedIds - client.id else selectedIds + client.id
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + client.id else selectedIds - client.id
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(client.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onConfirm(name, desc, "#0284C7", selectedIds)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_add_group")
            ) {
                Text("Create Group (${selectedIds.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ManageGroupMembersDialog(
    groupName: String,
    allClients: List<Client>,
    initialMemberIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialMemberIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredClients = remember(allClients, searchQuery) {
        if (searchQuery.isBlank()) allClients
        else allClients.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Contacts - $groupName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedIds.size} of ${allClients.size} Contacts Selected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = { selectedIds = allClients.map { it.id }.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Select All", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Deselect All", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search contact name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val isChecked = selectedIds.contains(client.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isChecked) selectedIds - client.id else selectedIds + client.id
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + client.id else selectedIds - client.id
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(client.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedIds) },
                modifier = Modifier.testTag("btn_save_group_members")
            ) {
                Text("Save Selection (${selectedIds.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun SelectableContactListModal(
    title: String,
    allClients: List<Client>,
    initialSelectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onConfirm: (Set<Long>) -> Unit
) {
    var selectedIds by remember { mutableStateOf(initialSelectedIds) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredClients = remember(allClients, searchQuery) {
        if (searchQuery.isBlank()) allClients
        else allClients.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${selectedIds.size} / ${allClients.size} Contacts Selected",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        TextButton(
                            onClick = { selectedIds = allClients.map { it.id }.toSet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Select All", fontSize = 11.sp)
                        }
                        TextButton(
                            onClick = { selectedIds = emptySet() },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Deselect All", fontSize = 11.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search name or phone...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredClients, key = { it.id }) { client ->
                        val isChecked = selectedIds.contains(client.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedIds = if (isChecked) selectedIds - client.id else selectedIds + client.id
                                }
                                .padding(vertical = 4.dp, horizontal = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = { checked ->
                                    selectedIds = if (checked) selectedIds + client.id else selectedIds - client.id
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(client.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedIds) }) {
                Text("Confirm (${selectedIds.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun BulkComposerDialog(
    groupName: String,
    initialClients: List<Client>,
    allAvailableClients: List<Client>,
    onDismiss: () -> Unit,
    onSendBulk: (selectedClients: List<Client>, messageText: String, audioUrl: String, videoUrl: String) -> Unit
) {
    var message by remember { mutableStateOf("Dear {client_name},\n\nWe are pleased to share our latest quarter market outlook & portfolio updates.\n\nWarm regards,\nYour Financial Advisor") }
    var attachedUri by remember { mutableStateOf("") }
    var attachedMimeType by remember { mutableStateOf("*/*") }
    var isTelugu by remember { mutableStateOf(false) }

    var selectedRecipients by remember { mutableStateOf(initialClients) }
    var showRecipientSelector by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val recipientIds = remember(selectedRecipients) { selectedRecipients.map { it.id }.toSet() }

    val filteredClientsToSelect = remember(allAvailableClients, searchQuery) {
        if (searchQuery.isBlank()) allAvailableClients
        else allAvailableClients.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery, ignoreCase = true)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Segment Broadcast - $groupName", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Target: ${selectedRecipients.size} Recipients",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    TextButton(
                        onClick = { showRecipientSelector = !showRecipientSelector },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (showRecipientSelector) "Hide Recipient List" else "Select / Deselect Contacts",
                            fontSize = 11.sp
                        )
                    }
                }

                AttachmentAndLanguageBar(
                    isTelugu = isTelugu,
                    onLanguageToggle = { newIsTelugu ->
                        isTelugu = newIsTelugu
                        message = TeluguTranslator.convertTextLanguage(message, newIsTelugu)
                    },
                    attachedUriStr = attachedUri,
                    onAttachmentChanged = { uri, mime ->
                        attachedUri = uri
                        attachedMimeType = mime
                    }
                )

                AnimatedVisibility(visible = showRecipientSelector) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recipient Selector", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Row {
                                TextButton(
                                    onClick = { selectedRecipients = allAvailableClients },
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Select All", fontSize = 10.sp)
                                }
                                TextButton(
                                    onClick = { selectedRecipients = emptyList() },
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Deselect All", fontSize = 10.sp)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search recipients...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyColumn(
                            modifier = Modifier.height(140.dp)
                        ) {
                            items(filteredClientsToSelect, key = { it.id }) { client ->
                                val isSelected = recipientIds.contains(client.id)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedRecipients = if (isSelected) {
                                                selectedRecipients.filter { it.id != client.id }
                                            } else {
                                                selectedRecipients + client
                                            }
                                        }
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            selectedRecipients = if (checked) {
                                                selectedRecipients + client
                                            } else {
                                                selectedRecipients.filter { it.id != client.id }
                                            }
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(client.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message Text (${if (isTelugu) "తెలుగు" else "English"})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 5
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedRecipients.isNotEmpty()) {
                        onSendBulk(selectedRecipients, message, attachedUri, "")
                    }
                },
                enabled = selectedRecipients.isNotEmpty(),
                modifier = Modifier.testTag("btn_queue_bulk_broadcast")
            ) {
                Text("Queue Broadcast (${selectedRecipients.size})")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
