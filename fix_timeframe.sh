#!/bin/bash
sed -i 's/listOf("1m", "5m", "15m", "1h", "4h", "D", "W").forEach { tf ->/Timeframe.entries.forEach { tf ->/g' app/src/main/java/com/example/ui/screens/ChartScreen.kt
sed -i 's/val isSelected = timeframe.label == tf/val isSelected = timeframe == tf/g' app/src/main/java/com/example/ui/screens/ChartScreen.kt
sed -i 's/text = tf,/text = tf.label,/g' app/src/main/java/com/example/ui/screens/ChartScreen.kt
sed -i 's/{ viewModel.setTimeframe(tf) }/{ viewModel.setTimeframe(tf) }/g' app/src/main/java/com/example/ui/screens/ChartScreen.kt
