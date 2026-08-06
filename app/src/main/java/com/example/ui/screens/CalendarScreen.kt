package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ApprovalType
import com.example.data.model.Client
import com.example.data.model.PolicyProduct
import com.example.service.CloudSyncState
import com.example.ui.AdviserViewModel
import com.example.ui.components.WishComposerModal
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

enum class CalendarViewMode {
    CALENDAR, LIST
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: AdviserViewModel,
    onNavigateToApprovals: () -> Unit = {},
    onNavigateToClientDetail: (Long) -> Unit = {}
) {
    val context = LocalContext.current
    val policies by viewModel.policies.collectAsState()
    val clients by viewModel.clients.collectAsState()
    val cloudSyncState by viewModel.cloudSyncState.collectAsState()

    var viewMode by remember { mutableStateOf(CalendarViewMode.CALENDAR) }
    var selectedCalendarYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var selectedCalendarMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) } // 0-indexed

    val todaySdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayDateStr = remember { todaySdf.format(Date()) }
    var selectedDateStr by remember { mutableStateOf(todayDateStr) }

    var activeWishClient by remember { mutableStateOf<Pair<Client, ApprovalType>?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf("ALL") } // ALL, RENEWALS, BIRTHDAYS, ANNIVERSARIES
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, DUE, OVERDUE, PAID

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }

    // Map policies by date "yyyy-MM-dd"
    val policiesByDate = remember(policies) {
        policies.groupBy { it.renewalDate }
    }

    // Map birthdays by "MM-dd"
    val birthdaysByMmDd = remember(clients) {
        clients.filter { it.dob.isNotBlank() && it.dob.length >= 5 }
            .groupBy { it.dob.takeLast(5) }
    }

    // Map anniversaries by "MM-dd"
    val anniversariesByMmDd = remember(clients) {
        clients.filter { it.anniversaryDate.isNotBlank() && it.anniversaryDate.length >= 5 }
            .groupBy { it.anniversaryDate.takeLast(5) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Renewal Calendar", fontWeight = FontWeight.Bold) },
                actions = {
                    // Cloud Sync Indicator & Button
                    IconButton(
                        onClick = { viewModel.triggerCloudSync() },
                        modifier = Modifier.testTag("btn_calendar_sync_cloud")
                    ) {
                        when (cloudSyncState) {
                            is CloudSyncState.Syncing -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            is CloudSyncState.Success -> Icon(Icons.Default.CloudDone, contentDescription = "Cloud Synced", tint = Color(0xFF4CAF50))
                            else -> Icon(Icons.Default.CloudUpload, contentDescription = "Sync to Cloud", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // View Mode Switcher Toggle Button
                    Row(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewMode = CalendarViewMode.CALENDAR },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_mode_calendar")
                                .background(
                                    if (viewMode == CalendarViewMode.CALENDAR) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Calendar View",
                                modifier = Modifier.size(18.dp),
                                tint = if (viewMode == CalendarViewMode.CALENDAR) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { viewMode = CalendarViewMode.LIST },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("btn_mode_list")
                                .background(
                                    if (viewMode == CalendarViewMode.LIST) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Default.FormatListNumbered,
                                contentDescription = "List View",
                                modifier = Modifier.size(18.dp),
                                tint = if (viewMode == CalendarViewMode.LIST) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            // VIEW MODE CONTENT
            AnimatedContent(
                targetState = viewMode,
                label = "CalendarViewTransition"
            ) { mode ->
                when (mode) {
                    CalendarViewMode.CALENDAR -> {
                        CalendarViewContent(
                            year = selectedCalendarYear,
                            month = selectedCalendarMonth,
                            selectedDateStr = selectedDateStr,
                            todayDateStr = todayDateStr,
                            policiesByDate = policiesByDate,
                            birthdaysByMmDd = birthdaysByMmDd,
                            anniversariesByMmDd = anniversariesByMmDd,
                            clients = clients,
                            currencyFormat = currencyFormat,
                            selectedCategoryFilter = selectedCategoryFilter,
                            onCategoryFilterSelected = { selectedCategoryFilter = it },
                            onMonthChange = { newYear, newMonth ->
                                selectedCalendarYear = newYear
                                selectedCalendarMonth = newMonth
                            },
                            onDateSelected = { dateStr ->
                                selectedDateStr = dateStr
                            },
                            onSendWish = { client, type ->
                                activeWishClient = Pair(client, type)
                            },
                            onTogglePaid = { policy ->
                                viewModel.togglePolicyPaidStatus(policy)
                            },
                            onNavigateToClientDetail = onNavigateToClientDetail
                        )
                    }
                    CalendarViewMode.LIST -> {
                        RenewalListViewContent(
                            policies = policies,
                            clients = clients,
                            selectedCategoryFilter = selectedCategoryFilter,
                            selectedStatusFilter = selectedStatusFilter,
                            currencyFormat = currencyFormat,
                            todayDateStr = todayDateStr,
                            onCategoryFilterSelected = { selectedCategoryFilter = it },
                            onStatusFilterSelected = { selectedStatusFilter = it },
                            onSendWish = { client, type ->
                                activeWishClient = Pair(client, type)
                            },
                            onTogglePaid = { policy ->
                                viewModel.togglePolicyPaidStatus(policy)
                            },
                            onNavigateToClientDetail = onNavigateToClientDetail
                        )
                    }
                }
            }
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
fun CalendarViewContent(
    year: Int,
    month: Int,
    selectedDateStr: String,
    todayDateStr: String,
    policiesByDate: Map<String, List<PolicyProduct>>,
    birthdaysByMmDd: Map<String, List<Client>>,
    anniversariesByMmDd: Map<String, List<Client>>,
    clients: List<Client>,
    currencyFormat: NumberFormat,
    selectedCategoryFilter: String,
    onCategoryFilterSelected: (String) -> Unit,
    onMonthChange: (year: Int, month: Int) -> Unit,
    onDateSelected: (String) -> Unit,
    onSendWish: (Client, ApprovalType) -> Unit,
    onTogglePaid: (PolicyProduct) -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    val monthCalendar = remember(year, month) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val monthName = remember(monthCalendar) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCalendar.time)
    }

    val daysInMonth = monthCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = monthCalendar.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 7 = Saturday
    val paddingDays = firstDayOfWeek - 1

    Column(modifier = Modifier.fillMaxSize()) {
        // Event Category Filter Bar (Birthdays, Renewals, Anniversaries, All)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val categories = listOf(
                "ALL" to "All Events",
                "RENEWALS" to "Policy Renewals 📄",
                "BIRTHDAYS" to "Birthdays 🎂",
                "ANNIVERSARIES" to "Anniversaries 💍"
            )
            items(categories) { (key, label) ->
                FilterChip(
                    selected = selectedCategoryFilter == key,
                    onClick = { onCategoryFilterSelected(key) },
                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("filter_cat_$key")
                )
            }
        }

        // Month Navigation Bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            add(Calendar.MONTH, -1)
                        }
                        onMonthChange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    },
                    modifier = Modifier.testTag("btn_prev_month")
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        monthName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val now = Calendar.getInstance()
                            onMonthChange(now.get(Calendar.YEAR), now.get(Calendar.MONTH))
                            onDateSelected(todayDateStr)
                        },
                        modifier = Modifier.testTag("btn_today")
                    ) {
                        Text("Today", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = {
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.YEAR, year)
                            set(Calendar.MONTH, month)
                            add(Calendar.MONTH, 1)
                        }
                        onMonthChange(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                    },
                    modifier = Modifier.testTag("btn_next_month")
                ) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
                }
            }
        }

        // Days of week header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT").forEach { day ->
                Text(
                    day,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Days Grid
        val totalCells = paddingDays + daysInMonth
        val gridRows = (totalCells + 6) / 7

        Column(modifier = Modifier.fillMaxWidth()) {
            for (row in 0 until gridRows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    for (col in 0..6) {
                        val cellIndex = row * 7 + col
                        val dayNum = cellIndex - paddingDays + 1

                        if (cellIndex in paddingDays until (paddingDays + daysInMonth)) {
                            val formattedDay = String.format(Locale.US, "%02d", dayNum)
                            val formattedMonth = String.format(Locale.US, "%02d", month + 1)
                            val dateStr = "$year-$formattedMonth-$formattedDay"
                            val mmDd = "$formattedMonth-$formattedDay"

                            val datePolicies = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "RENEWALS") policiesByDate[dateStr] ?: emptyList() else emptyList()
                            val dateBirthdays = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "BIRTHDAYS") birthdaysByMmDd[mmDd] ?: emptyList() else emptyList()
                            val dateAnniversaries = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "ANNIVERSARIES") anniversariesByMmDd[mmDd] ?: emptyList() else emptyList()

                            val hasEvents = datePolicies.isNotEmpty() || dateBirthdays.isNotEmpty() || dateAnniversaries.isNotEmpty()
                            val isToday = dateStr == todayDateStr
                            val isSelected = dateStr == selectedDateStr

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            isToday -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                                            hasEvents -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .border(
                                        width = if (isSelected) 2.dp else if (isToday) 1.dp else 0.5.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else if (isToday) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { onDateSelected(dateStr) }
                                    .testTag("cell_date_$dateStr"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        dayNum.toString(),
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(1.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 1.dp)
                                    ) {
                                        if (dateBirthdays.isNotEmpty()) {
                                            Text("🎂", fontSize = 8.sp)
                                        }
                                        if (dateAnniversaries.isNotEmpty()) {
                                            Text("💍", fontSize = 8.sp)
                                        }
                                        if (datePolicies.isNotEmpty()) {
                                            val hasUnpaid = datePolicies.any { !it.isPaid }
                                            val hasOverdue = datePolicies.any { !it.isPaid && dateStr < todayDateStr }
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        when {
                                                            hasOverdue -> MaterialTheme.colorScheme.error
                                                            hasUnpaid -> Color(0xFFFF9800)
                                                            else -> Color(0xFF4CAF50)
                                                        }
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Date Details Section
        val selectedMmDd = selectedDateStr.takeLast(5)
        val selectedPolicies = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "RENEWALS") policiesByDate[selectedDateStr] ?: emptyList() else emptyList()
        val selectedBirthdays = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "BIRTHDAYS") birthdaysByMmDd[selectedMmDd] ?: emptyList() else emptyList()
        val selectedAnniversaries = if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "ANNIVERSARIES") anniversariesByMmDd[selectedMmDd] ?: emptyList() else emptyList()

        val totalSelectedEvents = selectedPolicies.size + selectedBirthdays.size + selectedAnniversaries.size

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Events for $selectedDateStr",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            "$totalSelectedEvents Scheduled",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (totalSelectedEvents == 0) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.EventAvailable,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No birthdays, anniversaries, or renewals on this date.", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 1. Birthdays Section
                        items(selectedBirthdays) { client ->
                            CalendarBirthdayItemCard(
                                client = client,
                                onSendTeluguWish = { onSendWish(client, ApprovalType.BIRTHDAY) },
                                onSendEnglishWish = { onSendWish(client, ApprovalType.BIRTHDAY) },
                                onNavigateToClientDetail = onNavigateToClientDetail
                            )
                        }

                        // 2. Anniversaries Section
                        items(selectedAnniversaries) { client ->
                            CalendarAnniversaryItemCard(
                                client = client,
                                onSendTeluguWish = { onSendWish(client, ApprovalType.ANNIVERSARY) },
                                onSendEnglishWish = { onSendWish(client, ApprovalType.ANNIVERSARY) },
                                onNavigateToClientDetail = onNavigateToClientDetail
                            )
                        }

                        // 3. Renewals Section
                        items(selectedPolicies) { policy ->
                            val client = clients.find { it.id == policy.clientId }
                            CalendarPolicyItemCard(
                                policy = policy,
                                client = client,
                                currencyFormat = currencyFormat,
                                onSendWish = { if (client != null) onSendWish(client, ApprovalType.RENEWAL) },
                                onTogglePaid = { onTogglePaid(policy) },
                                onNavigateToClientDetail = onNavigateToClientDetail
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RenewalListViewContent(
    policies: List<PolicyProduct>,
    clients: List<Client>,
    selectedCategoryFilter: String,
    selectedStatusFilter: String,
    currencyFormat: NumberFormat,
    todayDateStr: String,
    onCategoryFilterSelected: (String) -> Unit,
    onStatusFilterSelected: (String) -> Unit,
    onSendWish: (Client, ApprovalType) -> Unit,
    onTogglePaid: (PolicyProduct) -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    val filteredPolicies = remember(policies, selectedStatusFilter, todayDateStr) {
        val sorted = policies.sortedBy { it.renewalDate }
        when (selectedStatusFilter) {
            "DUE" -> sorted.filter { !it.isPaid && it.renewalDate >= todayDateStr }
            "OVERDUE" -> sorted.filter { !it.isPaid && it.renewalDate < todayDateStr }
            "PAID" -> sorted.filter { it.isPaid }
            else -> sorted
        }
    }

    val birthdayClients = remember(clients) {
        clients.filter { it.dob.isNotBlank() }
    }

    val anniversaryClients = remember(clients) {
        clients.filter { it.anniversaryDate.isNotBlank() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Category Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            val categories = listOf(
                "ALL" to "All Events",
                "RENEWALS" to "Policy Renewals 📄",
                "BIRTHDAYS" to "Birthdays 🎂",
                "ANNIVERSARIES" to "Anniversaries 💍"
            )
            items(categories) { (key, label) ->
                FilterChip(
                    selected = selectedCategoryFilter == key,
                    onClick = { onCategoryFilterSelected(key) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("list_cat_$key")
                )
            }
        }

        if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "RENEWALS") {
            // Status Filter Chips Row for Policies
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                val filters = listOf(
                    "ALL" to "All Renewals (${policies.size})",
                    "DUE" to "Upcoming Due",
                    "OVERDUE" to "Overdue ⚠️",
                    "PAID" to "Paid ✅"
                )
                items(filters) { (key, label) ->
                    FilterChip(
                        selected = selectedStatusFilter == key,
                        onClick = { onStatusFilterSelected(key) },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_renewal_$key")
                    )
                }
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp)
        ) {
            if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "BIRTHDAYS") {
                if (birthdayClients.isNotEmpty()) {
                    item {
                        Text("Client Birthdays 🎂", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.tertiary)
                    }
                    items(birthdayClients) { client ->
                        CalendarBirthdayItemCard(
                            client = client,
                            onSendTeluguWish = { onSendWish(client, ApprovalType.BIRTHDAY) },
                            onSendEnglishWish = { onSendWish(client, ApprovalType.BIRTHDAY) },
                            onNavigateToClientDetail = onNavigateToClientDetail
                        )
                    }
                }
            }

            if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "ANNIVERSARIES") {
                if (anniversaryClients.isNotEmpty()) {
                    item {
                        Text("Client Anniversaries 💍", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                    }
                    items(anniversaryClients) { client ->
                        CalendarAnniversaryItemCard(
                            client = client,
                            onSendTeluguWish = { onSendWish(client, ApprovalType.ANNIVERSARY) },
                            onSendEnglishWish = { onSendWish(client, ApprovalType.ANNIVERSARY) },
                            onNavigateToClientDetail = onNavigateToClientDetail
                        )
                    }
                }
            }

            if (selectedCategoryFilter == "ALL" || selectedCategoryFilter == "RENEWALS") {
                item {
                    Text("Policy Renewals 📄", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                }
                items(filteredPolicies) { policy ->
                    val client = clients.find { it.id == policy.clientId }
                    CalendarPolicyItemCard(
                        policy = policy,
                        client = client,
                        currencyFormat = currencyFormat,
                        onSendWish = { if (client != null) onSendWish(client, ApprovalType.RENEWAL) },
                        onTogglePaid = { onTogglePaid(policy) },
                        onNavigateToClientDetail = onNavigateToClientDetail
                    )
                }
            }
        }
    }
}

@Composable
fun CalendarBirthdayItemCard(
    client: Client,
    onSendTeluguWish: () -> Unit,
    onSendEnglishWish: () -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎂 Birthday Event", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DOB: ${client.dob}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        client.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToClientDetail(client.id) }
                    )
                    Text("Phone: ${client.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onSendTeluguWish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("తెలుగు 🎂", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onSendEnglishWish,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("English", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarAnniversaryItemCard(
    client: Client,
    onSendTeluguWish: () -> Unit,
    onSendEnglishWish: () -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💍 Anniversary Event", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Date: ${client.anniversaryDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        client.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onNavigateToClientDetail(client.id) }
                    )
                    Text("Phone: ${client.phone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Button(
                        onClick = onSendTeluguWish,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("తెలుగు 💍", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onSendEnglishWish,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("English", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarPolicyItemCard(
    policy: PolicyProduct,
    client: Client?,
    currencyFormat: NumberFormat,
    onSendWish: () -> Unit,
    onTogglePaid: () -> Unit,
    onNavigateToClientDetail: (Long) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        policy.productType.displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        "No: ${policy.policyNumber} | ${policy.providerName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (client != null) {
                        Text(
                            "Client: ${client.name} (${client.phone})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clickable { onNavigateToClientDetail(client.id) }
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        currencyFormat.format(policy.premiumAmount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Due: ${policy.renewalDate}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (policy.isPaid) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (policy.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Text(
                        if (policy.isPaid) "STATUS: PAID ✅" else "STATUS: UNPAID / DUE ⚠️",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (policy.isPaid) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(
                        onClick = onSendWish,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("btn_send_wish_policy_${policy.id}")
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Send Reminder Wish",
                            tint = Color(0xFF25D366),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onTogglePaid,
                        modifier = Modifier.testTag("btn_toggle_paid_${policy.id}"),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Text(
                            if (policy.isPaid) "Mark Unpaid" else "Mark Paid",
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
