package de.emlidhelper.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Field-instrument palette: high contrast, crisp readability outdoors
val StatusOk = Color(0xFF22C55E)
val StatusOkContainerDark = Color(0xFF0B321B)
val StatusOkContainerLight = Color(0xFFDCFCE7)

val StatusWarn = Color(0xFFF59E0B)
val StatusWarnContainerDark = Color(0xFF3A2406)
val StatusWarnContainerLight = Color(0xFFFEF3C7)

val StatusError = Color(0xFFEF4444)
val StatusErrorContainerDark = Color(0xFF3D1214)
val StatusErrorContainerLight = Color(0xFFFEE2E2)

val StatusInfo = Color(0xFF38BDF8)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    onPrimary = Color(0xFF082032),
    primaryContainer = Color(0xFF0C3654),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = Color(0xFF22C55E),
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF0F3520),
    onSecondaryContainer = Color(0xFFDCFCE7),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = Color(0xFF334155)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0C4A6E),
    secondary = Color(0xFF16A34A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDCFCE7),
    onSecondaryContainer = Color(0xFF14532D),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun EmlidHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
