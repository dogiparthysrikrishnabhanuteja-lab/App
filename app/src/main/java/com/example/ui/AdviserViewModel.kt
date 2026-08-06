package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.remote.GeminiApiClient
import com.example.repository.AdviserRepository
import com.example.service.NotificationScheduler
import com.example.service.WhatsAppHelper
import com.example.service.CloudSyncState
import com.example.service.FirestoreSyncService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AdviserViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = AdviserRepository(db)

    val cloudSyncState = FirestoreSyncService.syncState

    val duplicateGroups = MutableStateFlow<List<DuplicateGroup>>(emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            scanDuplicates()
        }
        viewModelScope.launch {
            repository.allClients.collect {
                scanDuplicates()
            }
        }
    }

    fun scanDuplicates() {
        viewModelScope.launch {
            duplicateGroups.value = repository.findDuplicateGroups()
        }
    }

    fun triggerCloudSync() {
        viewModelScope.launch {
            FirestoreSyncService.syncAll(
                clients = clients.value,
                policies = policies.value,
                groups = groups.value,
                approvals = allApprovals.value
            )
        }
    }

    val clients: StateFlow<List<Client>> = repository.allClients
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val policies: StateFlow<List<PolicyProduct>> = repository.allPolicies
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val groups: StateFlow<List<CustomGroup>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingApprovals: StateFlow<List<ReminderApproval>> = repository.pendingApprovals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allApprovals: StateFlow<List<ReminderApproval>> = repository.allApprovals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<MessageTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = repository.auditLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val searchQuery = MutableStateFlow("")
    val selectedGroupFilter = MutableStateFlow<Long?>(null)

    val isPasscodeEnabled = MutableStateFlow(false)
    val isPasscodeLocked = MutableStateFlow(false)
    val currentPasscode = MutableStateFlow("1234")

    val aiGenerationState = MutableStateFlow<String?>(null)
    val isGeneratingAi = MutableStateFlow(false)

    // Filtered Clients based on Search and Group
    val filteredClients: StateFlow<List<Client>> = combine(
        clients, searchQuery, selectedGroupFilter
    ) { clientList, query, groupId ->
        clientList.filter { client ->
            val matchesQuery = query.isBlank() ||
                    client.name.contains(query, ignoreCase = true) ||
                    client.phone.contains(query, ignoreCase = true) ||
                    client.email.contains(query, ignoreCase = true)
            matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addClient(name: String, phone: String, email: String, dob: String, anniversaryDate: String, consentStatus: ConsentStatus, isProspect: Boolean, notes: String) {
        viewModelScope.launch {
            val client = Client(
                name = name.trim(),
                phone = phone.trim(),
                email = email.trim(),
                dob = dob.trim(),
                anniversaryDate = anniversaryDate.trim(),
                consentStatus = consentStatus,
                isProspect = isProspect,
                notes = notes.trim()
            )
            repository.insertClient(client)
            repository.logAction("ADD_CLIENT", name, "Added new client contact.")
        }
    }

    fun updateClient(client: Client) {
        viewModelScope.launch {
            repository.updateClient(client)
            repository.logAction("UPDATE_CLIENT", client.name, "Updated client details.")
        }
    }

    fun deleteClient(client: Client) {
        viewModelScope.launch {
            repository.deleteClient(client)
            repository.logAction("DELETE_CLIENT", client.name, "Deleted client record.")
        }
    }

    fun addPolicy(
        clientId: Long,
        clientName: String,
        productType: ProductType,
        policyNumber: String,
        providerName: String,
        premiumAmount: Double,
        paymentFrequency: PaymentFrequency,
        renewalDate: String,
        reminderLeadDays: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val policy = PolicyProduct(
                clientId = clientId,
                productType = productType,
                policyNumber = policyNumber.trim(),
                providerName = providerName.trim(),
                premiumAmount = premiumAmount,
                paymentFrequency = paymentFrequency,
                renewalDate = renewalDate.trim(),
                reminderLeadDays = reminderLeadDays,
                notes = notes.trim()
            )
            val policyId = repository.insertPolicy(policy)

            // Auto-create draft approval in queue
            val messageText = "Dear $clientName,\n\nYour ${productType.displayName} (Policy No: $policyNumber) with $providerName is due for renewal on $renewalDate.\nPremium Amount: ₹$premiumAmount.\n\nKindly acknowledge to receive the direct payment link."
            repository.insertApproval(
                ReminderApproval(
                    clientId = clientId,
                    clientName = clientName,
                    clientPhone = "", // Will be fetched when sending
                    type = ApprovalType.RENEWAL,
                    messageText = messageText,
                    policyId = policyId,
                    dueDate = renewalDate,
                    status = ApprovalStatus.PENDING
                )
            )

            repository.logAction("ADD_POLICY", clientName, "Added ${productType.displayName} ($policyNumber).")
        }
    }

    fun updatePolicy(policy: PolicyProduct) {
        viewModelScope.launch {
            repository.updatePolicy(policy)
            repository.logAction("UPDATE_POLICY", "Policy #${policy.id}", "Updated policy details.")
        }
    }

    fun markPolicyPaid(policy: PolicyProduct) {
        viewModelScope.launch {
            repository.updatePolicy(policy.copy(isPaid = true))
            repository.logAction("POLICY_PAID", "Policy #${policy.policyNumber}", "Marked policy as paid.")
        }
    }

    fun togglePolicyPaidStatus(policy: PolicyProduct) {
        viewModelScope.launch {
            val updated = policy.copy(isPaid = !policy.isPaid)
            repository.updatePolicy(updated)
            repository.logAction("TOGGLE_POLICY_PAID", "Policy #${policy.policyNumber}", "Updated paid status to ${updated.isPaid}.")
        }
    }

    fun deletePolicy(policy: PolicyProduct) {
        viewModelScope.launch {
            repository.deletePolicy(policy)
            repository.logAction("DELETE_POLICY", "Policy #${policy.id}", "Deleted policy record.")
        }
    }

    fun addGroup(name: String, description: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertGroup(CustomGroup(name = name.trim(), description = description.trim(), colorHex = colorHex))
            repository.logAction("ADD_GROUP", name, "Created custom group.")
        }
    }

    fun addGroupWithMembers(name: String, description: String, colorHex: String, memberClientIds: Set<Long>) {
        viewModelScope.launch {
            val groupId = repository.insertGroup(CustomGroup(name = name.trim(), description = description.trim(), colorHex = colorHex))
            memberClientIds.forEach { clientId ->
                repository.addClientToGroup(groupId, clientId)
            }
            repository.logAction("ADD_GROUP", name, "Created custom group with ${memberClientIds.size} members.")
        }
    }

    fun updateGroupMembers(groupId: Long, updatedClientIds: Set<Long>, currentClientIds: Set<Long>) {
        viewModelScope.launch {
            val toAdd = updatedClientIds - currentClientIds
            val toRemove = currentClientIds - updatedClientIds
            toAdd.forEach { repository.addClientToGroup(groupId, it) }
            toRemove.forEach { repository.removeClientFromGroup(groupId, it) }
        }
    }

    fun deleteGroup(group: CustomGroup) {
        viewModelScope.launch {
            repository.deleteGroup(group)
            repository.logAction("DELETE_GROUP", group.name, "Deleted custom group.")
        }
    }

    fun addClientToGroup(groupId: Long, clientId: Long) {
        viewModelScope.launch {
            repository.addClientToGroup(groupId, clientId)
        }
    }

    fun removeClientFromGroup(groupId: Long, clientId: Long) {
        viewModelScope.launch {
            repository.removeClientFromGroup(groupId, clientId)
        }
    }

    fun updateApprovalMessage(approval: ReminderApproval, newText: String, newAttachmentUrl: String = "") {
        viewModelScope.launch {
            repository.updateApproval(approval.copy(messageText = newText, attachmentUrl = newAttachmentUrl))
        }
    }

    fun approveAndDispatchWhatsApp(context: Context, approval: ReminderApproval) {
        viewModelScope.launch {
            repository.updateApprovalStatus(approval.id, ApprovalStatus.DISPATCHED)
            val client = repository.getClientByIdOneShot(approval.clientId)
            val phone = client?.phone ?: approval.clientPhone

            WhatsAppHelper.sendWhatsAppMessage(context, phone, approval.messageText, approval.attachmentUrl.ifBlank { null })
            repository.logAction("WHATSAPP_DISPATCH", approval.clientName, "Dispatched ${approval.type.name} via WhatsApp.")

            NotificationScheduler.showPendingActionNotification(
                context,
                "Message Dispatched",
                "Sent ${approval.type.name} reminder to ${approval.clientName}"
            )
        }
    }

    fun createWishApproval(
        clientId: Long,
        clientName: String,
        clientPhone: String,
        type: ApprovalType,
        messageText: String,
        attachmentUrl: String = "",
        onDone: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            repository.insertApproval(
                ReminderApproval(
                    clientId = clientId,
                    clientName = clientName,
                    clientPhone = clientPhone,
                    type = type,
                    messageText = messageText,
                    attachmentUrl = attachmentUrl,
                    dueDate = dateStr,
                    status = ApprovalStatus.PENDING
                )
            )
            repository.logAction("QUEUE_WISH", clientName, "Queued ${type.name} wish for approval.")
            onDone()
        }
    }

    fun discardApproval(approval: ReminderApproval) {
        viewModelScope.launch {
            repository.updateApprovalStatus(approval.id, ApprovalStatus.DISCARDED)
            repository.logAction("DISCARD_APPROVAL", approval.clientName, "Discarded pending approval.")
        }
    }

    fun generateAIWish(clientName: String, occasion: String, customPrompt: String = "") {
        viewModelScope.launch {
            isGeneratingAi.value = true
            val result = GeminiApiClient.generatePersonalizedWish(clientName, occasion, customPrompt)
            isGeneratingAi.value = false

            result.onSuccess { text ->
                aiGenerationState.value = text
            }.onFailure { err ->
                aiGenerationState.value = "Fallback Wish: Warmest $occasion wishes to dear $clientName! Wishing you health, peace, and financial prosperity today and always."
            }
        }
    }

    fun generateAIRenewalMessage(clientName: String, productType: String, policyNo: String, amount: Double, dueDate: String) {
        viewModelScope.launch {
            isGeneratingAi.value = true
            val result = GeminiApiClient.generateRenewalFollowUpMessage(clientName, productType, policyNo, amount, dueDate)
            isGeneratingAi.value = false

            result.onSuccess { text ->
                aiGenerationState.value = text
            }.onFailure { err ->
                aiGenerationState.value = "Dear $clientName,\nYour $productType (No: $policyNo) is due for renewal on $dueDate. Premium Amount: ₹$amount. Please contact us to complete payment."
            }
        }
    }

    fun importPhoneContacts(context: Context, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importPhoneContacts(context)
            scanDuplicates()
            onComplete(count)
        }
    }

    fun importPhoneContactsWithAutoMerge(context: Context, onResult: (ContactImportResult) -> Unit) {
        viewModelScope.launch {
            val result = repository.importPhoneContactsWithAutoMerge(context)
            scanDuplicates()
            onResult(result)
        }
    }

    fun autoMergeAllDuplicates(onComplete: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val count = repository.autoMergeAllDuplicates()
            scanDuplicates()
            onComplete(count)
        }
    }

    fun mergeSelectedDuplicateGroup(primaryId: Long, duplicateIds: List<Long>, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.mergeClientsInDatabase(primaryId, duplicateIds)
            scanDuplicates()
            onComplete()
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportDataToJson()
            onResult(json)
        }
    }

    fun restoreBackup(jsonStr: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.restoreDataFromJson(jsonStr)
            onResult(success)
        }
    }

    fun exportCsv(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val csv = repository.exportToCsv()
            onResult(csv)
        }
    }

    fun addDocument(clientId: Long, docType: DocType, title: String, notes: String) {
        viewModelScope.launch {
            repository.insertDocument(DocumentItem(clientId = clientId, docType = docType, title = title, fileUriOrNotes = notes))
        }
    }

    fun addTemplate(title: String, category: TemplateCategory, content: String, isApproved: Boolean, metaName: String) {
        viewModelScope.launch {
            repository.insertTemplate(MessageTemplate(title = title, category = category, content = content, isApprovedWhatsAppTemplate = isApproved, metaTemplateName = metaName))
        }
    }

    fun queuePolicyReminder(policy: PolicyProduct, client: Client?, onDone: () -> Unit) {
        viewModelScope.launch {
            val msg = "Dear ${client?.name ?: ""},\n\nYour ${policy.productType.displayName} (Policy No: ${policy.policyNumber}) is due on ${policy.renewalDate}. Premium: ₹${policy.premiumAmount}.\n\nPlease acknowledge for payment options."
            repository.insertApproval(
                ReminderApproval(
                    clientId = policy.clientId,
                    clientName = client?.name ?: "",
                    clientPhone = client?.phone ?: "",
                    type = ApprovalType.RENEWAL,
                    messageText = msg,
                    policyId = policy.id,
                    dueDate = policy.renewalDate,
                    status = ApprovalStatus.PENDING
                )
            )
            onDone()
        }
    }

    fun queueBulkCampaign(groupClients: List<Client>, fullContent: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            groupClients.forEach { client ->
                val personalized = fullContent.replace("{client_name}", client.name)
                repository.insertApproval(
                    ReminderApproval(
                        clientId = client.id,
                        clientName = client.name,
                        clientPhone = client.phone,
                        type = ApprovalType.CAMPAIGN,
                        messageText = personalized,
                        dueDate = dateStr,
                        status = ApprovalStatus.PENDING
                    )
                )
            }
            onDone()
        }
    }

    fun checkPasscode(pin: String): Boolean {
        if (pin == currentPasscode.value) {
            isPasscodeLocked.value = false
            return true
        }
        return false
    }
}
