package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ProductType(val displayName: String) {
    VEHICLE_INSURANCE("Vehicle Insurance"),
    HEALTH_INSURANCE("Health Insurance"),
    LIFE_INSURANCE("Life Insurance"),
    MUTUAL_FUND_SIP("Mutual Fund SIP"),
    MUTUAL_FUND_LUMPSUM("Mutual Fund Lumpsum"),
    TERM_PLAN("Term Plan"),
    COMMERCIAL_INSURANCE("Commercial Insurance")
}

enum class PaymentFrequency(val displayName: String) {
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    HALF_YEARLY("Half-Yearly"),
    YEARLY("Yearly"),
    ONE_TIME("One-Time")
}

@Entity(
    tableName = "policies",
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
data class PolicyProduct(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientId: Long,
    val productType: ProductType,
    val policyNumber: String,
    val providerName: String,
    val premiumAmount: Double,
    val paymentFrequency: PaymentFrequency,
    val renewalDate: String, // YYYY-MM-DD
    val reminderLeadDays: Int = 7, // Configurable lead time e.g., 7 days, 30 days
    val notes: String = "",
    val isPaid: Boolean = false
)
