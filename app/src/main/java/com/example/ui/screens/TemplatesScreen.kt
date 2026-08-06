package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.data.model.MessageTemplate
import com.example.data.model.TemplateCategory
import com.example.service.MasterTextManager
import com.example.ui.AdviserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplatesScreen(
    viewModel: AdviserViewModel
) {
    val context = LocalContext.current
    val templates by viewModel.templates.collectAsState()
    val isGeneratingAi by viewModel.isGeneratingAi.collectAsState()
    val aiGeneratedText by viewModel.aiGenerationState.collectAsState()

    var activeTab by remember { mutableIntStateOf(0) } // 0: Master Text Editor, 1: Approved Templates, 2: AI Playground
    var showAddTemplateDialog by remember { mutableStateOf(false) }

    // AI Playground state
    var aiClientName by remember { mutableStateOf("Rajesh Sharma") }
    var aiOccasion by remember { mutableStateOf("Birthday") }
    var aiCustomContext by remember { mutableStateOf("Wants a warm message mentioning 10 years of partnership") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Master Text & AI Studio", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            if (activeTab == 1) {
                FloatingActionButton(
                    onClick = { showAddTemplateDialog = true },
                    modifier = Modifier.testTag("fab_add_template"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Template")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            ScrollableTabRow(selectedTabIndex = activeTab, edgePadding = 0.dp) {
                Tab(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Master Text Body Editor")
                        }
                    }
                )
                Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Approved Templates") })
                Tab(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gemini AI Studio")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (activeTab) {
                0 -> {
                    MasterTextEditorSection()
                }
                1 -> {
                    // Templates list
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(templates, key = { it.id }) { template ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("template_card_${template.id}"),
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
                                        Text(template.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (template.isApprovedWhatsAppTemplate) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    "WhatsApp Meta Approved ✓",
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                        }
                                    }

                                    Text("Category: ${template.category.label}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Text(template.content, modifier = Modifier.padding(10.dp), fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // Gemini AI Studio
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Generate Personalized AI Wishes & Reminders", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Text("Powered by Gemini 3.5 Flash server-side AI model.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                                    Spacer(modifier = Modifier.height(12.dp))

                                    OutlinedTextField(
                                        value = aiClientName,
                                        onValueChange = { aiClientName = it },
                                        label = { Text("Client Name") },
                                        modifier = Modifier.fillMaxWidth().testTag("input_ai_client_name")
                                    )

                                    OutlinedTextField(
                                        value = aiOccasion,
                                        onValueChange = { aiOccasion = it },
                                        label = { Text("Occasion / Topic (e.g., Birthday, SIP TopUp, Renewal)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    OutlinedTextField(
                                        value = aiCustomContext,
                                        onValueChange = { aiCustomContext = it },
                                        label = { Text("Custom Tone / Key Details") },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = {
                                            viewModel.generateAIWish(aiClientName, aiOccasion, aiCustomContext)
                                        },
                                        modifier = Modifier.fillMaxWidth().testTag("btn_generate_ai_wish"),
                                        enabled = !isGeneratingAi
                                    ) {
                                        if (isGeneratingAi) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generating AI Content...")
                                        } else {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Generate Personalized Draft")
                                        }
                                    }
                                }
                            }
                        }

                        if (aiGeneratedText != null) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("AI Drafted Result", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                                            IconButton(onClick = {
                                                val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                                val clip = android.content.ClipData.newPlainText("AI Message", aiGeneratedText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                            }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            aiGeneratedText!!,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onTertiaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTemplateDialog) {
        AddTemplateModal(
            onDismiss = { showAddTemplateDialog = false },
            onConfirm = { title, cat, content, isApproved, metaName ->
                viewModel.addTemplate(title, cat, content, isApproved, metaName)
                showAddTemplateDialog = false
            }
        )
    }
}

@Composable
fun MasterTextEditorSection() {
    val context = LocalContext.current
    var isTeluguMode by remember { mutableStateOf(true) }

    // Master Text States
    var birthdayMaster by remember(isTeluguMode) {
        mutableStateOf(
            if (isTeluguMode) MasterTextManager.getMasterText(context, MasterTextManager.KEY_TELUGU_BIRTHDAY, MasterTextManager.DEFAULT_TELUGU_BIRTHDAY)
            else MasterTextManager.getMasterText(context, MasterTextManager.KEY_ENGLISH_BIRTHDAY, MasterTextManager.DEFAULT_ENGLISH_BIRTHDAY)
        )
    }

    var anniversaryMaster by remember(isTeluguMode) {
        mutableStateOf(
            if (isTeluguMode) MasterTextManager.getMasterText(context, MasterTextManager.KEY_TELUGU_ANNIVERSARY, MasterTextManager.DEFAULT_TELUGU_ANNIVERSARY)
            else MasterTextManager.getMasterText(context, MasterTextManager.KEY_ENGLISH_ANNIVERSARY, MasterTextManager.DEFAULT_ENGLISH_ANNIVERSARY)
        )
    }

    var renewalMaster by remember(isTeluguMode) {
        mutableStateOf(
            if (isTeluguMode) MasterTextManager.getMasterText(context, MasterTextManager.KEY_TELUGU_RENEWAL, MasterTextManager.DEFAULT_TELUGU_RENEWAL)
            else MasterTextManager.getMasterText(context, MasterTextManager.KEY_ENGLISH_RENEWAL, MasterTextManager.DEFAULT_ENGLISH_RENEWAL)
        )
    }

    var generalMaster by remember(isTeluguMode) {
        mutableStateOf(
            if (isTeluguMode) MasterTextManager.getMasterText(context, MasterTextManager.KEY_TELUGU_GENERAL, MasterTextManager.DEFAULT_TELUGU_GENERAL)
            else MasterTextManager.getMasterText(context, MasterTextManager.KEY_ENGLISH_GENERAL, MasterTextManager.DEFAULT_ENGLISH_GENERAL)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Language Selector Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Master Text Body Customizer", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Customize default greeting texts and placeholders used in wishes & reminders.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    FilterChip(
                        selected = isTeluguMode,
                        onClick = { isTeluguMode = true },
                        label = { Text("తెలుగు (Pure)", fontSize = 11.sp) },
                        modifier = Modifier.testTag("btn_master_telugu")
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    FilterChip(
                        selected = !isTeluguMode,
                        onClick = { isTeluguMode = false },
                        label = { Text("English", fontSize = 11.sp) },
                        modifier = Modifier.testTag("btn_master_english")
                    )
                }
            }
        }

        // Available Placeholders Tag Bar
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text("Dynamic Placeholders Supported:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("{client_name} - Client Full Name | {policy_no} - Policy Number | {due_date} - Renewal Date | {premium} - Premium Amount", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        // 1. Birthday Master Text Card
        MasterTextCard(
            title = if (isTeluguMode) "పుట్టినరోజు మాస్టర్ టెక్స్ట్ (Birthday Master)" else "Birthday Master Text 🎂",
            value = birthdayMaster,
            onValueChange = { birthdayMaster = it },
            onSave = {
                val key = if (isTeluguMode) MasterTextManager.KEY_TELUGU_BIRTHDAY else MasterTextManager.KEY_ENGLISH_BIRTHDAY
                MasterTextManager.saveMasterText(context, key, birthdayMaster)
                Toast.makeText(context, "Saved Birthday Master Text!", Toast.LENGTH_SHORT).show()
            }
        )

        // 2. Anniversary Master Text Card
        MasterTextCard(
            title = if (isTeluguMode) "పెళ్లి రోజు మాస్టర్ టెక్స్ట్ (Anniversary Master)" else "Anniversary Master Text 💍",
            value = anniversaryMaster,
            onValueChange = { anniversaryMaster = it },
            onSave = {
                val key = if (isTeluguMode) MasterTextManager.KEY_TELUGU_ANNIVERSARY else MasterTextManager.KEY_ENGLISH_ANNIVERSARY
                MasterTextManager.saveMasterText(context, key, anniversaryMaster)
                Toast.makeText(context, "Saved Anniversary Master Text!", Toast.LENGTH_SHORT).show()
            }
        )

        // 3. Policy Renewal Master Text Card
        MasterTextCard(
            title = if (isTeluguMode) "పాలసీ రెన్యూవల్ మాస్టర్ టెక్స్ట్ (Renewal Master)" else "Policy Renewal Master Text 📄",
            value = renewalMaster,
            onValueChange = { renewalMaster = it },
            onSave = {
                val key = if (isTeluguMode) MasterTextManager.KEY_TELUGU_RENEWAL else MasterTextManager.KEY_ENGLISH_RENEWAL
                MasterTextManager.saveMasterText(context, key, renewalMaster)
                Toast.makeText(context, "Saved Policy Renewal Master Text!", Toast.LENGTH_SHORT).show()
            }
        )

        // 4. Portfolio / General Master Text Card
        MasterTextCard(
            title = if (isTeluguMode) "జనరల్ / శ్రేయోభిలాషి మాస్టర్ టెక్స్ట్ (General Master)" else "General / Campaign Master Text 📢",
            value = generalMaster,
            onValueChange = { generalMaster = it },
            onSave = {
                val key = if (isTeluguMode) MasterTextManager.KEY_TELUGU_GENERAL else MasterTextManager.KEY_ENGLISH_GENERAL
                MasterTextManager.saveMasterText(context, key, generalMaster)
                Toast.makeText(context, "Saved General Master Text!", Toast.LENGTH_SHORT).show()
            }
        )

        // Reset All Button
        TextButton(
            onClick = {
                MasterTextManager.resetToDefaults(context)
                birthdayMaster = if (isTeluguMode) MasterTextManager.DEFAULT_TELUGU_BIRTHDAY else MasterTextManager.DEFAULT_ENGLISH_BIRTHDAY
                anniversaryMaster = if (isTeluguMode) MasterTextManager.DEFAULT_TELUGU_ANNIVERSARY else MasterTextManager.DEFAULT_ENGLISH_ANNIVERSARY
                renewalMaster = if (isTeluguMode) MasterTextManager.DEFAULT_TELUGU_RENEWAL else MasterTextManager.DEFAULT_ENGLISH_RENEWAL
                generalMaster = if (isTeluguMode) MasterTextManager.DEFAULT_TELUGU_GENERAL else MasterTextManager.DEFAULT_ENGLISH_GENERAL
                Toast.makeText(context, "Reset all Master Texts to defaults!", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 12.dp)
        ) {
            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset All Master Texts to Defaults")
        }
    }
}

@Composable
fun MasterTextCard(
    title: String,
    value: String,
    onValueChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Button(
                    onClick = onSave,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Text("Save", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                maxLines = 5,
                textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
            )
        }
    }
}

@Composable
fun AddTemplateModal(
    onDismiss: () -> Unit,
    onConfirm: (String, TemplateCategory, String, Boolean, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(TemplateCategory.RENEWAL) }
    var content by remember { mutableStateOf("") }
    var isApproved by remember { mutableStateOf(true) }
    var metaName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Message Template", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Template Title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("Template Content") }, modifier = Modifier.fillMaxWidth().height(100.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isApproved, onCheckedChange = { isApproved = it })
                    Text("Pre-Approved WhatsApp Business Template", fontSize = 12.sp)
                }
                if (isApproved) {
                    OutlinedTextField(value = metaName, onValueChange = { metaName = it }, label = { Text("Meta Template Code Name") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() && content.isNotBlank()) onConfirm(title, category, content, isApproved, metaName)
            }) { Text("Save Template") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
