package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.AccountEntity
import com.example.data.local.PositionEntity
import com.example.data.market.Quote
import com.example.domain.trading.OrderRequest
import com.example.domain.trading.OrderSide
import com.example.domain.trading.OrderType
import com.example.ui.theme.*

@Composable
fun OrderEntryForm(
    quote: Quote?,
    account: AccountEntity?,
    currentPosition: PositionEntity?,
    selectedFillPrice: Double?,
    onOrderSubmit: (OrderRequest) -> Unit,
    modifier: Modifier = Modifier
) {
    var orderType by remember { mutableStateOf(OrderType.MARKET) }
    var quantityText by remember { mutableStateOf("10") }
    var priceText by remember { mutableStateOf("") }

    // When a price is clicked in the order book ladder, fill it in
    LaunchedEffect(selectedFillPrice) {
        if (selectedFillPrice != null && selectedFillPrice > 0) {
            priceText = "%.2f".format(selectedFillPrice)
            if (orderType == OrderType.MARKET) {
                orderType = OrderType.LIMIT
            }
        }
    }

    LaunchedEffect(quote?.price) {
        if (priceText.isEmpty() && quote != null) {
            priceText = "%.2f".format(quote.price)
        }
    }

    val price = priceText.toDoubleOrNull() ?: quote?.price ?: 0.0
    val quantity = quantityText.toDoubleOrNull() ?: 0.0
    val totalOrderValue = price * quantity

    val cash = account?.cashBalance ?: 100000.0
    val leverage = account?.leverageMultiplier ?: 4.0
    val maxBuyingPower = cash * leverage
    val ownedShares = currentPosition?.shares ?: 0.0

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderDark),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("order_entry_form")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Order Type Selector Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceElevated)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                OrderType.values().forEach { type ->
                    val isSelected = orderType == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isSelected) M3PrimaryContainer else Color.Transparent)
                            .clickable { orderType = type }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.label,
                            color = if (isSelected) M3OnPrimaryContainer else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Price Input (if not pure Market order)
            if (orderType != OrderType.MARKET) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text(if (orderType == OrderType.LIMIT) "Limit Price ($)" else "Trigger Price ($)", fontSize = 12.sp) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = M3Primary,
                        unfocusedBorderColor = BorderDark,
                        focusedContainerColor = SurfaceElevated,
                        unfocusedContainerColor = SurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("order_price_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quantity Input
            OutlinedTextField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = { Text("Quantity (Shares / Units)", fontSize = 12.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = TextPrimary
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = M3Primary,
                    unfocusedBorderColor = BorderDark,
                    focusedContainerColor = SurfaceElevated,
                    unfocusedContainerColor = SurfaceElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("order_quantity_input")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Sizing Presets: 25%, 50%, 75%, 100%
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(0.25 to "25%", 0.50 to "50%", 0.75 to "75%", 1.0 to "MAX").forEach { (pct, label) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                if (price > 0) {
                                    val alloc = (maxBuyingPower * pct) / price
                                    quantityText = "%.2f".format(alloc)
                                }
                            }
                    ) {
                        Text(
                            text = label,
                            color = M3Primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(vertical = 6.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Quick +10, +50, +100 Share adders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("+10" to 10, "+50" to 50, "+100" to 100, "+500" to 500).forEach { (addStr, addVal) ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = M3SecondaryContainer,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                val currentQ = quantityText.toDoubleOrNull() ?: 0.0
                                quantityText = "${(currentQ + addVal).toInt()}"
                            }
                    ) {
                        Text(
                            text = addStr,
                            color = M3OnSecondaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .padding(vertical = 5.dp)
                                .wrapContentWidth(Alignment.CenterHorizontally)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pre-Trade Risk Summary Box
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = SurfaceElevated,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Order Value", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "$%,.2f".format(totalOrderValue),
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Avail. Buying Power (${leverage.toInt()}x)", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            "$%,.2f".format(maxBuyingPower),
                            color = M3Primary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (ownedShares > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Currently Holding", color = TextSecondary, fontSize = 11.sp)
                            Text(
                                "${"%.2f".format(ownedShares)} shares",
                                color = BullGreen,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons: Quick SELL (Red) and Quick BUY (Green) matching HTML mockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        if (quote != null && quantity > 0) {
                            onOrderSubmit(
                                OrderRequest(
                                    symbol = quote.symbol,
                                    side = OrderSide.SELL,
                                    type = orderType,
                                    quantity = quantity,
                                    limitPrice = if (orderType == OrderType.LIMIT) price else null,
                                    stopPrice = if (orderType == OrderType.STOP_LOSS || orderType == OrderType.TAKE_PROFIT) price else null
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BearRed,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("sell_order_button")
                ) {
                    Text(
                        text = "SELL",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }

                Button(
                    onClick = {
                        if (quote != null && quantity > 0) {
                            onOrderSubmit(
                                OrderRequest(
                                    symbol = quote.symbol,
                                    side = OrderSide.BUY,
                                    type = orderType,
                                    quantity = quantity,
                                    limitPrice = if (orderType == OrderType.LIMIT) price else null,
                                    stopPrice = if (orderType == OrderType.STOP_LOSS || orderType == OrderType.TAKE_PROFIT) price else null
                                )
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BullGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(50),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("buy_order_button")
                ) {
                    Text(
                        text = "BUY",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
