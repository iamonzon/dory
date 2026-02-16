package com.iamonzon.dory.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Purple80,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

data class DoryColors(
    val urgencyRed: Color,
    val urgencyYellow: Color,
    val urgencyGreen: Color
)

private val LightDoryColors = DoryColors(
    urgencyRed = UrgencyRedLight,
    urgencyYellow = UrgencyYellowLight,
    urgencyGreen = UrgencyGreenLight
)

private val DarkDoryColors = DoryColors(
    urgencyRed = UrgencyRedDark,
    urgencyYellow = UrgencyYellowDark,
    urgencyGreen = UrgencyGreenDark
)

val LocalDoryColors = staticCompositionLocalOf { LightDoryColors }

val MaterialTheme.doryColors: DoryColors
    @Composable
    get() = LocalDoryColors.current

@Composable
fun DoryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val doryColors = if (darkTheme) DarkDoryColors else LightDoryColors

    androidx.compose.runtime.CompositionLocalProvider(
        LocalDoryColors provides doryColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
