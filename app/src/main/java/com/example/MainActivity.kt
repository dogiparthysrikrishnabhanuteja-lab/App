package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.ui.AdviserViewModel
import com.example.ui.components.PasscodeLockOverlay
import com.example.ui.screens.*
import com.example.ui.theme.AdviserSyncTheme

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Dashboard : Screen("dashboard", "Home", Icons.Default.Dashboard)
    object Clients : Screen("clients", "Clients", Icons.Default.People)
    object Policies : Screen("policies", "Policies", Icons.Default.Description)
    object Calendar : Screen("calendar", "Calendar", Icons.Default.CalendarMonth)
    object Approvals : Screen("approvals", "Review", Icons.Default.PendingActions)
    object Groups : Screen("groups", "Groups", Icons.Default.Groups)
    object Templates : Screen("templates", "Studio", Icons.Default.AutoAwesome)
    object Backup : Screen("backup", "Cloud", Icons.Default.CloudSync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: AdviserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AdviserSyncTheme {
                PasscodeLockOverlay(viewModel = viewModel) {
                    MainAppLayout(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppLayout(viewModel: AdviserViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val pendingApprovals by viewModel.pendingApprovals.collectAsState()

    val navItems = listOf(
        Screen.Dashboard,
        Screen.Clients,
        Screen.Policies,
        Screen.Calendar,
        Screen.Approvals,
        Screen.Groups,
        Screen.Backup
    )

    Scaffold(
        bottomBar = {
            // Only show bottom navigation on top-level screens
            if (currentRoute in navItems.map { it.route }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    navItems.forEach { screen ->
                        val isSelected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = isSelected,
                            modifier = Modifier.testTag("nav_item_${screen.route}"),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (screen == Screen.Approvals && pendingApprovals.isNotEmpty()) {
                                    BadgedBox(
                                        badge = { Badge { Text(pendingApprovals.size.toString()) } }
                                    ) {
                                        Icon(screen.icon, contentDescription = screen.title)
                                    }
                                } else {
                                    Icon(screen.icon, contentDescription = screen.title)
                                }
                            },
                            label = {
                                Text(
                                    screen.title,
                                    fontSize = 11.sp,
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) },
                    onNavigateToClients = { navController.navigate(Screen.Clients.route) },
                    onNavigateToPolicies = { navController.navigate(Screen.Policies.route) },
                    onNavigateToTemplates = { navController.navigate(Screen.Templates.route) },
                    onNavigateToClientDetail = { clientId -> navController.navigate("client_detail/$clientId") }
                )
            }

            composable(Screen.Clients.route) {
                ClientsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { clientId -> navController.navigate("client_detail/$clientId") }
                )
            }

            composable(
                route = "client_detail/{clientId}",
                arguments = listOf(navArgument("clientId") { type = NavType.LongType })
            ) { backStackEntry ->
                val clientId = backStackEntry.arguments?.getLong("clientId") ?: 0L
                ClientDetailScreen(
                    clientId = clientId,
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) }
                )
            }

            composable(Screen.Policies.route) {
                PoliciesScreen(
                    viewModel = viewModel,
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) },
                    onNavigateToClientDetail = { clientId -> navController.navigate("client_detail/$clientId") }
                )
            }

            composable(Screen.Calendar.route) {
                CalendarScreen(
                    viewModel = viewModel,
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) },
                    onNavigateToClientDetail = { clientId -> navController.navigate("client_detail/$clientId") }
                )
            }

            composable(Screen.Approvals.route) {
                QueueApprovalsScreen(viewModel = viewModel)
            }

            composable(Screen.Groups.route) {
                GroupsScreen(
                    viewModel = viewModel,
                    onNavigateToApprovals = { navController.navigate(Screen.Approvals.route) }
                )
            }

            composable(Screen.Templates.route) {
                TemplatesScreen(viewModel = viewModel)
            }

            composable(Screen.Backup.route) {
                BackupAuditScreen(viewModel = viewModel)
            }
        }
    }
}
