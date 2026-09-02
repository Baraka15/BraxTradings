package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.market.MarketDataEngine
import com.example.domain.trading.*
import com.example.ui.TradingViewModel
import com.example.ui.components.*

@Composable
fun DashboardScreen(
    viewModel: TradingViewModel,
    onOpenAlerts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quotes by viewModel.quotes.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val candles by viewModel.candles.collectAsState()
    val timeframe by viewModel.timeframe.collectAsState()
    val chartStyle by viewModel.chartStyle.collectAsState()
    val activeIndicators by viewModel.activeIndicators.collectAsState()
    val drawingTool by viewModel.activeDrawingTool.collectAsState()
    val drawings by viewModel.drawings.collectAsState()
    val bids by viewModel.orderBookBids.collectAsState()
    val asks by viewModel.orderBookAsks.collectAsState()
    val trades by viewModel.recentTrades.collectAsState()
    val recentTriggeredAlert by viewModel.recentTriggeredAlert.collectAsState()

    var showAlertDialog by remember { mutableStateOf(false) }
    val currentQuote = quotes.find { it.symbol == selectedSymbol } ?: quotes.firstOrNull()

    val marketEngine = remember { MarketDataEngine() }
    val consensus = remember(selectedSymbol, currentQuote?.currentPrice) {
        marketEngine.getTechnicalConfluence(selectedSymbol, currentQuote?.currentPrice ?: 100.0)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Triggered Alert Banner (if any)
        recentTriggeredAlert?.let { alert ->
            item {
                TriggeredAlertBanner(alert = alert, onDismiss = { viewModel.dismissBanner() })
            }
        }

        // Ticker Carousel
        item {
            RealTimePriceTicker(
                quotes = quotes,
                selectedSymbol = selectedSymbol,
                onSelectSymbol = { viewModel.selectSymbol(it) }
            )
        }

        // Interactive TradingView Chart
        item {
            TradingViewInteractiveChart(
                symbol = selectedSymbol,
                candles = candles,
                timeframe = timeframe,
                onTimeframeChange = { viewModel.setTimeframe(it) },
                chartStyle = chartStyle,
                onChartStyleChange = { viewModel.setChartStyle(it) },
                activeIndicators = activeIndicators,
                onToggleIndicator = { viewModel.toggleIndicator(it) },
                drawingTool = drawingTool,
                onDrawingToolChange = { viewModel.setDrawingTool(it) },
                drawings = drawings,
                onAddDrawing = { viewModel.addDrawing(it) },
                onClearDrawings = { viewModel.clearDrawings() }
            )
        }

        // Technical Consensus Indicator
        item {
            TechnicalConfluenceCard(summary = consensus)
        }

        // Order Book & Time / Sales Tape
        item {
            OrderBookAndTapeView(bids = bids, asks = asks, trades = trades)
        }
    }
}
