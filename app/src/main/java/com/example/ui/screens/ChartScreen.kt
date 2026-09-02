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
    
    // Core drawing state
    var isDrawingMode by remember { mutableStateOf(false) }
    val drawnLines = remember { mutableStateListOf<TrendLine>() }

    val currentQuote = quotes.find { it.symbol == selectedSymbol }

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
                // Top Toolbar (Symbol + Scrollable Timeframes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        selectedSymbol,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
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
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                    
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Chart Type", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 8.dp))
                    Icon(Icons.Default.Add, contentDescription = "Compare", tint = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 8.dp))
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Main Chart Canvas with interactive Drawing Engine
                Box(modifier = Modifier.weight(1f)) {
                    AdvancedChartCanvas(
                        candles = candles,
                        isDrawingMode = isDrawingMode,
                        drawnLines = drawnLines,
                        onLineDrawn = { drawnLines.add(it) }
                    )
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

            // RIGHT SIDEBAR (Market Watch / Tools)
            if (isWide) {
                VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                
                Row(modifier = Modifier.width(360.dp).fillMaxHeight()) {
                    // Market Watch Component Content
                    Box(modifier = Modifier.weight(1f)) {
                        WatchlistScreen(viewModel = viewModel)
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
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Watchlist", tint = TvBlue, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.Notifications, contentDescription = "Alerts", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.Newspaper, contentDescription = "News", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Hotlists", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}
