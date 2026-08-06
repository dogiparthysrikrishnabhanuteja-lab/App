package com.example.data.database

import androidx.room.*
import com.example.data.model.Client
import com.example.data.model.CustomGroup
import com.example.data.model.GroupMemberCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupDao {
    @Query("SELECT * FROM custom_groups ORDER BY name ASC")
    fun getAllGroups(): Flow<List<CustomGroup>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: CustomGroup): Long

    @Delete
    suspend fun deleteGroup(group: CustomGroup)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addMemberToGroup(crossRef: GroupMemberCrossRef)

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND clientId = :clientId")
    suspend fun removeMemberFromGroup(groupId: Long, clientId: Long)

    @Query("SELECT c.* FROM clients c INNER JOIN group_members gm ON c.id = gm.clientId WHERE gm.groupId = :groupId")
    fun getClientsForGroup(groupId: Long): Flow<List<Client>>

    @Query("SELECT groupId FROM group_members WHERE clientId = :clientId")
    fun getGroupIdsForClient(clientId: Long): Flow<List<Long>>

    @Query("UPDATE OR IGNORE group_members SET clientId = :newClientId WHERE clientId = :oldClientId")
    suspend fun reassignGroupMembers(oldClientId: Long, newClientId: Long)

    @Query("DELETE FROM group_members WHERE clientId = :oldClientId")
    suspend fun removeAllGroupMembershipsForClient(oldClientId: Long)
}
