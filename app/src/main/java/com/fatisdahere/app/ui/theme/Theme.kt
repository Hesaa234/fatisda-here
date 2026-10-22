package com.fatisdahere.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UnsBlue = Color(0xFF0072BC)
val UnsBlueLight = Color(0xFF0088CE)
val UnsTeal = Color(0xFF00A3E0)
val UnsNavy = Color(0xFF0B3C5D)
val PageBackground = Color(0xFFF4F6F9)
val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1E293B)
val StatusSuccess = Color(0xFF28A745)
val StatusWarning = Color(0xFFFFC107)
val StatusDanger = Color(0xFFDC3545)

private val Scheme = lightColorScheme(
    primary = UnsBlue,
    onPrimary = Color.White,
    secondary = UnsTeal,
    onSecondary = Color.White,
    tertiary = UnsNavy,
    background = PageBackground,
    onBackground = TextPrimary,
    surface = SurfaceWhite,
    onSurface = TextPrimary,
    error = StatusDanger
)

@Composable
fun FatisdaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
