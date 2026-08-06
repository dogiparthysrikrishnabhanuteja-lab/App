package com.example.data.model

data class DuplicateGroup(
    val id: String,
    val matchType: String, // "PHONE", "EMAIL", "BOTH"
    val matchKey: String,
    val primaryClient: Client,
    val duplicateClients: List<Client>
)

data class ContactImportResult(
    val newContactsAdded: Int,
    val contactsAutoMerged: Int,
    val duplicateGroupsFlagged: Int,
    val duplicateGroups: List<DuplicateGroup> = emptyList()
)
