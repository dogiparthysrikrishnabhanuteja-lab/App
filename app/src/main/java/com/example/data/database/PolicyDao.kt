package com.example.data.database

import androidx.room.*
import com.example.data.model.PolicyProduct
import kotlinx.coroutines.flow.Flow

@Dao
interface PolicyDao {
    @Query("SELECT * FROM policies ORDER BY renewalDate ASC")
    fun getAllPolicies(): Flow<List<PolicyProduct>>

    @Query("SELECT * FROM policies WHERE clientId = :clientId ORDER BY renewalDate ASC")
    fun getPoliciesForClient(clientId: Long): Flow<List<PolicyProduct>>

    @Query("SELECT * FROM policies WHERE renewalDate BETWEEN :startDate AND :endDate ORDER BY renewalDate ASC")
    fun getPoliciesDueBetween(startDate: String, endDate: String): Flow<List<PolicyProduct>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicy(policy: PolicyProduct): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolicies(policies: List<PolicyProduct>)

    @Update
    suspend fun updatePolicy(policy: PolicyProduct)

    @Delete
    suspend fun deletePolicy(policy: PolicyProduct)

    @Query("SELECT SUM(premiumAmount) FROM policies")
    fun getTotalPremiumValue(): Flow<Double?>

    @Query("UPDATE policies SET clientId = :newClientId WHERE clientId = :oldClientId")
    suspend fun reassignClientPolicies(oldClientId: Long, newClientId: Long)
}
