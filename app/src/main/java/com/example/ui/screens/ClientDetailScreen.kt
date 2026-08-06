package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.data.model.*
import com.example.service.WhatsAppHelper
import com.example.ui.AdviserViewModel
import com.example.ui.components.WishComposerModal
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientDetailScreen(
    clientId: Long,
    viewModel: AdviserViewModel,
    onBack: () -> Unit,
    onNavigateToApprovals: () -> Unit
) {
    val context = LocalContext.current
    val clients by viewModel.clients.collectAsState()
    val client = remember(clients, clientId) { clients.find { it.id == clientId } }

    val policiesFlow = remember(clientId) { viewModel.repository.getPoliciesForClient(clientId) }
    val clientPolicies by policiesFlow.collectAsState(initial = emptyList())

    val documentsFlow = remember(clientId) { viewModel.repository.getDocumentsForClient(clientId) }
    val clientDocuments by documentsFlow.collectAsState(initial = emptyList())

    var showAddPolicyDialog by remember { mutableStateOf(false) }
    var showAddDocDialog by remember { mutableStateOf(false) }
    var showWishModal by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    if (client == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Client not found.")
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(client.name, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_back_client_detail")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            viewModel.deleteClient(client)
                            onBack()
                        },
                        modifier = Modifier.testTag("btn_delete_client")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Client", tint = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    client.name.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Text(client.phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (client.email.isNotBlank()) {
                                    Text(client.email, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Badges Row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    "Consent: ${client.consentStatus.name}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            if (client.isProspect) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer
                                ) {
                                    Text(
                                        "Prospect Lead",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }

                        if (client.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Notes: ${client.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Quick Communication Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { showWishModal = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_detail_wa"),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Send Wish / Msg")
                            }

                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${client.phone}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_detail_call")
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call")
                            }
                        }
                    }
                }
            }

            // Linked Policies & Products
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Linked Policies & Folios (${clientPolicies.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { showAddPolicyDialog = true }, modifier = Modifier.testTag("btn_add_policy_for_client")) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Policy")
                    }
                }
            }

            if (clientPolicies.isEmpty()) {
                item {
                    Text("No policies attached to this client yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(clientPolicies) { policy ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(policy.productType.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(currencyFormat.format(policy.premiumAmount), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Text("${policy.providerName} • No: ${policy.policyNumber}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Frequency: ${policy.paymentFrequency.displayName} | Due: ${policy.renewalDate}", fontSize = 11.sp)
                                if (policy.isPaid) {
                                    Text("PAID ✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                } else {
                                    TextButton(onClick = { viewModel.markPolicyPaid(policy) }) {
                                        Text("Mark Paid", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Client Documents
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("KYC & Policy Documents (${clientDocuments.size})", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { showAddDocDialog = true }, modifier = Modifier.testTag("btn_add_doc_for_client")) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Doc")
                    }
                }
            }

            if (clientDocuments.isEmpty()) {
                item {
                    Text("No KYC or policy documents attached yet.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(clientDocuments) { doc ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${doc.docType.displayName} • ${doc.fileUriOrNotes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    if (showWishModal) {
        WishComposerModal(
            client = client,
            occasionType = ApprovalType.CAMPAIGN,
            viewModel = viewModel,
            onDismiss = { showWishModal = false },
            onWishQueuedOrSent = { showWishModal = false }
        )
    }

    // Add Policy Modal
    if (showAddPolicyDialog) {
        AddPolicyModal(
            clientId = clientId,
            clientName = client.name,
            onDismiss = { showAddPolicyDialog = false },
            onConfirm = { pType, pNo, provider, amount, freq, date, lead, notes ->
                viewModel.addPolicy(clientId, client.name, pType, pNo, provider, amount, freq, date, lead, notes)
                showAddPolicyDialog = false
            }
        )
    }

    // Add Doc Modal
    if (showAddDocDialog) {
        AddDocModal(
            onDismiss = { showAddDocDialog = false },
            onConfirm = { docType, title, notes ->
                viewModel.addDocument(clientId, docType, title, notes)
                showAddDocDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPolicyModal(
    clientId: Long,
    clientName: String,
    onDismiss: () -> Unit,
    onConfirm: (ProductType, String, String, Double, PaymentFrequency, String, Int, String) -> Unit
) {
    var productType by remember { mutableStateOf(ProductType.LIFE_INSURANCE) }
    var policyNo by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var premium by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(PaymentFrequency.YEARLY) }
    var renewalDate by remember { mutableStateOf("2026-09-15") }
    var leadDays by remember { mutableStateOf("7") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Policy / Product", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Product Type:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                ScrollableTabRow(selectedTabIndex = productType.ordinal, edgePadding = 0.dp) {
                    ProductType.values().forEach { type ->
                        Tab(
                            selected = productType == type,
                            onClick = { productType = type },
                            text = { Text(type.displayName, fontSize = 11.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = policyNo,
                    onValueChange = { policyNo = it },
                    label = { Text("Policy / Folio Number *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_policy_number")
                )
                OutlinedTextField(
                    value = provider,
                    onValueChange = { provider = it },
                    label = { Text("Provider / Insurer / AMC *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = premium,
                        onValueChange = { premium = it },
                        label = { Text("Premium (₹) *") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_policy_premium")
                    )
                    OutlinedTextField(
                        value = renewalDate,
                        onValueChange = { renewalDate = it },
                        label = { Text("Due Date") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = leadDays,
                    onValueChange = { leadDays = it },
                    label = { Text("Reminder Lead Time (Days)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amountVal = premium.toDoubleOrNull() ?: 0.0
                    val leadVal = leadDays.toIntOrNull() ?: 7
                    if (policyNo.isNotBlank() && provider.isNotBlank()) {
                        onConfirm(productType, policyNo, provider, amountVal, frequency, renewalDate, leadVal, notes)
                    }
                },
                modifier = Modifier.testTag("btn_confirm_add_policy")
            ) {
                Text("Save Policy")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AddDocModal(
    onDismiss: () -> Unit,
    onConfirm: (DocType, String, String) -> Unit
) {
    var docType by remember { mutableStateOf(DocType.KYC) }
    var title by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Attach Document Record", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Document Title (e.g. Aadhaar Copy)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Details / Verification Notes") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank()) {
                    onConfirm(docType, title, notes)
                }
            }) { Text("Save Document") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
