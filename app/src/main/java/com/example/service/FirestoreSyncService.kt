package com.example.service

import android.util.Log
import com.example.data.model.Client
import com.example.data.model.CustomGroup
import com.example.data.model.PolicyProduct
import com.example.data.model.ReminderApproval
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class CloudSyncState {
    object Idle : CloudSyncState()
    object Syncing : CloudSyncState()
    data class Success(val message: String, val lastSyncTime: String) : CloudSyncState()
    data class Offline(val reason: String) : CloudSyncState()
}

object FirestoreSyncService {

    private const val TAG = "FirestoreSyncService"

    private val _syncState = MutableStateFlow<CloudSyncState>(CloudSyncState.Idle)
    val syncState: StateFlow<CloudSyncState> = _syncState

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseFirestore instance unavailable: ${e.message}")
            null
        }
    }

    suspend fun syncAll(
        clients: List<Client>,
        policies: List<PolicyProduct>,
        groups: List<CustomGroup>,
        approvals: List<ReminderApproval>
    ) = withContext(Dispatchers.IO) {
        val db = firestore
        if (db == null) {
            _syncState.value = CloudSyncState.Offline("Firestore service offline or local-only mode")
            return@withContext
        }

        _syncState.value = CloudSyncState.Syncing

        try {
            // Sync Clients
            for (client in clients) {
                val clientMap = hashMapOf(
                    "id" to client.id,
                    "name" to client.name,
                    "phone" to client.phone,
                    "email" to client.email,
                    "dob" to client.dob,
                    "anniversaryDate" to client.anniversaryDate,
                    "consentStatus" to client.consentStatus.name,
                    "isProspect" to client.isProspect,
                    "notes" to client.notes,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("clients").document(client.id.toString())
                    .set(clientMap, SetOptions.merge())
            }

            // Sync Policies
            for (policy in policies) {
                val policyMap = hashMapOf(
                    "id" to policy.id,
                    "clientId" to policy.clientId,
                    "productType" to policy.productType.displayName,
                    "policyNumber" to policy.policyNumber,
                    "providerName" to policy.providerName,
                    "premiumAmount" to policy.premiumAmount,
                    "paymentFrequency" to policy.paymentFrequency.displayName,
                    "renewalDate" to policy.renewalDate,
                    "reminderLeadDays" to policy.reminderLeadDays,
                    "isPaid" to policy.isPaid,
                    "notes" to policy.notes,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("policies").document(policy.id.toString())
                    .set(policyMap, SetOptions.merge())
            }

            // Sync Groups
            for (group in groups) {
                val groupMap = hashMapOf(
                    "id" to group.id,
                    "name" to group.name,
                    "description" to group.description,
                    "colorHex" to group.colorHex,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("groups").document(group.id.toString())
                    .set(groupMap, SetOptions.merge())
            }

            // Sync Approvals / Messages
            for (approval in approvals) {
                val approvalMap = hashMapOf(
                    "id" to approval.id,
                    "clientId" to approval.clientId,
                    "clientName" to approval.clientName,
                    "clientPhone" to approval.clientPhone,
                    "type" to approval.type.name,
                    "messageText" to approval.messageText,
                    "attachmentUrl" to approval.attachmentUrl,
                    "dueDate" to approval.dueDate,
                    "status" to approval.status.name,
                    "createdAt" to approval.createdAt,
                    "lastUpdated" to System.currentTimeMillis()
                )
                db.collection("messages").document(approval.id.toString())
                    .set(approvalMap, SetOptions.merge())
            }

            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val countSummary = "${clients.size} clients, ${policies.size} policies, ${approvals.size} messages"
            _syncState.value = CloudSyncState.Success("Cloud backup updated ($countSummary)", timeStr)
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing to Firestore", e)
            _syncState.value = CloudSyncState.Offline("Sync error: ${e.localizedMessage ?: "Offline"}")
        }
    }
}
