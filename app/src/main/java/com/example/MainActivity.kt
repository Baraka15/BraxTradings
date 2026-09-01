package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.TradingViewModel
import com.example.ui.components.TriggeredAlertBanner
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.PortfolioScreen
import com.example.ui.theme.BraxTradingsTheme
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BullishGreen
import kotlinx.coroutines.flow.collectLatest

enum class MainNavigationTab(val title: String) {
    MARKETS("Markets"),
    ALERTS("Price Alerts"),
    PORTFOLIO("Portfolio")
}

class MainActivity : ComponentActivity() {
    private val viewModel: TradingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BraxTradingsTheme {
                val context = LocalContext.current
                var currentTab by remember { mutableStateOf(MainNavigationTab.MARKETS) }
                val snackbarHostState = remember { SnackbarHostState() }

                var hasNotificationPermission by remember {
                    mutableStateOf(
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                    )
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                }

                // Handle intent extras if opened from notification click
                LaunchedEffect(intent) {
                    val alertSymbol = intent?.getStringExtra("EXTRA_SYMBOL")
                    if (alertSymbol != null) {
                        viewModel.selectSymbol(alertSymbol)
                        currentTab = MainNavigationTab.ALERTS
                    }
                }

                // Collect toast / snackbar messages
                LaunchedEffect(Unit) {
                    viewModel.toastMessage.collectLatest { message ->
                        snackbarHostState.showSnackbar(message, duration = SnackbarDuration.Short)
                    }
                }

                val triggeredAlertBanner by viewModel.triggeredAlertBanner.collectAsState()
                val alerts by viewModel.alerts.collectAsState()
                val activeAlertsCount = alerts.count { it.isEnabled && !it.isTriggered }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        NavigationBar(
                            containerColor = DarkSurfaceElevated,
                            contentColor = Color.White
                        ) {
                            NavigationBarItem(
                                selected = currentTab == MainNavigationTab.MARKETS,
                                onClick = { currentTab = MainNavigationTab.MARKETS },
                                icon = { Icon(Icons.Default.ShowChart, contentDescription = "Markets") },
                                label = { Text("Markets", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = DarkBackground
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == MainNavigationTab.ALERTS,
                                onClick = { currentTab = MainNavigationTab.ALERTS },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (activeAlertsCount > 0) {
                                                Badge(containerColor = BullishGreen) {
                                                    Text("$activeAlertsCount", color = DarkBackground)
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = "Price Alerts")
                                    }
                                },
                                label = { Text("Alerts", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = DarkBackground
                                )
                            )

                            NavigationBarItem(
                                selected = currentTab == MainNavigationTab.PORTFOLIO,
                                onClick = { currentTab = MainNavigationTab.PORTFOLIO },
                                icon = { Icon(Icons.Default.Analytics, contentDescription = "Portfolio") },
                                label = { Text("Portfolio", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AccentCyan,
                                    selectedTextColor = AccentCyan,
                                    indicatorColor = DarkBackground
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground)
                            .padding(innerPadding)
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // In-App Triggered Notification Banner
                            TriggeredAlertBanner(
                                alert = triggeredAlertBanner,
                                onDismiss = { viewModel.dismissBanner() },
                                onViewAlerts = {
                                    viewModel.dismissBanner()
                                    currentTab = MainNavigationTab.ALERTS
                                }
                            )

                            when (currentTab) {
                                MainNavigationTab.MARKETS -> DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToAlerts = { currentTab = MainNavigationTab.ALERTS }
                                )
                                MainNavigationTab.ALERTS -> AlertsScreen(
                                    viewModel = viewModel,
                                    hasNotificationPermission = hasNotificationPermission,
                                    onRequestNotificationPermission = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                )
                                MainNavigationTab.PORTFOLIO -> PortfolioScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}
