package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.ApprovalType
import com.example.data.model.Client
import com.example.service.TeluguTranslator
import com.example.service.WhatsAppHelper
import com.example.ui.AdviserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishComposerModal(
    client: Client,
    occasionType: ApprovalType = ApprovalType.BIRTHDAY,
    initialMessage: String = "",
    viewModel: AdviserViewModel,
    onDismiss: () -> Unit,
    onWishQueuedOrSent: () -> Unit
) {
    val context = LocalContext.current
    val isGeneratingAi by viewModel.isGeneratingAi.collectAsState()
    val aiStateText by viewModel.aiGenerationState.collectAsState()

    var isTelugu by remember { mutableStateOf(false) }
    var attachedUriStr by remember { mutableStateOf("") }
    var attachedMimeType by remember { mutableStateOf("*/*") }

    var messageText by remember(client, occasionType, isTelugu) {
        mutableStateOf(
            if (initialMessage.isNotBlank()) initialMessage
            else when (occasionType) {
                ApprovalType.BIRTHDAY -> TeluguTranslator.getBirthdayWish(client.name, isTelugu, context)
                ApprovalType.ANNIVERSARY -> TeluguTranslator.getAnniversaryWish(client.name, isTelugu, context)
                ApprovalType.RENEWAL -> TeluguTranslator.getRenewalReminder(client.name, "10293847", "Tomorrow", "15,000", isTelugu, context)
                ApprovalType.CAMPAIGN -> TeluguTranslator.getCampaignMessage(client.name, isTelugu, context)
            }
        )
    }

    LaunchedEffect(aiStateText) {
        val text = aiStateText
        if (!text.isNullOrBlank()) {
            messageText = text
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        when (occasionType) {
                            ApprovalType.BIRTHDAY -> "Birthday Wish 🎂"
                            ApprovalType.ANNIVERSARY -> "Anniversary Wish 💍"
                            ApprovalType.RENEWAL -> "Policy Renewal Reminder 📄"
                            ApprovalType.CAMPAIGN -> "Client Update / Wish 📢"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                Text("Client: ${client.name} (${client.phone})", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // ATTACHMENT AND LANGUAGE SWITCHER BAR
                AttachmentAndLanguageBar(
                    isTelugu = isTelugu,
                    onLanguageToggle = { newIsTelugu ->
                        isTelugu = newIsTelugu
                        messageText = TeluguTranslator.convertTextLanguage(messageText, newIsTelugu)
                    },
                    attachedUriStr = attachedUriStr,
                    onAttachmentChanged = { uri, mime ->
                        attachedUriStr = uri
                        attachedMimeType = mime
                    }
                )

                // Quick Preset Templates Bar (English & Telugu)
                Text("Preset Wish Templates:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { messageText = TeluguTranslator.getBirthdayWish(client.name, isTelugu, context) },
                            label = { Text(if (isTelugu) "పుట్టినరోజు 🎂" else "Birthday 🎂", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { messageText = TeluguTranslator.getAnniversaryWish(client.name, isTelugu, context) },
                            label = { Text(if (isTelugu) "పెళ్లి రోజు 💍" else "Anniversary 💍", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { messageText = TeluguTranslator.getRenewalReminder(client.name, "POLICY-123", "2 Days", "12,500", isTelugu, context) },
                            label = { Text(if (isTelugu) "పాలసీ రెన్యూవల్ 📄" else "Renewal 📄", fontSize = 11.sp) }
                        )
                    }
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { messageText = TeluguTranslator.getCampaignMessage(client.name, isTelugu, context) },
                            label = { Text(if (isTelugu) "పోర్ట్‌ఫోలియో 📈" else "Portfolio 📈", fontSize = 11.sp) }
                        )
                    }
                }

                // Message Text Field
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Wish / Message Content (${if (isTelugu) "తెలుగు" else "English"})") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .testTag("input_wish_message"),
                    maxLines = 7
                )

                // AI Polish Helper Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            val prompt = if (isTelugu) "Write a warm wish in Telugu for $client.name" else "Write a professional wish for $client.name"
                            viewModel.generateAIWish(client.name, occasionType.name, prompt)
                        },
                        modifier = Modifier.testTag("btn_wish_ai_polish")
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Button(
                    onClick = {
                        WhatsAppHelper.sendWhatsAppMessage(
                            context = context,
                            phone = client.phone,
                            message = messageText,
                            attachmentUriStr = attachedUriStr.ifBlank { null },
                            mimeTypeStr = attachedMimeType.ifBlank { null }
                        )
                        Toast.makeText(context, "Opening WhatsApp to send wish...", Toast.LENGTH_SHORT).show()
                        onWishQueuedOrSent()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_send_wish_whatsapp"),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Send Now via WhatsApp")
                }

                OutlinedButton(
                    onClick = {
                        viewModel.createWishApproval(
                            clientId = client.id,
                            clientName = client.name,
                            clientPhone = client.phone,
                            type = occasionType,
                            messageText = messageText,
                            attachmentUrl = attachedUriStr,
                            onDone = {
                                Toast.makeText(context, "Queued wish in Approval Queue!", Toast.LENGTH_SHORT).show()
                                onWishQueuedOrSent()
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("btn_queue_wish_approval")
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Review Queue")
                }
            }
        },
        dismissButton = null
    )
}
