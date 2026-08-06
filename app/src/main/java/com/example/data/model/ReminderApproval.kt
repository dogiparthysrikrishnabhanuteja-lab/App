package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ApprovalType {
    RENEWAL,
    BIRTHDAY,
    ANNIVERSARY,
    CAMPAIGN
}

enum class ApprovalStatus {
    PENDING,
    APPROVED,
    DISPATCHED,
    DISCARDED
}

@Entity(tableName = "reminder_approvals")
data class ReminderApproval(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val clientName: String,
    val clientPhone: String,
    val type: ApprovalType,
    val messageText: String,
    val attachmentUrl: String = "",
    val policyId: Long? = null,
    val dueDate: String = "",
    val status: ApprovalStatus = ApprovalStatus.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)
