package com.example.repository

import android.content.ContentResolver
import android.content.Context
import android.provider.ContactsContract
import com.example.data.database.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class AdviserRepository(private val db: AppDatabase) {

    val allClients: Flow<List<Client>> = db.clientDao().getAllClients()
    val allPolicies: Flow<List<PolicyProduct>> = db.policyDao().getAllPolicies()
    val allGroups: Flow<List<CustomGroup>> = db.groupDao().getAllGroups()
    val allApprovals: Flow<List<ReminderApproval>> = db.approvalDao().getAllApprovals()
    val pendingApprovals: Flow<List<ReminderApproval>> = db.approvalDao().getApprovalsByStatus(ApprovalStatus.PENDING)
    val allTemplates: Flow<List<MessageTemplate>> = db.templateDao().getAllTemplates()
    val auditLogs: Flow<List<AuditLog>> = db.auditLogDao().getAllLogs()

    suspend fun getClientById(id: Long): Flow<Client?> = db.clientDao().getClientById(id)
    suspend fun getClientByIdOneShot(id: Long): Client? = db.clientDao().getClientByIdOneShot(id)
    fun getPoliciesForClient(clientId: Long): Flow<List<PolicyProduct>> = db.policyDao().getPoliciesForClient(clientId)
    fun getDocumentsForClient(clientId: Long): Flow<List<DocumentItem>> = db.documentDao().getDocumentsForClient(clientId)
    fun getClientsForGroup(groupId: Long): Flow<List<Client>> = db.groupDao().getClientsForGroup(groupId)

    suspend fun insertClient(client: Client): Long = db.clientDao().insertClient(client)
    suspend fun updateClient(client: Client) = db.clientDao().updateClient(client)
    suspend fun deleteClient(client: Client) = db.clientDao().deleteClient(client)

    suspend fun insertPolicy(policy: PolicyProduct): Long = db.policyDao().insertPolicy(policy)
    suspend fun updatePolicy(policy: PolicyProduct) = db.policyDao().updatePolicy(policy)
    suspend fun deletePolicy(policy: PolicyProduct) = db.policyDao().deletePolicy(policy)

    suspend fun insertGroup(group: CustomGroup): Long = db.groupDao().insertGroup(group)
    suspend fun deleteGroup(group: CustomGroup) = db.groupDao().deleteGroup(group)
    suspend fun addClientToGroup(groupId: Long, clientId: Long) = db.groupDao().addMemberToGroup(GroupMemberCrossRef(groupId, clientId))
    suspend fun removeClientFromGroup(groupId: Long, clientId: Long) = db.groupDao().removeMemberFromGroup(groupId, clientId)

    suspend fun insertApproval(approval: ReminderApproval): Long = db.approvalDao().insertApproval(approval)
    suspend fun updateApproval(approval: ReminderApproval) = db.approvalDao().updateApproval(approval)
    suspend fun updateApprovalStatus(id: Long, status: ApprovalStatus) = db.approvalDao().updateStatus(id, status)
    suspend fun deleteApproval(approval: ReminderApproval) = db.approvalDao().deleteApproval(approval)

    suspend fun insertTemplate(template: MessageTemplate): Long = db.templateDao().insertTemplate(template)
    suspend fun insertDocument(doc: DocumentItem): Long = db.documentDao().insertDocument(doc)
    suspend fun deleteDocument(doc: DocumentItem) = db.documentDao().deleteDocument(doc)

    suspend fun logAction(actionType: String, clientName: String, details: String) {
        db.auditLogDao().insertLog(
            AuditLog(actionType = actionType, clientName = clientName, details = details)
        )
    }

    // Phone Contact Importer with Auto-Merge & Duplicate Detection
    fun normalizePhone(phone: String): String {
        val digits = phone.replace(Regex("[^0-9]"), "")
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun normalizeEmail(email: String): String {
        return email.trim().lowercase()
    }

    suspend fun findDuplicateGroups(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val clients = db.clientDao().getAllClients().firstOrNull() ?: emptyList()
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        val phoneGroups = clients.filter { normalizePhone(it.phone).length >= 8 }
            .groupBy { normalizePhone(it.phone) }
            .filter { it.value.size > 1 }

        val emailGroups = clients.filter { normalizeEmail(it.email).isNotBlank() }
            .groupBy { normalizeEmail(it.email) }
            .filter { it.value.size > 1 }

        val processedClientIds = mutableSetOf<Long>()

        phoneGroups.forEach { (phoneKey, group) ->
            val sortedGroup = group.sortedBy { it.id }
            val primary = sortedGroup.first()
            val duplicates = sortedGroup.drop(1)
            val allIds = sortedGroup.map { it.id }

            val primaryEmail = normalizeEmail(primary.email)
            val matchesEmail = primaryEmail.isNotBlank() && duplicates.any { normalizeEmail(it.email) == primaryEmail }

            duplicateGroups.add(
                DuplicateGroup(
                    id = "phone_$phoneKey",
                    matchType = if (matchesEmail) "BOTH" else "PHONE",
                    matchKey = phoneKey,
                    primaryClient = primary,
                    duplicateClients = duplicates
                )
            )
            processedClientIds.addAll(allIds)
        }

        emailGroups.forEach { (emailKey, group) ->
            val sortedGroup = group.sortedBy { it.id }
            val primary = sortedGroup.first()
            val duplicates = sortedGroup.drop(1).filter { !processedClientIds.contains(it.id) }

            if (duplicates.isNotEmpty()) {
                duplicateGroups.add(
                    DuplicateGroup(
                        id = "email_$emailKey",
                        matchType = "EMAIL",
                        matchKey = emailKey,
                        primaryClient = primary,
                        duplicateClients = duplicates
                    )
                )
            }
        }

        duplicateGroups
    }

    suspend fun mergeClientsInDatabase(primaryId: Long, duplicateIds: List<Long>): Client? = withContext(Dispatchers.IO) {
        val primary = db.clientDao().getClientByIdOneShot(primaryId) ?: return@withContext null
        val duplicates = duplicateIds.mapNotNull { db.clientDao().getClientByIdOneShot(it) }

        if (duplicates.isEmpty()) return@withContext primary

        var mergedName = primary.name
        var mergedPhone = primary.phone
        var mergedEmail = primary.email
        var mergedDob = primary.dob
        var mergedAnniversary = primary.anniversaryDate
        var mergedConsent = primary.consentStatus
        val notesList = mutableListOf<String>()
        if (primary.notes.isNotBlank()) notesList.add(primary.notes)

        for (dup in duplicates) {
            if (mergedName.length < dup.name.length && dup.name.isNotBlank()) {
                mergedName = dup.name
            }
            if (mergedPhone.isBlank() && dup.phone.isNotBlank()) {
                mergedPhone = dup.phone
            }
            if (mergedEmail.isBlank() && dup.email.isNotBlank()) {
                mergedEmail = dup.email
            }
            if (mergedDob.isBlank() && dup.dob.isNotBlank()) {
                mergedDob = dup.dob
            }
            if (mergedAnniversary.isBlank() && dup.anniversaryDate.isNotBlank()) {
                mergedAnniversary = dup.anniversaryDate
            }
            if (dup.consentStatus == ConsentStatus.CONSENTED) {
                mergedConsent = ConsentStatus.CONSENTED
            }
            if (dup.notes.isNotBlank() && !notesList.contains(dup.notes)) {
                notesList.add(dup.notes)
            }

            // Reassign foreign key entities
            db.policyDao().reassignClientPolicies(dup.id, primary.id)
            db.approvalDao().reassignClientApprovals(dup.id, primary.id, mergedName, mergedPhone)
            db.documentDao().reassignClientDocuments(dup.id, primary.id)
            db.groupDao().reassignGroupMembers(dup.id, primary.id)
            db.groupDao().removeAllGroupMembershipsForClient(dup.id)

            // Delete redundant duplicate client entry
            db.clientDao().deleteClient(dup)
        }

        val updatedPrimary = primary.copy(
            name = mergedName,
            phone = mergedPhone,
            email = mergedEmail,
            dob = mergedDob,
            anniversaryDate = mergedAnniversary,
            consentStatus = mergedConsent,
            notes = notesList.joinToString(" | ")
        )

        db.clientDao().updateClient(updatedPrimary)
        logAction("AUTO_MERGE_CLIENTS", updatedPrimary.name, "Merged ${duplicates.size} duplicate contact(s) into primary ID $primaryId")

        updatedPrimary
    }

    suspend fun autoMergeAllDuplicates(): Int = withContext(Dispatchers.IO) {
        val groups = findDuplicateGroups()
        var mergedCount = 0
        groups.forEach { group ->
            val primaryId = group.primaryClient.id
            val dupIds = group.duplicateClients.map { it.id }
            mergeClientsInDatabase(primaryId, dupIds)
            mergedCount += dupIds.size
        }
        mergedCount
    }

    suspend fun importPhoneContacts(context: Context): Int = withContext(Dispatchers.IO) {
        val res = importPhoneContactsWithAutoMerge(context)
        res.newContactsAdded + res.contactsAutoMerged
    }

    suspend fun importPhoneContactsWithAutoMerge(context: Context): ContactImportResult = withContext(Dispatchers.IO) {
        var newAdded = 0
        var autoMerged = 0

        try {
            val contentResolver: ContentResolver = context.contentResolver
            val cursor = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null,
                null,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )

            val existingClients = db.clientDao().getAllClients().firstOrNull() ?: emptyList()
            val clientMapByPhone = existingClients.associateBy { normalizePhone(it.phone) }.toMutableMap()

            cursor?.use { c ->
                val nameIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val pendingNewClients = mutableListOf<Client>()

                while (c.moveToNext()) {
                    val name = if (nameIndex >= 0) c.getString(nameIndex) ?: "" else ""
                    val rawPhone = if (numberIndex >= 0) c.getString(numberIndex) ?: "" else ""
                    val cleanPhone = normalizePhone(rawPhone)

                    if (name.isBlank() || cleanPhone.length < 8) continue

                    val matchedClient = clientMapByPhone[cleanPhone]

                    if (matchedClient != null) {
                        var changed = false
                        var updated = matchedClient

                        if (matchedClient.name.length < name.length && name.isNotBlank()) {
                            updated = updated.copy(name = name)
                            changed = true
                        }
                        if (changed) {
                            db.clientDao().updateClient(updated)
                            clientMapByPhone[cleanPhone] = updated
                            autoMerged++
                        }
                    } else {
                        val staged = pendingNewClients.find { normalizePhone(it.phone) == cleanPhone }
                        if (staged == null) {
                            val newC = Client(
                                name = name,
                                phone = rawPhone,
                                notes = "Imported from phone contacts"
                            )
                            pendingNewClients.add(newC)
                        } else {
                            autoMerged++
                        }
                    }
                }

                if (pendingNewClients.isNotEmpty()) {
                    db.clientDao().insertClients(pendingNewClients)
                    newAdded = pendingNewClients.size
                }
            }

            val postImportMerged = autoMergeAllDuplicates()
            autoMerged += postImportMerged

            val flaggedGroups = findDuplicateGroups()

            logAction(
                "CONTACT_IMPORT_AUTOMERGE",
                "System",
                "Imported $newAdded new contacts, auto-merged $autoMerged duplicate entries, flagged ${flaggedGroups.size} groups."
            )

            ContactImportResult(
                newContactsAdded = newAdded,
                contactsAutoMerged = autoMerged,
                duplicateGroupsFlagged = flaggedGroups.size,
                duplicateGroups = flaggedGroups
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ContactImportResult(0, 0, 0)
        }
    }

    // Seed realistic sample data if DB is empty
    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingCount = db.clientDao().getAllClients().firstOrNull()?.size ?: 0
        if (existingCount > 0) return@withContext

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val cal = Calendar.getInstance()

        // Clients
        val c1Id = db.clientDao().insertClient(Client(name = "Rajesh Sharma", phone = "+91 98765 43210", email = "rajesh.sharma@example.com", dob = "1984-08-15", anniversaryDate = "2010-11-24", consentStatus = ConsentStatus.CONSENTED, notes = "HNI Client - Preferred contact morning"))
        val c2Id = db.clientDao().insertClient(Client(name = "Priya Ananth", phone = "+91 98123 45678", email = "priya.a@example.com", dob = todayStr, anniversaryDate = "2018-05-12", consentStatus = ConsentStatus.CONSENTED, notes = "SIP Investor & Health Cover"))
        val c3Id = db.clientDao().insertClient(Client(name = "Vikram Malhotra", phone = "+91 99001 12233", email = "vikram.m@example.com", dob = "1990-02-18", anniversaryDate = todayStr, consentStatus = ConsentStatus.CONSENTED, notes = "Vehicle Fleet Owner"))
        val c4Id = db.clientDao().insertClient(Client(name = "Sneha Gupta", phone = "+91 97788 99000", email = "sneha.g@example.com", dob = "1995-12-05", consentStatus = ConsentStatus.PENDING, isProspect = true, notes = "Prospect - Interested in Term Plan"))

        // Groups
        val g1 = db.groupDao().insertGroup(CustomGroup(name = "Life Insurance Clients", description = "Term and Endowment policy holders", colorHex = "#0284C7"))
        val g2 = db.groupDao().insertGroup(CustomGroup(name = "Mutual Fund SIP Clients", description = "Active monthly SIP investors", colorHex = "#16A34A"))
        val g3 = db.groupDao().insertGroup(CustomGroup(name = "Q3 Renewals", description = "Policies renewing Jul-Sep", colorHex = "#D97706"))

        db.groupDao().addMemberToGroup(GroupMemberCrossRef(g1, c1Id))
        db.groupDao().addMemberToGroup(GroupMemberCrossRef(g2, c2Id))
        db.groupDao().addMemberToGroup(GroupMemberCrossRef(g3, c3Id))

        // Policies due dates
        cal.time = Date()
        cal.add(Calendar.DAY_OF_MONTH, 3)
        val dueIn3Days = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        cal.time = Date()
        cal.add(Calendar.DAY_OF_MONTH, 12)
        val dueIn12Days = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        cal.time = Date()
        cal.add(Calendar.DAY_OF_MONTH, -2)
        val overdueStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val p1 = db.policyDao().insertPolicy(
            PolicyProduct(
                clientId = c1Id,
                productType = ProductType.LIFE_INSURANCE,
                policyNumber = "HDFC-LIFE-882910",
                providerName = "HDFC Life",
                premiumAmount = 45000.0,
                paymentFrequency = PaymentFrequency.YEARLY,
                renewalDate = dueIn3Days,
                reminderLeadDays = 7,
                notes = "Sum Assured: 50 Lakhs"
            )
        )

        val p2 = db.policyDao().insertPolicy(
            PolicyProduct(
                clientId = c2Id,
                productType = ProductType.MUTUAL_FUND_SIP,
                policyNumber = "FOLIO-NIPPON-3391",
                providerName = "Nippon India MF",
                premiumAmount = 10000.0,
                paymentFrequency = PaymentFrequency.MONTHLY,
                renewalDate = dueIn12Days,
                reminderLeadDays = 5,
                notes = "Small Cap Fund - Monthly installment"
            )
        )

        val p3 = db.policyDao().insertPolicy(
            PolicyProduct(
                clientId = c3Id,
                productType = ProductType.VEHICLE_INSURANCE,
                policyNumber = "ICICI-MOT-992120",
                providerName = "ICICI Lombard",
                premiumAmount = 18500.0,
                paymentFrequency = PaymentFrequency.YEARLY,
                renewalDate = overdueStr,
                reminderLeadDays = 15,
                notes = "Comprehensive Commercial Vehicle Cover"
            )
        )

        // Pre-approved WhatsApp Templates
        db.templateDao().insertTemplates(
            listOf(
                MessageTemplate(
                    title = "Standard Policy Renewal",
                    category = TemplateCategory.RENEWAL,
                    content = "Dear {client_name},\n\nThis is a friendly reminder that your {product_name} (Policy No: {policy_number}) with {provider} is due for renewal on {due_date}.\n\nPremium Amount: ₹{amount}.\n\nKindly complete the renewal to maintain uninterrupted cover. Reply if you need assistance!\n\nBest regards,\nYour Insurance Advisor",
                    isApprovedWhatsAppTemplate = true,
                    metaTemplateName = "policy_renewal_reminder_v1"
                ),
                MessageTemplate(
                    title = "Birthday Greeting with Warm Wishes",
                    category = TemplateCategory.BIRTHDAY,
                    content = "Dear {client_name},\n\nWishing you a very Happy Birthday! 🎂 May this year bring you abundance, health, and peace of mind.\n\nThank you for trusting us with your financial journey.\n\nWarm regards,\nYour Financial Advisor",
                    isApprovedWhatsAppTemplate = true,
                    metaTemplateName = "birthday_wish_v2"
                ),
                MessageTemplate(
                    title = "Wedding Anniversary Greeting",
                    category = TemplateCategory.ANNIVERSARY,
                    content = "Dear {client_name},\n\nHappy Wedding Anniversary! 🎉 Wishing you and your spouse endless happiness, togetherness, and prosperity.\n\nWarmest regards,\nYour Advisory Team",
                    isApprovedWhatsAppTemplate = true,
                    metaTemplateName = "anniversary_wish_v1"
                ),
                MessageTemplate(
                    title = "SIP Top-Up Recommendation",
                    category = TemplateCategory.SIP_TOPUP,
                    content = "Hello {client_name},\n\nAs part of your annual wealth review, increasing your monthly SIP by 10% can significantly accelerate your long-term goal accumulation.\n\nLet's connect this week to evaluate your folio.\n\nRegards,\nWealth Advisory Practice",
                    isApprovedWhatsAppTemplate = false
                )
            )
        )

        // Queue Approvals
        db.approvalDao().insertApproval(
            ReminderApproval(
                clientId = c1Id,
                clientName = "Rajesh Sharma",
                clientPhone = "+91 98765 43210",
                type = ApprovalType.RENEWAL,
                messageText = "Dear Rajesh Sharma,\n\nYour HDFC Life policy (HDFC-LIFE-882910) is due for renewal on $dueIn3Days. Premium: ₹45,000.\n\nPlease confirm to send renewal payment link.",
                policyId = p1,
                dueDate = dueIn3Days,
                status = ApprovalStatus.PENDING
            )
        )

        db.approvalDao().insertApproval(
            ReminderApproval(
                clientId = c2Id,
                clientName = "Priya Ananth",
                clientPhone = "+91 98123 45678",
                type = ApprovalType.BIRTHDAY,
                messageText = "Dear Priya Ananth,\n\nWishing you a very Happy Birthday! 🎂 May your year be filled with success and health.",
                dueDate = todayStr,
                status = ApprovalStatus.PENDING
            )
        )

        db.approvalDao().insertApproval(
            ReminderApproval(
                clientId = c3Id,
                clientName = "Vikram Malhotra",
                clientPhone = "+91 99001 12233",
                type = ApprovalType.ANNIVERSARY,
                messageText = "Dear Vikram Malhotra,\n\nHappy Wedding Anniversary! 🎉 Wishing you and your family joy and prosperity.",
                dueDate = todayStr,
                status = ApprovalStatus.PENDING
            )
        )

        // Document
        db.documentDao().insertDocument(
            DocumentItem(
                clientId = c1Id,
                docType = DocType.KYC,
                title = "Aadhaar & PAN Verification Copy",
                fileUriOrNotes = "Verified KYC document on file."
            )
        )

        logAction("SYSTEM_INIT", "System", "Initialized AdviserSync database with sample practice data.")
    }

    // Backup Database to JSON String
    suspend fun exportDataToJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()

        val clients = db.clientDao().getAllClients().firstOrNull() ?: emptyList()
        val clientsArray = JSONArray()
        clients.forEach { c ->
            val obj = JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("phone", c.phone)
                put("email", c.email)
                put("dob", c.dob)
                put("anniversaryDate", c.anniversaryDate)
                put("consentStatus", c.consentStatus.name)
                put("isProspect", c.isProspect)
                put("notes", c.notes)
            }
            clientsArray.put(obj)
        }
        root.put("clients", clientsArray)

        val policies = db.policyDao().getAllPolicies().firstOrNull() ?: emptyList()
        val policiesArray = JSONArray()
        policies.forEach { p ->
            val obj = JSONObject().apply {
                put("id", p.id)
                put("clientId", p.clientId)
                put("productType", p.productType.name)
                put("policyNumber", p.policyNumber)
                put("providerName", p.providerName)
                put("premiumAmount", p.premiumAmount)
                put("paymentFrequency", p.paymentFrequency.name)
                put("renewalDate", p.renewalDate)
                put("reminderLeadDays", p.reminderLeadDays)
                put("notes", p.notes)
                put("isPaid", p.isPaid)
            }
            policiesArray.put(obj)
        }
        root.put("policies", policiesArray)

        val groups = db.groupDao().getAllGroups().firstOrNull() ?: emptyList()
        val groupsArray = JSONArray()
        groups.forEach { g ->
            val obj = JSONObject().apply {
                put("id", g.id)
                put("name", g.name)
                put("description", g.description)
                put("colorHex", g.colorHex)
            }
            groupsArray.put(obj)
        }
        root.put("groups", groupsArray)

        root.toString(2)
    }

    // Restore Database from JSON String
    suspend fun restoreDataFromJson(jsonStr: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonStr)

            if (root.has("clients")) {
                val array = root.getJSONArray("clients")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val client = Client(
                        name = obj.getString("name"),
                        phone = obj.getString("phone"),
                        email = obj.optString("email", ""),
                        dob = obj.optString("dob", ""),
                        anniversaryDate = obj.optString("anniversaryDate", ""),
                        consentStatus = try { ConsentStatus.valueOf(obj.optString("consentStatus", "CONSENTED")) } catch (e: Exception) { ConsentStatus.CONSENTED },
                        isProspect = obj.optBoolean("isProspect", false),
                        notes = obj.optString("notes", "")
                    )
                    db.clientDao().insertClient(client)
                }
            }

            if (root.has("policies")) {
                val array = root.getJSONArray("policies")
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val policy = PolicyProduct(
                        clientId = obj.getLong("clientId"),
                        productType = try { ProductType.valueOf(obj.getString("productType")) } catch (e: Exception) { ProductType.LIFE_INSURANCE },
                        policyNumber = obj.getString("policyNumber"),
                        providerName = obj.getString("providerName"),
                        premiumAmount = obj.getDouble("premiumAmount"),
                        paymentFrequency = try { PaymentFrequency.valueOf(obj.getString("paymentFrequency")) } catch (e: Exception) { PaymentFrequency.YEARLY },
                        renewalDate = obj.getString("renewalDate"),
                        reminderLeadDays = obj.optInt("reminderLeadDays", 7),
                        notes = obj.optString("notes", ""),
                        isPaid = obj.optBoolean("isPaid", false)
                    )
                    db.policyDao().insertPolicy(policy)
                }
            }

            logAction("BACKUP_RESTORE", "System", "Successfully restored database from JSON backup.")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Export Clients & Policies to CSV format
    suspend fun exportToCsv(): String = withContext(Dispatchers.IO) {
        val clients = db.clientDao().getAllClients().firstOrNull() ?: emptyList()
        val policies = db.policyDao().getAllPolicies().firstOrNull() ?: emptyList()
        val policyMap = policies.groupBy { it.clientId }

        val sb = StringBuilder()
        sb.append("Client ID,Client Name,Phone,Email,DOB,Anniversary,Consent,Product Type,Policy No,Provider,Premium,Renewal Date\n")

        clients.forEach { c ->
            val clientPolicies = policyMap[c.id] ?: emptyList()
            if (clientPolicies.isEmpty()) {
                sb.append("${c.id},\"${c.name}\",\"${c.phone}\",\"${c.email}\",${c.dob},${c.anniversaryDate},${c.consentStatus.name},,,,\n")
            } else {
                clientPolicies.forEach { p ->
                    sb.append("${c.id},\"${c.name}\",\"${c.phone}\",\"${c.email}\",${c.dob},${c.anniversaryDate},${c.consentStatus.name},\"${p.productType.displayName}\",\"${p.policyNumber}\",\"${p.providerName}\",${p.premiumAmount},${p.renewalDate}\n")
                }
            }
        }
        sb.toString()
    }
}
