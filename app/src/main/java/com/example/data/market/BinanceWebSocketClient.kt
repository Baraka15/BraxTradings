package com.example.data.market

import android.util.Log
import com.example.domain.trading.Candle
import com.example.domain.trading.OrderBookLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class BinanceWebSocketClient {
    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
        
    private var webSocket: WebSocket? = null
    
    private val _currentPrice = MutableStateFlow(0.0)
    val currentPrice: StateFlow<Double> = _currentPrice.asStateFlow()
    
    private val _liveCandle = MutableStateFlow<Candle?>(null)
    val liveCandle: StateFlow<Candle?> = _liveCandle.asStateFlow()

    private val _orderBookBids = MutableStateFlow<List<OrderBookLevel>>(emptyList())
    val orderBookBids: StateFlow<List<OrderBookLevel>> = _orderBookBids.asStateFlow()

    private val _orderBookAsks = MutableStateFlow<List<OrderBookLevel>>(emptyList())
    val orderBookAsks: StateFlow<List<OrderBookLevel>> = _orderBookAsks.asStateFlow()

    fun connect(symbol: String = "btcusdt") {
        webSocket?.cancel()
        
        // Multi-stream: kline 1m and depth 20 for orderbook
        val streamUrl = "wss://stream.binance.com:9443/stream?streams=${symbol}@kline_1m/${symbol}@depth20@100ms"
        val request = Request.Builder().url(streamUrl).build()
        
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                try {
                    val json = JSONObject(text)
                    val stream = json.optString("stream")
                    val data = json.optJSONObject("data") ?: return
                    
                    if (stream.endsWith("@kline_1m")) {
                        val k = data.getJSONObject("k")
                        val isClosed = k.getBoolean("x")
                        
                        val openTime = k.getLong("t")
                        val open = k.getString("o").toDouble()
                        val high = k.getString("h").toDouble()
                        val low = k.getString("l").toDouble()
                        val close = k.getString("c").toDouble()
                        val volume = k.getString("v").toDouble()
                        
                        _currentPrice.value = close
                        _liveCandle.value = Candle(
                            timestamp = openTime,
                            open = open,
                            high = high,
                            low = low,
                            close = close,
                            volume = volume
                        )
                    } else if (stream.endsWith("@depth20@100ms")) {
                        val bids = data.optJSONArray("bids")
                        val asks = data.optJSONArray("asks")
                        
                        val newBids = mutableListOf<OrderBookLevel>()
                        if (bids != null) {
                            var total = 0.0
                            for (i in 0 until bids.length()) {
                                val level = bids.getJSONArray(i)
                                val price = level.getString(0).toDouble()
                                val qty = level.getString(1).toDouble()
                                total += qty
                                newBids.add(OrderBookLevel(price, qty, total, (total / 50.0).toFloat().coerceAtMost(1f)))
                            }
                        }
                        
                        val newAsks = mutableListOf<OrderBookLevel>()
                        if (asks != null) {
                            var total = 0.0
                            for (i in 0 until asks.length()) {
                                val level = asks.getJSONArray(i)
                                val price = level.getString(0).toDouble()
                                val qty = level.getString(1).toDouble()
                                total += qty
                                newAsks.add(OrderBookLevel(price, qty, total, (total / 50.0).toFloat().coerceAtMost(1f)))
                            }
                        }
                        
                        if (newBids.isNotEmpty()) _orderBookBids.value = newBids
                        if (newAsks.isNotEmpty()) _orderBookAsks.value = newAsks
                    }
                } catch (e: Exception) {
                    Log.e("BinanceWS", "Error parsing json", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("BinanceWS", "WebSocket Failure", t)
                // Reconnect after delay could be implemented here
            }
        })
    }

    fun disconnect() {
        webSocket?.cancel()
        webSocket = null
    }
}
