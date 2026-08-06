package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TemplateCategory(val label: String) {
    RENEWAL("Policy Renewal"),
    BIRTHDAY("Birthday Wish"),
    ANNIVERSARY("Anniversary Wish"),
    SIP_TOPUP("SIP Top-Up"),
    GENERAL("General Broadcast")
}

@Entity(tableName = "message_templates")
data class MessageTemplate(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: TemplateCategory,
    val content: String,
    val isApprovedWhatsAppTemplate: Boolean = false,
    val metaTemplateName: String = ""
)
