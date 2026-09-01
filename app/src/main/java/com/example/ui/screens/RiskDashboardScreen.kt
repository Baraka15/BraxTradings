package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.RiskAlertLevel
import com.example.ui.TradingViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiskDashboardScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val riskMetrics by viewModel.accountRiskMetrics.collectAsState()
    val positionRisks by viewModel.positionRiskAssessments.collectAsState()
    val exposureSlices by viewModel.exposureSlices.collectAsState()

    var showGuardrailsDialog by remember { mutableStateOf(false) }
    var showDeRiskDialog by remember { mutableStateOf(false) }

    val levelColor = when (riskMetrics.riskLevel) {
        RiskAlertLevel.SAFE -> BullishGreen
        RiskAlertLevel.ELEVATED -> AccentCyan
        RiskAlertLevel.HIGH_RISK -> AccentOrange
        RiskAlertLevel.CRITICAL_MARGIN_CALL -> BearishRed
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Risk & Exposure",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = levelColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                riskMetrics.riskLevel.label,
                                color = levelColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                },
                actions = {
                    if (positionRisks.isNotEmpty()) {
                        IconButton(onClick = { showDeRiskDialog = true }) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = "Emergency De-Risk",
                                tint = if (riskMetrics.marginUtilizationPct > 70) BearishRed else AccentCyan
                            )
                        }
                    }
                    IconButton(onClick = { showGuardrailsDialog = true }) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Risk Guardrails",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurfaceElevated
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary KPI Stat Grid for Day Traders
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Margin Util Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Margin Util.", color = TextMuted, fontSize = 10.sp)
                            Text(
                                "${String.format("%.1f", riskMetrics.marginUtilizationPct)}%",
                                color = levelColor,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("Cap: 90%", color = TextSecondary, fontSize = 9.sp)
                        }
                    }

                    // Gross Exposure Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Gross Exposure", color = TextMuted, fontSize = 10.sp)
                            Text(
                                "\$${String.format("%,.0f", riskMetrics.grossNotionalExposure)}",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("${String.format("%.1f", riskMetrics.effectiveLeverage)}x Lev", color = AccentCyan, fontSize = 9.sp)
                        }
                    }

                    // VaR (1-Day 95%) Card
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("1-Day 95% VaR", color = TextMuted, fontSize = 10.sp)
                            Text(
                                "\$${String.format("%,.0f", riskMetrics.valueAtRisk95)}",
                                color = AccentGold,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("Est. Volatility Risk", color = TextMuted, fontSize = 9.sp)
                        }
                    }

                    // Nearest Liquidation Distance
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated)
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Min Liq Dist", color = TextMuted, fontSize = 10.sp)
                            Text(
                                if (riskMetrics.closestLiquidationDistancePct > 0) "${String.format("%.1f", riskMetrics.closestLiquidationDistancePct)}%" else "—",
                                color = if (riskMetrics.closestLiquidationDistancePct in 0.01..10.0) BearishRed else BullishGreen,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text("Safety Buffer", color = TextMuted, fontSize = 9.sp)
                        }
                    }
                }
            }

            // Margin Utilization Gauge Meter (Recharts-style multi-zone)
            item {
                MarginUtilizationGaugeMeter(
                    metrics = riskMetrics,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Recharts-inspired Interactive Donut Exposure Chart
            item {
                RechartsDonutExposureChart(
                    slices = exposureSlices,
                    totalExposure = riskMetrics.grossNotionalExposure,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Long vs Short Exposure Bar
            item {
                RechartsExposureBarComparison(
                    longExposure = riskMetrics.longExposure,
                    shortExposure = riskMetrics.shortExposure,
                    grossExposure = riskMetrics.grossNotionalExposure,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Day Trader Daily Circuit Breaker
            item {
                DayTraderCircuitBreakerProgress(
                    dailyLossLimit = riskMetrics.dailyLossLimit,
                    currentDailyPnL = riskMetrics.currentDailyPnL,
                    lossUtilizationPct = riskMetrics.dailyLossUtilizationPct,
                    onAdjustLimit = { showGuardrailsDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Position Risk & Liquidation Monitor Table
            item {
                PositionRiskTableCard(
                    positions = positionRisks,
                    onReduceExposure = { id, pct -> viewModel.reducePositionExposure(id, pct) },
                    onClosePosition = { id -> viewModel.closePosition(id) },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Interactive Market Crash Stress Tester
            item {
                MarketStressTestCard(
                    grossExposure = riskMetrics.grossNotionalExposure,
                    usedMargin = riskMetrics.usedMargin,
                    totalEquity = riskMetrics.totalEquity,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Dialogs
    if (showGuardrailsDialog) {
        EditRiskGuardrailsDialog(
            currentDailyLossLimit = riskMetrics.dailyLossLimit,
            onSave = { newLimit ->
                viewModel.updateDailyLossLimit(newLimit)
                showGuardrailsDialog = false
            },
            onDismiss = { showGuardrailsDialog = false }
        )
    }

    if (showDeRiskDialog) {
        EmergencyDeRiskConfirmDialog(
            onConfirm = {
                viewModel.emergencyDeRiskAll()
                showDeRiskDialog = false
            },
            onDismiss = { showDeRiskDialog = false }
        )
    }
}
