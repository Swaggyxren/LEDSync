package com.xiiann.ledsync.presentation.theme

import android.app.Activity
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val M3EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

private val AospExpressiveDarkColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color(0xFF00344A),
    primaryContainer = PrimaryContainerColor,
    onPrimaryContainer = OnPrimaryContainerColor,
    secondary = SecondaryColor,
    onSecondary = Color(0xFF1E3140),
    secondaryContainer = SecondaryContainerColor,
    onSecondaryContainer = OnSecondaryContainerColor,
    tertiary = TertiaryColor,
    onTertiary = Color(0xFF002F65),
    tertiaryContainer = TertiaryContainerColor,
    onTertiaryContainer = OnTertiaryContainerColor,
    surface = SurfaceContainerLowest,
    onSurface = Color(0xFFE1E2E6),
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainerBase,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    onSurfaceVariant = Color(0xFFC0C7CD),
    outline = Color(0xFF8A9298),
    outlineVariant = Color(0xFF40484E),
    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC)
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LEDSyncTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        dynamicDarkColorScheme(context)
    } else {
        AospExpressiveDarkColorScheme
    }

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        typography = Typography,
        content = content
    )
}

/**
 * Expressive tactile spring press modifier with system reduce-motion compliance.
 * Respects system animator duration scale accessibility settings.
 */
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

    val targetScale = if (enabled && isPressed && !isReduceMotion) 0.95f else 1.0f
    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = if (isReduceMotion) snap() else spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "expressiveBounceScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}
