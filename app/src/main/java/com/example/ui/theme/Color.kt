package com.example.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape

// Standard Material Colors
val Purple80 = Color(0xFFDCD5CE)
val PurpleGrey80 = Color(0xFFE2D1C3)
val Pink80 = Color(0xFFFDFCFB)

val Purple40 = Color(0xFF588157)
val PurpleGrey40 = Color(0xFF6B705C)
val Pink40 = Color(0xFFE2D1C3)

// MZ Generation Instagram Aesthetic Hip Palette - Frosted Glass Realignment
object MZTheme {
    val DarkSlate = Color(0xFF1E293B)  // Premium Slate
    val SoftBorder = Color(0x66FFFFFF) // Delicate translucent white border

    // Frosted Glass Core Colors
    val GlassBase = Color(0xFFF3F2EE)       // Refined base cream
    val GlassBgGradStart = Color(0xFFE2D1C3) // Warm Champagne
    val GlassBgGradEnd = Color(0xFFFDFCFB)   // Clean cream
    val GlassAuraSage = Color(0xFFC2C5AA)    // Sage accent glow
    val GlassPrimary = Color(0xFF588157)     // Forest Green
    val GlassSecondary = Color(0xFF6B705C)   // Moss Green

    // Pastels mapped to Glass Palette to retain compiling and aesthetics
    val AcidMint = Color(0xFFA3B18A)    // Elegant Sage Green
    val BubblePink = Color(0xFFE2D1C3)  // Warm Rose Champagne
    val SoftLilac = Color(0xFFC2C5AA)   // Warm grey-moss
    val NeonBlue = Color(0xFF588157)    // Classic forest accent
    val SunnyYellow = Color(0xFFE2D1C3) // Champagne accent sparkle
    val SoftCream = Color(0xFFF3F2EE)   // Glass base

    // Clean text colors
    val DarkText = Color(0xFF0F172A)    // Slate 900
    val LightText = Color(0xFFF8FAFC)   // Slate 50
    val MutedText = Color(0xFF64748B)   // Slate 500

    // Card Backgrounds - Translucent Glass effects
    val CardLight = Color(0x99FFFFFF)   // 60% opacity white
    val CardDark = Color(0xB20F172A)    // 70% opacity dark slate
}

// Glassmorphism Modifier Extensions
fun Modifier.glassBackground(isDark: Boolean = false): Modifier = this.drawBehind {
    if (isDark) {
        val gradient = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B))
        )
        drawRect(brush = gradient)
    } else {
        // Base light sand color
        drawRect(color = Color(0xFFF3F2EE))
        
        // Linear gradient (E2D1C3 -> FDFCFB)
        val gradient = Brush.linearGradient(
            colors = listOf(Color(0xFFE2D1C3), Color(0xFFFDFCFB)),
            start = androidx.compose.ui.geometry.Offset(0f, size.height),
            end = androidx.compose.ui.geometry.Offset(size.width, 0f)
        )
        drawRect(brush = gradient)

        // Glowing fuzzy aura circle at top right (C2C5AA)
        if (size.width > 0f) {
            val radiusVal = (size.width * 0.8f).coerceAtLeast(1f)
            val aura = Brush.radialGradient(
                colors = listOf(Color(0xFFC2C5AA).copy(alpha = 0.5f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.05f),
                radius = radiusVal
            )
            drawCircle(
                brush = aura,
                center = androidx.compose.ui.geometry.Offset(size.width * 0.9f, size.height * 0.05f),
                radius = radiusVal
            )
        }
    }
}

fun Modifier.glassCard(isDark: Boolean = false, cornerRadius: Int = 24): Modifier {
    val roundedShape = RoundedCornerShape(cornerRadius.dp)
    return this
        .background(
            color = if (isDark) Color(0x99111827) else Color(0xB3FFFFFF),
            shape = roundedShape
        )
        .border(
            width = 1.dp,
            color = if (isDark) Color(0x1FEEEEEE) else Color(0xE6FFFFFF),
            shape = roundedShape
        )
}


