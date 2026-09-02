package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.TradingViewModel
import com.example.ui.components.AdvancedChartCanvas
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

    val currentQuote = quotes.find { it.symbol == selectedSymbol }

    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val isWide = maxWidth > 600.dp
        
        Row(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.weight(1f)) {
                // Chart Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedSymbol, color = MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(timeframe.label, color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    if (currentQuote != null) {
                        val color = if (currentQuote.change >= 0) TvGreen else TvRed
                        Column(horizontalAlignment = Alignment.End) {
                            Text(String.format(Locale.US, "%.3f", currentQuote.currentPrice), color = color, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text(
                                String.format(Locale.US, "%+.3f (%+.2f%%)", currentQuote.change, currentQuote.changePercent),
                                color = color,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Main Chart Canvas
                Box(modifier = Modifier.weight(1f)) {
                    AdvancedChartCanvas(candles = candles)
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

                // Bottom Toolbar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(vertical = 12.dp, horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Create, contentDescription = "Draw", tint = MaterialTheme.colorScheme.onBackground)
                    Icon(Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Indicators", tint = MaterialTheme.colorScheme.onBackground)
                    Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = MaterialTheme.colorScheme.onBackground)
                    Icon(Icons.Default.MoreVert, contentDescription = "More", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
            
            if (isWide) {
                VerticalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
                Box(modifier = Modifier.width(300.dp)) {
                    WatchlistScreen(viewModel = viewModel)
                }
            }
        }
    }
}
