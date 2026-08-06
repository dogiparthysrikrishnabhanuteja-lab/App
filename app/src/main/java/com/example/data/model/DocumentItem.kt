package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DocType(val displayName: String) {
    KYC("KYC Document"),
    POLICY_PDF("Policy Bond/PDF"),
    RECEIPT("Premium Receipt"),
    OTHER("Other Attachment")
}

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = Client::class,
            parentColumns = ["id"],
            childColumns = ["clientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["clientId"])]
)
data class DocumentItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val docType: DocType,
    val title: String,
    val fileUriOrNotes: String,
    val createdAt: Long = System.currentTimeMillis()
)
