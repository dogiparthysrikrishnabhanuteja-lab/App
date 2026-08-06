package com.example.data.database

import androidx.room.*
import com.example.data.model.MessageTemplate
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {
    @Query("SELECT * FROM message_templates ORDER BY title ASC")
    fun getAllTemplates(): Flow<List<MessageTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: MessageTemplate): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<MessageTemplate>)

    @Update
    suspend fun updateTemplate(template: MessageTemplate)

    @Delete
    suspend fun deleteTemplate(template: MessageTemplate)
}
