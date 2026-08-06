package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ConsentStatus {
    CONSENTED,
    OPTED_OUT,
    PENDING
}

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String,
    val email: String = "",
    val dob: String = "", // YYYY-MM-DD
    val anniversaryDate: String = "", // YYYY-MM-DD
    val consentStatus: ConsentStatus = ConsentStatus.CONSENTED,
    val isProspect: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
