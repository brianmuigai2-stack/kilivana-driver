package com.example.kilivana_driver.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

/**
 * One full set of app colors. Both the light and dark palettes below are
 * instances of this same class, so they share a type — needed so the
 * `if (dark) darkPalette else lightPalette` in applyKilivanaPalette()
 * resolves to Palette rather than falling back to Any.
 */
private class Palette(
    val green: Color,
    val greenDark: Color,
    val surface: Color,
    val greenTint: Color,
    val textMuted: Color,
    val border: Color,
    val error: Color,
    val errorTint: Color,
    val background: Color,
    val greenCard: Color,
    val textPrimary: Color,
    val blue: Color,
    val blueTint: Color,
    val notificationRed: Color,
    val amber: Color,
    val amberTint: Color
)

private val lightPalette = Palette(
    green = Color(0xFF2E7D32),
    greenDark = Color(0xFF14361D),
    surface = Color(0xFFFFFFFF),
    greenTint = Color(0xFFEAF4EC),
    textMuted = Color(0xFF5F6B63),
    border = Color(0xFFD0D5D2),
    error = Color(0xFFC62828),
    errorTint = Color(0xFFFDECEA),
    background = Color(0xFFF5F7F5),
    greenCard = Color(0xFF1F6B2E),
    textPrimary = Color(0xFF1C1F1D),
    blue = Color(0xFF3B82F6),
    blueTint = Color(0xFFE8F1FD),
    notificationRed = Color(0xFFE53935),
    amber = Color(0xFFE08A00),
    amberTint = Color(0xFFFFF3DC)
)

private val darkPalette = Palette(
    green = Color(0xFF4CAF50),
    greenDark = Color(0xFF0D2413),
    surface = Color(0xFF1B211C),
    greenTint = Color(0xFF203324),
    textMuted = Color(0xFF9BA89D),
    border = Color(0xFF33403B),
    error = Color(0xFFEF5350),
    errorTint = Color(0xFF3B2020),
    background = Color(0xFF10140F),
    greenCard = Color(0xFF1F6B2E),
    textPrimary = Color(0xFFECF1ED),
    blue = Color(0xFF64A5FA),
    blueTint = Color(0xFF1C2C42),
    notificationRed = Color(0xFFEF5350),
    amber = Color(0xFFF2A93C),
    amberTint = Color(0xFF3A2E14)
)

// Live colors every screen reads (backed by Compose state). When
// applyKilivanaPalette() below changes these, every screen that reads
// them recomposes automatically — no per-screen edits needed.

var KilivanaGreen by mutableStateOf(lightPalette.green)
var KilivanaGreenDark by mutableStateOf(lightPalette.greenDark)
var KilivanaWhite by mutableStateOf(lightPalette.surface)
var KilivanaGreenTint by mutableStateOf(lightPalette.greenTint)
var KilivanaTextMuted by mutableStateOf(lightPalette.textMuted)
var KilivanaBorder by mutableStateOf(lightPalette.border)
var KilivanaError by mutableStateOf(lightPalette.error)
var KilivanaErrorTint by mutableStateOf(lightPalette.errorTint)
var KilivanaBackground by mutableStateOf(lightPalette.background)
var KilivanaGreenCard by mutableStateOf(lightPalette.greenCard)
var KilivanaTextPrimary by mutableStateOf(lightPalette.textPrimary)
var KilivanaBlue by mutableStateOf(lightPalette.blue)
var KilivanaBlueTint by mutableStateOf(lightPalette.blueTint)
var KilivanaNotificationRed by mutableStateOf(lightPalette.notificationRed)
var KilivanaAmber by mutableStateOf(lightPalette.amber)
var KilivanaAmberTint by mutableStateOf(lightPalette.amberTint)

internal fun applyKilivanaPalette(dark: Boolean) {
    val p = if (dark) darkPalette else lightPalette
    KilivanaGreen = p.green
    KilivanaGreenDark = p.greenDark
    KilivanaWhite = p.surface
    KilivanaGreenTint = p.greenTint
    KilivanaTextMuted = p.textMuted
    KilivanaBorder = p.border
    KilivanaError = p.error
    KilivanaErrorTint = p.errorTint
    KilivanaBackground = p.background
    KilivanaGreenCard = p.greenCard
    KilivanaTextPrimary = p.textPrimary
    KilivanaBlue = p.blue
    KilivanaBlueTint = p.blueTint
    KilivanaNotificationRed = p.notificationRed
    KilivanaAmber = p.amber
    KilivanaAmberTint = p.amberTint
}
