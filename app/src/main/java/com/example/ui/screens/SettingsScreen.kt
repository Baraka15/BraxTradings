package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel
import com.example.ui.theme.*

@Composable
fun SettingsScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val isStreaming by viewModel.marketEngine.isStreaming.collectAsState()
    val tickSpeedMs by viewModel.marketEngine.tickSpeedMs.collectAsState()
    val accountState by viewModel.accountState.collectAsState()

    var showResetConfirmation by remember { mutableStateOf<Double?>(null) }

    val currentLeverage = accountState?.leverageMultiplier ?: 4.0

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(horizontal = 12.dp)
            .testTag("settings_screen"),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // App Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = M3PrimaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = M3OnPrimaryContainer, modifier = Modifier.size(26.dp))
                        }
                    }
                    Column {
                        Text("@braxtradings Engine", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Day Trading Terminal & Market Alert System", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Section 1: Live Market Simulation Feed Speed
        item {
            SettingSectionHeader("MARKET FEED & TICK GENERATOR")
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Market Data Streaming", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(if (isStreaming) "Real-time tick engine is active" else "Engine paused", color = if (isStreaming) BullGreen else AlertOrange, fontSize = 12.sp)
                        }
                        Switch(
                            checked = isStreaming,
                            onCheckedChange = { viewModel.toggleStreaming() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = M3Primary,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = SurfaceElevated
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Tick Frequency (Simulation Speed)", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            200L to "200ms (Fast)",
                            600L to "600ms (Normal)",
                            1200L to "1.2s (Relaxed)"
                        ).forEach { (speed, label) ->
                            val isSel = tickSpeedMs == speed
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) M3PrimaryContainer else SurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) M3Primary else BorderDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.setSimulationSpeed(speed) }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) M3OnPrimaryContainer else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(vertical = 10.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Account Margin & Leverage Setting
        item {
            SettingSectionHeader("RISK & BUYING POWER MARGIN")
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Day Trading Buying Power Multiplier", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Standard FINRA day trading margin is 4x intra-day.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1.0 to "1x (Cash)", 2.0 to "2x (Reg T)", 4.0 to "4x (Day Trader)", 10.0 to "10x (Crypto)").forEach { (lev, label) ->
                            val isSel = currentLeverage == lev
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSel) M3PrimaryContainer else SurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) M3Primary else BorderDark),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.updateLeverage(lev) }
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) M3OnPrimaryContainer else TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier
                                        .padding(vertical = 8.dp)
                                        .wrapContentWidth(Alignment.CenterHorizontally)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Reset Account Balance
        item {
            SettingSectionHeader("PAPER TRADING ACCOUNT RESET")
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Reset Paper Trading Capital", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Clears open positions, pending orders, and resets cash equity.", color = TextSecondary, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25000.0 to "$25k (PDT Min)", 100000.0 to "$100k (Pro)", 1000000.0 to "$1M (Whale)").forEach { (bal, label) ->
                            OutlinedButton(
                                onClick = { showResetConfirmation = bal },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(label, fontSize = 10.sp, color = M3Primary)
                            }
                        }
                    }
                }
            }
        }

        // Section 4: System Architecture Specs
        item {
            SettingSectionHeader("SYSTEM ARCHITECTURE & ENGINE SPECS")
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SpecRow("Order Management System (OMS)", "Stateful Atomic In-Memory Engine")
                    SpecRow("Matching Engine", "Price-Time Priority (FIFO) Execution")
                    SpecRow("Risk Engine", "Pre-Trade Margin & Leverage Validator")
                    SpecRow("Persistence Layer", "Room Local SQLite Database")
                    SpecRow("Design System", "Material Design 3 (Android)")
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Reset Confirmation Dialog
    if (showResetConfirmation != null) {
        val targetCash = showResetConfirmation!!
        AlertDialog(
            onDismissRequest = { showResetConfirmation = null },
            title = { Text("Confirm Account Reset", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to reset your paper trading balance to $${"%,.0f".format(targetCash)}? All open positions and orders will be cleared.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetPaperTradingBalance(targetCash)
                        showResetConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BearRed, contentColor = Color.White),
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Reset Equity", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmation = null },
                    shape = RoundedCornerShape(50)
                ) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard,
            shape = RoundedCornerShape(24.dp)
        )
    }
}

@Composable
private fun SettingSectionHeader(title: String) {
    Text(
        text = title,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}
