package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Cinematic Deep Charcoal & Finance Blue
val BraxBackground = Color(0xFF090B0F) // Deepest charcoal/blue
val BraxPanel = Color(0xFF11151C) // Elevated surface
val BraxDivider = Color(0xFF1E232E) // Subtle borders

// Premium Accents
val BraxElectricBlue = Color(0xFF00D2FF) // Electric blue accent
val BraxMutedGold = Color(0xFFD4AF37) // For premium/pro badges

// Real-time Market Colors
val BraxGreen = Color(0xFF00E676) // Vibrant electric up-tick
val BraxRed = Color(0xFFFF1744) // Vibrant red down-tick

// Information Density Text Colors
val BraxTextPrimary = Color(0xFFF3F4F6) // High contrast clean white/gray
val BraxTextSecondary = Color(0xFF8B949E) // Muted secondary, very readable

// Light mode fallbacks (Though prompt focuses on Dark Theme first)
val BraxLightBackground = Color(0xFFF8F9FA)
val BraxLightPanel = Color(0xFFFFFFFF)
val BraxLightTextPrimary = Color(0xFF111827)
val BraxLightTextSecondary = Color(0xFF6B7280)
val BraxLightDivider = Color(0xFFE5E7EB)

// Aliasing for compatibility with existing UI code while transitioning
val TvBackground = BraxBackground
val TvPanel = BraxPanel
val TvTextPrimary = BraxTextPrimary
val TvTextSecondary = BraxTextSecondary
val TvLightBackground = BraxLightBackground
val TvLightPanel = BraxLightPanel
val TvLightTextPrimary = BraxLightTextPrimary
val TvLightTextSecondary = BraxLightTextSecondary
val TvLightDivider = BraxLightDivider
val TvLightHighlight = Color(0xFFF0F3FA)
val TvGreen = BraxGreen
val TvRed = BraxRed
val TvDivider = BraxDivider
val TvBlue = BraxElectricBlue
val TvHighlight = BraxDivider.copy(alpha = 0.5f)
