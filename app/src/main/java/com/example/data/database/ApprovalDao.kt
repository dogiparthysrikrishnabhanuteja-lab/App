package com.example.data.database

import androidx.room.*
import com.example.data.model.ApprovalStatus
import com.example.data.model.ReminderApproval
import kotlinx.coroutines.flow.Flow

@Dao
interface ApprovalDao {
    @Query("SELECT * FROM reminder_approvals ORDER BY createdAt DESC")
    fun getAllApprovals(): Flow<List<ReminderApproval>>

    @Query("SELECT * FROM reminder_approvals WHERE status = :status ORDER BY createdAt DESC")
    fun getApprovalsByStatus(status: ApprovalStatus): Flow<List<ReminderApproval>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApproval(approval: ReminderApproval): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovals(approvals: List<ReminderApproval>)

    @Update
    suspend fun updateApproval(approval: ReminderApproval)

    @Query("UPDATE reminder_approvals SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: ApprovalStatus)

    @Delete
    suspend fun deleteApproval(approval: ReminderApproval)

    @Query("SELECT COUNT(*) FROM reminder_approvals WHERE status = 'PENDING'")
    fun getPendingApprovalCount(): Flow<Int>

    @Query("UPDATE reminder_approvals SET clientId = :newClientId, clientName = :newName, clientPhone = :newPhone WHERE clientId = :oldClientId")
    suspend fun reassignClientApprovals(oldClientId: Long, newClientId: Long, newName: String, newPhone: String)

    @Query("DELETE FROM reminder_approvals WHERE clientId = :clientId AND status = 'PENDING'")
    suspend fun deletePendingApprovalsForClient(clientId: Long)
}
