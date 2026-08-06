package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentAndLanguageBar(
    isTelugu: Boolean,
    onLanguageToggle: (Boolean) -> Unit,
    attachedUriStr: String,
    onAttachmentChanged: (uriStr: String, mimeType: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showUrlInputDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachmentChanged(uri.toString(), "image/*")
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachmentChanged(uri.toString(), "video/*")
        }
    }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachmentChanged(uri.toString(), "audio/*")
        }
    }

    val docPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onAttachmentChanged(uri.toString(), "*/*")
        }
    }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // ROW 1: LANGUAGE SWITCHER TOGGLE & ATTACHMENT QUICK BUTTONS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Language Toggle Switcher Button (English <-> Telugu)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .testTag("btn_language_toggle")
                    .clickable { onLanguageToggle(!isTelugu) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Translate,
                        contentDescription = "Language Selector",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (!isTelugu) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            "English",
                            fontSize = 11.sp,
                            fontWeight = if (!isTelugu) FontWeight.Bold else FontWeight.Normal,
                            color = if (!isTelugu) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isTelugu) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Text(
                            "తెలుగు",
                            fontSize = 11.sp,
                            fontWeight = if (isTelugu) FontWeight.Bold else FontWeight.Normal,
                            color = if (isTelugu) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Attachment Quick Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = { photoPicker.launch("image/*") },
                    modifier = Modifier.testTag("btn_attach_photo")
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Attach Photo", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(
                    onClick = { videoPicker.launch("video/*") },
                    modifier = Modifier.testTag("btn_attach_video")
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = "Attach Video", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(
                    onClick = { audioPicker.launch("audio/*") },
                    modifier = Modifier.testTag("btn_attach_audio")
                ) {
                    Icon(Icons.Default.Mic, contentDescription = "Attach Audio", tint = MaterialTheme.colorScheme.tertiary)
                }
                IconButton(
                    onClick = { docPicker.launch("*/*") },
                    modifier = Modifier.testTag("btn_attach_doc")
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach Document / File", tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(
                    onClick = { showUrlInputDialog = true },
                    modifier = Modifier.testTag("btn_attach_link")
                ) {
                    Icon(Icons.Default.Link, contentDescription = "Attach URL / Link", tint = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // ROW 2: ATTACHED FILE CHIP DISPLAY
        if (attachedUriStr.isNotBlank()) {
            val fileName = remember(attachedUriStr) {
                try {
                    val uri = Uri.parse(attachedUriStr)
                    uri.lastPathSegment?.substringAfterLast('/') ?: attachedUriStr
                } catch (e: Exception) {
                    attachedUriStr
                }
            }
            val isPhoto = attachedUriStr.contains("image") || attachedUriStr.endsWith(".jpg") || attachedUriStr.endsWith(".png")
            val isVideo = attachedUriStr.contains("video") || attachedUriStr.endsWith(".mp4")
            val isAudio = attachedUriStr.contains("audio") || attachedUriStr.endsWith(".mp3")

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chip_attached_file")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            when {
                                isPhoto -> Icons.Default.Image
                                isVideo -> Icons.Default.Videocam
                                isAudio -> Icons.Default.Mic
                                else -> Icons.Default.InsertDriveFile
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                when {
                                    isPhoto -> "Attached Photo"
                                    isVideo -> "Attached Video"
                                    isAudio -> "Attached Audio Note"
                                    else -> "Attached File / Link"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                fileName,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = { onAttachmentChanged("", "") },
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("btn_remove_attachment")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Attachment", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showUrlInputDialog) {
        AlertDialog(
            onDismissRequest = { showUrlInputDialog = false },
            title = { Text("Attach Media / Document Link", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = customUrlInput,
                    onValueChange = { customUrlInput = it },
                    placeholder = { Text("e.g. https://drive.google.com/file... or photo link") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_custom_url"),
                    singleLine = true
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (customUrlInput.isNotBlank()) {
                            onAttachmentChanged(customUrlInput.trim(), "*/*")
                        }
                        showUrlInputDialog = false
                    },
                    modifier = Modifier.testTag("btn_confirm_url")
                ) {
                    Text("Attach Link")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUrlInputDialog = false }) { Text("Cancel") }
            }
        )
    }
}
