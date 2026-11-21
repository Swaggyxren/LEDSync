package com.xiiann.ledsync.presentation.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.xiiann.ledsync.domain.model.AppearanceMode

val M3EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

@Immutable
data class LEDSyncExtraColors(
    val accentContainer: Color,
    val onAccentContainer: Color,
    val warmContainer: Color,
    val onWarmContainer: Color,
    val warmAccent: Color
)

private val DarkExtraColors = LEDSyncExtraColors(
    accentContainer = FigmaDarkAccentContainer,
    onAccentContainer = FigmaDarkOnAccentContainer,
    warmContainer = FigmaDarkWarmContainer,
    onWarmContainer = FigmaDarkOnWarmContainer,
    warmAccent = FigmaDarkWarmAccent
)

private val LightExtraColors = LEDSyncExtraColors(
    accentContainer = FigmaLightAccentContainer,
    onAccentContainer = FigmaLightOnAccentContainer,
    warmContainer = FigmaLightWarmContainer,
    onWarmContainer = FigmaLightOnWarmContainer,
    warmAccent = FigmaLightWarmAccent
)

val LocalExtraColors = staticCompositionLocalOf { DarkExtraColors }

private val FigmaDarkColorScheme = darkColorScheme(
    primary = FigmaDarkPrimary,
    onPrimary = FigmaDarkOnPrimary,
    primaryContainer = FigmaDarkPrimaryContainer,
    onPrimaryContainer = FigmaDarkOnPrimaryContainer,
    secondary = FigmaDarkSecondary,
    onSecondary = FigmaDarkOnSecondary,
    secondaryContainer = FigmaDarkSecondaryContainer,
    onSecondaryContainer = FigmaDarkOnSecondaryContainer,
    tertiary = FigmaDarkTertiary,
    onTertiary = FigmaDarkOnTertiary,
    tertiaryContainer = FigmaDarkTertiaryContainer,
    onTertiaryContainer = FigmaDarkOnTertiaryContainer,
    background = FigmaDarkBg,
    onBackground = FigmaDarkText,
    surface = FigmaDarkBg,
    onSurface = FigmaDarkText,
    surfaceContainerLowest = FigmaDarkBg,
    surfaceContainerLow = FigmaDarkS1,
    surfaceContainer = FigmaDarkS2,
    surfaceContainerHigh = FigmaDarkS2,
    surfaceContainerHighest = FigmaDarkS3,
    onSurfaceVariant = FigmaDarkTextVariant,
    outline = FigmaDarkOutline,
    outlineVariant = FigmaDarkOutlineVariant,
    error = FigmaDarkError,
    onError = FigmaDarkOnError,
    errorContainer = FigmaDarkErrorContainer,
    onErrorContainer = FigmaDarkOnErrorContainer
)

private val FigmaLightColorScheme = lightColorScheme(
    primary = FigmaLightPrimary,
    onPrimary = FigmaLightOnPrimary,
    primaryContainer = FigmaLightPrimaryContainer,
    onPrimaryContainer = FigmaLightOnPrimaryContainer,
    secondary = FigmaLightSecondary,
    onSecondary = FigmaLightOnSecondary,
    secondaryContainer = FigmaLightSecondaryContainer,
    onSecondaryContainer = FigmaLightOnSecondaryContainer,
    tertiary = FigmaLightTertiary,
    onTertiary = FigmaLightOnTertiary,
    tertiaryContainer = FigmaLightTertiaryContainer,
    onTertiaryContainer = FigmaLightOnTertiaryContainer,
    background = FigmaLightBg,
    onBackground = FigmaLightText,
    surface = FigmaLightBg,
    onSurface = FigmaLightText,
    surfaceContainerLowest = FigmaLightBg,
    surfaceContainerLow = FigmaLightS1,
    surfaceContainer = FigmaLightS2,
    surfaceContainerHigh = FigmaLightS2,
    surfaceContainerHighest = FigmaLightS3,
    onSurfaceVariant = FigmaLightTextVariant,
    outline = FigmaLightOutline,
    outlineVariant = FigmaLightOutlineVariant,
    error = FigmaLightError,
    onError = FigmaLightOnError,
    errorContainer = FigmaLightErrorContainer,
    onErrorContainer = FigmaLightOnErrorContainer
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LEDSyncTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (appearanceMode) {
        AppearanceMode.SYSTEM -> systemDark
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }

    val view = LocalView.current
    val colorScheme = if (darkTheme) FigmaDarkColorScheme else FigmaLightColorScheme
    val extraColors = if (darkTheme) DarkExtraColors else LightExtraColors

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = !darkTheme
                    isAppearanceLightNavigationBars = !darkTheme
                }
            }
        }
    }

    CompositionLocalProvider(LocalExtraColors provides extraColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            typography = Typography,
            content = content
        )
    }
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val context = LocalContext.current
    val isReduceMotion = remember(context) {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1.0f
            ) == 0.0f
        } catch (_: Exception) {
            false
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = if (isReduceMotion) snap() else spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bounceClickScale"
    )

    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
    } else {
        Modifier
    }

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(clickModifier)
}
