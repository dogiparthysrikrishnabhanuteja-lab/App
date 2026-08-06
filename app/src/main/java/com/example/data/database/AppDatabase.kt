package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.*

@Database(
    entities = [
        Client::class,
        PolicyProduct::class,
        CustomGroup::class,
        GroupMemberCrossRef::class,
        ReminderApproval::class,
        MessageTemplate::class,
        AuditLog::class,
        DocumentItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun policyDao(): PolicyDao
    abstract fun groupDao(): GroupDao
    abstract fun approvalDao(): ApprovalDao
    abstract fun templateDao(): TemplateDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adviser_sync_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
