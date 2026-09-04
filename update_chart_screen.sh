#!/bin/bash

cat << 'INNER_EOF' > app/src/main/java/com/example/ui/screens/ChartScreen.kt
package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel
import com.example.ui.components.AdvancedChartCanvas
import com.example.ui.components.OrderBookComponent
import com.example.ui.components.TrendLine
import com.example.ui.theme.*
import java.util.Locale

@Composable
fun ChartScreen(
    viewModel: TradingViewModel,
    modifier: Modifier = Modifier
) {
    val quotes by viewModel.quotes.collectAsState()
    val selectedSymbol by viewModel.selectedSymbol.collectAsState()
    val timeframe by viewModel.timeframe.collectAsState()
    val candles by viewModel.candles.collectAsState()
    
    // Core drawing & Layout state
    var isDrawingMode by remember { mutableStateOf(false) }
    val drawnLines = remember { mutableStateListOf<TrendLine>() }
    var rightSidebarTab by remember { mutableStateOf(0) } // 0: Watchlist, 1: DOM
    var layoutMode by remember { mutableStateOf(1) } // 1: Single, 2: Split 2-Pane, 4: Grid 4-Pane
    var isSidebarCollapsed by remember { mutableStateOf(false) }

    val currentQuote = quotes.find { symbol -> symbol.symbol == selectedSymbol } ?: quotes.firstOrNull()

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = maxWidth > 800.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            
            // LEFT DRAWING TOOLBAR (Desktop Only - Exact TV Layout)
            if (isWide) {
                Column(
                    modifier = Modifier
                        .width(52.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))
                    IconButton(onClick = { isDrawingMode = false }) {
                        Icon(Icons.Default.NearMe, contentDescription = "Cursor", tint = if (!isDrawingMode) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { isDrawingMode = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Trend Line", tint = if (isDrawingMode) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { drawnLines.clear() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
            }

            // MAIN CHART AREA
            Column(modifier = Modifier.weight(1f)) {
                // Top Toolbar (Symbol + Scrollable Timeframes + Layout Switcher)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedSymbol,
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = 8.dp, end = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("1m", "5m", "15m", "1h", "4h", "D", "W").forEach { tf ->
                            val isSelected = timeframe.label == tf
                            Text(
                                text = tf,
                                color = if (isSelected) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier
                                    .padding(horizontal = 12.dp)
                                    .clickable { viewModel.setTimeframe(tf) }
                            )
                        }
                    }

                    // Layout Mode Switcher Buttons (1x1, 1x2, 2x2)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { layoutMode = 1 }) {
                            Icon(Icons.Default.CropSquare, contentDescription = "Single Pane", tint = if (layoutMode == 1) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { layoutMode = 2 }) {
                            Icon(Icons.Default.ViewAgenda, contentDescription = "Split 2-Pane", tint = if (layoutMode == 2) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { layoutMode = 4 }) {
                            Icon(Icons.Default.GridView, contentDescription = "Grid 4-Pane", tint = if (layoutMode == 4) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    
                    IconButton(onClick = { isSidebarCollapsed = !isSidebarCollapsed }) {
                        Icon(
                            if (isSidebarCollapsed) Icons.Default.KeyboardDoubleArrowLeft else Icons.Default.KeyboardDoubleArrowRight,
                            contentDescription = "Toggle Sidebar",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Multi-Pane Workspace Rendering
                Box(modifier = Modifier.weight(1f)) {
                    when (layoutMode) {
                        1 -> {
                            AdvancedChartCanvas(
                                candles = candles,
                                isDrawingMode = isDrawingMode,
                                drawnLines = drawnLines,
                                onLineDrawn = { drawnLines.add(it) }
                            )
                        }
                        2 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(modifier = Modifier.weight(1f)) {
                                    AdvancedChartCanvas(
                                        candles = candles,
                                        isDrawingMode = isDrawingMode,
                                        drawnLines = drawnLines,
                                        onLineDrawn = { drawnLines.add(it) }
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                Box(modifier = Modifier.weight(1f)) {
                                    // Secondary pane with same candles or alternate
                                    AdvancedChartCanvas(
                                        candles = candles,
                                        isDrawingMode = false,
                                        drawnLines = emptyList()
                                    )
                                }
                            }
                        }
                        else -> {
                            // 4-Pane Grid
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        AdvancedChartCanvas(candles = candles, isDrawingMode = false)
                                    }
                                    VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        AdvancedChartCanvas(candles = candles, isDrawingMode = false)
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        AdvancedChartCanvas(candles = candles, isDrawingMode = false)
                                    }
                                    VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                        AdvancedChartCanvas(candles = candles, isDrawingMode = false)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Bottom Toolbar (Mobile Only)
                if (!isWide) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { isDrawingMode = !isDrawingMode }) {
                            Icon(Icons.Default.Edit, contentDescription = "Draw", tint = if (isDrawingMode) TvBlue else MaterialTheme.colorScheme.onBackground)
                        }
                        IconButton(onClick = { drawnLines.clear() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onBackground)
                        }
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = MaterialTheme.colorScheme.onBackground)
                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
            }

            // RIGHT SIDEBAR (Market Watch / DOM Order Book / Tools)
            if (isWide && !isSidebarCollapsed) {
                VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                
                Row(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                    // Market Watch or Order Book Component Content
                    Box(modifier = Modifier.weight(1f)) {
                        if (rightSidebarTab == 0) {
                            WatchlistScreen(viewModel = viewModel)
                        } else {
                            OrderBookComponent(quote = currentQuote)
                        }
                    }
                    
                    VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                    
                    // Far-Right TradingView Navigation Rail
                    Column(
                        modifier = Modifier
                            .width(52.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        IconButton(onClick = { rightSidebarTab = 0 }) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Market Watch", tint = if (rightSidebarTab == 0) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(onClick = { rightSidebarTab = 1 }) {
                            Icon(Icons.Default.Layers, contentDescription = "DOM / Order Book", tint = if (rightSidebarTab == 1) TvBlue else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.Newspaper, contentDescription = "News", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { }) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = "Hotlists", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
INNER_EOF
