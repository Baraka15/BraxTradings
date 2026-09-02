package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.trading.*
import com.example.ui.theme.*

@Composable
fun TradingViewInteractiveChart(
    symbol: String,
    candles: List<Candle>,
    timeframe: Timeframe,
    onTimeframeChange: (Timeframe) -> Unit,
    chartStyle: ChartStyle,
    onChartStyleChange: (ChartStyle) -> Unit,
    activeIndicators: Set<TechnicalIndicator>,
    onToggleIndicator: (TechnicalIndicator) -> Unit,
    drawingTool: DrawingToolType,
    onDrawingToolChange: (DrawingToolType) -> Unit,
    drawings: List<DrawingShape>,
    onAddDrawing: (DrawingShape) -> Unit,
    onClearDrawings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showIndicatorMenu by remember { mutableStateOf(false) }
    var showDrawingMenu by remember { mutableStateOf(false) }
    var showStyleMenu by remember { mutableStateOf(false) }

    val lastCandle = candles.lastOrNull()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        // Toolbar 1: Timeframes & Chart Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Timeframe Chips
            Timeframe.values().forEach { tf ->
                val isSelected = tf == timeframe
                Surface(
                    onClick = { onTimeframeChange(tf) },
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) BlueAccent else SurfaceDark,
                    modifier = Modifier.height(28.dp)
                ) {
                    Box(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tf.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Chart Style Dropdown
            Box {
                OutlinedButton(
                    onClick = { showStyleMenu = true },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                ) {
                    Icon(Icons.Default.ShowChart, contentDescription = "Style", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(chartStyle.label, fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = showStyleMenu,
                    onDismissRequest = { showStyleMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    ChartStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.label, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                onChartStyleChange(style)
                                showStyleMenu = false
                            }
                        )
                    }
                }
            }

            // Indicators Button
            Box {
                Button(
                    onClick = { showIndicatorMenu = true },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (activeIndicators.isNotEmpty()) PurpleAccent else SurfaceDark)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Indicators", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Indicators (${activeIndicators.size})", fontSize = 11.sp)
                }
                DropdownMenu(
                    expanded = showIndicatorMenu,
                    onDismissRequest = { showIndicatorMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    TechnicalIndicator.values().forEach { ind ->
                        val isChecked = activeIndicators.contains(ind)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = isChecked,
                                        onCheckedChange = null,
                                        colors = CheckboxDefaults.colors(checkedColor = BlueAccent)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${ind.label} (${ind.category})", color = TextPrimary, fontSize = 12.sp)
                                }
                            },
                            onClick = { onToggleIndicator(ind) }
                        )
                    }
                }
            }

            // Drawing Tools Button
            Box {
                Button(
                    onClick = { showDrawingMenu = true },
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.height(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (drawingTool != DrawingToolType.NONE) CyanAccent else SurfaceDark)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Draw", modifier = Modifier.size(14.dp), tint = if (drawingTool != DrawingToolType.NONE) DarkBg else TextPrimary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(drawingTool.label, fontSize = 11.sp, color = if (drawingTool != DrawingToolType.NONE) DarkBg else TextPrimary)
                }
                DropdownMenu(
                    expanded = showDrawingMenu,
                    onDismissRequest = { showDrawingMenu = false },
                    modifier = Modifier.background(SurfaceDark)
                ) {
                    DrawingToolType.values().forEach { tool ->
                        DropdownMenuItem(
                            text = { Text(tool.label, color = TextPrimary, fontSize = 12.sp) },
                            onClick = {
                                onDrawingToolChange(tool)
                                showDrawingMenu = false
                            }
                        )
                    }
                }
            }

            if (drawings.isNotEmpty()) {
                IconButton(
                    onClick = onClearDrawings,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Clear", tint = RedBearish, modifier = Modifier.size(16.dp))
                }
            }
        }

        // OHLCV Bar
        if (lastCandle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "O: ${String.format("%.2f", lastCandle.open)}  H: ${String.format("%.2f", lastCandle.high)}  L: ${String.format("%.2f", lastCandle.low)}  C: ${String.format("%.2f", lastCandle.close)}",
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = if (lastCandle.close >= lastCandle.open) GreenBullish else RedBearish
                )
            }
        }

        // Main Chart Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            ChartCanvas(
                candles = candles,
                chartStyle = chartStyle,
                activeIndicators = activeIndicators,
                drawingTool = drawingTool,
                drawings = drawings,
                onAddDrawing = onAddDrawing,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
