package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApprovalStatus
import com.example.data.model.ApprovalType
import com.example.data.model.Client
import com.example.data.model.PolicyProduct
import com.example.data.model.ReminderApproval
import com.example.ui.AdviserViewModel
import com.example.ui.components.WishComposerModal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AdviserViewModel,
    onNavigateToApprovals: () -> Unit,
    onNavigateToClients: () -> Unit,
    onNavigateToPolicies: () -> Unit,
    onNavigateToTemplates: () -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    val clients by viewModel.clients.collectAsState()
    val policies by viewModel.policies.collectAsState()
    val pendingApprovals by viewModel.pendingApprovals.collectAsState()

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val totalPremium = remember(policies) { policies.sumOf { it.premiumAmount } }
    val dueTodayCount = remember(policies) { policies.count { it.renewalDate == todayStr && !it.isPaid } }
    val todaysWishes = remember(clients) {
        clients.filter { it.dob == todayStr || it.anniversaryDate == todayStr }
    }
    val upcomingPolicies = remember(policies) {
        policies.filter { !it.isPaid }.take(5)
    }

    var activeWishClient by remember { mutableStateOf<Pair<Client, ApprovalType>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "AdviserSync CRM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Insurance & Mutual Funds Advisory",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    BadgedBox(
                        badge = {
                            if (pendingApprovals.isNotEmpty()) {
                                Badge { Text(pendingApprovals.size.toString()) }
                            }
                        },
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("nav_approvals_badge")
                            .clickable { onNavigateToApprovals() }
                    ) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Pending Approvals Queue",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Pending Approvals Banner (Mandatory Review Step Alert)
            if (pendingApprovals.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_pending_approvals_alert")
                            .clickable { onNavigateToApprovals() },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.PendingActions,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${pendingApprovals.size} Pending Actions in Approval Queue",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Review & edit renewal reminders & birthday wishes before sending via WhatsApp.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
            }

            // Practice Executive Metrics Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricTile(
                        title = "Active Clients",
                        value = clients.size.toString(),
                        subtitle = "${clients.count { it.isProspect }} Prospects",
                        icon = Icons.Default.People,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_active_clients")
                            .clickable { onNavigateToClients() }
                    )
                    MetricTile(
                        title = "Total Premium",
                        value = currencyFormat.format(totalPremium),
                        subtitle = "${policies.size} Total Policies",
                        icon = Icons.Default.AccountBalanceWallet,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("metric_total_premium")
                            .clickable { onNavigateToPolicies() }
                    )
                }
            }

            // Quick Actions Chips
            item {
                Text(
                    "Quick Practice Tools",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        ActionChip(
                            label = "Add Client",
                            icon = Icons.Default.PersonAdd,
                            tag = "action_add_client",
                            onClick = onNavigateToClients
                        )
                    }
                    item {
                        ActionChip(
                            label = "Approval Queue",
                            icon = Icons.Default.CheckCircle,
                            tag = "action_approval_queue",
                            onClick = onNavigateToApprovals
                        )
                    }
                    item {
                        ActionChip(
                            label = "All Policies",
                            icon = Icons.Default.Description,
                            tag = "action_all_policies",
                            onClick = onNavigateToPolicies
                        )
                    }
                    item {
                        ActionChip(
                            label = "AI Composer",
                            icon = Icons.Default.AutoAwesome,
                            tag = "action_ai_composer",
                            onClick = onNavigateToTemplates
                        )
                    }
                }
            }

            // Today's Wishes Section (Birthdays & Anniversaries)
            if (todaysWishes.isNotEmpty()) {
                item {
                    Text(
                        "Today's Celebrations 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(todaysWishes) { client ->
                    val isBirthday = client.dob == todayStr
                    val celebrationType = if (isBirthday) "Birthday 🎂" else "Wedding Anniversary 💍"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_celebration_${client.id}"),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (isBirthday) Icons.Default.Cake else Icons.Default.Favorite,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    client.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    celebrationType,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Button(
                                onClick = {
                                    activeWishClient = Pair(client, if (isBirthday) ApprovalType.BIRTHDAY else ApprovalType.ANNIVERSARY)
                                },
                                modifier = Modifier.testTag("btn_wish_ai_${client.id}"),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Send Wish", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Upcoming Renewals Overview
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Upcoming Renewals & Due Dates",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToPolicies) {
                        Text("View All", fontSize = 13.sp)
                    }
                }
            }

            if (upcomingPolicies.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No pending policy renewals right now.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(upcomingPolicies) { policy ->
                    PolicyRenewalCard(
                        policy = policy,
                        clients = clients,
                        currencyFormat = currencyFormat,
                        onClientClick = { onNavigateToClientDetail(policy.clientId) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        if (activeWishClient != null) {
            val (client, occasionType) = activeWishClient!!
            WishComposerModal(
                client = client,
                occasionType = occasionType,
                viewModel = viewModel,
                onDismiss = { activeWishClient = null },
                onWishQueuedOrSent = { activeWishClient = null }
            )
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                title,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                fontSize = 11.sp,
                color = color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .testTag(tag)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun PolicyRenewalCard(
    policy: PolicyProduct,
    clients: List<Client>,
    currencyFormat: NumberFormat,
    onClientClick: () -> Unit
) {
    val client = remember(policy, clients) { clients.find { it.id == policy.clientId } }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClientClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    policy.productType.displayName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Client: ${client?.name ?: "Unknown"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${policy.providerName} • No: ${policy.policyNumber}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    currencyFormat.format(policy.premiumAmount),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        "Due: ${policy.renewalDate}",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
