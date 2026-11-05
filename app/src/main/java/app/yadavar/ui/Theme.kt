package app.yadavar.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.yadavar.R

/** تم تیره: پالت نئومورفیک سرمه‌ای با تاکید فیروزه‌ای (هماهنگ با [Neo]). */
private val DarkColors = darkColorScheme(
    primary = Neo.Teal, onPrimary = Neo.OnTeal,
    primaryContainer = Color(0xFF0F4C4A), onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = Neo.Cyan, onSecondary = Color(0xFF00212B),
    background = Neo.BgBottom, onBackground = Neo.TextPrimary,
    surface = Color(0xFF121F35), onSurface = Neo.TextPrimary,
    surfaceVariant = Color(0xFF16253D), onSurfaceVariant = Neo.TextSecondary,
    surfaceContainer = Color(0xFF16253D),
    surfaceContainerHigh = Color(0xFF182843),
    surfaceContainerHighest = Color(0xFF1B2C47),
    error = Color(0xFFFF8A80), errorContainer = Color(0xFF4A1F1F), onErrorContainer = Color(0xFFFFDAD6),
    tertiaryContainer = Color(0xFF3A2F12), onTertiaryContainer = Color(0xFFFFE9A8),
    outline = Color(0xFF3A4D6B),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF0F766E), onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1), onPrimaryContainer = Color(0xFF00201C),
    secondary = Color(0xFF4F46E5), onSecondary = Color(0xFFFFFFFF),
    background = Color(0xFFF6F8FA), onBackground = Color(0xFF0B0F14),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF0B0F14),
    surfaceVariant = Color(0xFFE8EDF2), onSurfaceVariant = Color(0xFF475467),
    error = Color(0xFFB3261E), errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    tertiaryContainer = Color(0xFFFFF1C2), onTertiaryContainer = Color(0xFF3A2F12),
    outline = Color(0xFFB8C2CC),
)

/** فونت وزیرمتن (OFL؛ third_party/vazirmatn/OFL.txt). */
private val Vazir = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
)

private val AppTypography: Typography = Typography().let { b ->
    Typography(
        displayLarge = b.displayLarge.copy(fontFamily = Vazir),
        displayMedium = b.displayMedium.copy(fontFamily = Vazir),
        displaySmall = b.displaySmall.copy(fontFamily = Vazir),
        headlineLarge = b.headlineLarge.copy(fontFamily = Vazir),
        headlineMedium = b.headlineMedium.copy(fontFamily = Vazir),
        headlineSmall = b.headlineSmall.copy(fontFamily = Vazir),
        titleLarge = b.titleLarge.copy(fontFamily = Vazir),
        titleMedium = b.titleMedium.copy(fontFamily = Vazir),
        titleSmall = b.titleSmall.copy(fontFamily = Vazir),
        bodyLarge = b.bodyLarge.copy(fontFamily = Vazir),
        bodyMedium = b.bodyMedium.copy(fontFamily = Vazir),
        bodySmall = b.bodySmall.copy(fontFamily = Vazir),
        labelLarge = b.labelLarge.copy(fontFamily = Vazir),
        labelMedium = b.labelMedium.copy(fontFamily = Vazir),
        labelSmall = b.labelSmall.copy(fontFamily = Vazir),
    )
}

@Composable
fun YadAvarTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, typography = AppTypography) {
        // ظاهر نئومورفیک فقط در تم تیره؛ در تم روشن همان ظاهر ساده Material می‌ماند
        CompositionLocalProvider(LocalNeo provides dark, content = content)
    }
}
