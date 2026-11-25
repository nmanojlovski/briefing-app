package com.nmanojlovski.briefing.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

// Headings use a serif font, like a newspaper. Body text keeps the phone's normal font.
private val Default = Typography()

val Typography = Typography(
    displaySmall = Default.displaySmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineLarge = Default.headlineLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineMedium = Default.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    headlineSmall = Default.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
    titleLarge = Default.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    titleMedium = Default.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
)
