package com.fatisdahere.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val UnsBlue = Color(0xFF0A84D6)
val UnsBlueLight = Color(0xFF3AA0E3)
val UnsTeal = Color(0xFF19B5C9)
val UnsNavy = Color(0xFF12263A)
val PageBackground = Color(0xFFF3F8FC)
val SurfaceWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF12263A)
val TextMuted = Color(0xFF6B8299)
val TrackBlue = Color(0xFFE3EDF5)
val StatusSuccess = Color(0xFF1FA971)
val StatusWarning = Color(0xFFF2A900)
val StatusDanger = Color(0xFFE5484D)
val StatusGray = Color(0xFF6B8299)
val StatusNeutral = StatusGray

val GradienUtama = Brush.linearGradient(listOf(UnsBlue, UnsTeal))

val BentukKartu = RoundedCornerShape(22.dp)
val BentukItem = RoundedCornerShape(18.dp)
val BentukField = RoundedCornerShape(14.dp)
val BentukPil = RoundedCornerShape(50)

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
    surfaceVariant = TrackBlue,
    error = StatusDanger
)

private val BentukMaterial = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp)
)

private val TipografiApp = Typography(
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold)
)

@Composable
fun FatisdaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, typography = TipografiApp, shapes = BentukMaterial, content = content)
}
