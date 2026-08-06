package com.example.data.database

import androidx.room.*
import com.example.data.model.DocumentItem
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {
    @Query("SELECT * FROM documents WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getDocumentsForClient(clientId: Long): Flow<List<DocumentItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(document: DocumentItem): Long

    @Delete
    suspend fun deleteDocument(document: DocumentItem)

    @Query("UPDATE documents SET clientId = :newClientId WHERE clientId = :oldClientId")
    suspend fun reassignClientDocuments(oldClientId: Long, newClientId: Long)
}
