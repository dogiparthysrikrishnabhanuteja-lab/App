package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import com.example.data.model.ApprovalStatus
import com.example.data.model.ApprovalType
import com.example.data.model.ReminderApproval
import com.example.service.TeluguTranslator
import com.example.ui.AdviserViewModel
import com.example.ui.components.AttachmentAndLanguageBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueApprovalsScreen(
    viewModel: AdviserViewModel
) {
    val context = LocalContext.current
    val allApprovals by viewModel.allApprovals.collectAsState()

    var selectedTab by remember { mutableStateOf(ApprovalStatus.PENDING) }
    var editingApproval by remember { mutableStateOf<ReminderApproval?>(null) }
    var editedText by remember { mutableStateOf("") }
    var editedAttachmentUrl by remember { mutableStateOf("") }
    var editedMimeType by remember { mutableStateOf("*/*") }
    var isTelugu by remember { mutableStateOf(false) }

    val isGeneratingAi by viewModel.isGeneratingAi.collectAsState()

    val filteredList = remember(allApprovals, selectedTab) {
        allApprovals.filter { it.status == selectedTab }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Mandatory Review & Approval Queue", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Nothing is sent without explicit review & approval", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Tab Row
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                ApprovalStatus.values().forEach { status ->
                    val count = allApprovals.count { it.status == status }
                    Tab(
                        selected = selectedTab == status,
                        onClick = { selectedTab = status },
                        text = { Text("${status.name} ($count)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (selectedTab == ApprovalStatus.PENDING) "All caught up! No pending messages to review."
                            else "No items in ${selectedTab.name} queue.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredList, key = { it.id }) { approval ->
                        ApprovalReviewCard(
                            approval = approval,
                            onEdit = {
                                editingApproval = approval
                                editedText = approval.messageText
                                editedAttachmentUrl = approval.attachmentUrl
                                isTelugu = false
                            },
                            onApproveSend = {
                                viewModel.approveAndDispatchWhatsApp(context, approval)
                            },
                            onDiscard = {
                                viewModel.discardApproval(approval)
                            }
                        )
                    }
                }
            }
        }
    }

    // Edit Approval Dialog
    if (editingApproval != null) {
        val approval = editingApproval!!

        AlertDialog(
            onDismissRequest = { editingApproval = null },
            title = { Text("Preview & Edit Message", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Client: ${approval.clientName} (${approval.clientPhone})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text("Type: ${approval.type.name} | Due: ${approval.dueDate}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)

                    AttachmentAndLanguageBar(
                        isTelugu = isTelugu,
                        onLanguageToggle = { newIsTelugu ->
                            isTelugu = newIsTelugu
                            editedText = TeluguTranslator.convertTextLanguage(editedText, newIsTelugu)
                        },
                        attachedUriStr = editedAttachmentUrl,
                        onAttachmentChanged = { uri, mime ->
                            editedAttachmentUrl = uri
                            editedMimeType = mime
                        }
                    )

                    OutlinedTextField(
                        value = editedText,
                        onValueChange = { editedText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .testTag("input_edit_approval_text"),
                        label = { Text("Message Content (${if (isTelugu) "తెలుగు" else "English"})") },
                        maxLines = 8
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                val prompt = if (isTelugu) "Write a wish in Telugu for ${approval.clientName}" else ""
                                viewModel.generateAIWish(approval.clientName, approval.type.name, prompt)
                            },
                            modifier = Modifier.testTag("btn_ai_polish_text")
                        ) {
                            if (isGeneratingAi) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AI Polish", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateApprovalMessage(approval, editedText, editedAttachmentUrl)
                        editingApproval = null
                        Toast.makeText(context, "Updated message & attachment.", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("btn_save_edited_approval")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingApproval = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ApprovalReviewCard(
    approval: ReminderApproval,
    onEdit: () -> Unit,
    onApproveSend: () -> Unit,
    onDiscard: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("approval_card_${approval.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    approval.clientName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (approval.type) {
                        ApprovalType.RENEWAL -> MaterialTheme.colorScheme.primaryContainer
                        ApprovalType.BIRTHDAY -> MaterialTheme.colorScheme.tertiaryContainer
                        ApprovalType.ANNIVERSARY -> MaterialTheme.colorScheme.secondaryContainer
                        ApprovalType.CAMPAIGN -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ) {
                    Text(
                        approval.type.name,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                "Phone: ${approval.clientPhone} • Due: ${approval.dueDate}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (approval.attachmentUrl.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Attached File: ${approval.attachmentUrl.takeLast(24)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Text(
                    approval.messageText,
                    modifier = Modifier.padding(10.dp),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (approval.status == ApprovalStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = onEdit, modifier = Modifier.testTag("btn_edit_approval_${approval.id}")) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Message")
                        }
                        IconButton(onClick = onDiscard, modifier = Modifier.testTag("btn_discard_approval_${approval.id}")) {
                            Icon(Icons.Default.Close, contentDescription = "Discard", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Button(
                        onClick = onApproveSend,
                        modifier = Modifier.testTag("btn_approve_send_${approval.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Approve & Send (WhatsApp)", fontSize = 12.sp)
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (approval.status == ApprovalStatus.DISPATCHED) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        "Status: ${approval.status.name}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
