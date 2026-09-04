#!/bin/bash
sed -i 's/fontWeight = FontWeight.Bold, fontSize = 16.sp/style = MaterialTheme.typography.labelLarge/g' app/src/main/java/com/example/ui/screens/WatchlistScreen.kt
sed -i 's/fontWeight = FontWeight.Bold, fontSize = 14.sp/style = MaterialTheme.typography.labelMedium/g' app/src/main/java/com/example/ui/screens/WatchlistScreen.kt
sed -i 's/fontSize = 24.sp, fontWeight = FontWeight.Bold/style = MaterialTheme.typography.headlineLarge/g' app/src/main/java/com/example/ui/screens/WatchlistScreen.kt
