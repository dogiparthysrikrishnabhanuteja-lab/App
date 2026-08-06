package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApprovalStatus
import com.example.data.model.ApprovalType
import com.example.data.model.PolicyProduct
import com.example.data.model.ReminderApproval
import com.example.ui.AdviserViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class PolicyFilter(val label: String) {
    ALL("All"),
    DUE_TODAY("Due Today"),
    DUE_7_DAYS("Due in 7 Days"),
    DUE_30_DAYS("Due in 30 Days"),
    OVERDUE("Overdue"),
    PAID("Paid")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoliciesScreen(
    viewModel: AdviserViewModel,
    onNavigateToApprovals: () -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    val policies by viewModel.policies.collectAsState()
    val clients by viewModel.clients.collectAsState()

    var activeFilter by remember { mutableStateOf(PolicyFilter.ALL) }
    var showAddPolicyDialog by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val filteredPolicies = remember(policies, activeFilter, todayStr) {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val todayDate = try { sdf.parse(todayStr) } catch (e: Exception) { Date() }

        policies.filter { p ->
            when (activeFilter) {
                PolicyFilter.ALL -> true
                PolicyFilter.DUE_TODAY -> p.renewalDate == todayStr && !p.isPaid
                PolicyFilter.OVERDUE -> {
                    val pDate = try { sdf.parse(p.renewalDate) } catch (e: Exception) { null }
                    pDate != null && pDate.before(todayDate) && !p.isPaid
                }
                PolicyFilter.DUE_7_DAYS -> {
                    val pDate = try { sdf.parse(p.renewalDate) } catch (e: Exception) { null }
                    if (pDate != null && !p.isPaid) {
                        val diffDays = (pDate.time - todayDate.time) / (1000 * 60 * 60 * 24)
                        diffDays in 0..7
                    } else false
                }
                PolicyFilter.DUE_30_DAYS -> {
                    val pDate = try { sdf.parse(p.renewalDate) } catch (e: Exception) { null }
                    if (pDate != null && !p.isPaid) {
                        val diffDays = (pDate.time - todayDate.time) / (1000 * 60 * 60 * 24)
                        diffDays in 0..30
                    } else false
                }
                PolicyFilter.PAID -> p.isPaid
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Policies & Renewals (${filteredPolicies.size})", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToApprovals, modifier = Modifier.testTag("btn_policies_approvals")) {
                        Icon(Icons.Default.PendingActions, contentDescription = "Approval Queue")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddPolicyDialog = true },
                modifier = Modifier.testTag("fab_add_policy"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Policy")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Filter Tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PolicyFilter.values()) { filter ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { activeFilter = filter },
                        label = { Text(filter.label) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredPolicies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No policies found for selected filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredPolicies, key = { it.id }) { policy ->
                        val client = remember(policy, clients) { clients.find { it.id == policy.clientId } }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("policy_card_${policy.id}")
                                .clickable { onNavigateToClientDetail(policy.clientId) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        policy.productType.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        currencyFormat.format(policy.premiumAmount),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    "Client: ${client?.name ?: "Unknown"}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${policy.providerName} • Policy No: ${policy.policyNumber}",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (policy.isPaid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer
                                    ) {
                                        Text(
                                            if (policy.isPaid) "PAID" else "Due: ${policy.renewalDate} (Lead: ${policy.reminderLeadDays}d)",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (policy.isPaid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (!policy.isPaid) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.queuePolicyReminder(policy, client) {
                                                        onNavigateToApprovals()
                                                    }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Queue Reminder", fontSize = 11.sp)
                                            }

                                            Button(
                                                onClick = { viewModel.markPolicyPaid(policy) },
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text("Mark Paid", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddPolicyDialog && clients.isNotEmpty()) {
        val firstClient = clients.first()
        AddPolicyModal(
            clientId = firstClient.id,
            clientName = firstClient.name,
            onDismiss = { showAddPolicyDialog = false },
            onConfirm = { pType, pNo, provider, amount, freq, date, lead, notes ->
                viewModel.addPolicy(firstClient.id, firstClient.name, pType, pNo, provider, amount, freq, date, lead, notes)
                showAddPolicyDialog = false
            }
        )
    }
}
