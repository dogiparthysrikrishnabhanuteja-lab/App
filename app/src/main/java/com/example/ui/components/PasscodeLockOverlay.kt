package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AdviserViewModel

@Composable
fun PasscodeLockOverlay(
    viewModel: AdviserViewModel,
    content: @Composable () -> Unit
) {
    val isPasscodeEnabled by viewModel.isPasscodeEnabled.collectAsState()
    val isLocked by viewModel.isPasscodeLocked.collectAsState()

    var pinInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    if (isPasscodeEnabled && isLocked) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        "AdviserSync Security Lock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )

                    Text(
                        "Enter PIN to access client CRM & financial records.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            pinInput = it
                            errorMsg = ""
                        },
                        label = { Text("Enter PIN (Default: 1234)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_security_pin")
                    )

                    if (errorMsg.isNotEmpty()) {
                        Text(errorMsg, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            if (viewModel.checkPasscode(pinInput)) {
                                pinInput = ""
                            } else {
                                errorMsg = "Incorrect PIN. Try default: 1234"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_unlock_pin")
                    ) {
                        Text("Unlock App")
                    }
                }
            }
        }
    } else {
        content()
    }
}
