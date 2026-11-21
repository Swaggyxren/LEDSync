package com.xiiann.ledsync.presentation.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

enum class ExpressiveBlob(val pathData: String) {
    /** Asymmetric 4-lobe tilted organic blob (Battery tile / Low Battery). */
    TILTED_PEBBLE(
        "M100.0 50.0L99.4 53.5L97.9 56.7L95.7 59.7L93.2 62.4L90.8 64.9L89.0 67.4L87.7 70.1L87.0 73.1L86.5 76.5L86.0 80.2L84.9 83.7L83.2 86.9L80.8 89.4L77.6 91.0L74.1 91.7L70.4 91.7L66.7 91.4L63.4 91.2L60.3 91.4L57.4 92.2L54.6 93.6L51.6 95.4L48.4 97.2L44.9 98.5L41.4 99.0L37.9 98.5L34.8 96.9L32.0 94.4L29.8 91.5L27.8 88.5L25.9 85.8L23.8 83.6L21.3 81.9L18.3 80.6L15.0 79.3L11.6 77.9L8.5 75.9L6.0 73.4L4.4 70.3L3.8 66.8L4.2 63.1L5.1 59.5L6.2 56.2L7.1 53.0L7.4 50.0L7.1 47.0L6.2 43.8L5.1 40.5L4.2 36.9L3.8 33.2L4.4 29.7L6.0 26.6L8.5 24.1L11.6 22.1L15.0 20.7L18.3 19.4L21.3 18.1L23.8 16.4L25.9 14.2L27.8 11.5L29.8 8.5L32.0 5.6L34.8 3.1L37.9 1.5L41.4 1.0L44.9 1.5L48.4 2.8L51.6 4.6L54.6 6.4L57.4 7.8L60.3 8.6L63.4 8.8L66.7 8.6L70.4 8.3L74.1 8.3L77.6 9.0L80.8 10.6L83.2 13.1L84.9 16.3L86.0 19.8L86.5 23.5L87.0 26.9L87.7 29.9L89.0 32.6L90.8 35.1L93.2 37.6L95.7 40.3L97.9 43.3L99.4 46.5Z"
    ),
    /** 8-lobe clover (Sysfs / Notification Access / Full Charge / RAM). */
    CLOVER(
        "M100.0 50.0L99.2 53.4L97.0 56.6L94.0 59.4L91.0 61.8L88.7 64.1L87.5 66.7L87.2 69.8L87.6 73.5L87.9 77.5L87.5 81.5L85.9 84.7L83.2 86.9L79.5 87.8L75.5 87.8L71.6 87.4L68.2 87.2L65.3 87.9L62.9 89.7L60.6 92.5L58.0 95.5L55.1 98.2L51.7 99.8L48.3 99.8L44.9 98.2L42.0 95.5L39.4 92.5L37.1 89.7L34.7 87.9L31.8 87.2L28.4 87.4L24.5 87.8L20.5 87.8L16.8 86.9L14.1 84.7L12.5 81.5L12.1 77.5L12.4 73.5L12.8 69.8L12.5 66.7L11.3 64.1L9.0 61.8L6.0 59.4L3.0 56.6L0.8 53.4L0.0 50.0L0.8 46.6L3.0 43.4L6.0 40.6L9.0 38.2L11.3 35.9L12.5 33.3L12.8 30.2L12.4 26.5L12.1 22.5L12.5 18.5L14.1 15.3L16.8 13.1L20.5 12.2L24.5 12.2L28.4 12.6L31.8 12.8L34.7 12.1L37.1 10.3L39.4 7.5L42.0 4.5L44.9 1.8L48.3 0.2L51.7 0.2L55.1 1.8L58.0 4.5L60.6 7.5L62.9 10.3L65.3 12.1L68.2 12.8L71.6 12.6L75.5 12.2L79.5 12.2L83.2 13.1L85.9 15.3L87.5 18.5L87.9 22.5L87.6 26.5L87.2 30.2L87.5 33.3L88.7 35.9L91.0 38.2L94.0 40.6L97.0 43.4L99.2 46.6Z"
    ),
    /** 12-petal scalloped burst (Audio Reactive / System App Sync). */
    SUNBURST(
        "M100.0 50.0L98.9 53.4L96.4 56.5L93.9 59.3L92.7 62.2L93.0 65.6L93.9 69.5L93.9 73.4L92.2 76.4L88.9 78.2L85.1 79.4L81.9 80.8L80.0 83.4L78.9 86.9L77.4 90.7L75.0 93.3L71.5 94.1L67.6 93.5L63.9 92.7L60.7 93.1L57.9 95.1L55.0 97.8L51.7 99.7L48.3 99.7L45.0 97.8L42.1 95.1L39.3 93.1L36.1 92.7L32.4 93.5L28.5 94.1L25.0 93.3L22.6 90.7L21.1 86.9L20.0 83.4L18.1 80.8L14.9 79.4L11.1 78.2L7.8 76.4L6.1 73.4L6.1 69.5L7.0 65.6L7.3 62.2L6.1 59.3L3.6 56.5L1.1 53.4L0.0 50.0L1.1 46.6L3.6 43.5L6.1 40.7L7.3 37.8L7.0 34.4L6.1 30.5L6.1 26.6L7.8 23.6L11.1 21.8L14.9 20.6L18.1 19.2L20.0 16.6L21.1 13.1L22.6 9.3L25.0 6.7L28.5 5.9L32.4 6.5L36.1 7.3L39.3 6.9L42.1 4.9L45.0 2.2L48.3 0.3L51.7 0.3L55.0 2.2L57.9 4.9L60.7 6.9L63.9 7.3L67.6 6.5L71.5 5.9L75.0 6.7L77.4 9.3L78.9 13.1L80.0 16.6L81.9 19.2L85.1 20.6L88.9 21.8L92.2 23.6L93.9 26.6L93.9 30.5L93.0 34.4L92.7 37.8L93.9 40.7L96.4 43.5L98.9 46.6Z"
    ),
    /** Soft rotated 4-corner star (App LED Sync / CPU Load). */
    SOFT_DIAMOND(
        "M100.0 50.0L99.7 53.5L98.8 56.9L97.3 60.1L95.4 63.0L93.1 65.7L90.7 68.1L88.1 70.3L85.6 72.3L83.2 74.1L80.9 76.0L78.8 77.9L76.9 79.9L75.0 82.0L73.2 84.4L71.3 86.9L69.2 89.4L66.9 91.9L64.4 94.3L61.6 96.4L58.5 98.1L55.2 99.3L51.7 99.9L48.3 99.9L44.8 99.3L41.5 98.1L38.4 96.4L35.6 94.3L33.1 91.9L30.8 89.4L28.7 86.9L26.8 84.4L25.0 82.0L23.1 79.9L21.2 77.9L19.1 76.0L16.8 74.1L14.4 72.3L11.9 70.3L9.3 68.1L6.9 65.7L4.6 63.0L2.7 60.1L1.2 56.9L0.3 53.5L0.0 50.0L0.3 46.5L1.2 43.1L2.7 39.9L4.6 37.0L6.9 34.3L9.3 31.9L11.9 29.7L14.4 27.7L16.8 25.9L19.1 24.0L21.2 22.1L23.1 20.1L25.0 18.0L26.8 15.6L28.7 13.1L30.8 10.6L33.1 8.1L35.6 5.7L38.4 3.6L41.5 1.9L44.8 0.7L48.3 0.1L51.7 0.1L55.2 0.7L58.5 1.9L61.6 3.6L64.4 5.7L66.9 8.1L69.2 10.6L71.3 13.1L73.2 15.6L75.0 18.0L76.9 20.1L78.8 22.1L80.9 24.0L83.2 25.9L85.6 27.7L88.1 29.7L90.7 31.9L93.1 34.3L95.4 37.0L97.3 39.9L98.8 43.1L99.7 46.5Z"
    ),
    /** Organic wave blob (Battery LED Studio / Critical / QS Tile). */
    WAVE_BLOB(
        "M100.0 50.0L99.6 53.5L98.6 56.8L96.9 60.0L94.8 62.8L92.4 65.4L90.0 67.8L87.7 70.1L85.6 72.3L83.8 74.5L82.2 77.0L80.7 79.7L79.3 82.6L77.8 85.6L76.1 88.6L74.0 91.5L71.5 94.1L68.6 96.1L65.5 97.6L62.0 98.3L58.5 98.3L55.0 97.7L51.6 96.6L48.4 95.1L45.4 93.6L42.6 92.1L39.8 90.7L37.1 89.7L34.3 88.9L31.3 88.4L28.1 87.9L24.7 87.4L21.3 86.7L17.9 85.6L14.7 84.1L11.9 82.0L9.5 79.4L7.8 76.4L6.7 73.0L6.2 69.5L6.2 65.9L6.6 62.4L7.1 59.1L7.7 55.9L8.1 52.9L8.3 50.0L8.1 47.1L7.7 44.1L7.1 40.9L6.6 37.6L6.2 34.1L6.2 30.5L6.7 27.0L7.8 23.6L9.5 20.6L11.9 18.0L14.7 15.9L17.9 14.4L21.3 13.3L24.7 12.6L28.1 12.1L31.3 11.6L34.3 11.1L37.1 10.3L39.8 9.3L42.6 7.9L45.4 6.4L48.4 4.9L51.6 3.4L55.0 2.3L58.5 1.7L62.0 1.7L65.5 2.4L68.6 3.9L71.5 5.9L74.0 8.5L76.1 11.4L77.8 14.4L79.3 17.4L80.7 20.3L82.2 23.0L83.8 25.5L85.6 27.7L87.7 29.9L90.0 32.2L92.4 34.6L94.8 37.2L96.9 40.0L98.6 43.2L99.6 46.5Z"
    )
}

@Composable
fun BlobIconBadge(
    blob: ExpressiveBlob,
    containerColor: Color,
    icon: ImageVector,
    iconTint: Color,
    size: Dp = 52.dp,
    iconSize: Dp = 23.dp
) {
    val parsedPath = remember(blob) {
        PathParser().parsePathString(blob.pathData).toPath()
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = this.size.width / 100f
            val scaleY = this.size.height / 100f
            val scaled = Path().apply {
                addPath(parsedPath)
                transform(androidx.compose.ui.graphics.Matrix().apply {
                    scale(scaleX, scaleY)
                })
            }
            drawPath(path = scaled, color = containerColor)
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun BlobLetterBadge(
    blob: ExpressiveBlob,
    containerColor: Color,
    letter: String,
    letterColor: Color,
    size: Dp = 52.dp
) {
    val parsedPath = remember(blob) {
        PathParser().parsePathString(blob.pathData).toPath()
    }
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = this.size.width / 100f
            val scaleY = this.size.height / 100f
            val scaled = Path().apply {
                addPath(parsedPath)
                transform(androidx.compose.ui.graphics.Matrix().apply {
                    scale(scaleX, scaleY)
                })
            }
            drawPath(path = scaled, color = containerColor)
        }
        Text(
            text = letter,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = letterColor
        )
    }
}

/**
 * Geometric stylized LH8n phone-back LightBelt artwork matching the hero card in
 * `ledsync-figma-dark.html`.
 */
@Composable
fun LightBeltHeroArtwork(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val bgS3 = cs.surfaceContainerHighest
    val bgSc = cs.secondaryContainer
    val bgS1 = cs.surfaceContainerLow
    val bgTc = cs.tertiaryContainer
    val stripColor = if (isActive) cs.primary else cs.outline

    Canvas(modifier = modifier) {
        // Map viewBox="39 90 193 91" with slice scaling
        val vbX = 39f
        val vbY = 90f
        val vbW = 193f
        val vbH = 91f
        val scale = maxOf(size.width / vbW, size.height / vbH)
        val dx = (size.width - vbW * scale) / 2f - vbX * scale
        val dy = (size.height - vbH * scale) / 2f - vbY * scale

        fun pt(x: Float, y: Float) = Offset(x * scale + dx, y * scale + dy)

        val p1 = Path().apply {
            moveTo(pt(30f, 161f).x, pt(30f, 161f).y)
            lineTo(pt(139f, 125f).x, pt(139f, 125f).y)
            lineTo(pt(139f, 80f).x, pt(139f, 80f).y)
            lineTo(pt(30f, 80f).x, pt(30f, 80f).y)
            close()
        }
        drawPath(p1, color = bgS3)

        val p2 = Path().apply {
            moveTo(pt(139f, 125f).x, pt(139f, 125f).y)
            lineTo(pt(139f, 80f).x, pt(139f, 80f).y)
            lineTo(pt(260f, 80f).x, pt(260f, 80f).y)
            lineTo(pt(260f, 301f).x, pt(260f, 301f).y)
            lineTo(pt(139f, 157f).x, pt(139f, 157f).y)
            close()
        }
        drawPath(p2, color = bgSc)

        val p3 = Path().apply {
            moveTo(pt(30f, 161f).x, pt(30f, 161f).y)
            lineTo(pt(139f, 125f).x, pt(139f, 125f).y)
            lineTo(pt(139f, 157f).x, pt(139f, 157f).y)
            lineTo(pt(260f, 301f).x, pt(260f, 301f).y)
            lineTo(pt(260f, 330f).x, pt(260f, 330f).y)
            lineTo(pt(30f, 330f).x, pt(30f, 330f).y)
            close()
        }
        drawPath(p3, color = bgS1)

        val p4 = Path().apply {
            moveTo(pt(139f, 125f).x, pt(139f, 125f).y)
            lineTo(pt(118f, 132f).x, pt(118f, 132f).y)
            lineTo(pt(139f, 157f).x, pt(139f, 157f).y)
            close()
        }
        drawPath(p4, color = bgTc)

        val strip = Path().apply {
            moveTo(pt(139f, 37f).x, pt(139f, 37f).y)
            lineTo(pt(139f, 157f).x, pt(139f, 157f).y)
            moveTo(pt(30f, 161f).x, pt(30f, 161f).y)
            lineTo(pt(139f, 125f).x, pt(139f, 125f).y)
            moveTo(pt(118f, 132f).x, pt(118f, 132f).y)
            lineTo(pt(185f, 212f).x, pt(185f, 212f).y)
        }
        drawPath(
            path = strip,
            color = stripColor.copy(alpha = if (isActive) 0.24f else 0.12f),
            style = Stroke(width = 10.5f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawPath(
            path = strip,
            color = stripColor,
            style = Stroke(width = 4.6f * scale, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Thick M3 Expressive track slider matching `.trk` (`18dp` rounded bar + `6x40dp` pill thumb)
 * from `ledsync-figma-dark.html`.
 */
@Composable
fun ExpressiveTrackSlider(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit = {},
    activeColor: Color,
    trackColor: Color,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(1f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)
    var widthPx by remember { mutableIntStateOf(1) }
    val density = LocalDensity.current

    fun fractionToValue(f: Float): Float =
        (valueRange.start + f.coerceIn(0f, 1f) * span)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    val f = (offset.x / widthPx.toFloat()).coerceIn(0f, 1f)
                    onValueChange(fractionToValue(f))
                    onValueChangeFinished()
                }
            }
            .pointerInput(enabled, valueRange) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val f = (change.position.x / widthPx.toFloat()).coerceIn(0f, 1f)
                        onValueChange(fractionToValue(f))
                    },
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() }
                )
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(trackColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                    .height(18.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (enabled) activeColor else activeColor.copy(alpha = 0.45f))
            )
        }
        val thumbHalfPx = with(density) { 3.dp.toPx() }
        val offsetX = (fraction * widthPx - thumbHalfPx).roundToInt().coerceIn(0, (widthPx - thumbHalfPx * 2).toInt().coerceAtLeast(0))
        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX, 0) }
                .width(6.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (enabled) activeColor else activeColor.copy(alpha = 0.45f))
        )
    }
}

@Composable
fun ExpressivePillSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val trackColor = if (checked) cs.primary else cs.surfaceContainerHighest
    val borderColor = if (checked) cs.primary else cs.outline
    val thumbColor = if (checked) cs.onPrimary else cs.onSurfaceVariant
    val thumbSize = if (checked) 26.dp else 18.dp
    val alpha = if (enabled) 1f else 0.5f

    Box(
        modifier = modifier
            .size(width = 56.dp, height = 34.dp)
            .clip(CircleShape)
            .background(trackColor.copy(alpha = alpha))
            .border(2.dp, borderColor.copy(alpha = alpha), CircleShape)
            .padding(horizontal = 4.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(checked) {
                        detectTapGestures { onCheckedChange(!checked) }
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(thumbSize)
                .clip(CircleShape)
                .background(thumbColor.copy(alpha = alpha))
        )
    }
}
