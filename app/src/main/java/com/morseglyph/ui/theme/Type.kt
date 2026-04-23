package com.morseglyph.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val RobotoMono = FontFamily.Monospace

val MorseTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RobotoMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 1.sp
    )
)
