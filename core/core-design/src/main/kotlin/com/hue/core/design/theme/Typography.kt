package com.hue.core.design.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.hue.core.design.R

// Playfair Display for headings, Work Sans for body
val PlayfairDisplay = FontFamily(
    Font(R.font.playfair_display_regular,   FontWeight.Normal),
    Font(R.font.playfair_display_italic,    FontWeight.Normal, FontStyle.Italic),
    Font(R.font.playfair_display_bold,      FontWeight.Bold)
)

val WorkSans = FontFamily(
    Font(R.font.work_sans_regular,  FontWeight.Normal),
    Font(R.font.work_sans_medium,   FontWeight.Medium),
    Font(R.font.work_sans_semibold, FontWeight.SemiBold)
)

val HueTypography = Typography(
    displayLarge  = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Normal, fontSize = 57.sp, lineHeight = 64.sp),
    displayMedium = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Normal, fontSize = 45.sp, lineHeight = 52.sp),
    displaySmall  = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Normal, fontSize = 36.sp, lineHeight = 44.sp),
    headlineLarge  = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Normal, fontSize = 28.sp, lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontFamily = PlayfairDisplay, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 32.sp),
    titleLarge   = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium  = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Medium,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    titleSmall   = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge    = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium   = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall    = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Normal,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge   = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Medium,   fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium  = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall   = TextStyle(fontFamily = WorkSans, fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp)
)
