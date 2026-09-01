package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TradingViewModel
import com.example.ui.UiMessage
import com.example.ui.screens.*
import com.example.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

enum class ScreenNav(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    TRADINGVIEW("TradingView", Icons.Filled.ShowChart, Icons.Outlined.ShowChart),
    TERMINAL("Terminal", Icons.Filled.TrendingUp, Icons.Outlined.TrendingUp),
    SCANNER("Market", Icons.Filled.Explore, Icons.Outlined.Explore),
    PORTFOLIO("Wallet", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
    ALERTS("Alerts", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    SETTINGS("System", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BraxTradingsTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: TradingViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(ScreenNav.DASHBOARD) }
    val snackbarHostState = remember { SnackbarHostState() }

    val isStreaming by viewModel.marketEngine.isStreaming.collectAsState()
    val summary by viewModel.portfolioSummary.collectAsState()
    val alerts by viewModel.alerts.collectAsState()
    val activeAlertCount = alerts.count { it.isActive && !it.isTriggered }

    // Listen for UI messages & Trade execution fills
    LaunchedEffect(Unit) {
        viewModel.uiMessages.collectLatest { msg ->
            when (msg) {
                is UiMessage.Success -> {
                    snackbarHostState.showSnackbar(msg.message)
                }
                is UiMessage.Error -> {
                    snackbarHostState.showSnackbar(msg.message)
                }
                is UiMessage.TradeFilled -> {
                    val fill = msg.fill
                    snackbarHostState.showSnackbar(
                        "TRADE FILLED: ${fill.side.name} ${fill.quantity} ${fill.symbol} @ $${"%.2f".format(fill.fillPrice)}"
                    )
                }
            }
        }
    }

    // Listen for Live Triggered Alerts
    LaunchedEffect(Unit) {
        viewModel.alertEngine.alertEvents.collectLatest { alertNotif ->
            snackbarHostState.showSnackbar(
                "ALERT TRIGGERED [${alertNotif.symbol}]: ${alertNotif.conditionText}"
            )
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("braxtradings_root"),
        containerColor = CanvasDark,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = M3Primary,
                        contentColor = Color.White,
                        actionColor = M3PrimaryContainer,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Avatar Badge "B"
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(M3Primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "B",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column {
                            Text(
                                text = "@BRAXTRADINGS",
                                color = M3Primary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (isStreaming) BullGreen else BearRed)
                                )
                                Text(
                                    text = if (isStreaming) "MARKET OPEN" else "MARKET PAUSED",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Net Liquidity Quick Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = M3SecondaryContainer,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                        modifier = Modifier
                            .clickable { currentScreen = ScreenNav.PORTFOLIO }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                "$%,.2f".format(summary.netLiquidity),
                                color = M3OnSecondaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Scanner Search Button
                    IconButton(
                        onClick = { currentScreen = ScreenNav.SCANNER },
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(M3PrimaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search Market",
                            tint = M3OnPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Alert Bell Icon
                    BadgedBox(
                        badge = {
                            if (activeAlertCount > 0) {
                                Badge(
                                    containerColor = BearRed,
                                    contentColor = Color.White
                                ) {
                                    Text("$activeAlertCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        IconButton(
                            onClick = { currentScreen = ScreenNav.ALERTS },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(if (currentScreen == ScreenNav.ALERTS) M3SecondaryContainer else M3PrimaryContainer)
                        ) {
                            Icon(
                                if (currentScreen == ScreenNav.ALERTS) Icons.Filled.Notifications else Icons.Outlined.Notifications,
                                contentDescription = "Alerts",
                                tint = M3OnPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CanvasDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceElevated,
                tonalElevation = 3.dp,
                modifier = Modifier
                    .border(androidx.compose.foundation.BorderStroke(1.dp, BorderDark))
                    .testTag("bottom_nav_bar")
            ) {
                ScreenNav.values().forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = M3OnSecondaryContainer,
                            selectedTextColor = TextPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = M3SecondaryContainer
                        ),
                        modifier = Modifier.testTag("nav_tab_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ScreenNav.DASHBOARD -> {
                    DashboardScreen(
                        viewModel = viewModel,
                        onNavigateToTradingView = { currentScreen = ScreenNav.TRADINGVIEW }
                    )
                }
                ScreenNav.TRADINGVIEW -> {
                    TradingViewScreen(
                        viewModel = viewModel
                    )
                }
                ScreenNav.TERMINAL -> {
                    TerminalScreen(
                        viewModel = viewModel
                    )
                }
                ScreenNav.SCANNER -> {
                    ScannerScreen(
                        viewModel = viewModel,
                        onNavigateToTerminal = { currentScreen = ScreenNav.DASHBOARD }
                    )
                }
                ScreenNav.PORTFOLIO -> {
                    PortfolioScreen(
                        viewModel = viewModel,
                        onNavigateToTerminal = { currentScreen = ScreenNav.DASHBOARD }
                    )
                }
                ScreenNav.ALERTS -> {
                    AlertsScreen(
                        viewModel = viewModel,
                        onNavigateToTerminal = { currentScreen = ScreenNav.DASHBOARD }
                    )
                }
                ScreenNav.SETTINGS -> {
                    SettingsScreen(
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
