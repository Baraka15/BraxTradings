package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.TradingViewModel
import com.example.ui.screens.*
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BraxTradingsTheme {
                MainAppScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    viewModel: TradingViewModel = viewModel()
) {
    var selectedScreen by remember { mutableStateOf(0) }

    Scaffold(
        containerColor = DarkBg,
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text("Brax", fontWeight = FontWeight.Black, fontSize = 20.sp, color = BlueAccent)
                        Text("Tradings", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBg,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CardBg,
                contentColor = TextPrimary
            ) {
                NavigationBarItem(
                    selected = selectedScreen == 0,
                    onClick = { selectedScreen = 0 },
                    icon = { Icon(Icons.Default.CandlestickChart, contentDescription = "Terminal") },
                    label = { Text("Terminal", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueAccent,
                        selectedTextColor = BlueAccent,
                        indicatorColor = SurfaceDark
                    )
                )
                NavigationBarItem(
                    selected = selectedScreen == 1,
                    onClick = { selectedScreen = 1 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                    label = { Text("Alerts", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueAccent,
                        selectedTextColor = BlueAccent,
                        indicatorColor = SurfaceDark
                    )
                )
                NavigationBarItem(
                    selected = selectedScreen == 2,
                    onClick = { selectedScreen = 2 },
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Portfolio") },
                    label = { Text("Portfolio", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueAccent,
                        selectedTextColor = BlueAccent,
                        indicatorColor = SurfaceDark
                    )
                )
                NavigationBarItem(
                    selected = selectedScreen == 3,
                    onClick = { selectedScreen = 3 },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Risk") },
                    label = { Text("Risk Guard", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BlueAccent,
                        selectedTextColor = BlueAccent,
                        indicatorColor = SurfaceDark
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (selectedScreen) {
                0 -> DashboardScreen(
                    viewModel = viewModel,
                    onOpenAlerts = { selectedScreen = 1 }
                )
                1 -> AlertsScreen(viewModel = viewModel)
                2 -> PortfolioScreen(viewModel = viewModel)
                3 -> RiskDashboardScreen(viewModel = viewModel)
            }
        }
    }
}
