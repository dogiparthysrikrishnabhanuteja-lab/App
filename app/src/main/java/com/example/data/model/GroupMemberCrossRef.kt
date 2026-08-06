package com.example.data.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "group_members",
    primaryKeys = ["groupId", "clientId"],
    indices = [Index(value = ["groupId"]), Index(value = ["clientId"])]
)
data class GroupMemberCrossRef(
    val groupId: Long,
    val clientId: Long
)
