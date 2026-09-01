package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.market.AssetType
import com.example.data.market.Quote
import com.example.data.market.TechnicalSignal
import com.example.ui.TradingViewModel
import com.example.ui.theme.*
import kotlin.math.max

enum class ScannerTab(val title: String) {
    GAINERS("Top Gainers"),
    LOSERS("Top Losers"),
    VOLUME("Volume Surges"),
    SIGNALS("Technical Radar"),
    WATCHLIST("My Watchlist")
}

@Composable
fun ScannerScreen(
    viewModel: TradingViewModel,
    onNavigateToTerminal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quotesMap by viewModel.marketEngine.quotes.collectAsState()
    val signals by viewModel.marketEngine.signals.collectAsState()
    val watchlist by viewModel.watchlist.collectAsState()

    var activeTab by remember { mutableStateOf(ScannerTab.GAINERS) }
    var selectedAssetFilter by remember { mutableStateOf<AssetType?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val allQuotes = quotesMap.values.toList()

    val filteredQuotes = remember(allQuotes, activeTab, selectedAssetFilter, searchQuery, watchlist) {
        var list = allQuotes

        if (selectedAssetFilter != null) {
            list = list.filter { it.assetType == selectedAssetFilter }
        }

        if (searchQuery.isNotBlank()) {
            list = list.filter {
                it.symbol.contains(searchQuery.trim(), ignoreCase = true) ||
                it.name.contains(searchQuery.trim(), ignoreCase = true)
            }
        }

        when (activeTab) {
            ScannerTab.GAINERS -> list.sortedByDescending { it.changePercent }
            ScannerTab.LOSERS -> list.sortedBy { it.changePercent }
            ScannerTab.VOLUME -> list.sortedByDescending { it.volume }
            ScannerTab.WATCHLIST -> {
                val watchSymbols = watchlist.map { it.symbol }.toSet()
                list.filter { it.symbol in watchSymbols }
            }
            ScannerTab.SIGNALS -> list
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CanvasDark)
            .padding(horizontal = 12.dp)
            .testTag("scanner_screen")
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search tickers, companies, crypto...", color = TextSecondary, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = M3Primary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = M3Primary,
                unfocusedBorderColor = BorderDark,
                focusedContainerColor = SurfaceCard,
                unfocusedContainerColor = SurfaceCard
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
                .testTag("scanner_search_input")
        )

        // Scanner Tabs Carousel
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ScannerTab.values()) { tab ->
                val isSel = activeTab == tab
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSel) M3PrimaryContainer else SurfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isSel) M3Primary else BorderDark),
                    modifier = Modifier.clickable { activeTab = tab }
                ) {
                    Text(
                        text = tab.title,
                        color = if (isSel) M3OnPrimaryContainer else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // Asset Type Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AssetChip(
                label = "All Assets",
                isSelected = selectedAssetFilter == null,
                onClick = { selectedAssetFilter = null }
            )
            AssetType.values().forEach { type ->
                AssetChip(
                    label = type.label,
                    isSelected = selectedAssetFilter == type,
                    onClick = { selectedAssetFilter = type }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scanner Content
        if (activeTab == ScannerTab.SIGNALS) {
            TechnicalSignalsList(
                signals = signals,
                onTradeSignal = { symbol ->
                    viewModel.selectSymbol(symbol)
                    onNavigateToTerminal()
                }
            )
        } else {
            if (filteredQuotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (activeTab == ScannerTab.WATCHLIST) "Your watchlist is empty." else "No securities matched your criteria.",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        if (activeTab == ScannerTab.WATCHLIST) {
                            Text(
                                text = "Star symbols in other tabs to build your live trader watchlist.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filteredQuotes, key = { it.symbol }) { quote ->
                        val isStarred = watchlist.any { it.symbol == quote.symbol }
                        ScannerQuoteCard(
                            quote = quote,
                            isStarred = isStarred,
                            onToggleStar = { viewModel.toggleWatchlist(quote.symbol, quote.name, quote.assetType) },
                            onSelect = {
                                viewModel.selectSymbol(quote.symbol)
                                onNavigateToTerminal()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssetChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) M3SecondaryContainer else SurfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) M3Primary else BorderDark),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) M3OnSecondaryContainer else TextSecondary,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun ScannerQuoteCard(
    quote: Quote,
    isStarred: Boolean,
    onToggleStar: () -> Unit,
    onSelect: () -> Unit
) {
    val isPos = quote.isPositive
    val badgeColor = if (isPos) BullGreen else BearRed

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .testTag("quote_card_${quote.symbol}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Star + Symbol info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onToggleStar,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Watchlist Star",
                        tint = if (isStarred) CryptoGold else TextSecondary
                    )
                }

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = quote.symbol,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = quote.assetType.label,
                            color = M3Primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(M3SecondaryContainer)
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    Text(
                        text = quote.name,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        modifier = Modifier.widthIn(max = 110.dp)
                    )
                }
            }

            // Center: Mini Sparkline
            Box(
                modifier = Modifier
                    .width(55.dp)
                    .height(26.dp)
            ) {
                MiniSparkline(
                    prices = quote.sparkline,
                    isPositive = isPos
                )
            }

            // Right: Price & % Change Pill + Trade button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$%,.2f".format(quote.price),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isPos) BullGreenBg else BearRedBg
                    ) {
                        Text(
                            text = "${if (isPos) "+" else ""}${"%.2f".format(quote.changePercent)}%",
                            color = badgeColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }

                Button(
                    onClick = onSelect,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = M3PrimaryContainer,
                        contentColor = M3OnPrimaryContainer
                    ),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MiniSparkline(
    prices: List<Double>,
    isPositive: Boolean
) {
    if (prices.size < 2) return

    val minP = prices.minOrNull() ?: 0.0
    val maxP = prices.maxOrNull() ?: 1.0
    val range = max(0.01, maxP - minP)
    val color = if (isPositive) BullGreen else BearRed

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val step = width / (prices.size - 1)

        val path = Path()
        for (i in prices.indices) {
            val x = i * step
            val normalized = ((maxP - prices[i]) / range).toFloat()
            val y = normalized * (height - 4f) + 2f

            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(path, color = color, style = Stroke(width = 1.6f))
    }
}

@Composable
private fun TechnicalSignalsList(
    signals: List<TechnicalSignal>,
    onTradeSignal: (String) -> Unit
) {
    if (signals.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Scanning market ticks for technical breakout patterns...", color = TextMuted, fontSize = 13.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(signals, key = { it.id }) { signal ->
                val isBull = signal.type.isBullish
                val color = if (isBull) BullGreen else BearRed

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(signal.symbol, color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isBull) BullGreenBg else BearRedBg
                                ) {
                                    Text(
                                        signal.type.title,
                                        color = color,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Text(
                                    "${signal.confidence}%",
                                    color = CryptoGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(signal.description, color = TextSecondary, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { onTradeSignal(signal.symbol) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isBull) BullGreen else BearRed,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Trade", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
