#!/bin/bash
sed -i 's/textAlign = android.graphics.Paint.Align.LEFT/textAlign = android.graphics.Paint.Align.LEFT\n                    typeface = android.graphics.Typeface.MONOSPACE/g' app/src/main/java/com/example/ui/components/AdvancedChartCanvas.kt
